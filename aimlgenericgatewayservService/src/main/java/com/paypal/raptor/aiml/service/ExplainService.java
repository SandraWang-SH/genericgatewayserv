package com.paypal.raptor.aiml.service;

import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.ExplainRequest;
import com.paypal.raptor.aiml.model.ExplainResponse;


/**
 * @author: qingwu
 **/
public interface ExplainService {
	ExplainResponse explain(String client, ExplainRequest request) throws GatewayException;
}
