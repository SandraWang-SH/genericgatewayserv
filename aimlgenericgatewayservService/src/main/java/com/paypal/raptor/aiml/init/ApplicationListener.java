package com.paypal.raptor.aiml.init;

import com.paypal.raptor.aiml.cache.CacheUpdateService;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.utils.CalLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static com.paypal.raptor.aiml.common.constants.CalConstants.*;


/**
 * This class can be used as a hook for post application initialization. At the
 * moment the postInitialization method is called, any Spring bean will be
 * already available, which means any Spring bean can be injected to this class
 * and used them in the postInitialization method.
 */
@Component
public class ApplicationListener {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationListener.class);

	@Inject
	CacheUpdateService cacheUpdateService;

	/**
	 * A listener method notifying that the application has started and all
	 * Spring beans are available.
	 */
	@EventListener
	public void postInitialization(ContextRefreshedEvent event) {
		logger.info("Start to initialize routing rule and auth rule cache.");
		try {
			cacheUpdateService.updateCache();
		} catch (Exception e) {
			logger.error("Update cache error. Error message:" + e.getMessage());
			CalLogHelper.logError(LOAD_ALL_COMPONENT_ERROR, GET_ALL_COMPONENT_INIT_ERROR);
			throw new GatewayException("Init fail when update cache, message: " + e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		}
		logger.info("Cache initialized, incoming traffic allowed.");
	}

}
