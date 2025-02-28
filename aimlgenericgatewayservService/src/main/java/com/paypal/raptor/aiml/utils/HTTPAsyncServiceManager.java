package com.paypal.raptor.aiml.utils;

import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.SELDON_ASYNC_API;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.APP_JSON;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.CONTENT_TYPE;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RAPTOR_MULTIPART_PREDICT_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ebay.kernel.cal.api.CalEvent;
import com.ebay.kernel.cal.api.CalStatus;
import com.ebay.kernel.cal.api.CalStream;
import com.ebay.kernel.cal.api.CalTransaction;
import com.ebay.kernel.cal.api.sync.CalEventHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.FileContent;
import com.paypal.raptor.aiml.model.Model;
import com.paypal.raptor.reactor.netty.http.client.HttpCalConstants;
import io.netty.handler.codec.http.HttpHeaderNames;


/**
 * @author: sheena
 **/
@Component
public class HTTPAsyncServiceManager {

	private static final Logger logger = LoggerFactory.getLogger(HTTPAsyncServiceManager.class);

	@Autowired
	private Configuration configuration;

	private CloseableHttpAsyncClient httpAsyncClientDefault;

	private volatile Map<Integer, CloseableHttpAsyncClient> httpAsyncClientCache;

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	private static final int INITIAL_CAPACITY = 256;

	private static final ThreadLocal<StringBuilder> localBuffer =
			ThreadLocal.withInitial(() -> new StringBuilder(INITIAL_CAPACITY));

	private static final Set<String> defaultHeadersToDenyList = new HashSet<>(Arrays.asList(
			"x-paypal-security-context",
			"x-paypal-oauth-context",
			"authorization",
			"cookie",
			"pp_remote_addr",
			"paypal-remote-address",
			"paypal-application-context",
			"pp_client_cert"
	)).stream()
			.map(String::toLowerCase)
			.collect(Collectors.toSet());


	enum CalType {
		SEND("SEND"),
		RECV("RECV");

		private final String keyName;
		CalType( String keyName ) { this.keyName = keyName; }
		@Override public String toString() { return keyName; }
	}

	enum EventAction {
		START("START"),
		END("END"),
		ERROR("ERROR");

		private final String keyName;
		EventAction(String keyName ) { this.keyName = keyName; }
		@Override public String toString() { return keyName; }
	}

	@PostConstruct
	public void init() {
		//default
		httpAsyncClientDefault = getHttpAsyncClient(5000);
		httpAsyncClientDefault.start();

		// connection pool cache
		httpAsyncClientCache = new HashMap<>();
	}

	private CloseableHttpAsyncClient getHttpAsyncClient(Integer timeout) {
		PoolingNHttpClientConnectionManager connManager = new AsyncSslSocketFactoryInitializer().getPoolingNHttpClientConnectionManager();
		connManager.setMaxTotal(40000);
		connManager.setDefaultMaxPerRoute(4000);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(configuration.getInt("http.connectionAsyncTimeOutInMs", 1000))
				.setConnectionRequestTimeout(configuration.getInt("http.connectionRequestAsyncTimeOutInMs", 5000))
				.setSocketTimeout(timeout != null ? timeout : configuration.getInt("http.socketAsyncTimeoutInMs", 5000)).build();

		return HttpAsyncClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig).build();
	}

	public CloseableHttpAsyncClient getHttpAsyncClient(HttpHost proxyUri, Integer timeout) {
		if (proxyUri == null && timeout == null) {
			return httpAsyncClientDefault;
		} else {
			CloseableHttpAsyncClient httpAsyncClient = httpAsyncClientCache.get(timeout);
			if (httpAsyncClient != null) {
				return httpAsyncClient;
			}
			httpAsyncClient = getHttpAsyncClient(timeout);
			httpAsyncClient.start();
			if (httpAsyncClientCache.size() < configuration.getInt("http.httpAsyncCacheSize", 500)) {
				httpAsyncClientCache.put(timeout, httpAsyncClient);
			}
			return httpAsyncClient;
		}
	}

	public <T> T httpAsyncPostQuery(String uri,
					Map<String, Object> queryParams,
					Map<String, Object> bodyParams,
					Class<T> resultClass,
					Map<String, String> headers,
					boolean setHeader,
					HttpHost proxyUri, Integer timeout)
			throws URISyntaxException, IOException, GatewayException, InterruptedException {
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setURI(composeQueryParam(httpPost.getURI(), queryParams));
		httpPost.setEntity(buildEntity(headers.get(CONTENT_TYPE), bodyParams));
		if (setHeader) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				if (APP_JSON.equals(entry.getValue())) {
					httpPost.setHeader(entry.getKey(), entry.getValue());
				}
			}
		}
		CloseableHttpAsyncClient client = getHttpAsyncClient(proxyUri, timeout);
		if (!client.isRunning()) {
			client.start();
		}
		CalTransaction calTransaction = createTransaction(SELDON_ASYNC_API, uri);
		try {
			Future<HttpResponse> future = client.execute(httpPost, new FutureCallback<HttpResponse>() {
				@Override
				public void completed(final HttpResponse response) {
					logger.info("get response:{}", response.getEntity());
				}
				@Override
				public void failed(final Exception ex) {
					CalEventHelper.sendImmediate("HTTP_REQUEST", "Failure", "1", ex.getMessage());
					calTransaction.setStatus(CalStatus.EXCEPTION);
					throw new GatewayException("Http failed. message: " + ex.getMessage() + ". Client running status:"
							+ client.isRunning(), INTERNAL_SERVER_ERROR);
				}
				@Override
				public void cancelled() {
					logger.info("http cancelled.");
				}
			});

			HttpResponse response = future.get(timeout == null ? 5000 : timeout, TimeUnit.MILLISECONDS);
			if (response != null) {
				String strResult = EntityUtils.toString(response.getEntity());
				if (response.getStatusLine().getStatusCode() == OK.getStatusCode()) {
					calTransaction.setStatus(CalStatus.SUCCESS);
					return gson.fromJson(strResult, resultClass);
				} else {
					CalEventHelper.sendImmediate("HTTP_REQUEST", "Failure", "1", strResult);
					calTransaction.setStatus(CalStatus.EXCEPTION);
					logger.error("Request failed for url:{}, error:{}",uri,strResult);
					throw new GatewayException("Http failed. result:" + strResult + ", status code: "
							+ response.getStatusLine().getStatusCode(), INTERNAL_SERVER_ERROR);
				}
			} else {
				calTransaction.setStatus(CalStatus.EXCEPTION);
				throw new GatewayException("Response is null from downstream model service.",
						INTERNAL_SERVER_ERROR);
			}
		} catch (ExecutionException e) {
			throw new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR);
		} catch (TimeoutException e) {
            throw new GatewayException("Timeout. " + e.getMessage(), INTERNAL_SERVER_ERROR);
        } finally {
			calTransaction.completed();
		}
	}

	public static URI composeQueryParam(URI uri, Map<String, Object> queryParams) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(uri);
		for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
			uriBuilder.addParameter(entry.getKey(), entry.getValue().toString());
		}
		return uriBuilder.build();
	}

	private static HttpEntity buildEntity(String contentType,
					Map<String, Object> bodyParams) throws UnsupportedEncodingException {
		List<Object> fcList = bodyParams.values().stream()
						.filter(e -> e instanceof FileContent)
						.collect(Collectors.toList());
		if (0 < fcList.size()) {
			return buildMultipartEntity(bodyParams);
		}
		else if (contentType != null && contentType.equalsIgnoreCase("multipart/form-data")) {
			return buildMultipartEntity(bodyParams);
		}
		else if (bodyParams.size() == 1 && bodyParams.containsKey(null)){
			return buildStringEntity(bodyParams.get(null));
		} else {
			return buildStringEntity(bodyParams);
		}
	}

	private static HttpEntity buildMultipartEntity(Map<String, Object> bodyParams) {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		for (Map.Entry<String, Object> entry : bodyParams.entrySet()) {
			if (entry.getValue() instanceof String) {
				builder.addTextBody(entry.getKey(), entry.getValue().toString());
			}
			else if (entry.getValue() instanceof byte[]) {
				builder.addBinaryBody(entry.getKey(), (byte[]) entry.getValue(),
								ContentType.APPLICATION_OCTET_STREAM, entry.getKey());
			}
			else if (entry.getValue() instanceof InputStream) {
				builder.addBinaryBody(entry.getKey(), (InputStream) entry.getValue(),
						ContentType.APPLICATION_OCTET_STREAM, entry.getKey());
			}
			else if (entry.getValue() instanceof FileContent) {
				FileContent fc = (FileContent) entry.getValue();
				builder.addPart(entry.getKey(),
								new InputStreamBody(new ByteArrayInputStream(fc.getFile()), fc.getFilename()));
			}
		}

		return builder.build();
	}

	private static HttpEntity buildStringEntity(Map<String, Object> bodyParams) throws UnsupportedEncodingException {
		return new StringEntity(gson.toJson(bodyParams), UTF_8);
	}

	private static HttpEntity buildStringEntity(Object plainText) throws UnsupportedEncodingException {
		return new StringEntity(plainText.toString(), UTF_8);
	}

	/**
	 * Create CAL Send Start event
	 */
	private static CalEvent createSendStartEvent(final CalStream calStream) {

		return createEvent(calStream, CalType.SEND, EventAction.START);
	}

	/**
	 * Create CAL Send End event
	 */
	private static CalEvent createSendEndEvent(final CalStream calStream,
											   final HttpPost req) {
		maybeCreateHeaderEvent(calStream, req);

		final CalEvent sendEndEvent = createEvent(calStream, CalType.SEND, EventAction.END);

		return maybeGetContentLength(req.getFirstHeader(HttpHeaderNames.CONTENT_LENGTH.toString()))
				.map(length -> sendEndEvent.addData(HttpCalConstants.TXN_DATA_PAYLOAD_SIZE, length))
				.orElse(sendEndEvent);
	}

	/**
	 * Create CAL sendError event
	 */
	private static CalEvent createSendErrorEvent(final CalStream calStream,
												 final HttpPost req) {
		maybeCreateHeaderEvent(calStream, req);

		final CalEvent sendErrorEvent = createEvent(calStream, CalType.SEND, EventAction.ERROR);

		return maybeGetContentLength(req.getFirstHeader(HttpHeaderNames.CONTENT_LENGTH.toString()))
				.map(length -> sendErrorEvent.addData(HttpCalConstants.TXN_DATA_PAYLOAD_SIZE, length))
				.orElse(sendErrorEvent);
	}

	/**
	 * Create CAL Receive Start event
	 */
	private static CalEvent createReceiveStartEvent(final CalStream calStream,
													final CloseableHttpResponse resp) {

		final CalEvent recvStartEvent = createEvent(calStream, CalType.RECV, EventAction.START);

		return maybeGetContentLength(resp.getFirstHeader(HttpHeaderNames.CONTENT_LENGTH.toString()))
				.map(length -> recvStartEvent.addData(HttpCalConstants.TXN_DATA_PAYLOAD_SIZE, length))
				.orElse(recvStartEvent);
	}

	/**
	 * Create CAL Receive End event
	 */
	private static CalEvent createReceiveEndEvent(final CalStream calStream) {
		return createEvent(calStream, CalType.RECV, EventAction.END);
	}

	private static void completeEventSuccessfully(final CalEvent event) {
		logger.debug("Completing {} {} Sucessfully" , event.getType() , event.getName());
		event.setStatus(CalStatus.SUCCESS).completed();
	}

	private static void completeEventWithException(final CalEvent event, String message) {
		event.addData("exception", message);
		event.setStatus(CalStatus.EXCEPTION).completed();
	}
	private static Optional<String> maybeGetContentLength(final Header contentLengthHeader) {
		String contentLength = contentLengthHeader.getValue();
		if (StringUtils.isNotEmpty(contentLength)) {
			return Optional.of(contentLength);
		}
		return Optional.empty();
	}



	private static CalEvent createEvent(
			final CalStream calStream,
			final CalType calType,
			final EventAction eventAction
	) {

		final CalEvent event = calStream.event(calType.toString());
		event.setName(eventAction.toString());
		logger.debug("Creating {} {}", event.getType(), event.getName());
		return event;
	}

	private static void maybeCreateHeaderEvent(final CalStream calStream,
											   final HttpPost httpPost ) {

		// We buffer the StringBuilder instance in thread local to reduce memory pressure.
		final StringBuilder data = localBuffer.get();
		Header[] allHeaders = httpPost.getAllHeaders();
		for (Header header : allHeaders) {
			String value = header.getValue();
			String name = header.getName();
			if ( value != null && !defaultHeadersToDenyList.contains(name)) {
				data.append(name).append("=").append(value).append("&");
			}
		}

		if (data.length() > 0) {
			data.setLength(data.length() - 1); // removes trailing `&`
			calStream
					.event(HttpCalConstants.EVENT_DATA_HEADER_INFO,
							HttpCalConstants.EVENT_DATA_HEADER,
							CalStatus.SUCCESS.toString(),
							data.toString())
					.completed();
		}
		data.setLength(0); // clearing the buffer
	}


	public static Response multipartPostQuery(byte[] binaryInput, Model model, String rawInput, WebTarget webTarget){
		MultipartFormDataOutput formData = new MultipartFormDataOutput();
		formData.addFormData("binary_contents", new ByteArrayInputStream(binaryInput), MediaType.APPLICATION_OCTET_STREAM_TYPE);
		formData.addFormData("model", model.getModel(), MediaType.TEXT_PLAIN_TYPE);
		formData.addFormData("raw_input", rawInput, MediaType.TEXT_PLAIN_TYPE);
		Response response = webTarget.path(RAPTOR_MULTIPART_PREDICT_PATH)
				.request()
				.post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE + "; boundary=" + formData.getBoundary()));

   return response;

	}

}
