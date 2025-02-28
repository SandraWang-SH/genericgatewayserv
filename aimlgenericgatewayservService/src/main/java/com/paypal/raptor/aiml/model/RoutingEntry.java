package com.paypal.raptor.aiml.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * @author: qingwu
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class RoutingEntry {
	private String project;
	private String endpoint;
	private String model;
	private String url;
	private String proxy;
	private String infra;
	private String project_id;// for raptor e2e model
	private ElmoResource elmoResource;

	@JsonProperty("elmo_resource")
	private String elmoResourceStr;

	@JsonProperty("traffic")
	private Integer enableTraffic;

	private Integer timeout;

	@JsonProperty("bypass_payload")
	private Boolean bypassPayload;

	@JsonProperty("is_grpc")
	private Integer isGRpc;

	@JsonProperty("bind_cluster")
	private String bindCluster;

	@JsonProperty("elmo_experiment_type")
	private Integer elmoExperimentType;
}
