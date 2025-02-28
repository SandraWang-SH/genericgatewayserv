package com.paypal.raptor.aiml.client;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.client.jaxrs.internal.ClientWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ebayinc.platform.services.EndPoint;
import lombok.Getter;


@Component
@Getter
public class SeldonServiceClient extends AbstractWebTargetClient {

    private static final Logger logger = LoggerFactory.getLogger(SeldonServiceClient.class);

    @Autowired(required=false)
    @EndPoint(service = "hrzseldonserv")
    private WebTarget hrzseldonserv;

    public Map<String, Object> predict(Map<String, Object> requestPayload, String seldonPredictPath, String seldonUri) {
        return requestWithResponse(seldonPredictPath, seldonUri,
                () -> getServiceClient(seldonUri).path(seldonPredictPath).request()
                        .post(Entity.json(requestPayload)));
    }

    @Override
    public WebTarget getServiceClient() {
        return hrzseldonserv;
    }

    @Override
    public WebTarget getServiceClient(String uri) {
        return ((ClientWebTarget) hrzseldonserv).getResteasyClient().target(uri);
    }

    @Override
    public String getClientName() {
        return this.getClass().getName();
    }

}
