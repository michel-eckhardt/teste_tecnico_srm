package com.srm.creditengine.web.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Error statuses an endpoint can answer besides the ones documented for every operation (400 when it
 * takes input, 404 when it has path variables, 500). Each status is documented in the OpenAPI
 * document as an RFC 9457 {@code application/problem+json} response.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProblemResponses {

    /** HTTP status codes, e.g. {@code {409, 412, 422, 428}}. */
    int[] value();
}
