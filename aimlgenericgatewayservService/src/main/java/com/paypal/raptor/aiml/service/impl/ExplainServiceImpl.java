package com.paypal.raptor.aiml.service.impl;

import org.springframework.stereotype.Component;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.ExplainRequest;
import com.paypal.raptor.aiml.model.ExplainResponse;
import com.paypal.raptor.aiml.service.ExplainService;


/**
 * @author: qingwu
 **/
@Component
public class ExplainServiceImpl implements ExplainService {
	@Override public ExplainResponse explain(final String client, final ExplainRequest request) throws GatewayException {
		return null;
	}
}
