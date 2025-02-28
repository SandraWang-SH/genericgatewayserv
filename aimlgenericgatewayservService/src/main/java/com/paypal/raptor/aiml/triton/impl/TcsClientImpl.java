package com.paypal.raptor.aiml.triton.impl;

import static com.paypal.raptor.aiml.common.constants.CalConstants.RAPTOR_PREDICT_ERROR;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RAPTOR_PREDICT_PATH;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RAPTOR_TIME_ONE_MINUTE;
import static com.paypal.raptor.aiml.common.constants.NumConstants.ONE_MINUTE;
import static com.paypal.raptor.aiml.common.constants.NumConstants.TEN_SECONDS;
import static com.paypal.raptor.aiml.utils.CommonUtils.compressStringAndBase64Encode;
import static com.paypal.raptor.aiml.utils.GatewayUtils.composeRaptorPredictResponse;
import static com.paypal.raptor.aiml.utils.GatewayUtils.composeRaptorRequest;
import static com.paypal.raptor.aiml.utils.GatewayUtils.putResponseToFptiEvent;
import static com.paypal.raptor.aiml.utils.GatewayUtils.putTritonRequestToFptiEvent;
import static com.paypal.raptor.aiml.utils.GatewayUtils.retrieveComputeResponse;
import static com.paypal.raptor.aiml.utils.GatewayUtils.retrieveTritonResponse;
import static com.paypal.raptor.aiml.utils.HTTPServiceManager.multipartPostQuery;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.ebayinc.platform.services.EndPoint;
import com.google.common.collect.Maps;
import com.paypal.edm.computeschema.ComputeRequest;
import com.paypal.edm.computeschema.ComputeResponse;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.PredictResult;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.triton.TcsClient;
import com.paypal.raptor.aiml.utils.CalLogHelper;

@Component
public class TcsClientImpl implements TcsClient {

    private static final Logger logger = LoggerFactory.getLogger(TcsClientImpl.class);

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelgpu2serv")
    public WebTarget aimlmodelgpu2serv;

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelgpu2serv1min")
    public WebTarget aimlmodelgpu2serv1min;

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelgpuserv")
    public WebTarget aimlmodelgpuserv;

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelgpuserv1min")
    public WebTarget aimlmodelgpuserv1min;

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelserv")
    public WebTarget aimlmodelserv;

    @Autowired(required=false)
    @EndPoint(service = "aimlmodelserv1min")
    public WebTarget aimlmodelserv1min;

    @Autowired(required=false)
    @EndPoint(service = "tensorcomputegpuserv")
    public WebTarget tensorcomputegpuserv;

    @Autowired(required=false)
    @EndPoint(service = "tensorcomputegpuserv1min")
    public WebTarget tensorcomputegpuserv1min;

    @Autowired(required=false)
    @EndPoint(service = "aimlregressionserv")
    public WebTarget aimlregressionserv;

    @Autowired(required=false)
    @EndPoint(service = "aimlregressionserv1min")
    public WebTarget aimlregressionserv1min;

    @Autowired(required=false)
    @EndPoint(service = "docaitensorcomputegpuserv")
    public WebTarget docaitensorcomputegpuserv;

    @Autowired(required=false)
    @EndPoint(service = "docnetaimlmodelgpu2serv")
    public WebTarget docnetaimlmodelgpu2serv;

    private Map<String, WebTarget> raptorPoolMap = Maps.newHashMap();

    @PostConstruct
    public void init() {
        raptorPoolMap.put("aimlmodelserv", aimlmodelserv);
        raptorPoolMap.put("aimlmodelgpuserv", aimlmodelgpuserv);
        raptorPoolMap.put("aimlmodelgpu2serv", aimlmodelgpu2serv);
        raptorPoolMap.put("tensorcomputegpuserv", tensorcomputegpuserv);
        raptorPoolMap.put("aimlregressionserv", aimlregressionserv);

        raptorPoolMap.put("aimlmodelserv1min", aimlmodelserv1min);
        raptorPoolMap.put("aimlmodelgpuserv1min", aimlmodelgpuserv1min);
        raptorPoolMap.put("aimlmodelgpu2serv1min", aimlmodelgpu2serv1min);
        raptorPoolMap.put("tensorcomputegpuserv1min", tensorcomputegpuserv1min);
        raptorPoolMap.put("aimlregressionserv1min", aimlregressionserv1min);

        raptorPoolMap.put("docaitensorcomputegpuserv", docaitensorcomputegpuserv);
        raptorPoolMap.put("docnetaimlmodelgpu2serv", docnetaimlmodelgpu2serv);
    }

    @Override
    public PredictResponse predictTcs(final PredictRequest request, final RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException {
        ComputeResponse computeResponse = getRaptorPredictRawResult(request, routingValue, aimlGatewayFptiEvent);
        return composeRaptorPredictResponse(computeResponse, request, routingValue, aimlGatewayFptiEvent);
    }

    @Override
    public PredictResult predictTcsMultipart(SinglePredictRequest singlePredictRequest, final RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException {
        WebTarget webTarget = getWebTarget(singlePredictRequest.getModel().getProject(), routingValue);
        try {
            String rawInput = "";
            if(!CollectionUtils.isEmpty(singlePredictRequest.getRawInputs())) {
                rawInput = singlePredictRequest.getRawInputs().get(0);
            }

            Response response = null;
            boolean isBinaryInput = singlePredictRequest.getBinaryInput() != null && 0 != singlePredictRequest.getBinaryInput().length;
            if (!isBinaryInput) {
                ComputeRequest computeRequest = composeRaptorRequest(rawInput, singlePredictRequest.getModel().getModel(), routingValue.getProject_id());
                putTritonRequestToFptiEvent(aimlGatewayFptiEvent, computeRequest.getContext(), routingValue);
                response = webTarget.path(RAPTOR_PREDICT_PATH)
                        .request(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.json(computeRequest));
            } else {
                putTritonRequestToFptiEvent(aimlGatewayFptiEvent, rawInput, routingValue);
                // Add the binary data as a file part
                response = multipartPostQuery(singlePredictRequest.getBinaryInput(), singlePredictRequest.getModel(), rawInput, webTarget);
            }
            response.bufferEntity();
            String entityStr = response.readEntity(String.class);
            putResponseToFptiEvent(aimlGatewayFptiEvent, entityStr, routingValue.getBypassPayload());
            if (response.getStatus() == 200) {
                return retrieveTritonResponse(entityStr, isBinaryInput, singlePredictRequest.getModel());
            }
            logger.error("Request failed for model:{} with endpoint:{}, error msg:{}", routingValue.getModel(),
                    routingValue.getEndpoint(), entityStr);
            throw new GatewayException(entityStr, INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            CalLogHelper.logException(RAPTOR_PREDICT_ERROR, e);
            String msg = e instanceof GatewayException ? ((GatewayException) e).getErrorMessage() : e.getMessage();
            throw new GatewayException(msg, INTERNAL_SERVER_ERROR);
        }
    }

    private WebTarget getWebTarget(String project, RoutingValue routingValue) {
        // docai triton model
        if ("DocAI_Services".equals(project) && "tensorcomputegpuserv".equals(routingValue.getEndpoint())) {
            return raptorPoolMap.get("docaitensorcomputegpuserv");
        }
        if ("DocNet".equals(project) && "aimlmodelgpu2serv".equals(routingValue.getEndpoint())) {
            return raptorPoolMap.get("docnetaimlmodelgpu2serv");
        }

        WebTarget webTarget = raptorPoolMap.get(routingValue.getEndpoint());
        if(null != routingValue.getTimeout() && routingValue.getTimeout() > TEN_SECONDS && routingValue.getTimeout() <= ONE_MINUTE) {
            webTarget = raptorPoolMap.get(routingValue.getEndpoint() + RAPTOR_TIME_ONE_MINUTE);
        }
        if (webTarget == null) {
            throw new GatewayException("Pool is not registered in gateway.", INTERNAL_SERVER_ERROR);
        }
        return webTarget;
    }

    /**
     * Compose request payload and get raw result from Raptor model runner.
     * @param request prediction request
     * @param routingValue routing value
     * @return prediction result
     */
    private ComputeResponse getRaptorPredictRawResult(PredictRequest request, RoutingValue routingValue,
            final AimlGatewayFptiEvent aimlGatewayFptiEvent) {
        WebTarget webTarget = getWebTarget(request.getProject(), routingValue);
        try {
            ComputeRequest computeRequest = composeRaptorRequest(request.getInputs().get(0),
                    request.getModel(), routingValue.getProject_id());
            putTritonRequestToFptiEvent(aimlGatewayFptiEvent,
                    compressStringAndBase64Encode(computeRequest.getContext()), routingValue);
            Response response = webTarget.path(RAPTOR_PREDICT_PATH).request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.json(computeRequest));
            response.bufferEntity();
            String entityStr = response.readEntity(String.class);
            if (response.getStatus() == 200) {
                return retrieveComputeResponse(entityStr);
            }
            logger.error("Request failed for model:{} with endpoint:{}, error msg:{}",routingValue.getModel(),
                    routingValue.getEndpoint(), entityStr);
            throw new GatewayException(entityStr, INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            CalLogHelper.logException(RAPTOR_PREDICT_ERROR, e);
            String msg = e instanceof GatewayException ? ((GatewayException) e).getErrorMessage() : e.getMessage();
            throw new GatewayException(msg, INTERNAL_SERVER_ERROR);
        }
    }
}
