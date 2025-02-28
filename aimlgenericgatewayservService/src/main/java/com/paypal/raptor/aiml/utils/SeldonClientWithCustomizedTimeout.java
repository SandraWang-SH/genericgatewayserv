package com.paypal.raptor.aiml.utils;

import com.ebay.kernel.cal.api.CalStatus;
import com.ebay.kernel.cal.api.CalTransaction;
import com.ebay.kernel.cal.api.sync.CalEventHelper;
import com.ebayinc.platform.jaxrs.client.factory.ClientFactory;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import lombok.Getter;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.SELDON_SYNC_API;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Component
@Getter
public class SeldonClientWithCustomizedTimeout {

    private static final Logger logger = LoggerFactory.getLogger(SeldonClientWithCustomizedTimeout.class);

    @Inject
    Configuration configuration;

    private Map<String, WebTarget> seldonClientPool = Maps.newConcurrentMap();
    private final ClientFactory clientFactory;
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final Integer DEFAULT_TIMEOUT = 5000;

    private Map<String, String> hostAndPoolMap = new HashMap<String, String>() {{
        put(SELDON_DEV51_HOST, "seldonserv_dev51");
        put(SELDON_SLC_HOST, "seldonserv_ccg15");
        put(SELDON_LVS_HOST, "seldonserv_ccg24");
        put(SELDON_LVS_HRZANA_HOST, "seldonserv_ccg24hrzana");
        put(SELDON_PHX_HOST, "seldonserv_dcg04");
    }};

    @Inject
    SeldonClientWithCustomizedTimeout(final ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public <T> T predict(Map<String, Object> requestPayload, String uri, Class<T> resultClass, Integer timeOut) throws URISyntaxException {
        URI url = new URI(uri);
        CalTransaction calTransaction = createTransaction(SELDON_SYNC_API, uri);

        try {
            WebTarget webTarget = getOrCreateWebtarget(timeOut, url.getHost());
            Response response =  webTarget.path(url.getPath())
                    .request(MediaType.APPLICATION_JSON)
                    .header(CONTENT_TYPE, APP_JSON)
                    .post(Entity.json(requestPayload));

            String strResult = null;
            if (response != null) {
                strResult = response.readEntity(String.class);
                if (response.getStatus() == OK.getStatusCode()) {
                    calTransaction.setStatus(CalStatus.SUCCESS);
                    return gson.fromJson(strResult, resultClass);
                } else {
                    CalEventHelper.sendImmediate("HTTP_REQUEST", "Failure", "1", strResult);
                    calTransaction.setStatus(CalStatus.EXCEPTION);
                    throw new GatewayException("Http failed. result:" + strResult + ", status code: " + response.getStatus(), INTERNAL_SERVER_ERROR);
                }
            } else {
                calTransaction.setStatus(CalStatus.EXCEPTION);
                throw new GatewayException("Response is null from downstream model service.", INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            calTransaction.completed();
        }
    }

    private WebTarget getOrCreateWebtarget(Integer timeOut, String hostName) throws MalformedURLException, URISyntaxException {
        WebTarget webTarget = null;
        timeOut = timeOut == null ? DEFAULT_TIMEOUT : timeOut;
        String clientPoolKey = hostAndPoolMap.get(hostName) + "_" + timeOut;
       if (seldonClientPool.containsKey(clientPoolKey)) {
            webTarget = seldonClientPool.get(clientPoolKey);
        } else {
            addNewConfigurationWithTimeOut(timeOut, clientPoolKey, hostName);
            webTarget = clientFactory.getWebTarget(clientPoolKey);
            seldonClientPool.put(clientPoolKey, webTarget);
        }
        return webTarget;
    }

    private void addNewConfigurationWithTimeOut(Integer timeout, String clientPoolKey, String hostName) {
        Integer oldConfigTimeOut = configuration.getInteger(clientPoolKey + "." + HTTP_READ_TIMEOUT_INMS, 0);
        if(!timeout.equals(oldConfigTimeOut)){
            configuration.addProperty(clientPoolKey + "." + HTTP_BASEURL, "https://" + hostName);
            configuration.addProperty(clientPoolKey + "." + HTTP_MAX_TOTAL_CONNECTIONS, 500);
            configuration.addProperty(clientPoolKey + "." + HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_INMS, 180000);
            configuration.addProperty(clientPoolKey + "." + HTTP_MAX_CONNECTION_LIFE_TIME_INMS, 300000);
            configuration.addProperty(clientPoolKey + "." + HTTP_CONNECT_TIMEOUT_IN_MS, 1000);
            configuration.addProperty(clientPoolKey + "." + HTTP_READ_TIMEOUT_INMS, timeout);
            configuration.addProperty(clientPoolKey + "." + HTTP_SSL_HANDSHAKE_TIMEOUT_INMS, 1000);
            configuration.addProperty(clientPoolKey + "." + HTTP_REQUEST_TIMEOUT_INMS, timeout);
            configuration.addProperty(clientPoolKey + "." + HTTP_IN_HTTP_CONTEXT, "false");
            configuration.addProperty(clientPoolKey + "." + HTTP_EXTERNAL, "true");
            configuration.addProperty(clientPoolKey + "." + HTTP_SSL_KEYSTORE, "digicert_rootca_keystore");
            configuration.addProperty(clientPoolKey + "." + HTTP_SSL_PROTOCOLS, "TLSv1.2");
            configuration.addProperty(clientPoolKey + "." + HEADER_PROPAGATION_ENABLED, "false");
        }
    }
}
