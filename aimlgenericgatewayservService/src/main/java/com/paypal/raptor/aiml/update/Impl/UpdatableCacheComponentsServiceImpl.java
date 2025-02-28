package com.paypal.raptor.aiml.update.Impl;

import com.paypal.raptor.aiml.cache.CacheUpdateService;
import com.paypal.raptor.aiml.common.exception.AuthException;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.RoutingException;
import com.paypal.raptor.aiml.model.*;
import com.paypal.raptor.aiml.update.UpdatableCacheComponentsService;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import java.util.*;

import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.SELDON_V2_ENABLE_KEY;
import static com.paypal.raptor.aiml.common.enums.InputType.RAW;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author: sheena
 **/
@Component
public class UpdatableCacheComponentsServiceImpl implements UpdatableCacheComponentsService {

	@Inject
	Configuration configuration;

	@Autowired
	CacheUpdateService cacheUpdateService;

	@Override
	public com.paypal.raptor.aiml.model.UpdatableComponents getUpdatableComponents(String key) throws RoutingException {
		return null;
	}

	@Override
	public RoutingValue getRoutingValue(final String key) throws RoutingException {
		// ternary cache
		RoutingValue routingValue = cacheUpdateService.getRoutingValueFromCache(key);
		if (null == routingValue && configuration.getBoolean(SELDON_V2_ENABLE_KEY, false)) {
			// binary cache
			Map<String, String> routingKeyMap =  parseRoutingKey(key);
			String routingKey = generateRoutingKeyString(routingKeyMap.get("project"), routingKeyMap.get("endpoint"));
			routingValue = cacheUpdateService.getRoutingValueFromCache(routingKey);
		}
		if(null == routingValue) {
			CalLogHelper.logException(ROUTING_FAILURE, key);
			throw new RoutingException("Failed to find routing target, please check if the "
					+ "model is traffic enabled and the predict request is well composed.", NOT_FOUND);
		}
		return routingValue;
	}

	@Override
	public void checkIfClientAuthorized(String client, String routingKey) throws AuthException {
		List<String> authClients = cacheUpdateService.getAuthClientsFromCache(routingKey);
		// no auth clients set for the model, skip auth check
		if (CollectionUtils.isEmpty(authClients)) {
			return;
		}
		if (!authClients.contains(client)) {
			String msg = String.format("Client %s is not authorized for endpoint.", client);
			CalLogHelper.logException(AUTH_FAILURE, msg);
			throw new AuthException(msg, FORBIDDEN);
		}
	}

	@Override
	public void checkIfValidRequest(String inputType, String jsonBody) throws GatewayException {
		if (RAW.name().equals(inputType) && !validJson(jsonBody)) {
			String msg = "Request is not well-formed, syntactically incorrect, or violates schema." +
					"Inputs Json Body is not valid. Please check request Inputs.";
			CalLogHelper.logException(BAD_REQUEST_FAILURE, msg);
			throw new GatewayException(msg, Response.Status.BAD_REQUEST);
		}
	}

}
