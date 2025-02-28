package com.paypal.raptor.aiml.abtest;

import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.Audience;
import com.paypal.raptor.aiml.model.ElmoResource;
import com.paypal.raptor.aiml.model.RoutingValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.paypal.raptor.aiml.utils.ElmoUtils.checkExperimentsEmpty;
import static com.paypal.raptor.aiml.utils.ElmoUtils.parseAudienceFromSecurityContext;

@Component
public class ElmoHelper {

    @Autowired
    ElmoService elmoService;

    public RoutingValue getElmoResult(RoutingValue routingValue, final Optional<SecurityContext> securityContext,
                                      final AimlGatewayFptiEvent aimlGatewayFptiEvent) {
        if(null != routingValue.getElmoResource() && null != routingValue.getElmoExperimentType()
                && routingValue.getElmoExperimentType() > 0) {
            // abtest enabled for current endpoint
            checkExperimentsEmpty(routingValue);
            ElmoResource.ElmoExperiment experiment = routingValue.getElmoResource().getExperiments().get(0);
            Audience audience = parseAudienceFromSecurityContext(experiment.getAudienceType(), securityContext);
            routingValue = elmoService.getElmoTestRoutingValue(routingValue, audience, null, aimlGatewayFptiEvent);
        }
        return routingValue;
    }


}
