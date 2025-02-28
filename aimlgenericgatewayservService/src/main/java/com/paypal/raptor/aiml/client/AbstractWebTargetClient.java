package com.paypal.raptor.aiml.client;

import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.SELDON_DR_API;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import com.ebay.kernel.cal.api.CalStatus;
import com.ebay.kernel.cal.api.CalTransaction;
import com.ebay.kernel.cal.api.sync.CalEventHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.raptor.aiml.common.exception.GatewayException;


public abstract class AbstractWebTargetClient implements ServiceClient<WebTarget> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWebTargetClient.class);

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public Map<String, Object> requestWithResponse(String path, String uri, Supplier<Response> action) {
        return (Map) requestWithResponse(path, uri, action, Map.class);
    }

    /**
     * Request target url with retry mechanism. If it reaches to MaxRetryTime, PipelineException will be thrown {code
     * 503}.
     *
     * @param path          Request target path for logging.
     * @param requestAction Request action.
     * @param returnType    Return type.
     * @return Class<T> response.
     */
    public <T> T requestWithResponse(String path, String uri, Supplier<Response> requestAction, Class<T> returnType) {
        CalTransaction calTransaction = createTransaction(SELDON_DR_API, uri + path);
        T response = null;
        try {
            final StopWatch watch = new StopWatch();
            watch.start();
            final Response supplierResp = requestAction.get();
            watch.stop();
            response = handleResponse(path, supplierResp, returnType);
            calTransaction.setStatus(CalStatus.SUCCESS).addData(String.valueOf(response));
        } catch (Exception e) {
            logger.error("Error occurred while predict for path:{}, error:{}", path, e.getMessage());
            CalEventHelper.sendImmediate("HTTP_REQUEST", "Failure", "1", e.getMessage());
            calTransaction.setStatus(CalStatus.EXCEPTION);
        } finally {
            calTransaction.completed();
        }
        return response;
    }

    protected <E> E handleResponse(String path, final Response response, final Class<E> responseClass) {
        try {
            if (Response.Status.Family.SUCCESSFUL == Response.Status.Family.familyOf(response.getStatus()) && response.hasEntity()) {
                final String entity = response.readEntity(String.class);
                if (StringUtils.isBlank(entity)) {
                    throw new ProcessingException("Unable to read the entity.");
                }
                return gson.fromJson(entity, responseClass);
            }
            return processUnsuccessfulResponse(response);
        } catch (Exception e) {
            logger.error("{} server error exception , Unable to send the request to {}", getClientName(), path, e);
            throw new GatewayException("Http failed. error message:" + e.getMessage(), INTERNAL_SERVER_ERROR);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private <E> E processUnsuccessfulResponse(Response response) {
        if (Response.Status.Family.CLIENT_ERROR == Response.Status.Family.familyOf(response.getStatus())) {
            String message = formatMsgWithStatusAndEntityFrom("BAD REQUEST - Server return response : ", response);
            throw new GatewayException(message, Response.Status.BAD_REQUEST);
        }
        throw new GatewayException(
                formatMsgWithStatusAndEntityFrom("HTTP invocation resulted in server error", response),
                Response.Status.INTERNAL_SERVER_ERROR);
    }

    private static String formatMsgWithStatusAndEntityFrom(String preamble, Response response) {
        if (response.hasEntity()) {
            String result = response.readEntity(String.class);
            return String.format(
                    "%s; service responded with [%s]: [%s]",
                    preamble, response.getStatusInfo().toString(), result);
        }
        return String.format(
                "%s; service responded with code: [%s]: [%s]",
                preamble, response.getStatus(), response.getStatusInfo().toString());
    }

    public abstract WebTarget getServiceClient(String endpoint);
}
