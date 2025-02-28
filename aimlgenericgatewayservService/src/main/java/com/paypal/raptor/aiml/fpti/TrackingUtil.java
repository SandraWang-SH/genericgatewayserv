package com.paypal.raptor.aiml.fpti;


import com.ebayinc.platform.services.EndPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.fpti.tracking.Event;
import com.paypal.fpti.tracking.HttpParams;
import com.paypal.fpti.tracking.TrackingImpl;
import com.paypal.infra.util.AZFinderUtil;
import com.paypal.infra.util.ApplicationInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.CONTENT_TYPE;


@Component
public class TrackingUtil {

    private static final Logger logger = LoggerFactory.getLogger(TrackingUtil.class);

    private static final String TAG_ACCEPT_CHARSET = "accept-charset";
    private static final String TAG_ACCEPT_LANG = "accept-language";
    private static final String TAG_HOST = "Host";
    private static final String TAG_USER_AGENT = "user-agent";
    private static final String TAG_REFFERER = "referrer";
    private static final String TAG_IP_ADDRESS = "ip-address";
    private static final String API_OPERATION = "biz_api_operation";
    private static final String SERVER_EPOCH_TIMESTAMP = "server_epoch_timestamp";
    private static final String COMP = "comp";
    private static final String ENRICH_TAG = "enrich";
    private static final String ENRICH_VALUE = "n";
    private static final String AVAILABILITY_ZONE = "az";
    private static final String DATA_CENTER = "data_center";
    private static final String TECH_STACK_TAG = "pgtf";
    private static final String TECH_STACK_VALUE = "raptor";

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Autowired(required=false)
    @EndPoint(service = "trackingeventrestserv")
    private WebTarget fptiClient;

    private static final String TRACKING_SERVICE_URL = "/v1/tracking/events";

    @Inject
    ApplicationInfo applicationInfo;
    /**
     * This method fetches httpParams from request context headers
     *
     * @param headers request context
     * @return map of http params and its values
     */
    public HttpParams getHttpParams(final javax.ws.rs.core.HttpHeaders headers) {
        HttpParams httpParams = new HttpParams();
        httpParams.setAcceptCharset(StringUtils.defaultString(headers.getHeaderString(TAG_ACCEPT_CHARSET)));
        httpParams.setUserAgent(StringUtils.defaultString(headers.getHeaderString(TAG_USER_AGENT)));
        httpParams.setAcceptLang(StringUtils.defaultString(headers.getHeaderString(TAG_ACCEPT_LANG)));
        httpParams.setHostHeader(StringUtils.defaultString(headers.getHeaderString(TAG_HOST)));
        httpParams.setIpAddress(StringUtils.defaultString(headers.getHeaderString(TAG_IP_ADDRESS)));
        httpParams.setReferrer(StringUtils.defaultString(headers.getHeaderString(TAG_REFFERER)));

        return httpParams;
    }

    public Map<String, String> getAutoLogTags() {

        Map<String, String> autoLogTags = new HashMap<>();

        try {
            autoLogTags.put(AVAILABILITY_ZONE, StringUtils.defaultString(AZFinderUtil.getCurrentAZ()));
            autoLogTags.put(DATA_CENTER, StringUtils.defaultString(AZFinderUtil.getCurrentRegion()));
        } catch (IllegalStateException exp) {
            logger.error("error while retrieving availability zone information");
        }

        autoLogTags.put(API_OPERATION, "POST");
        autoLogTags.put(COMP, "aimlgenericgatewayserv");
        autoLogTags.put(SERVER_EPOCH_TIMESTAMP, StringUtils.defaultString(String.valueOf(System.currentTimeMillis())));
        autoLogTags.put(TECH_STACK_TAG, TECH_STACK_VALUE);
        autoLogTags.put(ENRICH_TAG, ENRICH_VALUE);

        return autoLogTags;
    }

    /**
     * This method injects httpParams and autoLogTags into the event blob
     *
     * @param trackingEvents map of events to be tracked
     * @param httpParamsMap  map of http params
     * @param autoLogTags    map of auto log tags
     * @return list of maps that contains the entire event blob
     */
    public List<Event> transformData(List<Event> trackingEvents, HttpParams httpParamsMap,
                                     Map<String, String> autoLogTags) {

        if (!trackingEvents.isEmpty()) {
            for (Event trackEvent : trackingEvents) {
                List<Map<String, String>> eventParams = trackEvent.getEventParams();
                if (eventParams.isEmpty()) {
                    continue;
                }

                for (Map<String, String> eventMap : eventParams) {
                    for (Map.Entry<String, String> entry : autoLogTags.entrySet()) {
                        if (!eventMap.containsKey(entry.getKey())) {
                            eventMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                trackEvent.setEventParams(eventParams);
                trackEvent.setHttpParams(httpParamsMap);
            }

        }
        return trackingEvents;
    }

    Map<String, Event> prepareBlob(final List<Event> trackingEvent,
                                         final HttpParams httpParams) {
        List<Event> trackingEvents = transformData(trackingEvent, httpParams, getAutoLogTags());
        Map<String, Event> fptiData = new HashMap();
        fptiData.put("events", trackingEvents.get(0));
        // trackingEvents.get(0).getEventParams().get(0);
        return fptiData;
    }

    public void send2Fpti(final AimlGatewayFptiEvent aimlGatewayFptiEvent,
                          HttpParams headers) {

        String jsonFpti = getFptiJson(aimlGatewayFptiEvent, headers);

        Response response = fptiClient.path(TRACKING_SERVICE_URL)
                .request(MediaType.APPLICATION_JSON)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(Entity.json(jsonFpti));

        if(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            logger.debug("FPTI server async call completed.");
        } else {
            logger.error("FPTI server async call failed. Status code: " + response.getStatus());
        }
        try{
            response.close();
        } catch (ProcessingException e) {
            logger.error("Exception happened during async call to publish FPTI events (closing response)", e);
        }
    }

    String getFptiJson(final AimlGatewayFptiEvent aimlGatewayFptiEvent,
                       HttpParams headers) {

        TrackingImpl tracking = new TrackingImpl();
        tracking.trackEvent("", aimlGatewayFptiEvent.toMap());

        Map<String, Event> fptiData = prepareBlob(tracking.getTrackingEvents(), headers);

        Map<String, String> eventParam = new HashMap<>();
        fptiData.get("events").getEventParams().forEach(eventParam::putAll);

        FptiEvent fptiEvent = new FptiEvent();
        fptiEvent.setChannel(fptiData.get("events").getChannel());
        fptiEvent.setActor(fptiData.get("events").getActor());
        fptiEvent.setHttpParams(fptiData.get("events").getHttpParams());
        fptiEvent.setEventParams(eventParam);

        Map<String, FptiEvent> fptiJson = new HashMap<>();
        fptiJson.put("events", fptiEvent);
        return gson.toJson(fptiJson);
    }
}
