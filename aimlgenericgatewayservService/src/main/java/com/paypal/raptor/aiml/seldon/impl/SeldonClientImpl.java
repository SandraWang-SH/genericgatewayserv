package com.paypal.raptor.aiml.seldon.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.paypal.raptor.aiml.common.enums.SeldonProtocol;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.dr.DisasterRecoveryHelper;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.PredictResult;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.seldon.SeldonClient;
import com.paypal.raptor.aiml.utils.*;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.paypal.raptor.aiml.common.constants.CalConstants.SELDON_PREDICT_ERROR;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.CommonUtils.putIfNotNull;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;
import static com.paypal.raptor.aiml.utils.UrlUtils.getGatewayHost;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;


@Component
public class SeldonClientImpl implements SeldonClient {

    @Autowired
    HTTPAsyncServiceManager httpAsyncServiceManager;

    @Autowired
    HTTPServiceManager httpServiceManager;

    @Autowired
    GRpcServiceManager gRpcServiceManager;

    @Autowired
    DisasterRecoveryHelper disasterRecoveryHelper;

    @Autowired
    SeldonClientWithCustomizedTimeout seldonClientWithCustomizedTimeout;

    @Inject
    Configuration configuration;

    private String aimlgatewayHost = null;

    @PostConstruct
    public void init() {
        aimlgatewayHost = getGatewayHost();
    }

    @Override
    public PredictResponse predictSeldon(final PredictRequest request, final RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException, JsonProcessingException {
        Map<String, Object> predictResult = getSeldonPredictRawResult(
                request, routingValue, aimlGatewayFptiEvent, false);
        PredictResponse predictResponse = composeSeldonPredictResponse(predictResult, request);
        putResponseToFptiEvent(aimlGatewayFptiEvent, routingValue, predictResult);
        return predictResponse;
    }

    @Override
    public PredictResponse predictSeldonAsync(final PredictRequest request, final RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException, JsonProcessingException {
        Map<String, Object> predictResult = getSeldonPredictRawResult(
                request, routingValue, aimlGatewayFptiEvent, true);
        PredictResponse predictResponse = composeSeldonPredictResponse(predictResult, request);
        putResponseToFptiEvent(aimlGatewayFptiEvent, routingValue, predictResult);
        return predictResponse;
    }

    @Override
    public PredictResult predictMultipartSeldonAsync(SinglePredictRequest singlePredictRequest,
                                RoutingValue routingValue, final AimlGatewayFptiEvent aimlGatewayFptiEvent)
            throws GatewayException, JsonProcessingException {
        Map<String, Object> predictResult = getSeldonResultWithMultipart(singlePredictRequest, routingValue,
                                    aimlGatewayFptiEvent, true);
        PredictResult predictResponse = composeSeldonPredictResponse(singlePredictRequest.getModel(), predictResult);
        putResponseToFptiEvent(aimlGatewayFptiEvent, routingValue, predictResult);
        return predictResponse;
    }

    /**
     * Compose request payload and get raw result from Seldon deployments.
     * @param request prediction request
     * @param routingValue routing value
     * @return prediction result as a map
     * @throws GatewayException if failed to call Seldon endpoint
     */
    private Map<String, Object> getSeldonPredictRawResult(final PredictRequest request, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent, boolean useAsync)
            throws GatewayException, JsonProcessingException {
        Map<String, String> headers = new HashMap<>();
        putIfNotNull(CONTENT_TYPE, APP_JSON, headers);
        Map<String, Object> payload = composeSeldonPredictPayload(request, routingValue);
        putRequestToFptiEvent(routingValue, aimlGatewayFptiEvent, payload);
        // reset url: only support when seldon V2
        if (configuration.getBoolean(SELDON_V2_ENABLE_KEY, false)) {
            String url = convertV2Url(routingValue);
            routingValue.setUrl(url);
        }

        return sendSeldonRequest(request.getInputs(), null, routingValue, payload, headers, useAsync);
    }

    public Map<String, Object> getSeldonResultWithMultipart(SinglePredictRequest singlePredictRequest,
            RoutingValue routingValue, final AimlGatewayFptiEvent aimlGatewayFptiEvent, boolean useAsync)
            throws JsonProcessingException {
        Map<String, Object> payload = composeSeldonPredictPayload(singlePredictRequest, routingValue.getUrl());
        Map<String, String> headers = new HashMap<>();
        putIfNotNull(CONTENT_TYPE, (String) payload.get(CONTENT_TYPE), headers);
        SeldonProtocol seldonProtocol = (SeldonProtocol) payload.get(SELDON_PROTOCOL_KEY);
        payload.remove(CONTENT_TYPE);
        payload.remove(SELDON_PROTOCOL_KEY);
        putRequestToFptiEvent(routingValue, aimlGatewayFptiEvent, payload);

        // reset url: only support when seldon V2
        if (configuration.getBoolean(SELDON_V2_ENABLE_KEY, false)) {
            String url = convertV2Url(routingValue);
            routingValue.setUrl(url);
        }

        Map<String, Object> seldonResult = sendSeldonRequest(singlePredictRequest.getRawInputs(),
                            singlePredictRequest.getBinaryInput(), routingValue, payload, headers, useAsync);
        putIfNotNull(SELDON_PROTOCOL_KEY, seldonProtocol, seldonResult);
        return seldonResult;
    }

    private Map<String, Object> sendSeldonRequest(List<String> rawInputs, byte[] binaryInput, RoutingValue routingValue,
                                Map<String, Object> payload, Map<String, String> headers, boolean useAsync) {
        try {
            // enable DR
            if (null != routingValue.getUrl() && routingValue.getUrl().contains("|")) {
                if (null != aimlgatewayHost && (aimlgatewayHost.length() >= 5 && aimlgatewayHost.startsWith("dcg") || aimlgatewayHost.startsWith("te-alm")
                        || aimlgatewayHost.startsWith("LM-SHB"))) {
                    return disasterRecoveryHelper.predictWithDR(aimlgatewayHost, routingValue, payload, headers, useAsync);
                }
                throw new GatewayException("Do not support DR for endpoint." + routingValue.getEndpoint()
                        + ". Host is not right. host:" + aimlgatewayHost, INTERNAL_SERVER_ERROR);
            }

            // use grpc
            if(null != routingValue.getIsGRpc() && 1 == routingValue.getIsGRpc()) {
                return gRpcServiceManager.grpcQuery(rawInputs, binaryInput, routingValue);
            }

            // use rests
            if (useAsync) {
                return httpAsyncServiceManager.httpAsyncPostQuery(routingValue.getUrl(), Maps.newHashMap(),payload,
                        Map.class, headers, true,null, routingValue.getTimeout());
            } else {
                if (aimlgatewayHost.startsWith("te-alm") || aimlgatewayHost.startsWith("LM-SHB")) {
                    return httpServiceManager.httpPostQuery(routingValue.getUrl(), Maps.newHashMap(),payload,
                            Map.class, headers, true,null, routingValue.getTimeout());
                }
                // Content Moderation Platform:content-moderation-image:null dcg01 to hrz-ana
                if ("content-moderation-image".equals(routingValue.getEndpoint()) && aimlgatewayHost.startsWith("dcg14")) {
                    routingValue.setUrl("https://aiplatform.ccg24-hrzana-edk8s.ccg24.lvs.paypalinc.com/seldon/seldon/content-moderat-26475/api/v0.1/predictions");
                }
                return  seldonClientWithCustomizedTimeout.predict(payload, routingValue.getUrl(), Map.class, routingValue.getTimeout());
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            CalLogHelper.logException(SELDON_PREDICT_ERROR, e);
            throw new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR);
        } catch (GatewayException e) {
            CalLogHelper.logException(SELDON_PREDICT_ERROR, e);
            throw new GatewayException(e.getErrorMessage(), INTERNAL_SERVER_ERROR);
        }
    }
}
