package com.paypal.raptor.aiml.fpti;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import com.ebay.kernel.cal.api.sync.CalTransactionHelper;
import com.paypal.raptor.aiml.common.enums.AimlGatewayFptiTag;

public class AimlGatewayFptiEvent extends EnumMap<AimlGatewayFptiTag, String> {

    private AimlGatewayFptiEvent(final Class<AimlGatewayFptiTag> keyType) {
        super(keyType);
    }

    public static AimlGatewayFptiEvent initFptiMap(String componentName) {
        AimlGatewayFptiEvent aimlGatewayFptiEvent = new AimlGatewayFptiEvent(AimlGatewayFptiTag.class);
        aimlGatewayFptiEvent.put(AimlGatewayFptiTag.COMPONENT, componentName);
        aimlGatewayFptiEvent.put(AimlGatewayFptiTag.CORRELATION_ID, getCorrelationId());
        return aimlGatewayFptiEvent;
    }

    public AimlGatewayFptiEvent putApiName(String apiName) {
        this.put(AimlGatewayFptiTag.API_NAME, apiName);
        return this;
    }

    public AimlGatewayFptiEvent putBizApiName(String bizApiName) {
        this.put(AimlGatewayFptiTag.BIZ_API_NAME, bizApiName);
        return this;
    }

    public AimlGatewayFptiEvent putFlowName(String flowName) {
        this.put(AimlGatewayFptiTag.FLOW_NAME, flowName);
        return this;
    }

    public AimlGatewayFptiEvent putStatusCode(String statusCode) {
        this.put(AimlGatewayFptiTag.STATUS_CODE, statusCode);
        return this;
    }

    public AimlGatewayFptiEvent putErrorDesc(String errorMessage) {
        this.put(AimlGatewayFptiTag.INT_ERROR_DESC, errorMessage);
        return this;
    }

    public AimlGatewayFptiEvent putContextId(String contextId) {
        this.put(AimlGatewayFptiTag.CONTEXT_ID, contextId);
        return this;
    }

    public AimlGatewayFptiEvent putContextType(String contextType) {
        this.put(AimlGatewayFptiTag.CONTEXT_TYPE, contextType);
        return this;
    }

    public AimlGatewayFptiEvent putApiDuration(String apiDuration) {
        this.put(AimlGatewayFptiTag.API_DURATION, apiDuration);
        return this;
    }

    public AimlGatewayFptiEvent putRequestTs(String reqTs) {
        this.put(AimlGatewayFptiTag.TIMESTAMP_SERVERSIDE_REQUEST, reqTs);
        return this;
    }

    public AimlGatewayFptiEvent putResponseTs(String respTs) {
        this.put(AimlGatewayFptiTag.TIMESTAMP_SERVERSIDE_RESPONSE, respTs);
        return this;
    }

    public AimlGatewayFptiEvent putClientId(String clientId) {
        this.put(AimlGatewayFptiTag.CLIENT_ID, clientId);
        return this;
    }

    public AimlGatewayFptiEvent putEventName(String eventName) {
        this.put(AimlGatewayFptiTag.EVENT_NAME, eventName);
        return this;
    }

    public AimlGatewayFptiEvent putExperimentVersion(String experimentVersion) {
        this.put(AimlGatewayFptiTag.EXPERIMENT_VERSION, experimentVersion);
        return this;
    }

    public AimlGatewayFptiEvent putAllocatePercentage(String percentage) {
        this.put(AimlGatewayFptiTag.ALLOCATE_PERCENTAGE, percentage);
        return this;
    }

    /**
     * @param experimentId, treatmentId
     *            the map returned by elmoResponse.getTrackingParams()
     * @return this fpti event
     */
    public AimlGatewayFptiEvent putElmoTrackingParams(String experimentId, String treatmentId) {
        this.put(AimlGatewayFptiTag.EXPERIMENTATION_EXPERIENCE, experimentId);
        this.put(AimlGatewayFptiTag.EXPERIMENTATION_TREATMENT, treatmentId);
        return this;
    }

    public AimlGatewayFptiEvent putReq(String req) {
        this.put(AimlGatewayFptiTag.REQUEST, req);
        return this;
    }

    public AimlGatewayFptiEvent putResp(String resp) {
        this.put(AimlGatewayFptiTag.RESP, resp);
        return this;
    }


    /**
     * @return map of fpti keys as strings
     */
    public Map<String, String> toMap() {
        Map<String, String> stringMap = new HashMap<>();

        forEach((k, v) -> stringMap.put(k.toString(), v));

        return stringMap;
    }

    private static String getCorrelationId() {
        return CalTransactionHelper.getTopTransaction() == null ? null
                : CalTransactionHelper.getTopTransaction().getCorrelationId();
    }

    /**
     * Compares the specified object with this map for equality. Returns <tt>true</tt> if the given object is also a map
     * and the two maps represent the same mappings, as specified in the {@link Map#equals(Object)} contract.
     *
     * @param o
     *            the object to be compared for equality with this map
     * @return <tt>true</tt> if the specified object is equal to this map
     */
    @Override
    public boolean equals(final Object o) {
        return o != null && toString().equals(o.toString());
    }

    /**
     * Returns the hash code value for this map. The hash code of a map is defined to be the sum of the hash codes of
     * each entry in the map.
     */
    @Override
    public int hashCode() {
        return toMap().hashCode();
    }

    /**
     * Returns a string representation of this map. The string representation consists of a list of key-value mappings
     * in the order returned by the map's <tt>entrySet</tt> view's iterator, enclosed in braces (<tt>"{}"</tt>).
     * Adjacent mappings are separated by the characters <tt>", "</tt> (comma and space). Each key-value mapping is
     * rendered as the key followed by an equals sign (<tt>"="</tt>) followed by the associated value. Keys and values
     * are converted to strings as by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this map
     */
    @Override
    public String toString() {
        return toMap().toString();
    }
}
