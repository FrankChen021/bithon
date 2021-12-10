package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.metric.dimension.transformer.MappingTransformer;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.List;

/**
 * @author Frank Chen
 * @date 10/12/21 3:12 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "uri", value = MappingTransformer.class)
})
public interface ITraceMappingExtractor {
    List<TraceMapping> extract(TraceSpan span);
}
