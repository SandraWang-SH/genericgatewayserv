package com.paypal.raptor.aiml.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.raptor.aiml.common.enums.SeldonProtocol;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.grpc.HeaderClientInterceptor;
import com.paypal.raptor.aiml.model.RoutingValue;
import io.grpc.*;
import io.seldon.protos.Feedback;
import io.seldon.protos.SeldonGrpc;
import io.seldon.protos.SeldonMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;

import java.util.List;
import java.util.Map;

import static com.paypal.raptor.aiml.common.constants.GRpcConstants.*;
import static com.paypal.raptor.aiml.common.constants.GRpcConstants.ClusterName.*;
import static com.paypal.raptor.aiml.common.enums.SeldonProtocol.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.ifProd;
import static com.paypal.raptor.aiml.utils.UrlUtils.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.getProtocolFromURL;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Component
public class GRpcServiceManager {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    //https://stackoverflow.com/questions/42320492/do-i-need-to-pool-managedchannel-instances-for-a-multithreaded-java-grpc-1-1-2
    private ManagedChannel originChannelHRZ;
    private ManagedChannel originChannelHRZANA;
    private ManagedChannel originChannelGKEDEV;
    private PredictionServiceGrpc.PredictionServiceBlockingStub tfStub;
    private SeldonGrpc.SeldonBlockingStub seldonStub;

    //@PostConstruct
    public void init() {
        boolean isProd = ifProd();
        if (isProd) {
            originChannelHRZ = Grpc
                    .newChannelBuilderForAddress(HRZ_GRPC_HOST, 443, TlsChannelCredentials.create())
                    .overrideAuthority(HRZ_GRPC_HOST)
                    .build();
        } else {
            originChannelGKEDEV = Grpc
                    .newChannelBuilderForAddress(GKE_DVE_GRPC_HOST, 443, TlsChannelCredentials.create())
                    .overrideAuthority(GKE_DVE_GRPC_HOST)
                    .build();
        }
    }

    private void checkChannel(String bindCluster) {
        boolean isProd = ifProd();
        if(isProd && HRZ.name().equalsIgnoreCase(bindCluster) &&
                (null == originChannelHRZ || originChannelHRZ.isShutdown() || originChannelHRZ.isTerminated())) {
            originChannelHRZ = Grpc
                    .newChannelBuilderForAddress(HRZ_GRPC_HOST, 443, TlsChannelCredentials.create())
                    .overrideAuthority(HRZ_GRPC_HOST)
                    .build();
        }
        if(isProd && GKE.name().equalsIgnoreCase(bindCluster) &&
                (null == originChannelHRZANA || originChannelHRZANA.isShutdown() || originChannelHRZANA.isTerminated())) {
            originChannelHRZANA = Grpc
                    .newChannelBuilderForAddress(HRZ_ANA_GRPC_HOST, 443, TlsChannelCredentials.create())
                    .overrideAuthority(HRZ_ANA_GRPC_HOST)
                    .build();
        }
        if(!isProd && (GKE.name().equalsIgnoreCase(bindCluster) || HRZ.name().equalsIgnoreCase(bindCluster)) &&
                (null == originChannelGKEDEV || originChannelGKEDEV.isShutdown() || originChannelGKEDEV.isTerminated())) {
            originChannelGKEDEV = Grpc
                    .newChannelBuilderForAddress(GKE_DVE_GRPC_HOST, 443, TlsChannelCredentials.create())
                    .overrideAuthority(GKE_DVE_GRPC_HOST)
                    .build();
        }
    }

    Channel getChannelWithHeaders(String namespaceVal, String seldonVal, String bindCluster) {
        ClientInterceptor interceptor = new HeaderClientInterceptor(namespaceVal, seldonVal);
        Channel channel = null;
        boolean isProd = ifProd();
        if(isProd && HRZ.name().equalsIgnoreCase(bindCluster)) {
            channel = ClientInterceptors.intercept(originChannelHRZ, interceptor);
        } else if(isProd && GKE.name().equalsIgnoreCase(bindCluster)) {
            channel = ClientInterceptors.intercept(originChannelHRZANA, interceptor);
        } else if(!ifProd() && (GKE.name().equalsIgnoreCase(bindCluster) || HRZ.name().equalsIgnoreCase(bindCluster))) {
            channel = ClientInterceptors.intercept(originChannelGKEDEV, interceptor);
        } else {
            throw new GatewayException("Get channel with headers error. The bind cluster is not right.", INTERNAL_SERVER_ERROR);
        }
        return channel;
    }

    public String feedback(String jsonSeldon) {
        Feedback seldonFeedback = ProtobufUtils.toBean(jsonSeldon, Feedback.class);
        SeldonMessage seldonResponse = seldonStub.sendFeedback(seldonFeedback);
        return ProtobufUtils.toJson(seldonResponse);
    }

    public Map<String, Object> executeSeldonGRpc(String jsonSeldonPredict, byte[] binaryInput) {
        try {
            SeldonMessage seldonRequest = null;
            if(null != binaryInput && 0 != binaryInput.length) {
                seldonRequest = ProtobufUtils.toSeldonMessage(binaryInput);
            } else if(!StringUtils.isEmpty(jsonSeldonPredict)) {
                seldonRequest = ProtobufUtils.toBean(jsonSeldonPredict, SeldonMessage.class);
            }
            SeldonMessage seldonResponse = seldonStub.predict(seldonRequest);
            String responseJson = ProtobufUtils.toJson(seldonResponse);
            return gson.fromJson(responseJson, Map.class);
        } catch (StatusRuntimeException e) {
            throw new GatewayException("Call seldon grpc error. Message: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> executeTensorflowGRpc(String jsonTensorflowPredict, byte[] binaryInput) {
        try {
            Predict.PredictRequest request = null;
            if(!StringUtils.isEmpty(jsonTensorflowPredict)) {
                request = ProtobufUtils.toBean(jsonTensorflowPredict, Predict.PredictRequest.class);
            }
            if(null != binaryInput && 0 != binaryInput.length) {
                request = ProtobufUtils.toTensorPredict(binaryInput, request);
            }
            Predict.PredictResponse response = tfStub.predict(request);
            String responseJson = ProtobufUtils.toJson(response);
            return gson.fromJson(responseJson, Map.class);
        } catch (StatusRuntimeException e) {
            throw new GatewayException("Call tensorflow grpc error. Message: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> grpcQuery(List<String> rawInputs, byte[] binaryInputs, RoutingValue routingValue) {
        Map<String, String> headers = parseSeldonUrl(routingValue.getUrl());
        SeldonProtocol protocol = getProtocolFromURL(routingValue.getUrl());
        checkChannel(routingValue.getBindCluster());
        Channel channel = getChannelWithHeaders(headers.get(GRPC_HEADER_NAMESPACE), headers.get(GRPC_HEADER_SELDON),
                routingValue.getBindCluster());
        String rawInput = null == rawInputs || StringUtils.isEmpty(rawInputs.get(0)) ? null:rawInputs.get(0);
        if (protocol.equals(seldon)) {
            seldonStub = SeldonGrpc.newBlockingStub(channel);
            return executeSeldonGRpc(rawInput, binaryInputs);
        }
        if (protocol.equals(tensorflow)) {
            tfStub = PredictionServiceGrpc.newBlockingStub(channel);
            return executeTensorflowGRpc(rawInput, binaryInputs);
        }
        return null;
    }

    public void close() {
        originChannelHRZ.shutdown();
        originChannelHRZANA.shutdown();
        originChannelGKEDEV.shutdown();
    }

}
