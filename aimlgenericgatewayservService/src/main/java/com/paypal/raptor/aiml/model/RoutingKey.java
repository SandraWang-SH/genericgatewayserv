package com.paypal.raptor.aiml.model;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @author: qingwu
 **/
@Data
@AllArgsConstructor
public class RoutingKey {
	private String project;
	private String endpoint;
	private String model;
}
