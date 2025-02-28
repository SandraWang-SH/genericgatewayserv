package com.paypal.raptor.aiml.update;

import com.paypal.raptor.aiml.common.exception.AuthException;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.RoutingException;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.model.UpdatableComponents;


public interface UpdatableCacheComponentsService {

    /**
     * Get UpdatableComponents from cache.
     * @param key Routing Key composed by project, endpoint and model
     * @return Routing value
     * @throws RoutingException If failed to find routing target from cache.
     **/
     UpdatableComponents getUpdatableComponents(String key) throws RoutingException;

    /**
     * Get routing value from cache.
     * @param key Routing Key composed by project, endpoint and model
     * @return Routing value
     * @throws RoutingException If failed to find routing target from cache.
     **/
    RoutingValue getRoutingValue(String key) throws RoutingException;

    /**
     * @param routingKey combination string of project+endpoint+model
     * @return null if auth clients not set
     * @throws Exception if failed to get client whitelist from underlying datasource
     */
    void checkIfClientAuthorized(String client, String routingKey) throws AuthException;


    void checkIfValidRequest(String inputType, String jsonBody) throws GatewayException;
}
