package com.paypal.raptor.aiml.service;

import java.util.Optional;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;


/**
 * @author: qingwu
 * @created: 2022-06-26 23:49
 **/
public interface PredictService {
	PredictResponse predictModel(String client, PredictRequest request, Optional<SecurityContext> securityContext) throws GatewayException;
}
