package com.paypal.raptor.aiml.model;

import java.util.List;
import java.util.Map;

/**
 * The wrapper for all the updatable components, including routing rule and auth rule.
 *
 * All the components are stored in one object, so that we can update them atomically.
 */
public class UpdatableComponents {
    private final Map<String, RoutingValue> routingMap;

    private final Map<String, List<String>> authMap;

    public UpdatableComponents(Map<String, RoutingValue> routingMap,
                               Map<String, List<String>> authMap) {
        this.routingMap = routingMap;
        this.authMap = authMap;
    }

    public Map<String, RoutingValue> getRoutingMap() {
        return routingMap;
    }

    public Map<String, List<String>> getAuthMap() {
        return authMap;
    }
}
