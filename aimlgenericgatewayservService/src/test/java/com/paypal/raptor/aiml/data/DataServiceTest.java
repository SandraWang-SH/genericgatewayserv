package com.paypal.raptor.aiml.data;

import static com.paypal.raptor.aiml.common.enums.ModelServingInfra.RAPTOR;
import static com.paypal.raptor.aiml.common.enums.ModelServingInfra.SELDON;
import static org.mockito.ArgumentMatchers.anyString;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import com.paypal.raptor.aiml.model.RoutingValue;


/**
 * @author: qingwu
 **/
public class DataServiceTest {

	@Mock
	Configuration configuration;

	@InjectMocks
	DataService dataService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void retrieveRoutingValueFromRCSTest() {
    	Mockito.when(configuration.getString(anyString(), anyString()))
        .thenReturn(
            "[{\"project\":\"test_project\",\"endpoint\":null,\"infra\":\"SELDON\",\"url\":null},"
				            + "{\"project\":\"test_project_2\",\"endpoint\":\"endpoint\",\"infra\":\"RAPTOR\","
				            + "\"model\":\"test_model\",\"url\":null}]");
		RoutingValue result = dataService.getRoutingValueFromUCP("test_project:null:null");
		Assert.assertNull(result.getUrl());
		Assert.assertEquals(SELDON,result.getInfra());
		RoutingValue result2 = dataService.getRoutingValueFromUCP("test_project_2:endpoint:test_model");
		Assert.assertNull(result2.getUrl());
		Assert.assertEquals(RAPTOR,result2.getInfra());
	}
}
