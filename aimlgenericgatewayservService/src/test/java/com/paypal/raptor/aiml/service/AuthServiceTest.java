package com.paypal.raptor.aiml.service;

import static com.paypal.raptor.aiml.common.constants.GatewayConstants.EMPTY_LIST_STR;
import static com.paypal.raptor.aiml.common.constants.GatewayConstants.RCS_AUTH_KEY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import com.paypal.raptor.aiml.cache.CacheUpdateService;
import com.paypal.raptor.aiml.update.Impl.UpdatableCacheComponentsServiceImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.paypal.raptor.aiml.common.exception.AuthException;

/**
 * @author: qingwu
 **/
public class AuthServiceTest {
	@Mock
	Configuration configuration;

	@InjectMocks
	UpdatableCacheComponentsServiceImpl updatableComponentsService;

	@Mock
	CacheUpdateService cacheUpdateService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void checkIfClientAuthorizedTest() {
		Mockito.when(configuration.getString(RCS_AUTH_KEY, EMPTY_LIST_STR))
						.thenReturn(
										"[{\"project\":\"test_project\",\"endpoint\":null,\"infra\":\"SELDON\",\"url\":null},"
														+ "{\"project\":\"test_project_2\",\"endpoint\":\"endpoint\",\"infra\":\"RAPTOR\","
														+ "\"model\":\"test_model\",\"url\":null}]");
		Mockito.when(cacheUpdateService.getAuthClientsFromCache("project:endpoint:model")).thenReturn(
                Collections.singletonList("serviceB"));
		Exception exception = assertThrows(AuthException.class, () -> {
			updatableComponentsService.checkIfClientAuthorized("serviceA", "project:endpoint:model");
		});

		Mockito.when(cacheUpdateService.getAuthClientsFromCache("routingKey")).thenReturn(Collections.singletonList(""));
		updatableComponentsService.checkIfClientAuthorized("", "");
	}

}
