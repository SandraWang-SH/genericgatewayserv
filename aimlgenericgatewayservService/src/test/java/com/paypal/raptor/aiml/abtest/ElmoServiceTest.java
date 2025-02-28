package com.paypal.raptor.aiml.abtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.elmo.elmoservjava.api.ELMOResponse;
import com.paypal.elmo.elmoservjava.api.ELMOSmartClient;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.platform.auth.securitycontext.User;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.Audience;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.update.UpdatableCacheComponentsService;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ElmoServiceTest {

    @Mock
    Configuration configuration;

    @Mock
    UpdatableCacheComponentsService updatableComponentsService;

    @InjectMocks
    ElmoService elmoService;

    @InjectMocks
    ElmoHelper elmoHelper;

    @Mock
    ELMOSmartClient elmoSmartClient;

    static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getElmoTestRoutingValueTest() {
        ELMOResponse elmoResponse = new ELMOResponseTestImpl();
        Mockito.when(elmoSmartClient.elmo(any(), any(), any())).thenReturn(elmoResponse);

        Map<String, RoutingValue> mapRoutingValue = getRoutingVal();
        Mockito.when(updatableComponentsService.getRoutingValue("ML Platform:sheena-elmo-treatment:null")).thenReturn(mapRoutingValue.get("Treatment"));

        AimlGatewayFptiEvent aimlGatewayFptiEvent = AimlGatewayFptiEvent.initFptiMap("getElmoTestRoutingValueTest");
        Audience audience = new Audience(Audience.AudienceType.uid, "aaaaaaaaa");
        RoutingValue resRoutingValue = elmoService.getElmoTestRoutingValue(mapRoutingValue.get("Control"), audience,
                                null, aimlGatewayFptiEvent);
        Assert.assertEquals(resRoutingValue.getEndpoint(), "sheena-elmo-treatment");
    }

    @Test
    public void getElmoResultTest() {

        ELMOResponse elmoResponse = new ELMOResponseTestImpl();
        Mockito.when(elmoSmartClient.elmo(any(), any(), any())).thenReturn(elmoResponse);

        Map<String, RoutingValue> mapRoutingValue = getRoutingVal();
        Mockito.when(updatableComponentsService.getRoutingValue("ML Platform:sheena-elmo-treatment:null")).thenReturn(mapRoutingValue.get("Treatment"));

        AimlGatewayFptiEvent aimlGatewayFptiEvent = AimlGatewayFptiEvent.initFptiMap("getElmoTestRoutingValueTest");
        Exception exception = assertThrows(NullPointerException.class, () -> {
            RoutingValue resRoutingValue = elmoHelper.getElmoResult(mapRoutingValue.get("Control"), getSecurityContext(),
                    aimlGatewayFptiEvent);
        });
    }

    private Optional<SecurityContext> getSecurityContext() {
        User user = new User();
        user.setEncryptedAccountNumber("bbb");
        user.setAccountNumber("1154635410118335931");

        SecurityContext securityContext = new SecurityContext();
        securityContext.setActor(user);
        return Optional.of(securityContext);
    }

    private Map<String, RoutingValue> getRoutingVal() {
        String routingTreatment = "{\"infra\":\"SELDON\",\"endpoint\":\"sheena-elmo-treatment\","
                + "\"url\":\"https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sheena-elmo-tre-f46a6/api/v0.1/predictions\",\"project_id\":\"40288189790d23f401790d2411c20000\",\"timeout\":5000,\"bypassPayload\":false,\"isGRpc\":0,\"bindCluster\":\"gke\",\"elmoExperimentType\":0}";
        String routingControl = "{\"infra\":\"SELDON\",\"endpoint\":\"sheena-elmo-control\","
                + "\"url\":\"https://aiplatform.dev51.cbf.dev.paypalinc"
                + ".com/seldon/seldon/sheena-elmo-con-55680-111/api/v0.1/predictions|https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sheena-elmo-tre-f46a6/api/v0.1/predictions\",\"project_id\":\"40288189790d23f401790d2411c20000\",\"elmoResource\":{\"resourceName\":\"cosmos_ai_model_a\",\"experiments\":[{\"name\":\"cosmos_ai_model_a_exp_3\",\"treatments\":[\"Ctrl_cosmos_ai_model_a_exp_3\",\"Trmt_cosmos_ai_model_a_exp_3\"],\"audienceType\":\"uid\"}]},\"timeout\":5000,\"bypassPayload\":false,\"isGRpc\":0,\"bindCluster\":\"gke\",\"elmoExperimentType\":1}";
        RoutingValue routingValueTreat = gson.fromJson(routingTreatment, RoutingValue.class);
        RoutingValue routingValueControl = gson.fromJson(routingControl, RoutingValue.class);

        Map<String, RoutingValue> mapRoutingValue = new HashMap<>();
        mapRoutingValue.put("Control", routingValueControl);
        mapRoutingValue.put("Treatment", routingValueTreat);
        return mapRoutingValue;
    }


}
