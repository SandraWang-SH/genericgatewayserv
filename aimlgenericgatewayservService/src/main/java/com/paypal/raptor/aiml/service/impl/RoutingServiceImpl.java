package com.paypal.raptor.aiml.service.impl;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.*;

import java.util.List;
import javax.inject.Inject;

import com.paypal.raptor.aiml.data.DataService;
import com.paypal.raptor.aiml.model.*;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.paypal.raptor.aiml.service.RoutingService;


/**
 * @author: qingwu
 **/
@Component
public class RoutingServiceImpl implements RoutingService {

	@Inject
	Configuration configuration;

	@Autowired
	DataService dataService;

	private static final Logger logger = LoggerFactory.getLogger(RoutingServiceImpl.class);

	@Override
	public RoutingValue retrieveRoutingValueFromDataSource(final String key) throws Exception{
		if (configuration.getBoolean(RCS_DATA_ENABLE_KEY, true)) {
			return dataService.getRoutingValueFromUCP(key);
		}
		return dataService.getRoutingValueFromDB(key);
	}

	@Override
	public List<RoutingEntry> getAllRoutingEntries() throws Exception {
		if (configuration.getBoolean(RCS_DATA_ENABLE_KEY, true)) {
			logger.info("Init routing cache from data source: UCP.");
			return dataService.getAllRoutingEnetirsFromUCP();
		}
		logger.info("Init routing cache from data source: database.");
		return dataService.getAllRoutingEntriesFromDB();
	}

}
