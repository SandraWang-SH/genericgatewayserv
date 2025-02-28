package com.paypal.raptor.aiml.service.impl;

import com.ebay.kernel.cal.api.CalTransaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.fpti.tracking.HttpParams;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler;
import com.paypal.raptor.aiml.dr.DisasterRecoveryHelper;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.fpti.TrackingUtil;
import com.paypal.raptor.aiml.model.*;
import com.paypal.raptor.aiml.routing.RoutingHelper;
import com.paypal.raptor.aiml.service.PredictStreamModelService;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.ebay.kernel.cal.api.CalStatus.EXCEPTION;
import static com.ebay.kernel.configuration.ConfigurationContext.ERR_MSG;
import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.SELDON_FAILOVER_URL_KEY;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.SELDON_PRIMARY_URL_KEY;
import static com.paypal.raptor.aiml.common.enums.AimlGatewayFptiTag.*;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;
import static com.paypal.raptor.aiml.utils.UrlUtils.getGatewayHost;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;


/**
 * @author: sheena
 **/
@Component
public class PredictStreamModelServiceImpl implements PredictStreamModelService {

	private static final Logger logger = LoggerFactory.getLogger(PredictStreamModelServiceImpl.class);

	@Autowired
	RoutingHelper routingHelper;

	@Autowired
	DisasterRecoveryHelper disasterRecoveryHelper;

	@Inject
    private TrackingUtil trackingUtil;

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	private String aimlgatewayHost = null;

	@PostConstruct
	public void init() {
		aimlgatewayHost = getGatewayHost();
	}

	@Override
	public Flux<Response> predictStreamModel(String client, PredictRequest request,
										Optional<SecurityContext> securityContext,
										javax.ws.rs.core.HttpHeaders headers,
										CalTransaction calTransaction) throws GatewayException {
		long beginTs = System.currentTimeMillis();
		AimlGatewayFptiEvent aimlGatewayFptiEvent =
				AimlGatewayFptiEvent.initFptiMap(GATEWAYSERV)
						.putApiName(PREDICT_MODEL_STREAM).putBizApiName(BIZ_API_NAME_PREDICT_STREAM)
						.putFlowName(FLOW_NAME_PREDICT_STREAM)
						.putRequestTs(StringUtils.defaultString(String.valueOf(beginTs)))
						.putClientId(client);

		RoutingValue routingValue = routingHelper.getStreamRoutingAndAuth(request,
				aimlGatewayFptiEvent, client, securityContext);
		request.setModel(routingValue.getModel());

		HttpParams httpParams = trackingUtil.getHttpParams(headers);
		try {
			String url = convert2ValidUrl(parseSeldonProto(routingValue.getUrl()), routingValue.getModel(),
					routingValue.getUrl(), true);
			routingValue.setUrl(url);
			//enable DR
			if (null != routingValue.getUrl() && routingValue.getUrl().contains("|")) {
				if (null != aimlgatewayHost && (aimlgatewayHost.length() >= 5 && aimlgatewayHost.startsWith("dcg") || aimlgatewayHost.startsWith("te-alm")
						|| aimlgatewayHost.startsWith("LM-SHB"))) {
					return predictStreamWithDR(aimlgatewayHost, routingValue, request, beginTs,
							aimlGatewayFptiEvent, httpParams, headers, calTransaction);
				}
				throw new GatewayException("Do not support DR for SSE endpoint." + routingValue.getEndpoint()
						+ ". Host is not right. host:" + aimlgatewayHost, INTERNAL_SERVER_ERROR);
			}

			return sseHttp(routingValue.getUrl(), request, routingValue.getBypassPayload(),
					beginTs, aimlGatewayFptiEvent, httpParams, headers, calTransaction);
		} catch (GatewayException e) {
			CalLogHelper.logException(SELDON_PREDICT_ERROR, e);
			throw new GatewayException(e.getErrorMessage(), INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			CalLogHelper.logException(SELDON_PREDICT_ERROR, e);
			throw new GatewayException(e.getMessage(), INTERNAL_SERVER_ERROR);
		}
	}

	private Flux<Response> predictStreamWithDR(String gwHost, RoutingValue routingValue, PredictRequest request,
			long beginTs, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			HttpParams httpParams, javax.ws.rs.core.HttpHeaders headers, CalTransaction calTransaction) {
		Map<String, String> resultUrl = disasterRecoveryHelper.getPrimaryUrl(gwHost, routingValue.getUrl(), routingValue.getEndpoint());
		return sseHttpWithDR(resultUrl.get(SELDON_PRIMARY_URL_KEY), resultUrl.get(SELDON_FAILOVER_URL_KEY), request,
					routingValue.getBypassPayload(), beginTs, aimlGatewayFptiEvent, httpParams, headers,
				calTransaction);
	}


	private Flux<Response> sseHttpWithDR(String primaryUrl, String failoverUrl, PredictRequest request,
			Boolean bypassPayload, long beginTs, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			HttpParams httpParams, javax.ws.rs.core.HttpHeaders headers, CalTransaction calTransaction) {
		AtomicInteger chunksCount = new AtomicInteger();
		AtomicLong firstChunkReceivedTime = new AtomicLong();
		String jsonBody = request.getInputs().get(0);

		// fpti request
		putRequestToFptiEvent(bypassPayload, aimlGatewayFptiEvent, jsonBody);

        return Flux.just(primaryUrl, failoverUrl)
				.concatMap(url -> {
					if (url.equals(failoverUrl) && Objects.equals(aimlGatewayFptiEvent.get(STATUS_CODE), "200")) {
						return Flux.just();
					}
					WebClient webClient = WebClient.create(url);
					Flux<ServerSentEvent<String>> sseResultFlux = predictSse(webClient, jsonBody, aimlGatewayFptiEvent, headers);
					return sseResultFlux.doOnNext(event -> {
						if(chunksCount.get() == 0) {
							firstChunkReceivedTime.set(System.currentTimeMillis());
						}
						chunksCount.getAndIncrement();
						if(null != event && null != event.data() && (null == bypassPayload || !bypassPayload)) {
							putResponseToFptiEvent(aimlGatewayFptiEvent, event.data(), bypassPayload);
						}
						if (chunksCount.get() > 0) {
							aimlGatewayFptiEvent.put(STATUS_CODE, "200");
						}
					})
					.map(event -> Response.ok().entity(
							buildData2PredictResponse(event.data(), request.getEndpoint())).build())
					.onErrorReturn(GatewayExceptionHandler.toStreamErrorResponse(url.equals(primaryUrl) ?
							new GatewayException("", OK) :
							new GatewayException("Error predict from upstream seldon.", INTERNAL_SERVER_ERROR)))
					.doFinally((signalType) -> {
						if (url.equals(primaryUrl) && !Objects.equals(aimlGatewayFptiEvent.get(STATUS_CODE), "200")) {
							return;
						}
						if (chunksCount.get() > 0) {
							aimlGatewayFptiEvent.put(STATUS_CODE, "200");
							aimlGatewayFptiEvent.put(INT_ERROR_CODE, "200");
							aimlGatewayFptiEvent.putErrorDesc("Success");
						}
						putFinally(aimlGatewayFptiEvent, calTransaction, beginTs);
						calTransaction.completed();
						// write to fpti
						trackingUtil.send2Fpti(aimlGatewayFptiEvent, httpParams);
					});
				});
	}

	private Flux<Response> sseHttp(String url, PredictRequest request, Boolean bypassPayload,
			long beginTs, final AimlGatewayFptiEvent aimlGatewayFptiEvent,
			HttpParams trackingHttpParams, javax.ws.rs.core.HttpHeaders headers, final CalTransaction calTransaction) {
		AtomicInteger chunksCount = new AtomicInteger();
		AtomicLong firstChunkReceivedTime = new AtomicLong();
		String jsonBody = request.getInputs().get(0);

		// fpti request
		putResponseToFptiEvent(aimlGatewayFptiEvent, jsonBody, bypassPayload);

		WebClient webClient = WebClient.create(url);
		Flux<ServerSentEvent<String>> sseResultFlux = predictSse(webClient, jsonBody, aimlGatewayFptiEvent, headers);
		return sseResultFlux.onErrorResume(WebClientResponseException.class, err -> {
							aimlGatewayFptiEvent.put(INT_ERROR_CODE, String.valueOf(err.getStatusCode()));
							aimlGatewayFptiEvent.putErrorDesc(err.getMessage());
							throw new GatewayException(err.getMessage(), INTERNAL_SERVER_ERROR);
						})
						.doOnNext(event -> {
							if(chunksCount.get() == 0) {
								firstChunkReceivedTime.set(System.currentTimeMillis());
							}
							chunksCount.getAndIncrement();
							if(null != event && null != event.data() && (null == bypassPayload || !bypassPayload)) {
								putResponseToFptiEvent(aimlGatewayFptiEvent, event.data(), bypassPayload);
							}
						})
						.map(event -> Response.ok().entity(
								buildData2PredictResponse(event.data(), request.getEndpoint())
						).build())
						.onErrorReturn(GatewayExceptionHandler.toStreamErrorResponse(
							new GatewayException("Error predict from upstream seldon.", INTERNAL_SERVER_ERROR)))
						.doFinally((signalType) -> {
							if (chunksCount.get() > 0) {
								aimlGatewayFptiEvent.putIfAbsent(STATUS_CODE, "200");
							}
							putFinally(aimlGatewayFptiEvent, calTransaction, beginTs);
							calTransaction.completed();
							// write to fpti
							trackingUtil.send2Fpti(aimlGatewayFptiEvent, trackingHttpParams);
						});
	}

	private Flux<ServerSentEvent<String>> predictSse(WebClient webClient, String jsonBody,
			final AimlGatewayFptiEvent aimlGatewayFptiEvent, javax.ws.rs.core.HttpHeaders headers) {

		ParameterizedTypeReference<ServerSentEvent<String>> typeRef =
				new ParameterizedTypeReference<ServerSentEvent<String>>() {};

		return webClient.post().headers(httpHeaders -> setHeaders(httpHeaders, headers))
				.contentType(MediaType.APPLICATION_JSON).bodyValue(jsonBody)
				.retrieve().onStatus(status -> status != HttpStatus.OK, response -> {
					String message = String.valueOf(response.bodyToMono(String.class));
					aimlGatewayFptiEvent.put(INT_ERROR_CODE, String.valueOf(response.statusCode()));
					aimlGatewayFptiEvent.put(STATUS_CODE, String.valueOf(response.statusCode()));
					aimlGatewayFptiEvent.putErrorDesc(message);
					throw new GatewayException("call upstream seldon error, code:" + response.statusCode(),
							INTERNAL_SERVER_ERROR);
				})
				.bodyToFlux(typeRef);
	}

	private void putFinally(final AimlGatewayFptiEvent aimlGatewayFptiEvent, final CalTransaction calTransaction,
			long beginTs) {
		aimlGatewayFptiEvent.putResponseTs(Long.toString(System.currentTimeMillis()));
		aimlGatewayFptiEvent.putApiDuration(Long.toString(System.currentTimeMillis() - beginTs));
		if(!Objects.equals(aimlGatewayFptiEvent.get(STATUS_CODE), "200")) {
			calTransaction.setStatus(EXCEPTION);
			calTransaction.addData(ERR_MSG, aimlGatewayFptiEvent.get(INT_ERROR_DESC));
			CalLogHelper.logException(calTransaction.getType() + EXCEPTION_POSTFIX,
					new GatewayException(aimlGatewayFptiEvent.get(INT_ERROR_DESC), INTERNAL_SERVER_ERROR));
		}
	}

	private static void setHeaders(org.springframework.http.HttpHeaders httpHeaders, javax.ws.rs.core.HttpHeaders headers) {
		httpHeaders.add("Accept", "text/event-stream");
	}

	private String buildData2PredictResponse(String data, String endpoint) {
		PredictResponse response = new PredictResponse();
		response.setResults(Collections.singletonList(data));
		response.setEndpoint(endpoint);
		response.status("SUCCESS");

		return gson.toJson(response);
	}

}
