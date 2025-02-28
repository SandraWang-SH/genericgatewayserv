package com.paypal.raptor.aiml.update;

import com.paypal.raptor.aiml.common.exception.AuthException;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.RoutingException;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.model.UpdatableComponents;


public interface UpdatableAllComponentsService {

    /**
     * @return All routing/auth entries for gateway.
     * @throws Exception to fail app start
     */
    UpdatableComponents getAllComponents();

}
