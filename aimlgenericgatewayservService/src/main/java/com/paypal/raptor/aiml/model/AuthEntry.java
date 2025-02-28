package com.paypal.raptor.aiml.model;

import lombok.Data;


/**
 * @author: qingwu
 **/
@Data
public class AuthEntry {
	private String project;
	private String endpoint;
	private String model;
	private String clients;
}
