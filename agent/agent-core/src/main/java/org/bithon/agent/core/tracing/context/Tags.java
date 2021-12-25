package org.bithon.agent.core.tracing.context;

/**
 * @author Frank Chen
 * @date 25/12/21 5:56 PM
 */
public class Tags {
    public static final String HTTP_METHOD = "method";

    /**
     * For a {@link SpanKind#CLIENT}, the uri must be in the format of URI where the scheme represent the target service
     *
     * For example,
     *  http://localhost:8080
     *  redis://127.0.0.1:6379
     *  mongodb://127.0.0.1:8000
     *  mysql:127.0.0.1:3309
     *
     */
    public final static String URI = "uri";
    public final static String STATUS = "status";
}
