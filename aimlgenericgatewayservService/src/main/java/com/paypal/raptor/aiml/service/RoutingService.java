package com.paypal.raptor.aiml.service;

import java.util.List;
import com.paypal.raptor.aiml.model.RoutingEntry;
import com.paypal.raptor.aiml.model.RoutingValue;


/**
 * @author: qingwu
 **/
public interface RoutingService {
	/**
	 * @param key routing key composed of project, endpoint and model
	 * @return routing value from ucp or edm service, return null if not found
	 * @throws Exception if EDM API call failure or JSON process exception
	 */
	RoutingValue retrieveRoutingValueFromDataSource(String key) throws Exception;

	/**
	 * @return All routing entries for gateway.
	 * @throws Exception to fail app start
	 */
	List<RoutingEntry> getAllRoutingEntries() throws Exception;
}
