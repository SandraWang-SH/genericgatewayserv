package com.paypal.raptor.aiml.facade;

import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FptiPredictResult {

    PredictResult predictResult;

    AimlGatewayFptiEvent aimlGatewayFptiEvent;
}
