package com.paypal.raptor.aiml.abtest;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.ENDPOINT_KEY_IN_FACTORS;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.MODEL_KEY_IN_FACTORS;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.PROJECT_KEY_IN_FACTORS;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.paypal.raptor.aiml.update.UpdatableCacheComponentsService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.paypal.elmo.elmoservjava.api.ELMOResponse;
import com.paypal.elmo.elmoservjava.api.ELMOSmartClient;
import com.paypal.elmoserv.models.response.TreatmentsResponse;
import com.paypal.raptor.aiml.common.exception.AbtestException;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.fpti.AimlGatewayFptiEvent;
import com.paypal.raptor.aiml.model.Audience;
import com.paypal.raptor.aiml.model.ElmoResource;
import com.paypal.raptor.aiml.model.RoutingValue;


/**
 * @author: qingwu
 **/
@Component
public class ElmoService {

	private static final Logger logger = LoggerFactory.getLogger(ElmoService.class);

	@Inject
	ELMOSmartClient elmoClient;

	@Autowired
	UpdatableCacheComponentsService updatableComponentsService;

	public RoutingValue getElmoTestRoutingValue(RoutingValue routingValue, Audience audience,
											  Map<String, String> filters,
											  final AimlGatewayFptiEvent aimlGatewayFptiEvent) throws GatewayException {
		// Assume only one experiment for the resource(endpoint)
		ElmoResource elmoResource = routingValue.getElmoResource();
		ElmoResource.ElmoExperiment experiment = elmoResource.getExperiments().get(0);
		String resourceName = elmoResource.getResourceName();
		String experimentName = experiment.getName();
		List<String> treatments = experiment.getTreatments();
		Map<String, String> params = new HashMap<String, String>(){{
				put(audience.getAudienceType().name(),audience.getAudienceValue());
		}};
		TreatmentsResponse treatmentRsp = executeElmo(resourceName, experimentName, params, filters, treatments);
		String routingKey = getRoutingKeyFromTreatment(treatmentRsp.getFactors());
		logger.info("RoutingKey:{} in treatment:{}, audienceVal:{}, audienceType:{}",
				routingKey, treatmentRsp.getTreatmentName(), audience.getAudienceValue(), experiment.getAudienceType());

		aimlGatewayFptiEvent.putElmoTrackingParams(treatmentRsp.getExperimentId(), treatmentRsp.getTreatmentId());
		aimlGatewayFptiEvent.putExperimentVersion(Double.toString(treatmentRsp.getExperimentVersion()));
		// put  to mab percent
		if(routingValue.getElmoExperimentType() != null && routingValue.getElmoExperimentType() == 2) {
			aimlGatewayFptiEvent.putAllocatePercentage(Float.toString(treatmentRsp.getAllocationPercent()));
		}
		// override routing target as event name
		aimlGatewayFptiEvent.putEventName(routingKey);
		aimlGatewayFptiEvent.putContextId(audience.getAudienceValue()).putContextType(audience.getAudienceType().name());

		routingValue = updatableComponentsService.getRoutingValue(routingKey);
		// one case : DB may be null but model in factors not null
		String modelVal	= treatmentRsp.getFactors().getOrDefault(MODEL_KEY_IN_FACTORS,"");
		routingValue.setModel(modelVal);
		return routingValue;
	}

	/**
	*  Compose routing key from factors.
	*/
	public String getRoutingKeyFromTreatment(Map<String, String> factors) throws AbtestException {
		if(MapUtils.isEmpty(factors)) {
			throw new AbtestException("Factors is not defined, please check experiment configuration.", NOT_FOUND);
		}
		String projectVal  = factors.get(PROJECT_KEY_IN_FACTORS);
		String endpointVal = factors.get(ENDPOINT_KEY_IN_FACTORS);
		if(StringUtils.isEmpty(projectVal) || StringUtils.isEmpty(endpointVal)) {
			throw new AbtestException("Project and endpoint names are mandatory in factors.", NOT_FOUND);
		}

		String modelVal	= factors.getOrDefault(MODEL_KEY_IN_FACTORS,"null");
		return String.join(":", projectVal, endpointVal, modelVal);
	}

	public TreatmentsResponse executeElmo(final String resourceName, final String experimentName,
										   final Map<String, String> params, Map<String, String> filters,
										   List<String> treatments) throws AbtestException {
		// get rsp from elmo
		ELMOResponse elmoResponse = elmoClient.elmo(resourceName, params, filters);
		String treatment = null;
		for (String t : treatments) {
			if (elmoResponse.hasTreatment(experimentName, t)) {
				treatment = t;
				logger.info("Found treatment:{} for experiment:{}, resource:{}", t, experimentName, resourceName);
				break;
			}
		}
		if(null == treatment) {
			throw new AbtestException("Treatment is not found, please check if A/B Test correctly configured on gateway.",
							NOT_FOUND);
		}
		return elmoResponse.getTreatment(experimentName, treatment);
	}

}
