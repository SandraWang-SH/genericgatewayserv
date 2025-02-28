package com.paypal.raptor.aiml.update.Impl;

import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_ALL_AUTH_RULES_ERROR;
import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_ALL_ROUTING_RULES_ERROR;
import static com.paypal.raptor.aiml.common.constants.CalConstants.LOAD_AUTH_VALUE_ERROR;
import static com.paypal.raptor.aiml.common.constants.CalConstants.LOAD_ROUTING_VALUE_ERROR;
import static com.paypal.raptor.aiml.common.enums.ModelServingInfra.SELDON;
import static com.paypal.raptor.aiml.utils.GatewayUtils.generateRoutingKeyString;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.paypal.raptor.aiml.common.enums.ModelServingInfra;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.AuthEntry;
import com.paypal.raptor.aiml.model.RoutingEntry;
import com.paypal.raptor.aiml.model.RoutingKey;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.model.UpdatableComponents;
import com.paypal.raptor.aiml.service.AuthService;
import com.paypal.raptor.aiml.service.RoutingService;
import com.paypal.raptor.aiml.update.UpdatableAllComponentsService;
import com.paypal.raptor.aiml.utils.CalLogHelper;

@Component
public class UpdatableAllComponentsServiceImpl implements UpdatableAllComponentsService {

    @Autowired
    RoutingService routingService;

    @Autowired
    AuthService authService;

    @Override
    public UpdatableComponents getAllComponents() {
        // get routing cache
        List<RoutingEntry> routingEntries;
        try {
            routingEntries = routingService.getAllRoutingEntries();
        } catch (Exception e) {
            CalLogHelper.logError(LOAD_ROUTING_VALUE_ERROR, GET_ALL_ROUTING_RULES_ERROR);
            throw new GatewayException("Fail when getAllRoutingEntries.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        // get auth cache
        List<AuthEntry> authEntries;
        try {
            authEntries = authService.getAllAuthEntries();
        } catch (Exception e) {
            CalLogHelper.logError(LOAD_AUTH_VALUE_ERROR, GET_ALL_AUTH_RULES_ERROR);
            throw new GatewayException("Fail when getAllAuthEntries.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        // put into routing map
        Map<String, RoutingValue> routingMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(routingEntries)) {
            routingEntries.forEach(e -> {
                // ternary cache
                String key = generateRoutingKeyString(new RoutingKey(e.getProject(), e.getEndpoint(), e.getModel()));
                routingMap.put(key, new RoutingValue(ModelServingInfra.valueOf(e.getInfra()), e.getEndpoint(),
                        e.getModel(), e.getUrl(), e.getProxy(), e.getProject_id(), e.getElmoResource(),
                        e.getTimeout(), e.getBypassPayload(), e.getIsGRpc(), e.getBindCluster(), e.getElmoExperimentType()));
                // binary cache
                if(SELDON.name().equals(e.getInfra())) {
                    key = generateRoutingKeyString(e.getProject(), e.getEndpoint());
                    routingMap.put(key, new RoutingValue(ModelServingInfra.valueOf(e.getInfra()), e.getEndpoint(),
                            e.getModel(), e.getUrl(), e.getProxy(), e.getProject_id(), e.getElmoResource(),
                            e.getTimeout(), e.getBypassPayload(), e.getIsGRpc(), e.getBindCluster(),
                            e.getElmoExperimentType()));
                }
            });
        }

        // put into auth map
        Map<String, List<String>> authMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(authEntries)) {
            authEntries.forEach(e -> {
                List<String> clients = Arrays.asList(e.getClients().split("\\|"));
                // ternary cache
                String key = generateRoutingKeyString(new RoutingKey(e.getProject(), e.getEndpoint(), e.getModel()));
                authMap.put(key, clients);

                // binary cache
                key = generateRoutingKeyString(e.getProject(), e.getEndpoint());
                authMap.put(key, clients);
            });
        }

        return new com.paypal.raptor.aiml.model.UpdatableComponents(routingMap, authMap);
    }
}
