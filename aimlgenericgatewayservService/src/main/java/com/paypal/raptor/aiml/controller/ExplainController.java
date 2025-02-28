package com.paypal.raptor.aiml.controller;

import static com.paypal.raptor.aiml.common.constants.CalConstants.EXPLAIN;
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
import com.paypal.raptor.aiml.api.ExplainModel;
import com.paypal.raptor.aiml.model.ExplainRequest;
import com.paypal.raptor.aiml.service.ExplainService;
import com.paypal.raptor.aiml.utils.GatewayUtils;
import com.paypal.raptor.aiml.utils.RequestHandler;


/**
 * @author: qingwu
 **/
@Component
@Scope("request")
public class ExplainController implements ExplainModel {

	@Autowired
	ExplainService explainService;

	@Context
	private HttpHeaders headers;

	private final Optional<SecurityContext> securityContext;

	public ExplainController(Optional<SecurityContext> securityContext) {
		this.securityContext = securityContext;
	}

	@Override public Response explainModel(final String contentType, final ExplainRequest body) {
		String client = parseClientName(headers.getHeaderString(CAL_POOL));
		return RequestHandler.doRequest(EXPLAIN, GatewayUtils.generateCalNameForEndpoint(body), () -> explainService.explain(client, body));
	}
}
