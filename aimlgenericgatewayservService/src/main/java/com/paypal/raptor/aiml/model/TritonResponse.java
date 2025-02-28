package com.paypal.raptor.aiml.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TritonResponse {
    @JsonProperty("model_name")
    private String modelName;
    @JsonProperty("model_version")
    private String modelVersion;
    private List<NestedDataOutput> outputs;
    private long latency;
    private String error;

    public TritonResponse() {

    }

    public TritonResponse(String modelName) {
        this.modelName = modelName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public List<NestedDataOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<NestedDataOutput> outputs) {
        this.outputs = outputs;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

}
