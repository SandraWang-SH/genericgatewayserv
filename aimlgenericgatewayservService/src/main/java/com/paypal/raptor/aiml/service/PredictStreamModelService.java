package com.paypal.raptor.aiml.service;

import com.ebay.kernel.cal.api.CalTransaction;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.paypal.raptor.aiml.model.PredictRequest;
import reactor.core.publisher.Flux;

import java.util.Optional;

public interface PredictStreamModelService {
	Flux<Response> predictStreamModel(String client, PredictRequest request,
								 Optional<SecurityContext> securityContext,
								 HttpHeaders headers, CalTransaction calTransaction) throws GatewayException;
}
