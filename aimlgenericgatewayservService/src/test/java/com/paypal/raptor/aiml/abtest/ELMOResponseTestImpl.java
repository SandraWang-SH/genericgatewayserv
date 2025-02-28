package com.paypal.raptor.aiml.abtest;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.ENDPOINT_KEY_IN_FACTORS;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.PROJECT_KEY_IN_FACTORS;
import java.util.HashMap;
import java.util.Map;
import com.paypal.elmo.elmoservjava.api.ELMOResponse;
import com.paypal.elmoserv.models.response.TreatmentResponse;
import com.paypal.elmoserv.models.response.TreatmentsResponse;


public class ELMOResponseTestImpl implements ELMOResponse {
    @Override
    public boolean hasTreatment(final String experimentName, final String treatmentName) {
        return true;
    }

    @Override
    public TreatmentsResponse getTreatment(final String experimentName, final String treatmentName) {
        return getTreatmentsResponse();
    }

    @Override
    public Map<String, String> getTrackingParams() {
        return null;
    }

    private TreatmentsResponse getTreatmentsResponse() {
        TreatmentsResponse treatmentsResponse = new TreatmentsResponse();
        Map<String, String> factorsInRsp = new HashMap<>();
        factorsInRsp.put(PROJECT_KEY_IN_FACTORS, "ML Platform");
        factorsInRsp.put(ENDPOINT_KEY_IN_FACTORS, "sheena-elmo-treatment");
        treatmentsResponse.setFactors(factorsInRsp);
        TreatmentResponse treatmentResponse = new TreatmentResponse();
        treatmentResponse.setTreatmentVersion(1.0);
        treatmentsResponse.setTreatment(treatmentResponse);
        return treatmentsResponse;
    }
}
