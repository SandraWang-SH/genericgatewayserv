package com.paypal.raptor.aiml.controller;

import com.ebay.kernel.cal.api.CalTransaction;
import com.paypal.infra.util.StringUtil;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.facade.PredictStreamModel;
import com.paypal.raptor.aiml.cache.CacheUpdateService;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler;
import com.paypal.raptor.aiml.limit.RateLimit;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.service.PredictStreamModelService;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import com.paypal.raptor.aiml.utils.GatewayUtils;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.ebay.kernel.cal.api.CalStatus.EXCEPTION;
import static com.ebay.kernel.configuration.ConfigurationContext.ERR_MSG;
import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.parseClientName;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * @author: sheena
 **/
@Component
@Scope("request")
public class PredictStreamModelController implements PredictStreamModel {

    private static final Logger logger = LoggerFactory.getLogger(PredictStreamModelController.class);

    @Autowired
    PredictStreamModelService predictStreamModelService;

    @Context
    private HttpHeaders headers;

    @Autowired
    CacheUpdateService cacheUpdateService;

    @Inject
    Configuration configuration;

    private final Optional<SecurityContext> securityContext;

    public PredictStreamModelController(Optional<SecurityContext> securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    @RateLimit
    public Flux<Response> predictStreamModel(String contentType, PredictRequest body) {

        String sa = headers.getHeaderString(SA_TOKEN);
        Response response;
        if(sa != null && !sa.isEmpty()) {
            String keyName = configuration.getString(KEYMAKER_KEY_NAME, DEFAULT_KEY_NAME);
            String secretValueFromCache = cacheUpdateService.getSecretValueFromCache(keyName);
            logger.info("secretValueFromCache {} keyname {}", secretValueFromCache, keyName);
            if(!StringUtil.isEmpty(secretValueFromCache) && !sa.equalsIgnoreCase(secretValueFromCache)) {
                logger.error("Failed to predict cause service account is not valid,sa in header [{}], secret value from keymaker [{}]",
                        sa, secretValueFromCache);
                response = GatewayExceptionHandler.toErrorResponse(
                        new GatewayException("predict auth failed, pls input valid service account", INTERNAL_SERVER_ERROR));
                return Flux.just(response);
            }
        }

        return doStreamRequest(body, PREDICT_STREAM_MODEL, GatewayUtils.generateCalNameForEndpoint(body));
    }

    public Flux<Response> doStreamRequest(PredictRequest body, String action, String target) {
        String client = parseClientName(headers.getHeaderString(CAL_POOL));
        Flux<Response> result = null;
        CalTransaction calTransaction = createTransaction(action, target);
        try {
            result = predictStreamModelService.predictStreamModel(client, body, securityContext, headers, calTransaction);
        } catch (GatewayException e) {
            calTransaction.setStatus(EXCEPTION);
            calTransaction.addData(ERR_MSG, e.getErrorMessage());
            CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
            logger.error("Failed to {} for model:{}, errorMsg:{}",
                    action, target, e.getErrorMessage());
            calTransaction.completed();
            return Flux.just(GatewayExceptionHandler.toStreamErrorResponse(e));
        } catch (Exception e) {
            calTransaction.setStatus(EXCEPTION);
            calTransaction.addData(ERR_MSG, CalLogHelper.getStackTrace(e));
            CalLogHelper.logException(action + EXCEPTION_POSTFIX, e);
            logger.error("Failed to {} for model:{}, errorMsg:{}",
                    action, target, e.getMessage(), e);
            calTransaction.completed();
            return Flux.just(
                    GatewayExceptionHandler.toStreamErrorResponse(
                            new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR)));
        }
        return result;
    }
}
