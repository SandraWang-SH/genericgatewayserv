package com.paypal.raptor.aiml.tests;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

import com.paypal.test.qi.mako.common.api.Environment;
import com.paypal.test.qi.mako.rest.AutoCloseableResponseWrapper;
import com.paypal.test.qi.mako.rest.api.RestClient;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Sample test class that uses the restclients from Mako.
 */
public class MakoSampleFunctionalTest {

    // This client will use the configured "protocol, host, port, path" values from global "MakoConfig"
    // reads configured values from testng suite file / property file / env vars / system props.
    @Inject
    @RestClient(path = "/v1/sampleresource/hello")
    private WebTarget target;

    // Same as above client but we use "restjaws" as a service "id" to read mako configured properties ("mako.<id>.*")
    @Inject
    @RestClient(id = "restjaws")
    private WebTarget restJawsClient;

    // This is how to detect what environment makotests are executing in.
    @Inject
    private Environment environment;

    int STATUS_OK = HttpStatus.SC_OK;


    @Test
    public void testGetWithJawsRestClient() {
        Response resp = restJawsClient.path("/bankinfo").queryParam("CountryCode", "US").request().get();
        assertEquals(resp.getStatus(), STATUS_OK);
        assertTrue(resp.readEntity(String.class).contains("United States"));
        resp.close();
    }

    @Test
    public void testinvokeRestJawsCurrency() throws Exception {
        // How to invoke the RESTJAWS endpoint for currency.
        // please refer to this link to see what
        // RestJaws provides http://jaws.qa.paypal.com/v1/QIJawsServices/

        WebTarget currencyresource = restJawsClient.path("/currency");
        Response resp = currencyresource.request(MediaType.APPLICATION_JSON).get();
        try (AutoCloseableResponseWrapper closeable = new AutoCloseableResponseWrapper(resp)) {
            // assert that the call was successful
            assertEquals(resp.getStatus(), STATUS_OK);
            // now assert that there is a payload in the response
            assertTrue(resp.hasEntity());
            // dump the currency responses
            String currencies = resp.readEntity(String.class);
            assertNotNull(currencies);
            assertFalse(currencies.isEmpty());
        }
    }

    @Test
    public void testConfigurationCiEnvironment() {

        // Environment exposes methods to query what env we are executing in ("DEV" / "CI" ...)
        boolean ciExecution = environment.isCiExecution();

        boolean jenkinsHomeExists = (System.getenv("JENKINS_HOME") != null);
        if (jenkinsHomeExists) {
            assertTrue(ciExecution);
        } else {
            assertFalse(ciExecution);
        }
    }

}
