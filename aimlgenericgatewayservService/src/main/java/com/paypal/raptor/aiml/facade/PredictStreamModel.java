package com.paypal.raptor.aiml.facade;

import com.paypal.raptor.aiml.model.PredictRequest;
import reactor.core.publisher.Flux;

import javax.ws.rs.core.Response;
import javax.ws.rs.*;

import javax.validation.constraints.*;
import javax.validation.Valid;


@Path("/v1/analytics/predict-stream-model")


@javax.annotation.Generated(value = "com.paypal.ppaas.swagger.codegen.PPaaSJAXRSSwaggerCodegen", date = "2024-02-28T14:20:41.567+08:00[Asia/Shanghai]")
public interface PredictStreamModel  {


    /**
     * Predict model with SSE(server-sent event)
     */
    @POST

    @Consumes({ "application/json" })
    @Produces({ "text/event-stream" })

    Flux<Response> predictStreamModel(



            final @NotNull(message="MISSING_REQUIRED_PARAMETER") @Pattern(regexp="^.*$",message="INVALID_PARAMETER_SYNTAX") @Size(min=1,max=128,message="INVALID_STRING_LENGTH")
            @HeaderParam("Content-Type") String contentType

            ,


            final                        @Valid
            @NotNull(message = "MISSING_REQUIRED_PARAMETER")


            PredictRequest body

    );
}
