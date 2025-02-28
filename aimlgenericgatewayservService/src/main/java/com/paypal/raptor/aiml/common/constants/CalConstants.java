package com.paypal.raptor.aiml.common.constants;

/**
 * @author: qingwu
 **/
public class CalConstants {
	public static final String EXCEPTION_POSTFIX = "_EXCEPTION";
	public static final String PREDICT = "PREDICT";

	public static final String PREDICT_MODELS = "PREDICT_MODELS";

	public static final String PREDICT_STREAM_MODEL = "PREDICT_STREAM_MODEL";

	public static final String FEEDBACK = "FEEDBACK";

	public static final String EXPLAIN = "EXPLAIN";

	public static final String LOAD_ROUTING_VALUE_ERROR = "LOAD_ROUTING_VALUE_ERROR";

	public static final String LOAD_AUTH_VALUE_ERROR = "LOAD_AUTH_VALUE_ERROR";

	public static final String LOAD_SECRET_VALUE_FROM_KEY_MAKER_ERROR = "LOAD_SECRET_VALUE_FROM_KEY_MAKER_ERROR";

	public static final String LOAD_ALL_COMPONENT_ERROR = "LOAD_ALL_COMPONENT_ERROR";

	public static final String GET_ALL_ROUTING_RULES_ERROR = "GET_ALL_ROUTING_RULES_ERROR";

	public static final String GET_ALL_AUTH_RULES_ERROR = "GET_ALL_AUTH_RULES_ERROR";

	public static final String GET_SECRET_VALUE_FROM_KEY_MAKER_ERROR = "GET_SECRET_VALUE_FROM_KEY_MAKER_ERROR";

	public static final String GET_ALL_COMPONENT_INIT_ERROR = "GET_ALL_COMPONENT_INIT_ERROR";

	public static final String GET_ALL_COMPONENT_UPDATE_ERROR = "GET_ALL_COMPONENT_UPDATE_ERROR";

	public static final String GATEWAYSERV = "aimlgenericgatewayserv";

	public static final String PREDICT_MODEL = "predict-model";

	public static final String PREDICT_MODEL_LIST = "predict-models";

	public static final String PREDICT_MODEL_STREAM = "predict-stream-model";

	public static final String BIZ_API_NAME_PREDICT_STREAM = "/v1/analytics/predict-stream-model";
	public static final String FLOW_NAME_PREDICT_STREAM = "ppaas_1_2.v1.analytics.predict-stream-model.POST";

	public static final String BIZ_API_NAME_PREDICT = "/v1/analytics/predict-model";
	public static final String FLOW_NAME_PREDICT = "ppaas_1_2.v1.analytics.predict-model.POST";

	public static final String BIZ_API_NAME_PREDICT_LIST = "/v1/analytics/predict-models";
	public static final String FLOW_NAME_PREDICT_LIST = "ppaas_1_2.v1.analytics.predict-models.POST";

	public static final String EDM_API = "EDM_API";

	public static final String GET_ROUTING_ENTRY = "getRoutingEntry";

	public static final String GET_AUTH_ENTRY = "getAuthEntry";

	public static final String GET_ALL_AUTH = "getAllAuthEntries";

	public static final String GET_ALL_ROUTING = "getAllRoutingEntries";

	public static final String SELDON_API = "SELDON_API";

	public static final String SELDON_DR_API = "SELDON_DR_API";

	public static final String SELDON_SYNC_API = "SELDON_SYNC_API";

	public static final String SELDON_ASYNC_API = "SELDON_ASYNC_API";

	public static final String SELDON_PREDICT_CALL = "SELDON_PREDICT_CALL";

	public static final String INFRA = "infra";

	public static final String AUTH_FAILURE = "REQUEST_AUTH_ERROR";

	public static final String ROUTING_FAILURE = "REQUEST_ROUTING_ERROR";

	public static final String BAD_REQUEST_FAILURE = "BAD_REQUEST_ERROR";

	public static final String SELDON_PREDICT_ERROR = "SELDON_PREDICT_ERROR";

	public static final String RAPTOR_PREDICT_ERROR = "RAPTOR_PREDICT_ERROR";

}
