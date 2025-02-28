package com.paypal.raptor.aiml.controller;

import com.paypal.infra.util.StringUtil;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.cache.CacheUpdateService;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler;
import com.paypal.raptor.aiml.facade.PredictModels;
import com.paypal.raptor.aiml.limit.RateLimit;
import com.paypal.raptor.aiml.service.PredictModelsService;
import com.paypal.raptor.aiml.utils.GatewayUtils;
import com.paypal.raptor.aiml.utils.RequestHandler;
import org.apache.commons.configuration.Configuration;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.paypal.raptor.aiml.common.constants.CalConstants.PREDICT_MODELS;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.parseClientName;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Component
@Scope("request")
public class PredictModelsController implements PredictModels {

    private static final Logger logger = LoggerFactory.getLogger(PredictModelsController.class);

    @Autowired
    PredictModelsService predictModelsService;

    @Context
    private HttpHeaders headers;

    @Autowired
    CacheUpdateService cacheUpdateService;

    @Inject
    Configuration configuration;

    private final Optional<SecurityContext> securityContext;

    public PredictModelsController(Optional<SecurityContext> securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    @RateLimit
    public Response predictModelList(MultipartFormDataInput input, String contentType) {
        String client = parseClientName(headers.getHeaderString(CAL_POOL));
        String sa = headers.getHeaderString(SA_TOKEN);
        if(sa != null && !sa.isEmpty()) {
            String keyName = configuration.getString(KEYMAKER_KEY_NAME, DEFAULT_KEY_NAME);
            String secretValueFromCache = cacheUpdateService.getSecretValueFromCache(keyName);
            logger.info("secretValueFromCache {} keyname {}", secretValueFromCache, keyName);
            if(!StringUtil.isEmpty(secretValueFromCache) && !sa.equalsIgnoreCase(secretValueFromCache)) {
                logger.error("Failed to predict cause service account is not valid,sa in header [{}], secret value from keymaker [{}]", sa, secretValueFromCache);
                return GatewayExceptionHandler.toErrorResponse(new GatewayException("predict auth failed, pls input valid service account", INTERNAL_SERVER_ERROR));
            }
        }

        return RequestHandler.doRequest(PREDICT_MODELS, GatewayUtils.generateCalNameForMultiEndpoint(input), () -> predictModelsService.predictModels(client,
                input, securityContext));
    }
}
