package com.paypal.raptor.aiml.common.exception;

import javax.ws.rs.core.Response;


/**
 * @author: qingwu
 **/
public class RoutingException extends GatewayException {
	public RoutingException() {
	}

	public RoutingException(final Response.Status status) {
		super(status);
	}

	public RoutingException(final String errorMessage, final Response.Status status) {
		super(errorMessage, status);
	}
}
