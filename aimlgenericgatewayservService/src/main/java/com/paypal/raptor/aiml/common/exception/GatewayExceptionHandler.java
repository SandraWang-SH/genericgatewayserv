package com.paypal.raptor.aiml.common.exception;

import static com.paypal.platform.auth.util.v2.AuthException.ErrorCode.NOT_AUTHORIZED;
import static com.paypal.platform.auth.util.v2.AuthException.ErrorCode.PERMISSION_DENIED;
import static com.paypal.platform.error.api.CommonError.INTERNAL_SERVICE_ERROR;
import static com.paypal.platform.error.api.CommonError.VALIDATION_ERROR;
import static javax.ws.rs.core.Response.Status.*;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.ebay.kernel.cal.api.sync.CalTransactionHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.raptor.aiml.model.*;
import com.paypal.raptor.aiml.model.Error;

/**
 * @author: qingwu
 **/
public class GatewayExceptionHandler {

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	public static Response toErrorResponse(GatewayException exception) {
		if (exception instanceof AuthException) {
			return toAuthErrorResponse(exception);
		} else if (exception instanceof RoutingException) {
			return toRoutingErrorResponse(exception);
		} else if (exception instanceof AbtestException) {
			return toAbtestErrorResponse(exception);
		} else {
			return toGatewayErrorResponse(exception);
		}
	}

	public static Response toStreamErrorResponse(GatewayException exception) {
		if (exception instanceof AuthException) {
			return toStreamAuthErrorResponse(exception);
		} else if (exception instanceof RoutingException) {
			return toStreamRoutingErrorResponse(exception);
		} else if (exception instanceof AbtestException) {
			return toStreamAbtestErrorResponse(exception);
		} else {
			return toStreamGatewayErrorResponse(exception);
		}
	}

	public static Response toAuthErrorResponse(GatewayException exception) {
    return Response.status(FORBIDDEN).entity(newAuthError(exception.getErrorMessage())).type(
				    MediaType.APPLICATION_JSON).build();
	}

	public static Response toStreamAuthErrorResponse(GatewayException exception) {
		return Response.status(FORBIDDEN).entity(gson.toJson(newAuthError(exception.getErrorMessage())))
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response toRoutingErrorResponse(GatewayException exception) {
		return Response.status(422)
				.entity(newRoutingError(exception.getErrorMessage()))
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response toStreamRoutingErrorResponse(GatewayException exception) {
		return Response.status(422)
				.entity(gson.toJson(newRoutingError(exception.getErrorMessage())))
				.type(MediaType.APPLICATION_JSON).build();
	}


	public static Response toAbtestErrorResponse(GatewayException exception) {
		return Response.status(422).entity(newAbtestError(exception.getErrorMessage())).type(
				MediaType.APPLICATION_JSON).build();
	}

	public static Response toStreamAbtestErrorResponse(GatewayException exception) {
		return Response.status(422)
				.entity(gson.toJson(newAbtestError(exception.getErrorMessage())))
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response toGatewayErrorResponse(GatewayException exception) {
		return Response.status(exception.getErrorCode()).entity(newInternalError(exception.getErrorMessage())).type(
						MediaType.APPLICATION_JSON).build();
	}

	public static Response toStreamGatewayErrorResponse(GatewayException exception) {
		Response response;
		if (exception.getErrorCode() == 400) {
			response = Response.status(exception.getErrorCode())
					.entity(gson.toJson(newBadRequstError(exception.getErrorMessage())))
					.type(MediaType.APPLICATION_JSON).build();
		} else if (exception.getErrorCode() == 200) { //special case
			response = Response.status(OK)
					.entity("").type(MediaType.APPLICATION_JSON).build();
		} else {
			response = Response.status(exception.getErrorCode())
					.entity(gson.toJson(newInternalError(exception.getErrorMessage())))
					.type(MediaType.APPLICATION_JSON).build();
		}
		return response;
	}

	public static Error newAuthError(String errorDetails){
		Error error = new Error();
		error.setName(NOT_AUTHORIZED.name());
		error.setMessage(NOT_AUTHORIZED.getErrorMessage());
		error.setDebugId(getCorrelationId());
		List<ErrorDetails> details = new ArrayList<>();
		ErrorDetails detail = new ErrorDetails();
		detail.setIssue(PERMISSION_DENIED.name());
		detail.setDescription(errorDetails);
		details.add(detail);
		error.setDetails(details);
		return error;
	}

	public static Error newRoutingError(String errorDetails){
		Error error = new Error();
		error.setName("UNPROCESSABLE_ENTITY");
		error.setMessage("The requested action could not be performed, semantically incorrect, or failed business validation.");
		error.setDebugId(getCorrelationId());
		List<ErrorDetails> details = new ArrayList<>();
		ErrorDetails detail = new ErrorDetails();
		detail.setIssue("Routing target not found.");
		detail.setDescription(errorDetails);
		details.add(detail);
		error.setDetails(details);
		return error;
	}

	public static Error newAbtestError(String errorDetails){
		Error error = new Error();
		error.setName("UNPROCESSABLE_ENTITY");
		error.setMessage("The requested action could not be performed, semantically incorrect, or failed business validation.");
		error.setDebugId(getCorrelationId());
		List<ErrorDetails> details = new ArrayList<>();
		ErrorDetails detail = new ErrorDetails();
		detail.setIssue("Please check if A/B Test params are correctly configured and audience value is present in the predict request.");
		detail.setDescription(errorDetails);
		details.add(detail);
		error.setDetails(details);
		return error;
	}

	public static Error newInternalError(String errorDetails){
		Error error = new Error();
		error.setName(INTERNAL_SERVER_ERROR.name());
		error.setMessage(INTERNAL_SERVICE_ERROR.getMessage());
		error.setDebugId(getCorrelationId());
		List<ErrorDetails> details = new ArrayList<>();
		ErrorDetails detail = new ErrorDetails();
		detail.setIssue(INTERNAL_SERVICE_ERROR.name());
		detail.setDescription(errorDetails);
		details.add(detail);
		error.setDetails(details);
		return error;
	}

	public static Error newBadRequstError(String errorDetails){
		Error error = new Error();
		error.setName(BAD_REQUEST.name());
		error.setMessage(VALIDATION_ERROR.getMessage());
		error.setDebugId(getCorrelationId());
		List<ErrorDetails> details = new ArrayList<>();
		ErrorDetails detail = new ErrorDetails();
		detail.setIssue(BAD_REQUEST.name());
		detail.setDescription(errorDetails);
		details.add(detail);
		error.setDetails(details);
		return error;
	}

	public static String getCorrelationId() {
		return (null != CalTransactionHelper.getTopTransaction())
						? CalTransactionHelper.getTopTransaction().getCorrelationId() : "NULL";
	}

	public static PredictError exception2Error(GatewayException exception) {
		if (exception instanceof AuthException) {
			return toAuthError(exception);
		} else if (exception instanceof RoutingException) {
			return toRoutingError(exception);
		} else if (exception instanceof AbtestException) {
			return toAbtestError(exception);
		} else {
			return toGatewayError(exception);
		}
	}

	public static PredictError toAuthError(GatewayException exception) {
		PredictError predictError = new PredictError();
		predictError.setMessage(NOT_AUTHORIZED.getErrorMessage());
		predictError.setErrorCode("403");
		predictError.setName(NOT_AUTHORIZED.name());
		predictError.setDebugId(getCorrelationId());

		List<PredictErrorDetails> details = new ArrayList<>();
		PredictErrorDetails detail = new PredictErrorDetails();
		detail.setIssue(PERMISSION_DENIED.name());
		detail.setDescription(exception.getErrorMessage());
		details.add(detail);
		predictError.setDetails(details);

		return predictError;
	}

	public static PredictError toRoutingError(GatewayException exception) {
		PredictError predictError = new PredictError();
		predictError.setMessage("The requested action could not be performed, semantically incorrect, or failed business validation.");
		predictError.setErrorCode("422");
		predictError.setName("UNPROCESSABLE_ENTITY");
		predictError.setDebugId(getCorrelationId());

		List<PredictErrorDetails> details = new ArrayList<>();
		PredictErrorDetails detail = new PredictErrorDetails();
		detail.setIssue("Routing target not found.");
		detail.setDescription(exception.getErrorMessage());
		details.add(detail);
		predictError.setDetails(details);

		return predictError;
	}

	public static PredictError toAbtestError(GatewayException exception) {
		PredictError predictError = new PredictError();
		predictError.setMessage("The requested action could not be performed, semantically incorrect, or failed business validation.");
		predictError.setErrorCode("422");
		predictError.setName("UNPROCESSABLE_ENTITY");
		predictError.setDebugId(getCorrelationId());

		List<PredictErrorDetails> details = new ArrayList<>();
		PredictErrorDetails detail = new PredictErrorDetails();
		detail.setIssue("Please check if A/B Test params are correctly configured and audience value is present in the predict request.");
		detail.setDescription(exception.getErrorMessage());
		details.add(detail);
		predictError.setDetails(details);

		return predictError;
	}

	public static PredictError toGatewayError(GatewayException exception) {
		PredictError predictError = new PredictError();
		predictError.setMessage(INTERNAL_SERVICE_ERROR.getMessage());
		predictError.setErrorCode("500");
		predictError.setName(INTERNAL_SERVER_ERROR.name());
		predictError.setDebugId(getCorrelationId());

		List<PredictErrorDetails> details = new ArrayList<>();
		PredictErrorDetails detail = new PredictErrorDetails();
		detail.setIssue(INTERNAL_SERVICE_ERROR.name());
		detail.setDescription(exception.getErrorMessage());
		details.add(detail);
		predictError.setDetails(details);

		return predictError;
	}

	public static PredictResult buildErrorPredictResult(Model model, GatewayException exception) {
		PredictResult result = new PredictResult();
		result.setStatus("FAILURE");
		result.setProject(model.getProject());
		result.setEndpoint(model.getEndpoint());
		result.setModel(model.getModel());
		result.setError(exception2Error(exception));
		return result;
	}
}
