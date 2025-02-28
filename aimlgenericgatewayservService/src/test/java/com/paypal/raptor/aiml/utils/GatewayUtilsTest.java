package com.paypal.raptor.aiml.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Maps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.edm.computeschema.ComputeRequest;
import com.paypal.edm.computeschema.ComputeResponse;
import com.paypal.raptor.aiml.model.ExplainRequest;
import com.paypal.raptor.aiml.model.FeedbackRequest;
import com.paypal.raptor.aiml.model.FeedbackResponse;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.RoutingValue;

import static com.paypal.raptor.aiml.utils.GatewayUtils.*;


/**
 * @author: qingwu
 **/
public class GatewayUtilsTest {
	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	@Test
	public void composeSeldonPredictPayloadTest() throws JsonProcessingException {
		// v2 JSON
		PredictRequest request = new PredictRequest();
		request.addInputsItem("{\"parameters\":{\"content_type\":\"base64\"},"
				+ "\"name\":\"predict\",\"datatype\":\"BYTES\",\"data\":[{\"message_id\":238303376,\"sender\":{\"account_id\":\"G42ZUNWUQMNAG\"},\"message_payload\":{\"type\":\"TEXT\",\"message_text\":\"You are fat.\"},\"ChatChannel\":{\"channel_id\":\"CID-XHTKKQQYYUUIP\",\"members\":[{\"account_id\":\"G42ZUNWUQMNAG\"},{\"account_id\":\"N9ZXXK2PVTFWN\"}]}}],\"shape\":[2]}");
		request.addInputsItem("{\"parameters\":{\"content_type\":\"base64\"},"
				+ "\"name\":\"predict\",\"datatype\":\"BYTES\",\"data\":[{\"message_id\":238303376,\"sender\":{\"account_id\":\"G42ZUNWUQMNAG\"},\"message_payload\":{\"type\":\"TEXT\",\"message_text\":\"You are fat.\"},\"ChatChannel\":{\"channel_id\":\"CID-XHTKKQQYYUUIP\",\"members\":[{\"account_id\":\"G42ZUNWUQMNAG\"},{\"account_id\":\"N9ZXXK2PVTFWN\"}]}}],\"shape\":[2]}");
		request.setInputType("JSON");
		request.setParameters("{\"parameters\":{\"content_type\":\"base64\"},\"id\":\"e3189000-8140-4fa1-85f8-db90fcbee102\",\"outputs\":[]}");
		RoutingValue routingValue = new RoutingValue().setUrl("/seldon/seldon/triton/v2/models/simple/infer");
		Map<String, Object> payload = composeSeldonPredictPayload(request, routingValue);
		Assert.assertEquals(2,((List)payload.get("inputs")).size());
		Assert.assertNotNull(payload.get("outputs"));
		Assert.assertNotNull(payload.get("parameters"));
		Assert.assertNotNull(payload.get("id"));

		// tensorflow RAW
		PredictRequest request2 = new PredictRequest();
		request2.addInputsItem( "{\"instances\": [1.0, 2.0, 5.0]}");
		request2.setInputType("RAW");
		RoutingValue routingValue2 = new RoutingValue().setUrl("/seldon/seldon/example-tfserving/v1/models/halfplustwo/:predict");
		Map<String, Object> payload2 = composeSeldonPredictPayload(request2, routingValue2);
		Assert.assertEquals(3,((List)payload2.get("instances")).size());
	}

	@Test
	public void composeSeldonPredictResponseTest() {
		Map<String, Object> resultMap = Maps.newHashMap();
		resultMap.put("model_name","testModel");
		resultMap.put("model_version","1");
		resultMap.put("id","abc");
		resultMap.put("parameters",new HashMap<String, Object>() {
			{
				put("content_type","b64");
			}
		});
		resultMap.put("outputs", Arrays.asList(gson.fromJson("{\"name\":\"predict\",\"shape\":[2],\"datatype\":\"BYTES\","
				+ "\"parameters\":null,\"data\":\"[{\\\"INHOUSE-TEXT-TOXIC\\\": {\\\"label\\\": true, \\\"score\\\": 0"
				+ ".9976, \\\"meta_data\\\": {}, \\\"response_uuid\\\": \\\"c82ada62-4012-4afd-a223-839034d1b7d3\\\", "
				+ "\\\"response_status\\\": {\\\"code\\\": 200, \\\"msg\\\": \\\"ok\\\"}}}, {\\\"INHOUSE-TEXT-TOXIC\\\": "
				+ "{\\\"label\\\": true, \\\"score\\\": 0.9976, \\\"meta_data\\\": {}, \\\"response_uuid\\\": "
				+ "\\\"033dae14-4889-4080-9fa0-f588faec1b93\\\", \\\"response_status\\\": {\\\"code\\\": 200, "
				+ "\\\"msg\\\": \\\"ok\\\"}}}]\"}",Map.class)));
		PredictRequest request = new PredictRequest();
		request.setInputType("JSON");
		request.setEndpoint("endpoint");
		PredictResponse response = composeSeldonPredictResponse(resultMap, request);
		Assert.assertEquals("endpoint",response.getEndpoint());
		Assert.assertEquals(1, response.getResults().size());
		Assert.assertEquals("testModel", response.getModelName());
	}


	@Test
	public void getFeedbackUrlTest() {
		String feedbackUrl = getFeedbackUrl("http://10.176.24.85:30966/seldon/seldon/aaa/api/v0.1/predictions");
		Assert.assertEquals("http://10.176.24.85:30966/seldon/seldon/aaa/api/v0.1/feedback", feedbackUrl);
		String feedbackUrl2 = getFeedbackUrl("http://10.176.24.85:30966/seldon/seldon/aaa/v1/models/:predict");
		Assert.assertEquals("http://10.176.24.85:30966/seldon/seldon/aaa/api/v1.0/feedback", feedbackUrl2);
	}

	@Test
	public void composeRaptorRequestTest() throws JsonProcessingException {
		ComputeRequest computeRequest = composeRaptorRequest("{}","modelA","aaa");
		Assert.assertEquals(1,computeRequest.getComputeUnits().size());
		Assert.assertEquals("aaa___modelA",computeRequest.getComputeUnits().get(0).getId());
		ComputeRequest computeRequest2 = composeRaptorRequest("{}","modelA",null);
		Assert.assertEquals("modelA",computeRequest2.getComputeUnits().get(0).getId());
	}

	@Test
	public void retrieveComputeResponseTest() throws JsonProcessingException {
		String str = "{\n"
				+ "    \"return_code\": {\n"
				+ "        \"code\": \"STATUS_FAILURE\"\n"
				+ "    },\n"
				+ "    \"compute_unit_responses\": [\n"
				+ "        {\n"
				+ "            \"id\": \"qingwu_bank_0830\",\n"
				+ "            \"status\": \"FAILURE\",\n"
				+ "            \"model_latency\": 0\n"
				+ "        }\n"
				+ "    ]\n"
				+ "}";
		ComputeResponse response = retrieveComputeResponse(str);
		Assert.assertEquals(0,response.getComputeUnitResponses().get(0).getAdditionalProperties().get(
				"model_latency"));
		PredictRequest request = new PredictRequest();
		request.setEndpoint("endpoint");
		request.setInputType("RAW");
		PredictResponse predictResponse = composeRaptorPredictResponse(response, request, null, null);
		Assert.assertEquals("qingwu_bank_0830", predictResponse.getModelName());
		Assert.assertEquals("FAILURE",predictResponse.getStatus());
		Assert.assertNull(predictResponse.getModelVersion());
	}

	@Test
	public void composeSeldonFeedbackResponseTest() {
		FeedbackRequest request = new FeedbackRequest();
		request.setEndpoint("endpoint");
		FeedbackResponse feedbackResponse = composeSeldonFeedbackResponse(Maps.newHashMap(),request);
		Assert.assertEquals("SUCCESS",feedbackResponse.getStatus());
		Assert.assertEquals("endpoint", feedbackResponse.getEndpoint());
	}

	@Test
	public void retrieveRealModelNameTest() {
		String modelName = "aaa___model_1";
		Assert.assertEquals("model_1",retrieveRealModelName(modelName));
		String modelName2 = "model_2";
		Assert.assertEquals("model_2",retrieveRealModelName(modelName2));
		String projectId = "aaa";
		String nameWithProjectId = generateModelNameWithProjectId(retrieveRealModelName(modelName),projectId);
		Assert.assertEquals(modelName,nameWithProjectId);
	}

	@Test
	public void generateRoutingKeyStringTest() throws IllegalAccessException {
		PredictRequest predictRequest = new PredictRequest();
		predictRequest.setProject("p");
		predictRequest.setModel("m");
		Assert.assertEquals("p:null:m", generateCalNameForEndpoint(predictRequest));

		ExplainRequest explainRequest = new ExplainRequest();
		explainRequest.setProject("pp");
		explainRequest.setEndpoint("ee");
		explainRequest.setModel(null);
		Assert.assertEquals("pp:ee:null", generateCalNameForEndpoint(explainRequest));

		FeedbackRequest feedbackRequest = new FeedbackRequest();
		feedbackRequest.setProject("ppp");
		feedbackRequest.setEndpoint("eee");
		Assert.assertEquals("ppp:eee:null", generateCalNameForEndpoint(feedbackRequest));
	}

	@Test
	public void parseRoutingKeyTest() {
		Map<String,String> mapRoutingKey = parseRoutingKey("ppp:eee:null");
		Assert.assertEquals(mapRoutingKey.get("project"), "ppp");
		Assert.assertEquals(mapRoutingKey.get("endpoint"), "eee");
		Assert.assertEquals(mapRoutingKey.get("model"), "");
	}

	@Test
	public void parseClientNameTest() {
		String pool = null;
		Assert.assertNull(parseClientName(pool));
		pool = "stage2msmaster_cssearchserv:v1.analytics"
						+ ".predict-model*CalThreadId\\u003d115*TopLevelTxnStartTime\\u003d1845ba98e95*Host\\u003dmsmaster5int"
						+ "-g_cssearchserv_0*pid\\u003d4472";
		Assert.assertEquals("stage2msmaster_cssearchserv",parseClientName(pool));
		pool = "paymentserv";
		Assert.assertEquals("paymentserv",parseClientName(pool));
		pool = "edmcompcomputeserv:v1.analytics.predict-model*CalThreadId=52918*TopLevelTxnStartTime=1845c13438e*Host"
						+ "=dcg02edmcompcomputeserv1*pid=5805*SpanId=9ba9205cb0687da3";
		Assert.assertEquals("edmcompcomputeserv",parseClientName(pool));

	}

	@Test
	public void resetPredictUrlTest() {
		//seldon
		String predictUrl = "https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sheena-test-v12-bdedc/api/v0.1/predictions";
		String result = convert2ValidUrl(parseSeldonProto(predictUrl), null, predictUrl, false);
		Assert.assertEquals(predictUrl, result);

		//v2
		predictUrl = "https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sywu-multimodel/v2/models/bully-text/infer";
		String realUrl = "https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sywu-multimodel/v2/models/toxic-text/infer";
		String model = "toxic-text";
		result = convert2ValidUrl(parseSeldonProto(predictUrl), model, predictUrl, false);
		Assert.assertEquals(realUrl, result);

		//tensorflow
		predictUrl = "https://aiplatform.dev51.cbf.dev.paypalinc.com/seldon/seldon/sheena-test-v12-bdedc/v1/models/:predict";
		result = convert2ValidUrl(parseSeldonProto(predictUrl), null, predictUrl, false);
		Assert.assertEquals(predictUrl, result);
	}

}
