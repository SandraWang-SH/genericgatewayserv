package com.paypal.raptor.aiml.facade;

import com.paypal.fpti.tracking.api.Fpti;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/v1/analytics/predict-models")


@javax.annotation.Generated(value = "com.paypal.ppaas.swagger.codegen.PPaaSJAXRSSwaggerCodegen", date = "2023-04-19T19:50:06.686+08:00[Asia/Shanghai]")
@Fpti
public interface PredictModels {


        /**
         * Predict model list with multipart
         */
        @POST
        
        @Consumes({ "multipart/form-data" })
        @Produces({ "application/json" })
        
        Response predictModelList(
        final MultipartFormDataInput input,

final @NotNull(message="MISSING_REQUIRED_PARAMETER") @Pattern(regexp="^.*$",message="INVALID_PARAMETER_SYNTAX") @Size(min=1,max=128,message="INVALID_STRING_LENGTH")
 @HeaderParam("Content-Type") String contentType

        
        );
    }
