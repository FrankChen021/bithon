package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 10/12/21 3:12 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "uri", value = URIParameterExtractor.class),
    @JsonSubTypes.Type(name = "name", value = NameValueExtractor.class),
})
public interface ITraceMappingExtractor {
    void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback);
}
