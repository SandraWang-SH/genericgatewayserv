package com.paypal.raptor.aiml.common.exception;

import javax.ws.rs.core.Response;


/**
 * @author: qingwu
 **/
public class GatewayException extends RuntimeException{
	private int errorCode;

	private String errorMessage;

	public GatewayException() {
		super();
	}

	public GatewayException(Response.Status status) {
		super(status.getReasonPhrase());
		this.errorCode = status.getStatusCode();
		this.errorMessage = status.getReasonPhrase();
	}

	public GatewayException(String errorMessage, Response.Status status) {
		super(status.getReasonPhrase());
		this.errorCode = status.getStatusCode();
		this.errorMessage = errorMessage;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
