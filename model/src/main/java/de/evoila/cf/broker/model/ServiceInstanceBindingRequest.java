package de.evoila.cf.broker.model;

import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Request sent from the cloud controller to bind to a service 
 * instance.
 * 
 * @author sgreenberg@gopivotal.com
 * @author Johannes Hiemer.
 *
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ServiceInstanceBindingRequest {

	@NotEmpty
	@JsonSerialize
	@JsonProperty("service_id")
	private String serviceDefinitionId;
	
	@NotEmpty
	@JsonSerialize
	@JsonProperty("plan_id")
	private String planId;

	@NotEmpty
	@JsonSerialize
	@JsonProperty("app_guid")
	private String appGuid;
	
	@JsonSerialize
	@JsonProperty("parameters")
	private Map<String, String> parameters;
	
	public ServiceInstanceBindingRequest() {}
	
	public ServiceInstanceBindingRequest(String serviceDefinitionId, String planId, String appGuid) {
		this.serviceDefinitionId = serviceDefinitionId;
		this.planId = planId;
		this.appGuid = appGuid;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}
	
	public void setServiceDefinitionId(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	public String getPlanId() {
		return planId;
	}
	
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getAppGuid() {
		return appGuid;
	}

	public void setAppGuid(String appGuid) {
		this.appGuid = appGuid;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
}
