package com.paypal.raptor.aiml.service.impl;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import com.paypal.raptor.aiml.update.UpdatableCacheComponentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.common.collect.Maps;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.FeedbackRequest;
import com.paypal.raptor.aiml.model.FeedbackResponse;
import com.paypal.raptor.aiml.model.RoutingKey;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.service.FeedbackService;
import com.paypal.raptor.aiml.utils.CommonUtils;
import com.paypal.raptor.aiml.utils.HTTPServiceManager;


/**
 * @author: qingwu
 **/
@Component
public class FeedbackServiceImpl implements FeedbackService {

	@Autowired
	UpdatableCacheComponentsService updatableComponentsService;

	@Autowired
	HTTPServiceManager httpServiceManager;


	@Override
	public FeedbackResponse feedback(final String client, final FeedbackRequest request)
					throws GatewayException {
		String routingKey = generateRoutingKeyString(new RoutingKey(request.getProject(),
						request.getEndpoint(), null));
		RoutingValue routingValue = updatableComponentsService.getRoutingValue(routingKey);
		return composeSeldonFeedbackResponse(getFeedbackRawResult(request,routingValue),request);
	}


	private Map<String, Object> getFeedbackRawResult(FeedbackRequest request, RoutingValue routingValue) {
		Map<String, String> headers = new HashMap<>();
		CommonUtils.putIfNotNull(CONTENT_TYPE, APP_JSON, headers);
		Map<String, Object> payload = composeSeldonFeedbackPayload(request);

		try{
			return httpServiceManager.httpPostQuery(getFeedbackUrl(routingValue.getUrl()),
							Maps.newHashMap(),payload,
							Map.class,
							headers,
							true,null, routingValue.getTimeout());
		} catch (IOException | URISyntaxException e) {
			throw new GatewayException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
