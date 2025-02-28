package com.paypal.raptor.aiml.utils;

import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Maps;

import java.util.Map;

import static com.paypal.raptor.aiml.utils.CommonUtils.putIfNotNull;


/**
 * @author: qingwu
 **/
public class CommonUtilsTest {
	@Test
	public void putIfNotNullTest() {
		Map<String, Object> map = Maps.newHashMap();
		putIfNotNull("key",null,map);
		putIfNotNull(null,"value",map);
		putIfNotNull("k",1,map);
		Assert.assertEquals(1,map.size());
		Assert.assertEquals(1,map.get("k"));
	}
}
