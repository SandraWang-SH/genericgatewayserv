package com.paypal.raptor.aiml.utils;

import static com.paypal.edm.computeschema.ReturnCode.Code.STATUS_SUCCESS;
import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GRpcConstants.GRPC_HEADER_SELDON;
import static com.paypal.raptor.aiml.common.constants.GRpcConstants.V2_MODEL_NAME;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.APP_JSON;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.CONTENT_TYPE;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.DEFAULT_USECASE;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.MULTIPART;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.SELDON_PROTOCOL_KEY;
import static com.paypal.raptor.aiml.common.enums.InputType.JSON;
import static com.paypal.raptor.aiml.common.enums.InputType.RAW;
import static com.paypal.raptor.aiml.common.enums.SeldonProtocol.seldon;
import static com.paypal.raptor.aiml.common.enums.SeldonProtocol.tensorflow;
import static com.paypal.raptor.aiml.common.enums.SeldonProtocol.v2;
import static com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler.getCorrelationId;
import static com.paypal.raptor.aiml.utils.CommonUtils.compressStringAndBase64Encode;
import static com.paypal.raptor.aiml.utils.CommonUtils.putIfNotNull;
import static com.paypal.raptor.aiml.utils.UrlUtils.parseSeldonUrl;

import com.ebay.aero.kernel.context.AppBuildConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.paypal.api.platform.insights.model.DecisionContext;
import com.paypal.edm.computeschema.ComputeRequest;
import com.paypal.edm.computeschema.ComputeResponse;
import com.paypal.edm.computeschema.ComputeUnit;
import com.paypal.edm.computeschema.ComputeUnitResponse;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.enums.AimlGatewayFptiTag;
import com.paypal.raptor.aiml.common.enums.SeldonProtocol;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.FeedbackRequest;
import com.paypal.raptor.aiml.model.FeedbackResponse;
import com.paypal.raptor.aiml.model.Model;
import com.paypal.raptor.aiml.model.PredictModelsRequest;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.model.PredictResponse;
import com.paypal.raptor.aiml.model.PredictResult;
import com.paypal.raptor.aiml.model.RoutingKey;
import com.paypal.raptor.aiml.model.RoutingValue;
import com.paypal.raptor.aiml.model.TritonResponse;
import com.paypal.raptor.aiml.service.impl.PredictServiceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;



/**
 * Utils for gateway related business.
 * @author: qingwu
 **/
public class GatewayUtils {

	private static final Logger logger = LoggerFactory.getLogger(PredictServiceImpl.class);

	private static final String PROJECT_ID_SEP = "___";

	private static final ObjectMapper mapper = new ObjectMapper()
			.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	public static ComputeRequest composeRaptorRequest(String decisionContextStr, String modelName, String projectId) throws JsonProcessingException {
		ComputeRequest request = new ComputeRequest();
		request.setCapability("Analytics");
		ComputeUnit computeUnit = new ComputeUnit();
		computeUnit.setId(generateModelNameWithProjectId(modelName, projectId));
		request.setComputeUnits(Collections.singletonList(computeUnit));
		Map<String, Object> context = Maps.newHashMap();
		DecisionContext decisionContext = new DecisionContext();
		if (decisionContextStr.contains("domain_data")) {
			// raw decision context as input
			decisionContext = mapper.readValue(decisionContextStr, DecisionContext.class);
		} else {
			// raw domain data as input
			decisionContext.setUsecaseId(DEFAULT_USECASE);
			decisionContext.setDomainData(decisionContextStr);
		}
		context.put("decision_context", decisionContext);
		request.setContext(mapper.writeValueAsString(context));
		return request;
	}

	public static String composeTritonMultipartContext(String decisionContextStr) throws JsonProcessingException {
		Map<String, Object> context = Maps.newHashMap();
		DecisionContext decisionContext = new DecisionContext();
		if (decisionContextStr.contains("domain_data")) {
			// raw decision context as input
			decisionContext = mapper.readValue(decisionContextStr, DecisionContext.class);
		} else {
			// raw domain data as input
			decisionContext.setUsecaseId(DEFAULT_USECASE);
			decisionContext.setDomainData(decisionContextStr);
		}
		context.put("decision_context", decisionContext);
		return mapper.writeValueAsString(context);
	}

	public static ComputeResponse retrieveComputeResponse(String entityStr) throws JsonProcessingException {
		ComputeResponse computeResponse = mapper.readValue(entityStr, ComputeResponse.class);
		return computeResponse;
	}

	public static PredictResult retrieveTritonResponse(String entityStr, boolean isBinaryInput, Model model) throws JsonProcessingException {
		PredictResult predictResult = new PredictResult();
		predictResult.setModel(model.getModel());
		predictResult.setEndpoint(model.getEndpoint());
		predictResult.setProject(model.getProject());
		predictResult.setStatus("SUCCESS");
		if(!isBinaryInput){
			ComputeResponse computeResponse = mapper.readValue(entityStr, ComputeResponse.class);
			ComputeUnitResponse computeUnitResponse = computeResponse.getComputeUnitResponses().get(0);
			if (STATUS_SUCCESS.equals(computeResponse.getReturnCode().getCode())) {
				predictResult.setStatus("SUCCESS");
				predictResult.setResults(Collections.singletonList(computeUnitResponse.getValue()));
			} else {
				predictResult.setStatus("FAILURE");
				entityStr = computeResponse.toString();
				try {
					entityStr = mapper.writeValueAsString(computeUnitResponse.getAdditionalProperties());
				} catch (JsonProcessingException e) {
					logger.error("Failed to transfer computeUnitResponse to string.", e);
				}
				CalLogHelper.logException(RAPTOR_PREDICT_ERROR, entityStr);
				predictResult.setResults(Collections.singletonList(entityStr));
			}
			return predictResult;
		}
		TritonResponse tritonResponse = mapper.readValue(entityStr, TritonResponse.class);
		if(tritonResponse.getOutputs() != null && tritonResponse.getOutputs().size() >= 1 && tritonResponse.getOutputs().get(0).getData() != null && tritonResponse.getOutputs().get(0).getData().size() >= 1){
			predictResult.setResults(tritonResponse.getOutputs().stream().map(data -> {
				try {
					return mapper.writeValueAsString(data);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList()));
		} else{
			predictResult.setStatus("FAILURE");
			predictResult.setResults(Collections.singletonList(mapper.writeValueAsString(tritonResponse)));
		}
		return predictResult;
	}

	public static PredictResponse composeRaptorPredictResponse(final ComputeResponse computeResponse,
			final PredictRequest request, RoutingValue routingValue, final AimlGatewayFptiEvent aimlGatewayFptiEvent) {
		if (null != routingValue && (null == routingValue.getBypassPayload() || !routingValue.getBypassPayload())) {
			String rspVal = StringUtils.join(computeResponse.getComputeUnitResponses().stream()
					.map(ComputeUnitResponse::getValue).collect(Collectors.toList()), ",");
			putResponseToFptiEvent(aimlGatewayFptiEvent, rspVal, routingValue.getBypassPayload());
		}

		PredictResponse response = new PredictResponse();
		if (RAW.name().equals(request.getInputType())) {
			response.setEndpoint(request.getEndpoint());
			ComputeUnitResponse computeUnitResponse = computeResponse.getComputeUnitResponses().get(0);
			if (STATUS_SUCCESS.equals(computeResponse.getReturnCode().getCode())) {
				response.setStatus("SUCCESS");
				response.setResults(Collections.singletonList(computeUnitResponse.getValue()));
			} else {
				response.setStatus("FAILURE");
				String entityStr = computeResponse.toString();
				try {
					entityStr = mapper.writeValueAsString(computeUnitResponse.getAdditionalProperties());
				} catch (JsonProcessingException e) {
					logger.error("Failed to transfer computeUnitResponse to string.", e);
				}
				CalLogHelper.logException(RAPTOR_PREDICT_ERROR, entityStr);
			}
			response.setModelName(retrieveRealModelName(computeUnitResponse.getId()));
			return response;
		} else if (JSON.name().equals(request.getInputType())) {
			// TODO: support JSON list response, currently both modelrunner and triton do not support list<domain_data> for
			// single model
			return response;
		}
		return response;
	}

	public static PredictResponse composeSeldonPredictResponse(final Map<String, Object> predictResult,
															   final PredictRequest request) {
		PredictResponse response = new PredictResponse();
		// default and tensorflow protocol
		if (RAW.name().equals(request.getInputType())) {
			response.setResults(Collections.singletonList(gson.toJson(predictResult)));
		} else if (JSON.name().equals(request.getInputType())) {
			// v2 protocol
			response.setModelName((String)predictResult.get("model_name"));
			response.setModelVersion((String)predictResult.get("model_version"));
			Map<String, Object> meta = Maps.newHashMap();
			putIfNotNull("id",predictResult.get("id"),meta);
			putIfNotNull("parameters",predictResult.get("parameters"),meta);
			try{
				response.setMeta(mapper.writeValueAsString(meta));
			} catch (JsonProcessingException ignore) {
			}
			List<String> results =
					(List<String>) ((List)predictResult.get("outputs")).stream().map(a -> gson.toJson(a)).collect(Collectors.toList());
			response.setResults(results);
		}
		response.setEndpoint(request.getEndpoint());
		response.status("SUCCESS");
		return response;
	}

	public static PredictResult composeSeldonPredictResponse(Model model, final Map<String, Object> predictResult) {
		PredictResult result = new PredictResult();
		result.setProject(model.getProject());
		result.setEndpoint(model.getEndpoint());

		SeldonProtocol seldonProtocol = (SeldonProtocol) predictResult.get(SELDON_PROTOCOL_KEY);
		predictResult.remove(SELDON_PROTOCOL_KEY);

		if (seldon.equals(seldonProtocol) || tensorflow.equals(seldonProtocol) || null == seldonProtocol) {
			result.setResults(Collections.singletonList(gson.toJson(predictResult)));
		} else if (v2.equals(seldonProtocol)) {
			result.setModel((String)predictResult.get("model_name"));
			result.setModelVersion((String)predictResult.get("model_version"));
			Map<String, Object> meta = Maps.newHashMap();
			putIfNotNull("id",predictResult.get("id"),meta);
			putIfNotNull("parameters",predictResult.get("parameters"),meta);
			try{
				result.setMeta(mapper.writeValueAsString(meta));
			} catch (JsonProcessingException ignore) {
			}
			List<String> results =
					(List<String>) ((List)predictResult.get("outputs")).stream().map(a -> gson.toJson(a)).collect(Collectors.toList());
			result.setResults(results);
		}
		result.setStatus("SUCCESS");
		return result;
	}

	public static FeedbackResponse composeSeldonFeedbackResponse(final Map<String, Object> feedbackResult,
																 final FeedbackRequest request) {
		FeedbackResponse response = new FeedbackResponse();
		response.setEndpoint(request.getEndpoint());
		response.setStatus("SUCCESS");
		response.setFeedbackResult(gson.toJson(feedbackResult));
		return response;
	}

	public static Map<String, Object> composeV2Payload(String requestParameters, List<String> inputs)
            throws JsonProcessingException {
		Map<String, Object> v2Payload = Maps.newHashMap();
		if (null != requestParameters) {
			Map<String, Object> parameters = mapper.readValue(requestParameters, new TypeReference<HashMap<String, Object>>() {});
			if (MapUtils.isNotEmpty(parameters)) {
				putIfNotNull("id",parameters.get("id"),v2Payload);
				putIfNotNull("parameters",parameters.get("parameters"),v2Payload);
				putIfNotNull("outputs",parameters.get("outputs"),v2Payload);
			}
		}
		v2Payload.put("inputs", inputs.stream()
				.map(a -> {
                    try {
                        return mapper.readValue(a, new TypeReference<HashMap<String, Object>>() {});
                    } catch (JsonProcessingException e) {
						logger.info("json to map error. input:{}", a);
                        throw new RuntimeException(e);
                    }
                })
				.collect(Collectors.toList()));
		return v2Payload;
	}

  /**
   * Compose Seldon predict payload by protocol and input type.
   */
  public static Map<String, Object> composeSeldonPredictPayload(
      PredictRequest request, RoutingValue routingValue) throws JsonProcessingException {
	  String inputType = request.getInputType();
	  if (RAW.name().equals(inputType)) {
		  if (request.getInputs().size() != 1) {
			  throw new GatewayException("Should be only one input as RAW data payload.", Response.Status.BAD_REQUEST);
		  }
		  Map<String, Object> payload = mapper.readValue(request.getInputs().get(0), new TypeReference<HashMap<String, Object>>() {});
		  if (null == payload || payload.isEmpty()) {
			  logger.error("not support request:{}", request);
			  throw new GatewayException("Should be only null input json as RAW data payload.", Response.Status.BAD_REQUEST);
		  }
		  return mergeMeta(payload);
	  } else {
		  SeldonProtocol protocol = getProtocolFromURL(routingValue.getUrl());
		  if (protocol.equals(seldon) || protocol.equals(tensorflow)) {
			  throw new GatewayException(protocol.name() + " protocol doesn't support JSON input for now.",
					  Response.Status.BAD_REQUEST);
		  }
		  return composeV2Payload(request.getParameters(), request.getInputs());
	  }
  }

	public static Map<String, Object> composeSeldonPredictPayload(SinglePredictRequest singlePredictRequest, String url)
            throws JsonProcessingException {
		SeldonProtocol protocol = getProtocolFromURL(url);
		Map<String, Object> bodyPayload = Maps.newHashMap();
		if (seldon.equals(protocol)) { // support binary and raw data
			Map<String, String> correlationId = new HashMap<>();
			putIfNotNull("corrId", getCorrelationId(), correlationId);
			Map<String, Object> tags = new HashMap<>();
			putIfNotNull("tags", correlationId, tags);
			if(!CollectionUtils.isEmpty(singlePredictRequest.getRawInputs())) {
				bodyPayload = mapper.readValue(singlePredictRequest.getRawInputs().get(0), new TypeReference<HashMap<String, Object>>() {});
				bodyPayload = mergeMeta(bodyPayload);
				putIfNotNull(CONTENT_TYPE, APP_JSON, bodyPayload);
			}
			if(null != singlePredictRequest.getBinaryInput() && 0 != singlePredictRequest.getBinaryInput().length) {
				putIfNotNull("binData", singlePredictRequest.getBinaryInput(), bodyPayload);
				putIfNotNull("meta", gson.toJson(tags), bodyPayload);
				putIfNotNull(CONTENT_TYPE, MULTIPART, bodyPayload);
			}
		} else if (tensorflow.equals(protocol)) { // only support raw data
			if(null != singlePredictRequest.getBinaryInput()) {
				throw new GatewayException("Tensorflow protocol not support binary inputs. You can use base64 to encode your input and put it into the raw_inputs of the request.", Response.Status.BAD_REQUEST);
			}
			bodyPayload = mapper.readValue(singlePredictRequest.getRawInputs().get(0), new TypeReference<HashMap<String, Object>>() {});
			putIfNotNull(CONTENT_TYPE, APP_JSON, bodyPayload);
		} else if (v2.equals(protocol)) { // only support raw data
			if(null != singlePredictRequest.getBinaryInput()) {
				throw new GatewayException("V2 protocol not support binary inputs. You can use base64 to encode your input and put it into the raw_inputs of the request.", Response.Status.BAD_REQUEST);
			}
			bodyPayload = composeV2Payload(singlePredictRequest.getParameters(), singlePredictRequest.getRawInputs());
			putIfNotNull(CONTENT_TYPE, APP_JSON, bodyPayload);
		}
		putIfNotNull(SELDON_PROTOCOL_KEY, protocol, bodyPayload);
		return bodyPayload;
	}

	public static Map<String, Object> mergeMeta(Map<String, Object> payload) {
	  	if (null == payload) {
			payload = new HashMap<>();
		}

		Map<String, String> correlationId = new HashMap<>();
		putIfNotNull("corrId", getCorrelationId(), correlationId);
		Map<String, Object> tags = new HashMap<>();
		putIfNotNull("tags", correlationId, tags);
		if(null == payload.get("meta")) {
			putIfNotNull("meta", tags, payload);
			return payload;
		}

		Map<String, Object> payloadMeta = (Map<String, Object>) payload.get("meta");
		if(payloadMeta.get("tags") != null) {
			Map<String, Object> combineResultTag = new HashMap<>();
			combineResultTag.putAll(correlationId);
			combineResultTag.putAll((Map<? extends String, ?>) payloadMeta.get("tags"));
			putIfNotNull("tags", combineResultTag, tags);
			putIfNotNull("tags", combineResultTag, payloadMeta);
		}

		Map<String, Object> combineResultMap = new HashMap<>();
		combineResultMap.putAll(tags);
		combineResultMap.putAll(payloadMeta);
		putIfNotNull("meta", combineResultMap, payload);
	  	return payload;
	}

	public static Map<String, Object> composeSeldonFeedbackPayload(FeedbackRequest request) {
		return gson.fromJson(request.getFeedbackData(),Map.class);
	}

	public static void putTritonRequestToFptiEvent(final AimlGatewayFptiEvent aimlGatewayFptiEvent, String request,
											 RoutingValue routingValue) {
		if (null == routingValue.getBypassPayload() || !routingValue.getBypassPayload()) {
			try {
				aimlGatewayFptiEvent.putReq(compressStringAndBase64Encode(GatewayUtils.composeTritonMultipartContext(request)));
			} catch (IOException e) {
				logger.error("Failed to compress and encode request payload", e);
			}
		}
	}

	public static void putRequestToFptiEvent(RoutingValue routingValue, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			Map<String, Object> payload) {
		if (null == routingValue.getBypassPayload() || !routingValue.getBypassPayload()) {
			try {
				aimlGatewayFptiEvent.putReq(compressStringAndBase64Encode(new ObjectMapper().writeValueAsString(payload)));
			} catch (IOException e) {
				logger.error("Failed to compress and encode request payload", e);
			}
		}
	}

	public static void putRequestToFptiEvent(Boolean bypassPayload, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			String jsonBody) {
		if (null == bypassPayload || !bypassPayload) {
			try {
				aimlGatewayFptiEvent.putReq(compressStringAndBase64Encode(jsonBody));
			} catch (IOException e) {
				logger.error("Failed to compress and encode request payload", e);
			}
		}
	}

	public static void putIfNotExistRequestToFptiEvent(final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			final Object request, RoutingValue routingValue) {
	    if (null != aimlGatewayFptiEvent.get(AimlGatewayFptiTag.REQUEST) || null == request || null == routingValue) {
			return;
		}
		if (null == routingValue.getBypassPayload() || !routingValue.getBypassPayload()) {
			try {
				aimlGatewayFptiEvent.putReq(compressStringAndBase64Encode(gson.toJson(request)));
			} catch (IOException e) {
				logger.error("Failed to compress and encode request payload", e);
			}
		}
	}

	public static void putResponseToFptiEvent(final AimlGatewayFptiEvent aimlGatewayFptiEvent, String rspVal,
			Boolean bypassPayload) {
		if(null == bypassPayload || !bypassPayload) {
			try {
				aimlGatewayFptiEvent.putResp(compressStringAndBase64Encode(rspVal));
			} catch (IOException e) {
				logger.error("Failed to compress and encode predict result. Message:{}", e.getMessage());
			}
		}
	}

	public static void putResponseToFptiEvent(final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			RoutingValue routingValue, Map<String, Object> payload) {
		if(null == routingValue.getBypassPayload() || !routingValue.getBypassPayload()) {
			try {
				String rspVal = new ObjectMapper().writeValueAsString(payload);
				aimlGatewayFptiEvent.putResp(compressStringAndBase64Encode(rspVal));
			} catch (IOException e) {
				logger.error("Failed to compress and encode predict result. Message:{}", e.getMessage());
			}
		}
	}

	public static String getFeedbackUrl(String predictUrl) {
		if (predictUrl.contains("/v0.1/predictions")) {
			return predictUrl.replace("predictions","feedback");
		} else if (predictUrl.contains("/v1/models/")) {
			return predictUrl.replace("/v1/models/:predict","/api/v1.0/feedback");
		}
		//TODO v2 feedback?
		return null;
	}

	public static SeldonProtocol getProtocolFromURL(String predictUrl) {
		if (predictUrl.contains("/v2/models/")) {
			return v2;
		} else if (predictUrl.contains("/v1/models/")) {
			return tensorflow;
		} else {
			return seldon;
		}
	}

	public static String retrieveRealModelName(String modelName) {
		if (StringUtils.isEmpty(modelName)) {
			return "";
		}
		if (!modelName.contains(PROJECT_ID_SEP)) {
			return modelName;
		}
		return Splitter.on(PROJECT_ID_SEP).splitToList(modelName).get(1);
	}

	public static String generateModelNameWithProjectId(String modelName, String projectId) {
		if (!StringUtils.isEmpty(projectId)) {
			return projectId + PROJECT_ID_SEP + modelName;
		}
		return modelName;
	}

	public static String generateRoutingKeyString(RoutingKey key) {
		String project = key.getProject() == null ? "null" : key.getProject();
		String endpoint = key.getEndpoint() == null ? "null" : key.getEndpoint();
		String model = StringUtils.isEmpty(key.getModel()) ? "null" : key.getModel();
		return String.join(":", project, endpoint, model);
	}

	public static String generateRoutingKeyString(String routingProject, String routingEndpoint,
			String routingModel) {
		String project = routingProject == null ? "null" : routingProject;
		String endpoint = routingEndpoint == null ? "null" : routingEndpoint;
		String model = StringUtils.isEmpty(routingModel) ? "null" : routingModel;
		return String.join(":", project, endpoint, model);
	}

	public static String generateRoutingKeyString(String project, String endpoint) {
		project = project == null ? "null" : project;
		endpoint = endpoint == null ? "null" : endpoint;
		return String.join(":", project, endpoint);
	}

	public static String generateCalNameForMultiEndpoint(Object body) {
		StringBuilder target = new StringBuilder("multi-target");
	  	try {
			MultipartFormDataInput input = (MultipartFormDataInput) body;
			PredictModelsRequest predictModelsRequest = getRequestFromMultipart(input);
			if(CollectionUtils.isEmpty(predictModelsRequest.getModelList())) {
				return target.toString();
			}
			target = new StringBuilder();
			for(Model model:predictModelsRequest.getModelList()) {
				target.append(model.getProject()).append(":").append(model.getEndpoint()).append(":")
                        .append(model.getModel());
				target.append("|");
			}
			target = new StringBuilder(target.substring(0, target.length() - 1));
		} catch(GatewayException ignore) {
		}
		return target.toString();
	}

	public static String getTargetFromRequest(SinglePredictRequest singlePredictRequest) {
		String target = "";
		Model model = singlePredictRequest.getModel();
		if(null == model) {
			return target;
		}
		target += model.getProject() + ":" + model.getEndpoint() + ":" + model.getModel();
		return target;
	}

	public static String generateCalNameForEndpoint(Object body) {
		String project = "null";
		String endpoint = "null";
		String model = "null";
		Class clazz = body.getClass();
		Field[] fields = clazz.getDeclaredFields();
		try {
			for (Field field : fields) {
				if (field.getName().equals("project")) {
					field.setAccessible(true);
					project = (String)field.get(body);
				}
				if (field.getName().equals("endpoint")) {
					field.setAccessible(true);
					endpoint = (String)field.get(body);
				}
				if (field.getName().equals("model")) {
					field.setAccessible(true);
					model = (String)field.get(body);
				}
			}
		} catch (IllegalAccessException ignore) {
		}
		return String.join(":", project, endpoint, model);
	}

	public static Map<String,String> parseRoutingKey(String routingKey) {
		List<String> list = Splitter.on(":").splitToList(routingKey);
		Map<String, String> map = Maps.newHashMap();
		map.put("project", list.get(0));
		map.put("endpoint", list.get(1));
		if(list.size() > 2) {
			map.put("model", "null".equals(list.get(2)) ? "" : list.get(2));
		}
		return map;
	}

	public static String parseClientName(String poolStack) {
		if (StringUtils.isEmpty(poolStack)) {
			return poolStack;
		}
		return Splitter.on(":").splitToList(poolStack).get(0);
	}

    /*
	Get the correct prediction url.
	*/
	public static String parseSeldonProto(String originPredictUrl) {
		if (null != originPredictUrl && originPredictUrl.contains("|")) {
			String[] urlList = originPredictUrl.split("\\|");
			if (urlList.length < 2) {
				return originPredictUrl;
			}
			originPredictUrl = urlList[0];
		}

		String proto = "seldon";
		Matcher m = null;
		// a sample prediction URL of seldon protocol: https://.../seldon/seldon/<deploy-name>/api/v0.1/predictions
		m = Pattern.compile("(.+)(/seldon/)(.+)(/api/v(.+)/predictions|/api/v(.+)/predictions)").matcher(originPredictUrl);
		if (m.matches()) {
			proto = "seldon";
			return proto;
		}

		// a sample prediction URL of seldon v2 protocol: https://.../seldon/seldon/<deploy-name>/v2/models/<model-name>/infer
		m = Pattern.compile("(.+)(/seldon/)(.+)(/v2/models/infer|/v2/models/(.+)/infer)").matcher(originPredictUrl);
		if (m.matches()) {
			proto = "v2";
			return proto;
		}

		// a sample prediction URL of tensorflow protocol: https://.../seldon/seldon/<deploy-name>/v1/models/:predict
		m = Pattern.compile("(.+)(/seldon/)(.+)(/v1/models/:predict)").matcher(originPredictUrl);
		if (m.matches()) {
			proto = "tensorflow";
		}
		return proto;
	}

	public static String convert2ValidUrl(String proto, String modelName, String originPredictUrl, boolean isStream) {
		if(StringUtils.isEmpty(proto) || StringUtils.isEmpty(originPredictUrl)) {
			return originPredictUrl;
		}
		if(seldon.name().equals(proto) || tensorflow.name().equals(proto)) {
			return originPredictUrl;
		}
		if(originPredictUrl.contains("|")) {
			String[] urlList = originPredictUrl.split("\\|");
			if (urlList.length < 2) {
				return originPredictUrl;
			}
			return convert2ValidUrl(proto, modelName, urlList[0], isStream) + "|" +
					convert2ValidUrl(proto, modelName, urlList[1], isStream);
		}

		// https://.../seldon/seldon/<deploy-name>/v2/models/<model-name>/infer
		if(v2.name().equals(proto)) {
			Map<String, String> mapResult = parseSeldonUrl(originPredictUrl);
			String path = mapResult.get("path");
			String deployName = mapResult.get(GRPC_HEADER_SELDON);
			modelName = StringUtils.isEmpty(modelName) ? mapResult.get(V2_MODEL_NAME) : modelName;
			String url = originPredictUrl.replaceAll(path, "");
			url += "/seldon/seldon/" + deployName + "/v2/models/" + modelName;
			if (!isStream) {
				url += "/infer";
			} else {
				url += "/stream";
			}
			return url;
		}
		return originPredictUrl;
	}

	public static PredictModelsRequest getRequestFromMultipart(MultipartFormDataInput input) throws GatewayException {
		Map<String, List<InputPart>> inputFormDataMap = input.getFormDataMap();
		List<InputPart> bodyList = inputFormDataMap.get("body");
		if(null == bodyList || bodyList.isEmpty()) {
			throw new GatewayException("Body Not Found In Multipart Key.", Response.Status.BAD_REQUEST);
		}

		InputPart body = bodyList.get(0);
		body.setMediaType(new MediaType("text", "plain", "utf-8"));
		PredictModelsRequest request = null;
		String requestBody = "";
		try {
			requestBody = body.getBodyAsString();
			logger.info("requestBody:{}", requestBody);
			mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			request = mapper.readValue(requestBody, PredictModelsRequest.class);
		} catch (IOException e) {
			CalLogHelper.logException("PREDICT_MODELS" + EXCEPTION_POSTFIX, e.getMessage());
			throw new GatewayException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
		return request;
	}

	// only one binary file
	public static byte[] getBinaryInputFromMultipart(MultipartFormDataInput input) {
		Map<String, List<InputPart>> inputFormDataMap = input.getFormDataMap();
		List<InputPart> binaryInputs = inputFormDataMap.get("binary_inputs");
		if(null == binaryInputs || binaryInputs.size() == 0) {
			logger.info("This no binary inputs.");
			return null;
		}

		byte[] bytes = null;
		try {
			InputStream inputStream = binaryInputs.get(0).getBody(InputStream.class, null);
			bytes = IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			throw new GatewayException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
		return bytes;
	}

	public static List<SinglePredictRequest> buildSingleRequests(String client, PredictModelsRequest request, byte[] binaryInput,
																 Optional<SecurityContext> securityContext) {
		List<SinglePredictRequest> singleRequests = new ArrayList<>();
		for(Model model:request.getModelList()) {
			singleRequests.add(new SinglePredictRequest(model, client,
					securityContext, request.getRawInputs(), request.getAbtestParams(),
					request.getParameters(), binaryInput));
		}
		return singleRequests;
	}

	public static void checkMultipartInput(PredictModelsRequest request, byte[] binaryInput) throws GatewayException {
		if(CollectionUtils.isEmpty(request.getRawInputs()) && (null == binaryInput || 0 == binaryInput.length)) {
			throw new GatewayException("Raw inputs and binary inputs cannot be empty at the same time. Please check.",
					Response.Status.INTERNAL_SERVER_ERROR);
		}

		if(CollectionUtils.isEmpty(request.getModelList()) || request.getModelList().size() > 20) {
			throw new GatewayException("The length of model list is between 1 and 20. Please check.",
					Response.Status.INTERNAL_SERVER_ERROR);
		}

		String project = request.getModelList().get(0).getProject();
		String infra = "";
		for(Model model:request.getModelList()) {
			if(!project.equals(model.getProject())) {
				throw new GatewayException("Project of all models should be the same. Please check.",
						Response.Status.INTERNAL_SERVER_ERROR);
			}
			if("aimlmodelserv".equals(model.getEndpoint()) || "aimlmodelgpuserv".equals(model.getEndpoint()) ||
					"aimlmodelgpu2serv".equals(model.getEndpoint()) || "tensorcomputegpuserv".equals(model.getEndpoint())) {
				if("SELDON".equals(infra)) {
					throw new GatewayException("All endpoints of models should be either raptor or seldon, please check.",
							Response.Status.INTERNAL_SERVER_ERROR);
				}
				infra = "RAPTOR";
			} else {
				if("RAPTOR".equals(infra)) {
					throw new GatewayException("All endpoints of models should be either raptor or seldon, please check.",
							Response.Status.INTERNAL_SERVER_ERROR);
				}
				infra = "SELDON";
			}
		}
	}

	public static boolean validJson(String jsonStr) {

		if (StringUtils.isEmpty(jsonStr)) {
			return false;
		}

		JsonElement jsonElement;
		try {
			jsonElement = new JsonParser().parse(jsonStr);
		} catch (Exception e) {
			return false;
		}
		if (jsonElement == null) {
			return false;
		}
        return jsonElement.isJsonObject();
    }

	public static boolean ifProd() {
		boolean isProd = AppBuildConfig.getInstance().isProduction();
		if(isProd) {
			logger.info("Current environment is in production.");
			return true;
		}
		return false;
	}

	public static String convertV2Url(RoutingValue routingValue) {
		try {
            return convert2ValidUrl(parseSeldonProto(routingValue.getUrl()),
					routingValue.getModel(), routingValue.getUrl(), false);
		} catch (GatewayException e) {
			CalLogHelper.logException(SELDON_PREDICT_ERROR, e);
			throw new GatewayException(e.getErrorMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	public static boolean checkEndpointsInConfig(String endpoint, Set<String> configEndpoints) {
		if (CollectionUtils.isEmpty(configEndpoints) || StringUtils.isEmpty(endpoint)) {
			return false;
		}
		return configEndpoints.contains(endpoint);
	}

	public static Set<String> getConfigEndpoints(String configEndpoints) {
		try {
			return new HashSet<>(Arrays.asList(configEndpoints.split("\\|")));
		} catch (Exception e) {
			return null;
		}
	}
}
