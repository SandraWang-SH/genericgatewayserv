package com.paypal.raptor.aiml.client;

public interface ServiceClient<R> {

    /**
     * Get service client.
     */
    R getServiceClient();

    /**
     * Get Service client name.
     */
    String getClientName();
}
