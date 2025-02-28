package com.paypal.raptor.aiml.utils;

import com.paypal.raptor.aiml.common.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.ws.rs.core.Response;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

import static com.paypal.raptor.aiml.common.constants.GRpcConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.utils.CommonUtils.putIfNotNull;



@Component
public class UrlUtils {

    private static final Logger logger = LoggerFactory.getLogger(UrlUtils.class);

    public static Map<String, String> parseSeldonUrl(String seldonUrlStr) {
        Map<String, String> headers = new HashMap<>();
        try {
            URL seldonUrl = new URL(seldonUrlStr);
            String path = seldonUrl.getPath();
            String[] splitStr = path.split("/");
            headers.put(GRPC_HEADER_NAMESPACE, splitStr[1]);
            headers.put(GRPC_HEADER_SELDON, splitStr[3]);
            if(splitStr.length > 6) {
                headers.put(V2_MODEL_NAME, splitStr[6]);
            }
            headers.put("path", path);
            return headers;
        } catch (MalformedURLException e) {
            throw new GatewayException("Parse seldon url error.", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public static Map<String, String> parseMultiUrl(String originUrl, String primaryHost, String failoverHost) {
        Map<String, String> mapResult = new HashMap<>();
        String[] urlList = originUrl.split("\\|");
        if (urlList.length < 2) {
            logger.error("Url length is less than 2! originUrl url:" + originUrl);
            throw new GatewayException("Url length is less than 2 when parse url. originUrl url:" + originUrl,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        try {
            URL url1 = new URL(urlList[0]), url2 = new URL(urlList[1]);
            if (primaryHost.equals(url2.getHost())) {
                putIfNotNull(SELDON_PRIMARY_URL_KEY, urlList[1], mapResult);
                putIfNotNull(SELDON_FAILOVER_URL_KEY, urlList[0], mapResult);
                putIfNotNull(SELDON_PRIMARY_PATH, url2.getPath(), mapResult);
                putIfNotNull(SELDON_FAILOVER_PATH, url1.getPath(), mapResult);
                putIfNotNull(SELDON_PRIMARY_HOST, url2.getHost(), mapResult);
                putIfNotNull(SELDON_FAILOVER_HOST, url1.getHost(), mapResult);
            } else {
                putIfNotNull(SELDON_PRIMARY_URL_KEY, urlList[0], mapResult);
                putIfNotNull(SELDON_FAILOVER_URL_KEY, urlList[1], mapResult);
                putIfNotNull(SELDON_PRIMARY_PATH, url1.getPath(), mapResult);
                putIfNotNull(SELDON_FAILOVER_PATH, url2.getPath(), mapResult);
                putIfNotNull(SELDON_PRIMARY_HOST, url1.getHost(), mapResult);
                putIfNotNull(SELDON_FAILOVER_HOST, url2.getHost(), mapResult);
            }
        } catch (MalformedURLException e) {
            throw new GatewayException("Parse url error.", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return mapResult;
    }

    public static String getGatewayHost() {
        String aimlgatewayHost = null;
        try {
            aimlgatewayHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("get gatewayHost error. gatewayHost:{}", aimlgatewayHost);
        }
        return aimlgatewayHost;
    }
}

