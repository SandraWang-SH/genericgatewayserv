package com.paypal.raptor.aiml.service.impl;

import static com.ebay.kernel.cal.api.CalStatus.EXCEPTION;
import static com.ebay.kernel.configuration.ConfigurationContext.ERR_MSG;
import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import static com.paypal.raptor.aiml.common.enums.ModelServingInfra.*;
import static com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler.buildErrorPredictResult;
import static com.paypal.raptor.aiml.utils.GatewayUtils.*;

import com.ebay.kernel.cal.api.CalTransaction;
import com.paypal.fpti.tracking.api.Tracking;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.facade.FptiPredictResult;
import com.paypal.raptor.aiml.facade.SinglePredictRequest;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.*;
import com.paypal.raptor.aiml.routing.RoutingHelper;
import com.paypal.raptor.aiml.seldon.SeldonClient;
import com.paypal.raptor.aiml.service.PredictModelsService;
import com.paypal.raptor.aiml.triton.TcsClient;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author: sheena
 **/
@Component
public class PredictModelsServiceImpl implements PredictModelsService {

	private static final Logger logger = LoggerFactory.getLogger(PredictModelsServiceImpl.class);

	@Inject
	@Qualifier("calAwareThreadPoolExecutor")
	Executor executor;

	@Autowired
	RoutingHelper routingHelper;

	@Autowired
	SeldonClient seldonClient;

	@Autowired
	TcsClient tcsClient;

	@Inject
	private Tracking tracking;

	@Override
	public PredictModelsResponse predictModels(String client, MultipartFormDataInput input, Optional<SecurityContext> securityContext) throws GatewayException {
		PredictModelsRequest request = getRequestFromMultipart(input);
		byte[] binaryInput = getBinaryInputFromMultipart(input);
		checkMultipartInput(request, binaryInput);

		List<SinglePredictRequest> singleRequests = buildSingleRequests(client, request, binaryInput, securityContext);
        return getPredictResponse(singleRequests);
	}

	public PredictModelsResponse getPredictResponse(List<SinglePredictRequest> singleRequests) {
		PredictModelsResponse response = new PredictModelsResponse();
		try {
			List<CompletableFuture<FptiPredictResult>> futures = singleRequests.stream()
					.map(singleRequest -> CompletableFuture.supplyAsync(() -> {
						CalTransaction calTransaction = createTransaction(PREDICT, getTargetFromRequest(singleRequest));
						AimlGatewayFptiEvent aimlGatewayFptiEvent = AimlGatewayFptiEvent.initFptiMap(GATEWAYSERV)
								.putApiName(PREDICT_MODEL_LIST).putClientId(singleRequest.getClient());
						long beginTs = System.currentTimeMillis();
						try {
							return predictWithFpti(singleRequest, aimlGatewayFptiEvent, beginTs);
						} catch (GatewayException e) {
							logger.error("Predict current model error! model:{}, error message:{}", singleRequest.getModel(), e.getErrorMessage());
							calTransaction.setStatus(EXCEPTION);
							calTransaction.addData(ERR_MSG, e.getErrorMessage());
							CalLogHelper.logException(PREDICT + EXCEPTION_POSTFIX, e);
							PredictResult predictResult =  buildErrorPredictResult(singleRequest.getModel(), e);
							aimlGatewayFptiEvent.putApiDuration(Long.toString(System.currentTimeMillis() - beginTs));
							try {
								RoutingValue routingValue = routingHelper.getRoutingAndAuth(singleRequest,
										aimlGatewayFptiEvent);
								putIfNotExistRequestToFptiEvent(aimlGatewayFptiEvent, singleRequest.getRawInputs(), routingValue);
							} catch (Exception exception) {
								logger.error("get routing error:{}", exception.getMessage());
							}
							aimlGatewayFptiEvent.putErrorDesc(e.getErrorMessage());
							return new FptiPredictResult(predictResult, aimlGatewayFptiEvent);
						} finally {
							calTransaction.completed();
						}
					}, executor)).collect(Collectors.toList());

			List<FptiPredictResult> results = new ArrayList<>();
			for (int i = 0; i < futures.size(); i++) {
				SinglePredictRequest singlePredictRequest = singleRequests.get(i);
				try {
					RoutingValue routingValue = routingHelper.getRouting(singlePredictRequest);
					FptiPredictResult fptiPredictResult;
                    if (null != routingValue.getTimeout()) {
                        fptiPredictResult = futures.get(i).get(routingValue.getTimeout(), TimeUnit.SECONDS);
                    } else {
                        fptiPredictResult = futures.get(i).get(DEFAULT_RAPTOR_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                    }
                    results.add(fptiPredictResult);
                } catch (Exception e) {
					logger.error("predict model {}, error message:{}", singlePredictRequest.getModel(), e.getMessage());
					results.add(futures.get(i).get(DEFAULT_RAPTOR_REQUEST_TIMEOUT, TimeUnit.SECONDS));
				}
			}

			AtomicReference<Boolean> successStatus = new AtomicReference<>(false);
			List<PredictResult> predictResults = new ArrayList<>();
			results.forEach(result -> {
				predictResults.add(result.getPredictResult());
				if(null != result.getAimlGatewayFptiEvent()) {
					tracking.trackEvent(org.apache.commons.lang.StringUtils.EMPTY, result.getAimlGatewayFptiEvent().toMap());
				}
				if(null != result.getPredictResult() && "SUCCESS".equals(result.getPredictResult().getStatus())) {
					successStatus.set(true);
				}
			});
			response.setResultList(predictResults);
			if(successStatus.get()) {
				response.setStatus("SUCCESS");
			} else {
				response.setStatus("FAILURE");
			}
		} catch (Exception e) {
			logger.error("Predict modelist error. Error message:{}", e.getMessage());
			response.setStatus("FAILURE");
			throw new GatewayException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	public FptiPredictResult predictWithFpti(SinglePredictRequest singlePredictRequest,
			final AimlGatewayFptiEvent aimlGatewayFptiEvent, Long beginTs) {
		try {
			// routing
			RoutingValue routingValue = routingHelper.getRoutingAndAuth(singlePredictRequest, aimlGatewayFptiEvent);
			singlePredictRequest.getModel().setEndpoint(routingValue.getEndpoint());
			singlePredictRequest.getModel().setModel(routingValue.getModel());
			PredictResult predictResult = null;
			if(SELDON.equals(routingValue.getInfra())) {
				predictResult = seldonClient.predictMultipartSeldonAsync(singlePredictRequest, routingValue, aimlGatewayFptiEvent);
			} else if (RAPTOR.equals(routingValue.getInfra())) {
				predictResult = tcsClient.predictTcsMultipart(singlePredictRequest, routingValue, aimlGatewayFptiEvent);
			}
			aimlGatewayFptiEvent.putApiDuration(Long.toString(System.currentTimeMillis() - beginTs));
			return new FptiPredictResult(predictResult, aimlGatewayFptiEvent);
		} catch (GatewayException e) {
			throw e;
		} catch (Exception e) {
			throw new GatewayException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
