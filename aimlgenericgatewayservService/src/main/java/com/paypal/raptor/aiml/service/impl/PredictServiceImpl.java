package com.paypal.raptor.aiml.service.impl;

import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.ASYNC_SELDON_ENDPOINTS;
import static com.paypal.raptor.aiml.common.enums.ModelServingInfra.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import com.paypal.fpti.tracking.api.Tracking;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.routing.RoutingHelper;
import com.paypal.raptor.aiml.seldon.SeldonClient;
import com.paypal.raptor.aiml.service.PredictService;
import com.paypal.raptor.aiml.triton.TcsClient;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author: qingwu
 **/
@Component
public class PredictServiceImpl implements PredictService {

	private static final Logger logger = LoggerFactory.getLogger(PredictServiceImpl.class);

	@Autowired
	RoutingHelper routingHelper;

	@Autowired
	SeldonClient seldonClient;

	@Autowired
	TcsClient tcsClient;

	@Inject
	Configuration configuration;

	@Inject
	private Tracking tracking;

	Set<String> asyncEndpoints;

	@PostConstruct
	public void init() {
		asyncEndpoints = getConfigEndpoints(configuration.getString(ASYNC_SELDON_ENDPOINTS));
	}

	@Override
	public PredictResponse predictModel(final String client, final PredictRequest request,
			final Optional<SecurityContext> securityContext)
			throws GatewayException {
		long beginTs = System.currentTimeMillis();
		AimlGatewayFptiEvent aimlGatewayFptiEvent = AimlGatewayFptiEvent.initFptiMap(GATEWAYSERV)
				.putApiName(PREDICT_MODEL).putClientId(client);
		PredictResponse predictResponse = null;
		RoutingValue routingValue = null;
		try {
			// routing
			routingValue = routingHelper.getRoutingAndAuth(request, aimlGatewayFptiEvent, client, securityContext);
			request.setModel(routingValue.getModel());

			// predict
			if (SELDON.equals(routingValue.getInfra())) {
				if (checkEndpointsInConfig(routingValue.getEndpoint(), asyncEndpoints)) {
					predictResponse = seldonClient.predictSeldonAsync(request, routingValue, aimlGatewayFptiEvent);
				} else {
					predictResponse = seldonClient.predictSeldon(request, routingValue, aimlGatewayFptiEvent);
				}
			} else if (RAPTOR.equals(routingValue.getInfra())) {
				predictResponse = tcsClient.predictTcs(request, routingValue, aimlGatewayFptiEvent);
			}
		} catch (GatewayException e) {
			aimlGatewayFptiEvent.putErrorDesc(e.getErrorMessage());
			throw e;
		} catch (Exception e) {
			aimlGatewayFptiEvent.putErrorDesc(e.getMessage());
			throw new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR);
		} finally {
			putIfNotExistRequestToFptiEvent(aimlGatewayFptiEvent, request, routingValue);
			aimlGatewayFptiEvent.putApiDuration(Long.toString(System.currentTimeMillis() - beginTs));
			// write to fpti
			tracking.trackEvent(org.apache.commons.lang.StringUtils.EMPTY, aimlGatewayFptiEvent.toMap());
		}
		//TODO support Vertex AI
		return predictResponse;
	}
}
