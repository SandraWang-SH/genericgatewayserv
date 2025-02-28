package com.paypal.raptor.aiml.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.seldon.protos.Meta;
import io.seldon.protos.SeldonMessage;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import org.tensorflow.framework.TensorProto;
import tensorflow.serving.Predict;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler.getCorrelationId;

public class ProtobufUtils {
    public static String toJson(Message message) {
        if (message == null) {
            return "";
        }

        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static <T extends Message> T toBean(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        try {
            final Method method = clazz.getMethod("newBuilder");
            final Message.Builder builder = (Message.Builder) method.invoke(null);

            JsonFormat.parser().merge(json, builder);

            return (T) builder.build();
        } catch (Exception e) {
            throw new RuntimeException("ProtobufUtils toMessage happen error, class: " + clazz + ", json: " + json, e);
        }
    }

    public static SeldonMessage toSeldonMessage(byte[] binary) {
        if(null == binary || 0 == binary.length) {
            return null;
        }
        Value corrId = Value.newBuilder().setStringValue(getCorrelationId()).build();
        Meta meta = Meta.newBuilder().putTags("corrId", corrId)
                .build();
        return SeldonMessage.newBuilder().setBinData(ByteString.copyFrom(binary)).setMeta(meta).build();
    }

    // add binary input to request
    public static Predict.PredictRequest toTensorPredict(byte[] binary, Predict.PredictRequest request) {
        if(null == binary || 0 == binary.length) {
            return null;
        }

        List<Predict.PredictRequest> requestList = new ArrayList<>();
        request.getInputsMap().forEach((k,v)->{
            TensorProto tensorProto = TensorProto.newBuilder()
                    .setTensorContent(ByteString.copyFrom(binary))
                    .setTensorShape(v.getTensorShape())
                    .setDtype(v.getDtype()).build();
            Predict.PredictRequest predictRequest = Predict.PredictRequest.newBuilder().setModelSpec(request.getModelSpec())
                    .putInputs(k, tensorProto).build();
            requestList.add(predictRequest);
        });

        return CollectionUtils.isEmpty(requestList) ? null:requestList.get(0);
    }

}
