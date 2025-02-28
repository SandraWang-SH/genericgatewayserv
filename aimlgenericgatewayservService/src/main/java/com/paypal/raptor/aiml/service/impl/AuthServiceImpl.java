package com.paypal.raptor.aiml.service.impl;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;
import java.util.List;
import javax.inject.Inject;
import com.paypal.raptor.aiml.data.DataService;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.paypal.raptor.aiml.model.AuthEntry;
import com.paypal.raptor.aiml.service.AuthService;

/**
 * @author: qingwu
 **/
@Component
public class AuthServiceImpl implements AuthService {

	@Inject
	Configuration configuration;

	@Autowired
	DataService dataService;

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

	@Override
	public List<String> retrieveAuthorizedClientsFromDataSource(String routingKey) throws Exception {
		if (configuration.getBoolean(RCS_DATA_ENABLE_KEY, true)) {
			return dataService.getAuthorizedClientsFromUCP(routingKey);
		}
		return dataService.getAuthorizedClientsFromDB(routingKey);
	}

	@Override
	public List<AuthEntry> getAllAuthEntries() throws Exception {
		if (configuration.getBoolean(RCS_DATA_ENABLE_KEY, true)) {
			logger.info("Init auth cache from data source: UCP.");
			return dataService.getAllAuthEntriesFromUCP();
		}
		// return all auth entries from DB
		logger.info("Init auth cache from data source: database.");
		return dataService.getAllAuthEntriesFromDB();
	}

}
