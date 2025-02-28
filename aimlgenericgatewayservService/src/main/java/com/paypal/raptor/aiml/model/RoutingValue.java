package com.paypal.raptor.aiml.model;

import com.paypal.raptor.aiml.common.enums.ModelServingInfra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * @author: qingwu
 **/
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutingValue {
	private ModelServingInfra infra;
	private String endpoint;
	private String model;
	private String url;
	private String proxy;
	private String project_id;
	private ElmoResource elmoResource;
	private Integer timeout;
	private Boolean bypassPayload;
	private Integer isGRpc;
	private String bindCluster;
	private Integer elmoExperimentType;
}
