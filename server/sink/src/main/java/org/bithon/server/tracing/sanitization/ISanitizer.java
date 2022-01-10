package org.bithon.server.tracing.sanitization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.tracing.sink.TraceSpan;

/**
 * @author Frank Chen
 * @date 10/1/22 2:28 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "uri", value = UrlSanitizer.class),
})
public interface ISanitizer {
    void sanitize(TraceSpan span);
}
