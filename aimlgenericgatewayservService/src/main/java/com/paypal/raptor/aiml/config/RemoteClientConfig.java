package com.paypal.raptor.aiml.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.paypal.aiml.common.remoteclient.RemoteCallClient;
import com.paypal.aiml.common.remoteclient.impl.RemoteCallClientImpl;


/**
 * @author: qingwu
 **/
@Configuration
public class RemoteClientConfig {

	@Bean
	public RemoteCallClient remoteCallClient() {
		return RemoteCallClientImpl.getDefaultClient();
	}

	@Bean
	public RemoteCallClient remoteCallClientNoRetry() {
		return new RemoteCallClientImpl(1);
	}

}
