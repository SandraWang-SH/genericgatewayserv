package com.paypal.raptor.aiml.controller;

import static com.paypal.raptor.aiml.common.constants.CalConstants.FEEDBACK;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.CAL_POOL;
import static com.paypal.raptor.aiml.utils.GatewayUtils.parseClientName;
import java.util.Optional;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.api.Feedback;
import com.paypal.raptor.aiml.model.FeedbackRequest;
import com.paypal.raptor.aiml.service.FeedbackService;
import com.paypal.raptor.aiml.utils.GatewayUtils;
import com.paypal.raptor.aiml.utils.RequestHandler;


/**
 * @author: qingwu
 **/
@Component
@Scope("request")
public class FeedbackController implements Feedback {

	@Autowired
	FeedbackService feedbackService;

	@Context
	private HttpHeaders headers;

	private final Optional<SecurityContext> securityContext;

	public FeedbackController(Optional<SecurityContext> securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public Response feedback(final String contentType, final FeedbackRequest body) {
		String client = parseClientName(headers.getHeaderString(CAL_POOL));
		return RequestHandler.doRequest(FEEDBACK, GatewayUtils.generateCalNameForEndpoint(body), () -> feedbackService.feedback(client,
						body));
	}
}
