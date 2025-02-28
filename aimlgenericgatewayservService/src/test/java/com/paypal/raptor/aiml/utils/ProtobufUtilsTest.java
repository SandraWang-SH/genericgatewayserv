package com.paypal.raptor.aiml.utils;

import io.seldon.protos.Feedback;
import io.seldon.protos.SeldonMessage;
import org.junit.Assert;
import org.junit.Test;
import java.util.regex.Pattern;

public class ProtobufUtilsTest {
    @Test
    public void toJsonTest() {

        String json = "{\"data\":{\"names\":[\"0\",\"1\",\"2\",\"3\",\"4\",\"5\"],\"ndarray\":[[0.0,0.0,0.0,0.0,0.0,1.0]]}}";
        SeldonMessage message = ProtobufUtils.toBean(json, SeldonMessage.class);

        Assert.assertEquals(6, message.getData().getNamesCount());
        Assert.assertEquals(1, message.getData().getNdarray().getValuesCount());

        String result = ProtobufUtils.toJson(message);

        Pattern p = Pattern.compile( "\\s*|\t|\r|\n" );
        result = p.matcher(result).replaceAll( "" );
        json = p.matcher(json).replaceAll( "" );;

        Assert.assertEquals(json, result);

        String jsonFeedback = "{\"request\":{\"data\":{\"ndarray\":[[0.0,0.0,0.0]]}},\"response\":{\"meta\":{\"requestPath\":{\"b\":\"dockerhub.paypalcorp.com/aiplatform/mymodel:0.0.5\"},\"routing\":{\"b\":-1,\"mab\":1}},\"data\":{\"names\":[\"t:0\",\"t:1\",\"t:2\"],\"ndarray\":[[0.0,0.0,0.0]]}},\"reward\":1,\"truth\":{\"data\":{\"ndarray\":[1]}}}";
        Feedback feedback = ProtobufUtils.toBean(jsonFeedback, Feedback.class);
        result = ProtobufUtils.toJson(feedback);

        Assert.assertNotNull(result);

    }

}
