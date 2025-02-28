package com.paypal.raptor.aiml.cache;

import com.ebayinc.platform.security.SecretProvider;
import com.paypal.raptor.aiml.model.*;
import com.paypal.raptor.aiml.update.UpdatableAllComponentsService;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.paypal.raptor.aiml.common.constants.CalConstants.*;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.KEYMAKER_KEY_NAME;

/**
 * @author: sheena
 **/
@EnableScheduling
@Component
public class CacheUpdateService {
	private static final Logger logger = LoggerFactory.getLogger(CacheUpdateService.class);

	@Autowired
	UpdatableAllComponentsService updatableAllComponentsService;

  	@Inject
  	SecretProvider secretProvider;

	@Inject
	Configuration configuration;

	private volatile Map<String, RoutingValue> routingCache;

	private volatile Map<String, List<String>> authCache;

	private volatile Map<String, String> secretCache;

	@Scheduled(initialDelay = 600000, fixedDelay = 600000)
	public void updateScheduler() {
		try{
			updateCache();
		} catch (Exception e) {
			logger.error("Update cache error when scheduled! message:{}", e.getMessage());
			CalLogHelper.logError(LOAD_ALL_COMPONENT_ERROR, GET_ALL_COMPONENT_UPDATE_ERROR);
		}
	}

	public void updateCache() {
		// get all components
		UpdatableComponents updatableComponents = updatableAllComponentsService.getAllComponents();
		routingCache = updatableComponents.getRoutingMap();
		authCache = updatableComponents.getAuthMap();
		if (null == secretCache) {
			secretCache = new HashMap<>();
		}

		try {
			String secret = new String(secretProvider.getSecret(configuration.getString(KEYMAKER_KEY_NAME)),
                    StandardCharsets.UTF_8);
			secretCache.put(configuration.getString(KEYMAKER_KEY_NAME), secret);
		} catch (Exception e) {
			logger.error("Get secret cache error! message:{}", e.getMessage());
			CalLogHelper.logError(LOAD_SECRET_VALUE_FROM_KEY_MAKER_ERROR, GET_SECRET_VALUE_FROM_KEY_MAKER_ERROR);
		}
	}

	public RoutingValue getRoutingValueFromCache(String key) {
		return CollectionUtils.isEmpty(routingCache) ? null : routingCache.get(key);
	}

	public List<String> getAuthClientsFromCache(String key) {
		return CollectionUtils.isEmpty(authCache) ? null : authCache.get(key);
	}

	public String getSecretValueFromCache(String key) {
		return CollectionUtils.isEmpty(secretCache) ? null : secretCache.get(key);
	}

}
