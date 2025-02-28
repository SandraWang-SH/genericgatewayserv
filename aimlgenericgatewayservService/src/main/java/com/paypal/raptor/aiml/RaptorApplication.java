package com.paypal.raptor.aiml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

/**
 * This is your Raptor Spring Boot main class.
 *
 * <strong>Important:</strong> All of its annotations are necessary, please do not remove them
 */
@ComponentScan({ "com.paypal.raptor.aiml", "com.paypal.graphql", "com.paypal.elmo", "com.paypal.fpti.*"})
@EnableAutoConfiguration
public class RaptorApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(RaptorApplication.class);
	}

}
