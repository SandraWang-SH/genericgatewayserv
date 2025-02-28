package com.paypal.raptor.aiml.service;

import java.util.List;
import com.paypal.raptor.aiml.model.AuthEntry;


/**
 * @author: qingwu
 **/
public interface AuthService {
  /**
   * @param routingKey combination string of project+endpoint+model
   * @return null if auth clients not set
   * @throws Exception if failed to get client whitelist from underlying datasource
   */
  List<String> retrieveAuthorizedClientsFromDataSource(String routingKey) throws Exception;

	/**
	 * @return All auth entries for gateway.
	 * @throws Exception to fail app start
	 */
  List<AuthEntry> getAllAuthEntries() throws Exception;
}
