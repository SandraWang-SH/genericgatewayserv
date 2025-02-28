package com.paypal.raptor.aiml.triton;

import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.PredictResult;
import com.paypal.raptor.aiml.model.RoutingValue;


public interface TcsClient {

    PredictResponse predictTcs(final PredictRequest request, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException;

    PredictResult predictTcsMultipart(SinglePredictRequest singlePredictRequest, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException;

}
