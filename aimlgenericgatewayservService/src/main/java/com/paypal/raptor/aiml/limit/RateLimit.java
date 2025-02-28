package com.paypal.raptor.aiml.limit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;


@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum Access Restrictions
     * Default five request every seconds
     */
    double permitsPerSecond() default 20D;

    /**
     *Maximum waiting time for get tokens
     *
     */
    long timeOut() default 5L;

    /**
     * Unit of Maximum waiting time for get tokens, Default: milliseconds
     *
     */
    TimeUnit timeunit() default TimeUnit.SECONDS;

}
