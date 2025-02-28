package com.paypal.raptor.aiml.service;

import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.PredictModelsResponse;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.util.Optional;

public interface PredictModelsService {
	PredictModelsResponse predictModels(String client, MultipartFormDataInput input, Optional<SecurityContext> securityContext) throws GatewayException;
}
