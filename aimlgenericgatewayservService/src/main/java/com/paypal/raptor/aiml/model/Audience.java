package com.paypal.raptor.aiml.model;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @author: qingwu
 **/
@Data
@AllArgsConstructor
public class Audience {
	private AudienceType audienceType;
	private String audienceValue;

	public enum AudienceType {
		guid,
		uid,
		account_id,
		device_id,
		encoded_corp_id,
		cal_correlation_id,
		domain_name,
		context_id_ec_token,
		context_id_wps,
		context_id_prox,
		context_id_cartid
	}
}
