package com.paypal.raptor.aiml.data;

import static com.paypal.raptor.aiml.common.constants.CalConstants.EDM_API;
import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_ALL_AUTH;
import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_ALL_ROUTING;
import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_AUTH_ENTRY;
import static com.paypal.raptor.aiml.common.constants.CalConstants.GET_ROUTING_ENTRY;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.AUTH_RULE_PATH;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.EDMSTUDISERV_PATH;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.EMPTY_LIST_STR;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RCS_AUTH_KEY;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RCS_ROUTING_KEY;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.ROUTING_RULE_PATH;
import static com.paypal.raptor.aiml.utils.GatewayUtils.generateRoutingKeyString;
import static com.paypal.raptor.aiml.utils.GatewayUtils.parseRoutingKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.ebayinc.platform.services.EndPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.aiml.common.remoteclient.RemoteCallClient;
import com.paypal.raptor.aiml.common.enums.ModelServingInfra;
import com.paypal.raptor.aiml.model.DBAuthEntryList;
import com.paypal.raptor.aiml.model.DBRoutingEntryList;
import com.paypal.raptor.aiml.model.ElmoResource;
import com.paypal.raptor.aiml.model.AuthEntry;
import com.paypal.raptor.aiml.model.RoutingEntry;
import com.paypal.raptor.aiml.model.RoutingKey;
import com.paypal.raptor.aiml.model.RoutingValue;
@Component
public class DataService {

    @Inject
    Configuration configuration;

    @Autowired
    @EndPoint(service = "edmstudiserv")
    public WebTarget edmstudiserv;

    @Autowired
    @Qualifier("remoteCallClient")
    private RemoteCallClient remoteCallClient;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public List<RoutingEntry> getAllRoutingEnetirsFromUCP() {
        return gson.fromJson(
                configuration.getString(
                        RCS_ROUTING_KEY, EMPTY_LIST_STR),
                new TypeToken<List<RoutingEntry>>() {}.getType());
    }

    public List<RoutingEntry> getAllRoutingEntriesFromDB() throws Exception {
        String entityStr = remoteCallClient.get(edmstudiserv.path(EDMSTUDISERV_PATH + ROUTING_RULE_PATH),
                String.class,
                EDM_API, GET_ALL_ROUTING);
        DBRoutingEntryList routingEntries = mapper.readValue(entityStr, DBRoutingEntryList.class);
        for(RoutingEntry routingEntry:routingEntries.getRoutingEntry()) {
            if(StringUtils.isEmpty(routingEntry.getElmoResourceStr())) {
                continue;
            }
            routingEntry.setElmoResource(mapper.readValue(routingEntry.getElmoResourceStr(), ElmoResource.class));
        }

        List<RoutingEntry> routingEntriesFromDB = new ArrayList<>();
        for(RoutingEntry e:routingEntries.getRoutingEntry()) {
            if(0 == e.getEnableTraffic()) {
                continue;
            }
            routingEntriesFromDB.add(e);
        }
        return routingEntriesFromDB;
    }

    public RoutingValue getRoutingValueFromUCP(String key) {
        List<RoutingEntry> routingList = gson.fromJson(configuration.getString(RCS_ROUTING_KEY, EMPTY_LIST_STR),
                new TypeToken<List<RoutingEntry>>(){}.getType());
        for (RoutingEntry e : routingList) {
            String routingKey = generateRoutingKeyString(e.getProject(),e.getEndpoint(),e.getModel());
            if (key.equals(routingKey)) {
                return new RoutingValue(ModelServingInfra.valueOf(e.getInfra()), e.getEndpoint(), e.getModel(), e.getUrl(),
                        e.getProxy(), e.getProject_id(), e.getElmoResource(), e.getTimeout(), e.getBypassPayload(),
                        e.getIsGRpc(), e.getBindCluster(), e.getElmoExperimentType());
            }
        }
        return null;
    }

    /**
     * @param routingKey routing key composed of project, endpoint and model
     * @return routing value from edm service, return null if not found
     * @throws Exception if EDM API call failure or JSON process exception
     */
    public RoutingValue getRoutingValueFromDB(String routingKey) throws Exception {
        Map<String, String> mapRoutingKey = parseRoutingKey(routingKey);
        String entityStr =
                remoteCallClient.get(edmstudiserv.path(EDMSTUDISERV_PATH + ROUTING_RULE_PATH).queryParam(
                "project", mapRoutingKey.get("project"))
                        .queryParam("endpoint", mapRoutingKey.get("endpoint"))
                        .queryParam("model", mapRoutingKey.get("model")), String.class,
                        EDM_API, GET_ROUTING_ENTRY);
        DBRoutingEntryList routingEntries = mapper.readValue(entityStr, DBRoutingEntryList.class);
        for(RoutingEntry e:routingEntries.getRoutingEntry()) {
            String key = generateRoutingKeyString(new RoutingKey(e.getProject(),e.getEndpoint(),e.getModel()));
            if (routingKey.equals(key) && 1 == e.getEnableTraffic()) {
                if(!StringUtils.isEmpty(e.getElmoResourceStr())) {
                    e.setElmoResource(mapper.readValue(e.getElmoResourceStr(), ElmoResource.class));
                }
                return new RoutingValue(ModelServingInfra.valueOf(e.getInfra()), e.getEndpoint(), e.getModel(), e.getUrl(),
                        e.getProxy(), e.getProject_id(), e.getElmoResource(), e.getTimeout(), e.getBypassPayload(),
                        e.getIsGRpc(), e.getBindCluster(),e.getElmoExperimentType());
            }
        }
        return null;
    }

    /**
     * @param routingKey routing key composed of project, endpoint and model
     * @return authorized clients from edm service, return empty list if not configured
     * @throws Exception if EDM API call failure or JSON process exception
     */
    public List<String> getAuthorizedClientsFromDB(String routingKey) throws Exception {
        Map<String, String> mapRoutingKey = parseRoutingKey(routingKey);
        String entityStr =
                remoteCallClient.get(edmstudiserv.path(EDMSTUDISERV_PATH + AUTH_RULE_PATH).queryParam(
                "project", mapRoutingKey.get("project"))
                            .queryParam("endpoint", mapRoutingKey.get("endpoint"))
                            .queryParam("model", mapRoutingKey.get("model")), String.class,
                    EDM_API, GET_AUTH_ENTRY);
        DBAuthEntryList authEntryList = mapper.readValue(entityStr, DBAuthEntryList.class);
        for (DBAuthEntryList.AuthEntry e : authEntryList.getAuthEntryList()) {
            String keyStr = generateRoutingKeyString(new RoutingKey(e.getProject(),e.getEndpoint(),e.getModel()));
            if (keyStr.equals(routingKey)) {
                return e.getClients() == null ? Lists.newArrayList() : e.getClients();
            }
        }
        return Lists.newArrayList();
    }

    public List<String> getAuthorizedClientsFromUCP(String routingKey) {
        List<AuthEntry> authList =
                gson.fromJson(
                        configuration.getString(
                                RCS_AUTH_KEY,
                                EMPTY_LIST_STR),
                        new TypeToken<List<AuthEntry>>() {}.getType());
        for (AuthEntry e : authList) {
            String keyStr = generateRoutingKeyString(new RoutingKey(e.getProject(),e.getEndpoint(),e.getModel()));
            if (keyStr.equals(routingKey)) {
                return Arrays.asList(e.getClients().split("\\|"));
            }
        }
        return null;
    }

    public List<AuthEntry> getAllAuthEntriesFromDB() throws Exception {
        //try 3 times
        String entityStr = remoteCallClient.get(edmstudiserv.path(EDMSTUDISERV_PATH + AUTH_RULE_PATH),
                String.class,
                EDM_API, GET_ALL_AUTH);
        DBAuthEntryList authEntryList = mapper.readValue(entityStr, DBAuthEntryList.class);
        List<DBAuthEntryList.AuthEntry> authEntries = authEntryList.getAuthEntryList();
        List<AuthEntry> authList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(authEntries)) {
            authEntries.forEach(e -> {
                AuthEntry authEntry = new AuthEntry();
                authEntry.setProject(e.getProject());
                authEntry.setEndpoint(e.getEndpoint());
                authEntry.setModel(e.getModel());
                authEntry.setClients(org.apache.commons.lang3.StringUtils.join(e.getClients(), "|"));
                if(!StringUtils.isEmpty(authEntry.getClients())) {
                    authList.add(authEntry);
                }
            });
        }
        return authList;
    }

    public List<AuthEntry> getAllAuthEntriesFromUCP() {
        return gson.fromJson(
                configuration.getString(
                        RCS_AUTH_KEY, EMPTY_LIST_STR),
                new TypeToken<List<AuthEntry>>() {}.getType());
    }
}
