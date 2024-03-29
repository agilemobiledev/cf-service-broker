package de.evoila.cf.broker.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An instance of a ServiceDefinition.
 * 
 * @author sgreenberg@gopivotal.com
 * @author Johannes Hiemer.
 *
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ServiceInstance implements BaseEntity<String> {

	@JsonSerialize
	@JsonProperty("service_instance_id")
	private String id;

	@JsonSerialize
	@JsonProperty("service_id")
	private String serviceDefinitionId;

	@JsonSerialize
	@JsonProperty("plan_id")
	private String planId;

	@JsonSerialize
	@JsonProperty("organization_guid")
	private String organizationGuid;

	@JsonSerialize
	@JsonProperty("space_guid")
	private String spaceGuid;

	@JsonIgnore
	private String dashboardUrl;

	@JsonSerialize
	@JsonProperty("parameters")
	private ConcurrentMap<String, String> parameters = new ConcurrentHashMap<String, String>();

	@JsonSerialize
	@JsonProperty("internal_id")
	private String internalId;

	@JsonSerialize
	@JsonProperty("port")
	private int port;

	@JsonSerialize
	@JsonProperty("host")
	private String host;

	@SuppressWarnings("unused")
	private ServiceInstance() {
	}

	public ServiceInstance(String id, String serviceDefinitionId, String planId, String organizationGuid,
			String spaceGuid, Map<String, String> parameters, String dashboardUrl) {
		initialize(id, serviceDefinitionId, planId, organizationGuid, spaceGuid, parameters);
		setDashboardUrl(dashboardUrl);
	}

	private void initialize(String id, String serviceDefinitionId, String planId, String organizationGuid,
			String spaceGuid, Map<String, String> parameters) {
		setId(id);
		setServiceDefinitionId(serviceDefinitionId);
		setPlanId(planId);
		setOrganizationGuid(organizationGuid);
		setSpaceGuid(spaceGuid);
		setParameters(parameters);
	}

	public ServiceInstance(String serviceInstanceId, String serviceDefintionId, String planId, String organizationGuid,
			String spaceGuid, Map<String, String> parameters, String dashboardUrl, String internalId) {
		initialize(id, serviceDefinitionId, planId, organizationGuid, spaceGuid, parameters);
		setInternalId(internalId);
		setDashboardUrl(dashboardUrl);
	}

	public ServiceInstance(ServiceInstance serviceInstance, String dashboardUrl, String internalId) {
		initialize(serviceInstance.id, serviceInstance.serviceDefinitionId, serviceInstance.planId,
				serviceInstance.organizationGuid, serviceInstance.spaceGuid, serviceInstance.parameters);
		setInternalId(internalId);
		setDashboardUrl(dashboardUrl);
	}

	public ServiceInstance(ServiceInstance serviceInstance, String dashboardUrl, String internalId, String host,
			int port) {
		initialize(serviceInstance.id, serviceInstance.serviceDefinitionId, serviceInstance.planId,
				serviceInstance.organizationGuid, serviceInstance.spaceGuid, serviceInstance.parameters);
		setInternalId(internalId);
		setDashboardUrl(dashboardUrl);
		setHost(host);
		setPort(port);
	}

	public ServiceInstance(String serviceInstanceId, String serviceDefinitionId, String planId, String organizationGuid,
			String spaceGuid, Map<String, String> parameters) {
		initialize(serviceInstanceId, serviceDefinitionId, planId, organizationGuid, spaceGuid, parameters);
	}

	@Override
	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	private void setServiceDefinitionId(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	public String getPlanId() {
		return planId;
	}

	private void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getOrganizationGuid() {
		return organizationGuid;
	}

	private void setOrganizationGuid(String organizationGuid) {
		this.organizationGuid = organizationGuid;
	}

	public String getSpaceGuid() {
		return spaceGuid;
	}

	private void setSpaceGuid(String spaceGuid) {
		this.spaceGuid = spaceGuid;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	private void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	private void setParameters(Map<String, String> parameters) {
		this.parameters = new ConcurrentHashMap<>(parameters);
	}

	public String getInternalId() {
		return internalId;
	}

	private void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

}
