package com.paypal.raptor.aiml.init;

import org.testng.annotations.Test;
import javax.ws.rs.ApplicationPath;

import static org.testng.Assert.assertEquals;

/**
 *  Unit Test for {@link ApplicationConfig}.
 */

public class ApplicationConfigTest {
    private static final String APPLICATION_PATH = "/";

    @Test
    public void testApplicationPath() throws ClassNotFoundException {
        ApplicationConfig c = new ApplicationConfig();
        String result = (c.getClass().getAnnotation(ApplicationPath.class)).value();
        assertEquals(result, APPLICATION_PATH);
    }

}
