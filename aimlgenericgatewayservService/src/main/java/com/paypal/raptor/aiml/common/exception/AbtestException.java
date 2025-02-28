package com.paypal.raptor.aiml.common.exception;

import javax.ws.rs.core.Response;


/**
 * @author: qingwu
 **/
public class AbtestException extends GatewayException {
	public AbtestException() {
	}

	public AbtestException(final Response.Status status) {
		super(status);
	}

	public AbtestException(final String errorMessage, final Response.Status status) {
		super(errorMessage, status);
	}
}
