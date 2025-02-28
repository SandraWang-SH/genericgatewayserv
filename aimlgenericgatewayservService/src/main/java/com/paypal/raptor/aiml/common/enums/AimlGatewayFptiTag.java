package com.paypal.raptor.aiml.common.enums;

public enum AimlGatewayFptiTag {
    ACCOUNT_NUMBER,
    API_NAME,
    BIZ_API_NAME,
    FLOW_NAME,
    CLIENT_ID,
    COMPONENT,
    CORRELATION_ID,
    EVENT_NAME,
    CUST_ID,
    EXPERIMENTATION_EXPERIENCE,
    EXPERIMENTATION_TREATMENT,
    EXPERIMENT_VERSION,
    API_DURATION,
    TIMESTAMP_SERVERSIDE_REQUEST,
    TIMESTAMP_SERVERSIDE_RESPONSE,
    CONTEXT_ID,
    CONTEXT_TYPE,
    RESP,
    ALLOCATE_PERCENTAGE,
    REQUEST,
    STATUS_CODE,
    INT_ERROR_CODE,
    INT_ERROR_DESC;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
