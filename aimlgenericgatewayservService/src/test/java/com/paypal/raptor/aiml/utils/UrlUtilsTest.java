package com.paypal.raptor.aiml.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static com.paypal.raptor.aiml.common.constants.GRpcConstants.GRPC_HEADER_NAMESPACE;
import static com.paypal.raptor.aiml.common.constants.GRpcConstants.GRPC_HEADER_SELDON;
import static com.paypal.raptor.aiml.utils.UrlUtils.parseSeldonUrl;

public class UrlUtilsTest {
    @Test
    public void parseSeldonUrlTest() {

        String seldonUrlStr = "http://10.176.24.85:30966/seldon/seldon/sheena-test-v1-255dc/api/v0.1/predictions";
        Map<String, String> result = parseSeldonUrl(seldonUrlStr);
        Assert.assertNotNull(result);
        Assert.assertEquals("seldon", result.get(GRPC_HEADER_NAMESPACE));
        Assert.assertEquals("sheena-test-v1-255dc", result.get(GRPC_HEADER_SELDON));

        seldonUrlStr = "http://aiplatform.ccg24-hrz-gke-generic.ccg24.lvs.paypalinc.com/seldon/seldon/sheena-test-v1-255dc/api/v0.1/predictions";
        result = parseSeldonUrl(seldonUrlStr);
        Assert.assertNotNull(result);
        Assert.assertEquals("seldon", result.get(GRPC_HEADER_NAMESPACE));
        Assert.assertEquals("sheena-test-v1-255dc", result.get(GRPC_HEADER_SELDON));
    }
}
