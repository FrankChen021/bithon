package org.bithon.agent.core.tracing.context;

/**
 * @author Frank Chen
 * @date 25/12/21 5:56 PM
 */
public class Tags {
    public static final String HTTP_METHOD = "method";
    public final static String URI = "uri";
    public final static String STATUS = "status";

    /**
     * {@link SpanKind#CLIENT} should set this
     */
    public final static String TARGET = "target";

    /**
     * {@link SpanKind#CLIENT} should set this
     */
    public final static String TARGET_TYPE = "targetType";

    public enum TargetType {
        MongoDb,
        HttpService,
        Database,
        Redis,
        MySQL
    }
}
