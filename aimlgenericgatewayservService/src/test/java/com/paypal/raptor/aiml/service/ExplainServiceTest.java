package com.paypal.raptor.aiml.service;

import com.paypal.raptor.aiml.service.impl.ExplainServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;


/**
 * @author: qingwu
 **/
public class ExplainServiceTest {
	@InjectMocks
	ExplainServiceImpl explainService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void explainTest() {
		explainService.explain("", null);
	}

}
