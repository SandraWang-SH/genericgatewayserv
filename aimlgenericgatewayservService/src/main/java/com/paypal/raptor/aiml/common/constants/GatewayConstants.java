package com.paypal.raptor.aiml.common.constants;

/**
 * @author: qingwu
 **/
public class GatewayConstants {
	public static final String RCS_DATA_ENABLE_KEY = "enable_rcs_as_data_source";
	public static final String SELDON_V2_ENABLE_KEY = "enable_seldon_v2";
	public static final String KEYMAKER_KEY_NAME = "keymaker_key_name";
	public static final String DEFAULT_KEY_NAME = "raptor_batchft_password";

	public static final String CACHE_REFRESH_INTERVAL = "cache_refresh_interval";
	public static final String EMPTY_LIST_STR = "[]";
	public static final String RCS_ROUTING_KEY = "routing_table";
	public static final String RCS_AUTH_KEY = "auth_table";
	public static final String RAPTOR_PREDICT_PATH = "/v1/genericcomputeserv/modelrunner/batch";

	public static final String RAPTOR_MULTIPART_PREDICT_PATH = "/v1/genericcomputeserv/modelrunner/model-prediction";
	public static final String PROJECT_KEY_IN_FACTORS = "project";
	public static final String ENDPOINT_KEY_IN_FACTORS = "endpoint";
	public static final String MODEL_KEY_IN_FACTORS = "model";
	public static final String USE_ELMO_SMART_CLIENT_KEY = "use_elmo_smart_client";
	public static final String DEFAULT_USECASE = "Paypal_AI_Platform_Modelrunner_Analytics";
	public static final String EDMSTUDISERV_PATH = "/v1/edmstudiserv";
	public static final String AUTH_RULE_PATH = "/auth-rule";
	public static final String ROUTING_RULE_PATH = "/routing-rule";

	public static final String SELDON_PROTOCOL_KEY = "SELDON_PROTOCOL";

	public static final String CAL_POOL = "cal_poolstack";

	public static final String SA_TOKEN = "sa";

	public static final String CONTENT_TYPE = "Content-type";

	public static final String APP_JSON = "application/json";

	public static final String MULTIPART = "multipart/form-data";

	public static final String RAPTOR_TIME_ONE_MINUTE = "1min";
	public static final int DEFAULT_RAPTOR_REQUEST_TIMEOUT = 10;
	public static final String SELDON_LVS_HOST = "aiplatform.ccg24-hrz-gke-generic.ccg24.lvs.paypalinc.com";
	public static final String SELDON_LVS_HRZANA_HOST = "aiplatform.ccg24-hrzana-edk8s.ccg24.lvs.paypalinc.com";

	public static final String SELDON_SLC_HOST = "aiplatform.ccg15-hrz-gke-generic.ccg15.slc.paypalinc.com";
	public static final String SELDON_PHX_HOST = "aiplatform.us-we8-lsp-cdz-gke-generic.dcg04.phx.paypalinc.com";
	public static final String SELDON_PRIMARY_URL_KEY = "SELDON_PRIMARY_URL";
	public static final String SELDON_FAILOVER_URL_KEY = "SELDON_FAILOVER_URL";
	public static final String SELDON_PRIMARY_PATH = "SELDON_PRIMARY_PATH";
	public static final String SELDON_FAILOVER_PATH = "SELDON_FAILOVER_PATH";
	public static final String SELDON_PRIMARY_HOST = "SELDON_PRIMARY_HOST";
	public static final String SELDON_FAILOVER_HOST= "SELDON_FAILOVER_HOST";
	public static final String SELDON_DEV51_HOST = "aiplatform.dev51.cbf.dev.paypalinc.com";

	public static final String ASYNC_SELDON_ENDPOINTS = "async_seldon_endpoints";
	public static final String ENABLE_PHX_ENDPOINTS = "enable_phx_endpoints";

	//http client properties
	public static final String HTTP_BASEURL = "baseUrl";
	public static final String HTTP_MAX_TOTAL_CONNECTIONS = "maxTotalConnections";
	public static final String HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_INMS = "idleConnectionInPoolTimeoutInMs";
	public static final String HTTP_CONNECT_TIMEOUT_IN_MS = "http.connectionTimeOutInMs";
	public static final String HTTP_MAX_CONNECTION_LIFE_TIME_INMS = "http.maxConnectionLifetimeInMs";
	public static final String HTTP_SSL_HANDSHAKE_TIMEOUT_INMS = "http.sslHandshakeTimeoutInMs";
	public static final String HTTP_READ_TIMEOUT_INMS = "http.readTimeoutInMs";
	public static final String HTTP_REQUEST_TIMEOUT_INMS = "http.requestTimeoutInMs";
	public static final String HTTP_IN_HTTP_CONTEXT = "http.inHttpContext";
	public static final String HTTP_EXTERNAL = "http.external";
	public static final String HTTP_SSL_KEYSTORE = "http.ssl.keystore";
	public static final String HTTP_SSL_PROTOCOLS = "http.sslProtocols";
	public static final String HEADER_PROPAGATION_ENABLED = "http.headerPropagationEnabled";


}
