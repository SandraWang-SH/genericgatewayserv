package com.paypal.raptor.aiml.fpti;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import com.paypal.fpti.tracking.HttpParams;
import com.paypal.fpti.tracking.api.TrackingSession;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class FptiEvent {

    private String channel;
    private TrackingSession actor;

    @SerializedName("http_params")
    private HttpParams httpParams;

    @SerializedName("event_params")
    private Map<String, String> eventParams;
}
