package com.paypal.raptor.aiml.service;

import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.model.FeedbackRequest;
import com.paypal.raptor.aiml.model.FeedbackResponse;


/**
 * @author: qingwu
 * @created: 2022-07-12 17:16
 **/
public interface FeedbackService {
	FeedbackResponse feedback(String client, FeedbackRequest request) throws GatewayException;
}
