package com.paypal.raptor.aiml.facade;

import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.model.AbtestParams;
import com.paypal.raptor.aiml.model.Model;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Getter
public class SinglePredictRequest {
    Model model;

    String client;

    Optional<SecurityContext> securityContext;

    List<String> rawInputs;

    AbtestParams abtestParams;

    String parameters;

    byte[] binaryInput;
}
