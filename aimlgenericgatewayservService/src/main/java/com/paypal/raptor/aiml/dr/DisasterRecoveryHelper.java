package com.paypal.raptor.aiml.dr;

import com.google.common.collect.Maps;
import com.paypal.raptor.aiml.client.SeldonServiceClient;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.utils.HTTPAsyncServiceManager;
import com.paypal.raptor.aiml.utils.HTTPServiceManager;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.checkEndpointsInConfig;
import static com.paypal.raptor.aiml.utils.GatewayUtils.getConfigEndpoints;
import static com.paypal.raptor.aiml.utils.GatewayUtils.ifProd;
import static com.paypal.raptor.aiml.utils.UrlUtils.parseMultiUrl;
import javax.annotation.PostConstruct;
import javax.inject.Inject;


@Component
public class DisasterRecoveryHelper {

    private static final Logger logger = LoggerFactory.getLogger(DisasterRecoveryHelper.class);

    @Autowired
    SeldonServiceClient seldonServiceClient;

    @Autowired
    HTTPAsyncServiceManager httpAsyncServiceManager;

    @Autowired
    HTTPServiceManager httpServiceManager;

    Set<String> phxEndpoints;

    @Inject
    Configuration configuration;

    @PostConstruct
    public void init() {
        phxEndpoints = getConfigEndpoints(configuration.getString(ENABLE_PHX_ENDPOINTS));
    }

    public Map<String, Object> predictWithDR(String gwHost, RoutingValue routingValue, Map<String, Object> payload,
                                             Map<String, String> headers, boolean useAsync)
            throws URISyntaxException, IOException, InterruptedException {
        if (useAsync) {
            return getSeldonAsyncWithUrl(getPrimaryUrl(gwHost, routingValue.getUrl(), routingValue.getEndpoint()),
                    routingValue, payload, headers);
        } else {
            return getSeldonWithUrl(getPrimaryUrl(gwHost, routingValue.getUrl(), routingValue.getEndpoint()),
                    routingValue, payload, headers);
        }
    }

    public Map<String, String> getPrimaryUrl(String gwHost, String originUrl, String endpoint) {
        String primaryHost, failoverHost;
        if (ifProd()) {
            String az = gwHost.substring(0,5);
            boolean isPhxEndpoint = checkEndpointsInConfig(endpoint, phxEndpoints);
            if ("dcg01".equals(az) || "dcg02".equals(az) || "dcg04".equals(az)) {
                primaryHost = isPhxEndpoint ? SELDON_PHX_HOST:SELDON_LVS_HOST;
                failoverHost = SELDON_SLC_HOST;
            } else {
                primaryHost = SELDON_SLC_HOST;
                failoverHost = isPhxEndpoint ? SELDON_PHX_HOST:SELDON_LVS_HOST;
            }
        } else {
            primaryHost = SELDON_DEV51_HOST;
            failoverHost = SELDON_DEV51_HOST;
        }
        return parseMultiUrl(originUrl, primaryHost, failoverHost);
    }

    private Map<String, Object> getSeldonWithUrl(Map<String, String> resultUrl,
                                                 RoutingValue routingValue, Map<String, Object> payload,
                                                 Map<String, String> headers) throws URISyntaxException, IOException {
        try {
            if (SELDON_PHX_HOST.equals(resultUrl.get(SELDON_PRIMARY_HOST))) {
                return seldonServiceClient.predict(payload, resultUrl.get(SELDON_PRIMARY_PATH),
                        "https://" + SELDON_PHX_HOST);
            }
            return httpServiceManager.httpPostQuery(resultUrl.get(SELDON_PRIMARY_URL_KEY), Maps.newHashMap(), payload,
                    Map.class, headers, true,null, routingValue.getTimeout());
        } catch (Exception exception) {
            logger.info("primary down. message:{}. start failover. projectId:{}, endpoint:{}.",
                    exception.getMessage(), routingValue.getProject_id(), routingValue.getEndpoint());
            if (SELDON_PHX_HOST.equals(resultUrl.get(SELDON_FAILOVER_HOST))) {
                return seldonServiceClient.predict(payload, resultUrl.get(SELDON_FAILOVER_PATH),
                        "https://" + SELDON_PHX_HOST);
            }
            return httpServiceManager.httpPostQuery(resultUrl.get(SELDON_FAILOVER_URL_KEY), Maps.newHashMap(),payload,
                    Map.class, headers, true,null, routingValue.getTimeout());
        }
    }

    private Map<String, Object> getSeldonAsyncWithUrl(Map<String, String> resultUrl,
            RoutingValue routingValue, Map<String, Object> payload,
            Map<String, String> headers) throws URISyntaxException, IOException, InterruptedException {
        Map<String, Object> httpResponse;
        try {
            httpResponse = httpAsyncServiceManager.httpAsyncPostQuery(resultUrl.get(SELDON_PRIMARY_URL_KEY),
                    Maps.newHashMap(), payload, Map.class, headers,
                    true,null, routingValue.getTimeout());
            logger.info("call primary. uri:{}, projectId:{}, endpoint:{}.",
                    resultUrl.get(SELDON_PRIMARY_URL_KEY), routingValue.getProject_id(), routingValue.getEndpoint());
        } catch (Exception exception) {
            logger.info("primary down. message:{}. start failover. projectId:{}, endpoint:{}, uri:{}.",
                    exception.getMessage(), routingValue.getProject_id(), routingValue.getEndpoint(), resultUrl.get(SELDON_FAILOVER_URL_KEY));
            httpResponse = httpAsyncServiceManager.httpAsyncPostQuery(resultUrl.get(SELDON_FAILOVER_URL_KEY),
                    Maps.newHashMap(), payload, Map.class, headers,
                    true,null, routingValue.getTimeout());
        }
        return httpResponse;
    }

}
