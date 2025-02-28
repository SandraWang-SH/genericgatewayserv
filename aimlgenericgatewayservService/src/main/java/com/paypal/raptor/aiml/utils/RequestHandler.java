package com.paypal.raptor.aiml.utils;

import static com.ebay.kernel.cal.api.CalStatus.EXCEPTION;
import static com.ebay.kernel.configuration.ConfigurationContext.ERR_MSG;
import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.EXCEPTION_POSTFIX;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.function.Supplier;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ebay.kernel.cal.api.CalTransaction;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler;
import reactor.core.publisher.Mono;


/**
 * @author: qingwu
 **/
public class RequestHandler {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

	public static <R> Response doRequest(String action, String target, Supplier<R> s) {
		return doRequest(action, target, s, logger);
	}
	/**
	 * Execute common request.
	 */
	public static <R> Response doRequest(String action, String target, Supplier<R> s, Logger logger) {
		R result = null;
		CalTransaction calTransaction = createTransaction(action, target);
		try {
			result = s.get();
		} catch (GatewayException e) {
			calTransaction.setStatus(EXCEPTION);
			calTransaction.addData(ERR_MSG, e.getErrorMessage());
			CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
			logger.error("Failed to {} for model:{}, errorMsg:{}", action, target, e.getErrorMessage());
			return GatewayExceptionHandler.toErrorResponse(e);
		} catch (Exception e) {
			calTransaction.setStatus(EXCEPTION);
			calTransaction.addData(ERR_MSG, CalLogHelper.getStackTrace(e));
			CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
			logger.error("Failed to {} for model:{}, errorMsg:{}", action, target, e.getMessage(), e);
			return GatewayExceptionHandler.toErrorResponse(new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR));
		} finally {
			calTransaction.completed();
		}
		return Response.ok().entity(result).build();
	}

	public static <R> Mono<Response> doMonoRequest(String action, String target, Supplier<Mono<R>> s) {
		return doMonoRequest(action, target, s, logger);
	}

	public static <R> Mono<Response> doMonoRequest(String action, String target, Supplier<Mono<R>> s, Logger logger) {
		Mono<R> resultMono = null;
		CalTransaction calTransaction = createTransaction(action, target);
		try {
			resultMono = s.get();
		} catch (GatewayException e) {
			calTransaction.setStatus(EXCEPTION);
			calTransaction.addData(ERR_MSG, e.getErrorMessage());
			CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
			logger.error("Failed to {} for model:{}, errorMsg:{}", action, target, e.getErrorMessage());
			return Mono.just(GatewayExceptionHandler.toErrorResponse(e));
		} catch (Exception e) {
			calTransaction.setStatus(EXCEPTION);
			calTransaction.addData(ERR_MSG, CalLogHelper.getStackTrace(e));
			CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
			logger.error("Failed to {} for model:{}, errorMsg:{}", action, target, e.getMessage(), e);
			return Mono.just(GatewayExceptionHandler.toErrorResponse(
					new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR)));
		} finally {
			calTransaction.completed();
		}
		return resultMono.map(result -> Response.ok().entity(result).build());
	}

}
