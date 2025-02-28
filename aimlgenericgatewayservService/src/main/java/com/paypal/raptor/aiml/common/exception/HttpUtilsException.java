package com.paypal.raptor.aiml.common.exception;

/**
 * @author: qingwu
 **/
public class HttpUtilsException extends RuntimeException {
	/**
	 * Constructor.
	 *
	 * @param message String
	 */
	public HttpUtilsException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message String
	 * @param cause Throwable
	 */
	public HttpUtilsException(String message, Throwable cause) {
		super(message, cause);
	}


}
