package com.paypal.raptor.aiml.seldon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.PredictResult;
import com.paypal.raptor.aiml.model.RoutingValue;


public interface SeldonClient {
    PredictResponse predictSeldon(final PredictRequest request, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException, JsonProcessingException;

    PredictResponse predictSeldonAsync(final PredictRequest request, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException, JsonProcessingException;
    PredictResult predictMultipartSeldonAsync(SinglePredictRequest singlePredictRequest,
            RoutingValue routingValue, final AimlGatewayFptiEvent aimlGatewayFptiEvent)
            throws GatewayException, JsonProcessingException;
}
