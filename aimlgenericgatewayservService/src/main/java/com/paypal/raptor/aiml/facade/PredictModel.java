package com.paypal.raptor.aiml.facade;

import com.paypal.fpti.tracking.api.Fpti;
import com.paypal.raptor.aiml.model.PredictRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.*;

import javax.validation.constraints.*;
import javax.validation.Valid;


@Path("/v1/analytics/predict-model")

@Fpti
@javax.annotation.Generated(value = "com.paypal.ppaas.swagger.codegen.PPaaSJAXRSSwaggerCodegen", date = "2024-04-30T11:41:23.252+08:00[Asia/Shanghai]")
public interface PredictModel  {


    /**
     * Predict model
     */
    @POST

    @Consumes({ "application/json", "multipart/form-data" })
    @Produces({ "application/json", "text/event-stream" })

    Response predictModel(



            final @NotNull(message="MISSING_REQUIRED_PARAMETER")
            @HeaderParam("Content-Type") String contentType

            ,


            final                        @Valid
            @NotNull(message = "MISSING_REQUIRED_PARAMETER")


            PredictRequest body

    );
}
