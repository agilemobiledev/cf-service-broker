package de.evoila.cf.cpi.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.glassfish.jersey.internal.ServiceConfigurationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.LogConfig.LoggingType;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.SSLConfig;

import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;
import de.evoila.cf.broker.service.PlatformService;

/**
 * 
 * @author Dennis Mueller.
 *
 */
public abstract class DockerServiceFactory implements PlatformService {

	private final static String DOCKER_SERVICE_KEY = "dockerFactoryService";

	private Logger logger = LoggerFactory.getLogger(getClass());

	Map<String, Map<String, Object>> containerCredentialMap = new HashMap<String, Map<String, Object>>();

	@Value("${docker.offset}")
	private int offset;

	@Value("${docker.imageName}")
	private String imageName;

	@Value("${docker.containerPort}")
	private int containerPort;

	@Value("${docker.passwordEnv}")
	private String passwordEnv;

	@Value("${docker.usernameEnv}")
	private String usernameEnv;

	@Value("${docker.vhostEnv}")
	private String vHostEnv;

	@Value("${docker.ssl.enabled:false}")
	private boolean dockerSSLEnabled;

	@Value("${docker.host}")
	private String dockerHost;

	@Value("${docker.port}")
	private String dockerPort;

	@Value("${docker.volume.service.port}")
	private String dockerVolumePort;

	@Value("${docker.portRange.start}")
	private int portRangeStart;

	@Value("${docker.portRange.end}")
	private int portRangeEnd;

	@Value("${docker.syslogAddress}")
	private String syslogAddress;

	@Autowired
	private DockerVolumeServiceBroker dockerVolumeServiceBroker;

	@Autowired
	private EndpointAvailabilityService endpointAvailabilityService;

	private List<Integer> availablePorts = new ArrayList<Integer>();

	private List<Integer> usedPorts = new ArrayList<Integer>();

	@PostConstruct
	public void initialize() throws PlatformException {
		createDockerClientInstance();
	}

	private void updateAvailablePorts() throws PlatformException {
		this.listUsedPort();
		this.intersect();
	}

	private void listUsedPort() throws PlatformException {
		usedPorts = new ArrayList<Integer>();
		DockerClient dockerClient = this.createDockerClientInstance();
		List<Container> containers = dockerClient.listContainersCmd()
				.withShowAll(true).exec();
		for (Container container : containers) {
			InspectContainerResponse i = dockerClient.inspectContainerCmd(
					container.getId()).exec();
			Ports portBindings = i.getHostConfig().getPortBindings();
			if (portBindings == null)
				continue;
			Set<Entry<ExposedPort, Binding[]>> bindings = portBindings
					.getBindings().entrySet();
			for (Entry<ExposedPort, Binding[]> binding : bindings) {
				Binding[] bs = binding.getValue();
				if (bs == null)
					continue;
				for (Binding b : bs) {
					usedPorts.add(b.getHostPort());
				}
			}
		}
		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at listing used ports!");
		}
	}

	private void intersect() throws PlatformException {
		availablePorts.removeAll(usedPorts);
	}

	private Integer resolveNextAvailablePort() throws PlatformException {
		initAvailablePorts();
		updateAvailablePorts();
		return availablePorts.get(0);
	}

	private void initAvailablePorts() {
		availablePorts = new ArrayList<Integer>();
		for (int i = this.portRangeStart; i <= this.portRangeEnd; i++) {
			availablePorts.add(i);
		}
	}

	private DockerClient createDockerClientInstance() throws PlatformException {
		DockerClient dockerClient = null;
		try {
			if (endpointAvailabilityService.isAvailable(DOCKER_SERVICE_KEY)) {
				String certsPath = this.getClass().getResource("/docker/")
						.getPath();
				SSLConfig sslConfig = new LocalDirectorySSLConfig(certsPath);
				DockerClientConfigBuilder dockerClientConfigBuilder = new DockerClientConfigBuilder();
				String protocol = "http";
				if (dockerSSLEnabled) {
					dockerClientConfigBuilder = dockerClientConfigBuilder
							.withSSLConfig(sslConfig);
					protocol = "https";
				}

				DockerClientConfig dockerClientConfig = dockerClientConfigBuilder
						.withUri(
								protocol + "://" + dockerHost + ":"
										+ dockerPort).build();
				dockerClient = DockerClientBuilder.getInstance(
						dockerClientConfig).build();

				endpointAvailabilityService.add(DOCKER_SERVICE_KEY,
						new EndpointServiceState(DOCKER_SERVICE_KEY,
								AvailabilityState.AVAILABLE));
			}
		} catch (Exception ex) {
			endpointAvailabilityService.add(DOCKER_SERVICE_KEY,
					new EndpointServiceState(DOCKER_SERVICE_KEY,
							AvailabilityState.ERROR, ex.toString()));
		}
		return dockerClient;
	}

	private String getEnviornment(String key, String value) {
		return key + "='" + value + "'";
	}

	private CreateContainerResponse createDockerContainer(String vhost,
			String username, String password) throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();

		Binding binding = new Binding(this.resolveNextAvailablePort());
		ExposedPort exposedPort = new ExposedPort(this.containerPort);
		PortBinding portBinding = new PortBinding(binding, exposedPort);
		LogConfig logConfig = new LogConfig();
		logConfig.setType(LoggingType.SYSLOG);
		Map<String, String> config = new HashMap<String, String>();
		config.put("syslog-address", syslogAddress);
		logConfig.setConfig(config);
		CreateContainerCmd containerCmd = dockerClient
				.createContainerCmd(imageName).withPortBindings(portBinding)
				.withRestartPolicy(RestartPolicy.alwaysRestart())
				.withLogConfig(logConfig);

		if (usernameEnv != null)
			containerCmd.withEnv(getEnviornment(usernameEnv, username));

		if (passwordEnv != null)
			containerCmd.withEnv(getEnviornment(passwordEnv, password));

		if (vHostEnv != null)
			containerCmd.withEnv(getEnviornment(vHostEnv, vhost));

		logger.trace(containerCmd.toString());

		CreateContainerResponse container = containerCmd.exec();
		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at creating docker container!");
		}
		return container;
	}

	private String getContainerVolumeHostPath(String containerId)
			throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();
		InspectContainerResponse i = dockerClient.inspectContainerCmd(
				containerId).exec();
		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at getting container's volume host path!");
		}
		return i.getMounts().get(0).getSource();
	}

	private String getContainerNodeName(String containerId)
			throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();
		InspectContainerCmd inspectContainerCmd = dockerClient
				.inspectContainerCmd(containerId);
		InspectContainerResponse i = inspectContainerCmd.exec();
		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at getting container's node name!");
		}
		return i.getNode().getName();
	}

	private Integer getContainerExposedPort(String containerId)
			throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();
		InspectContainerCmd inspectContainerCmd = dockerClient
				.inspectContainerCmd(containerId);
		InspectContainerResponse i = inspectContainerCmd.exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at getting container's exposed port!");
		}

		for (Binding[] binding : i.getHostConfig().getPortBindings()
				.getBindings().values()) {
			for (Binding b : binding) {
				return b.getHostPort();
			}
		}

		return null;
	}

	private void startContainer(CreateContainerResponse container)
			throws PlatformException {
		DockerClient dockerClient = this.createDockerClientInstance();
		dockerClient.startContainerCmd(container.getId()).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at starting docker container!");
		}
	}

	private void killContainer(String containerId) throws PlatformException {
		DockerClient dockerClient = createDockerClientInstance();
		dockerClient.killContainerCmd(containerId).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at killing docker container!");
		}
	}

	private void removeContainer(String containerId) throws PlatformException {
		DockerClient dockerClient = createDockerClientInstance();
		dockerClient.removeContainerCmd(containerId).exec();

		try {
			dockerClient.close();
		} catch (IOException e) {
			logger.warn("Cannot close docker client at killing docker container!");
		}
	}

	public CreateContainerResponse createDockerContainer(String instanceId,
			int voluneSize, String vhost, String username, String password)
			throws PlatformException {
		CreateContainerResponse container;
		try {
			container = this.createDockerContainer(vhost, username, password);

		} catch (Exception e) {
			logger.error("Cannot create docker container");
			throw e;
		}
		logger.trace("Docker container '" + container.getId()
				+ "' created with: -p " + this.containerPort + ":" + "PORT"
				+ " -e " + this.vHostEnv + "='" + vhost + "' -e "
				+ this.usernameEnv + "='" + username + "' -e "
				+ this.passwordEnv + "='" + password + this.imageName);

		String nodeName = this.getContainerNodeName(container.getId());
		String mountPoint = this.getContainerVolumeHostPath(container.getId());
		try {
			this.dockerVolumeServiceBroker.createVolume(nodeName, mountPoint,
					offset + voluneSize);
		} catch (Exception e) {
			this.removeContainer(container.getId());
			logger.error(e.getMessage());
			throw new ServiceConfigurationError(e);
		}

		startContainer(container);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("host", dockerHost);
		credentials
				.put("port", this.getContainerExposedPort(container.getId()));
		credentials.put("name", container.getId());
		credentials.put("vhost", vhost);
		credentials.put("username", username);
		credentials.put("password", password);

		containerCredentialMap.put(container.getId(), credentials);

		return container;
	}

	public void removeDockerContainer(String containerId)
			throws PlatformException {
		this.killContainer(containerId);
		String nodeName = this.getContainerNodeName(containerId);

		String mountPoint = this.getContainerVolumeHostPath(containerId);
		try {
			dockerVolumeServiceBroker.deleteVolume(nodeName, mountPoint);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.removeContainer(containerId);
	}

}
