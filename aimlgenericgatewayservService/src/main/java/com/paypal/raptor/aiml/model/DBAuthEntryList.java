package com.paypal.raptor.aiml.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: sheena
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class DBAuthEntryList {
	@JsonProperty("data")
	private List<AuthEntry> authEntryList;

	@Data
	public static class AuthEntry {
		@JsonProperty("project")
		String project;
		@JsonProperty("endpoint")
		String endpoint;
		@JsonProperty("model")
		String model;
		@JsonProperty("clients")
		List<String> clients;
	}
}
