package com.paypal.raptor.aiml.common.enums;

import java.util.concurrent.TimeUnit;

public enum LimiterParameters {
    GENAI("genai",10D, 2, TimeUnit.SECONDS),
    GDS_CUSTOMER("gds-customer", 40D, 2, TimeUnit.SECONDS);

    private String modelProject;
    private double permitsPerSecond;
    private long timeOut;
    private TimeUnit timeUnit;

    LimiterParameters(String modelProject, double permitsPerSecond, long timeOut, TimeUnit timeUnit) {
        this.modelProject = modelProject;
        this.permitsPerSecond = permitsPerSecond;
        this.timeOut = timeOut;
        this.timeUnit = timeUnit;
    }

    public String getModelProject() {
        return modelProject;
    }

    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public static LimiterParameters getLimiterParameters(String modelProject) {
        for (LimiterParameters parameters : LimiterParameters.values()) {
            if (modelProject.equals(parameters.getModelProject())) {
                return parameters;
            }
        }
        return null;
    }

}
