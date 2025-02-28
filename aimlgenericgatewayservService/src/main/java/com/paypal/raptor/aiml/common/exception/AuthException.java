package com.paypal.raptor.aiml.common.exception;

import javax.ws.rs.core.Response;


/**
 * @author: qingwu
 **/
public class AuthException extends GatewayException {
	public AuthException() {
	}

	public AuthException(Response.Status status) {
		super(status);
	}

	public AuthException(String errorMessage, Response.Status status) {
		super(errorMessage, status);
	}
}
