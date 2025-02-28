package com.paypal.raptor.aiml.routing;

import static com.paypal.raptor.aiml.common.constants.CalConstants.INFRA;
import static com.paypal.raptor.aiml.utils.GatewayUtils.generateRoutingKeyString;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ebay.kernel.cal.api.sync.CalTransactionHelper;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.abtest.ElmoHelper;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.Model;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.RoutingKey;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.update.UpdatableCacheComponentsService;

@Component
public class RoutingHelper {

    @Autowired
    UpdatableCacheComponentsService updatableComponentsService;

    @Autowired
    ElmoHelper elmoHelper;

    public RoutingValue getRoutingAndAuth(final PredictRequest request, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
            String client, final Optional<SecurityContext> securityContext) {
        return getRoutingAndAuth(request.getProject(), request.getEndpoint(), request.getModel(), client,
                securityContext, aimlGatewayFptiEvent);
    }

    public RoutingValue getStreamRoutingAndAuth(final PredictRequest request, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
            String client, final Optional<SecurityContext> securityContext) {

        // check auth and
        updatableComponentsService.checkIfValidRequest(request.getInputType(), request.getInputs().get(0));

        // if input type is 'json', not 'raw', input need to change to be a map

        return getRoutingAndAuth(request.getProject(), request.getEndpoint(), request.getModel(),
                client, securityContext, aimlGatewayFptiEvent);
    }

    public RoutingValue getRoutingAndAuth(SinglePredictRequest singlePredictRequest,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) {
        Model model = singlePredictRequest.getModel();
        return getRoutingAndAuth(model.getProject(), model.getEndpoint(), model.getModel(), singlePredictRequest.getClient(),
                singlePredictRequest.getSecurityContext(), aimlGatewayFptiEvent);
    }

    public RoutingValue getRouting(SinglePredictRequest singlePredictRequest) {
        Model model = singlePredictRequest.getModel();
        String routingKey = generateRoutingKeyString(new RoutingKey(model.getProject(), model.getEndpoint(), model.getModel()));
        return updatableComponentsService.getRoutingValue(routingKey);
    }

    private RoutingValue getRoutingAndAuth(String project, String endpoint, String model, String client,
            final Optional<SecurityContext> securityContext,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) {
        String routingKey = generateRoutingKeyString(new RoutingKey(project,
                endpoint, model));
        aimlGatewayFptiEvent.putEventName(routingKey);

        // routing
        RoutingValue routingValue = updatableComponentsService.getRoutingValue(routingKey);
        routingValue.setModel(model);

        // cal transaction for each model infra
        CalTransactionHelper.getCurrent().addData(INFRA, routingValue.getInfra().name());
        updatableComponentsService.checkIfClientAuthorized(client, routingKey);

        // abtest
        routingValue = elmoHelper.getElmoResult(routingValue, securityContext, aimlGatewayFptiEvent);
        return routingValue;
    }

}
