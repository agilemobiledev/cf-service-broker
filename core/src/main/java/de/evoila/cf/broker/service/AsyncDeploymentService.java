package de.evoila.cf.broker.service;

import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.service.impl.DeploymentServiceImpl;

/**
 * 
 * @author Dennis Mueller, evoila GmbH, Sep 9, 2015
 *
 */

public interface AsyncDeploymentService {

	void asyncCreateInstance(DeploymentServiceImpl deploymentService, ServiceInstance serviceInstance, Plan plan,
			PlatformService platformService);

	void asyncDeleteInstance(DeploymentServiceImpl deploymentService, String instanceId,
			ServiceInstance serviceInstance, PlatformService platformService)
					throws ServiceInstanceDoesNotExistException;

	String getProgress(String serviceInstanceId);
}
