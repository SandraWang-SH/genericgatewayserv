package com.paypal.raptor.aiml.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


/**
 * @author: qingwu
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class ElmoResource {
	private String resourceName;
	private List<ElmoExperiment> experiments;

	@Data
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class ElmoExperiment {
		private String name;
		private List<String> treatments;
		private String audienceType;
	}
}
