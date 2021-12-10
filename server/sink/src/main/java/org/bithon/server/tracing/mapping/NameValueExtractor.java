package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 10/12/21 4:32 PM
 */
public class NameValueExtractor implements ITraceMappingExtractor {

    private final Collection<String> names;

    public NameValueExtractor(@JsonProperty("names") Collection<String> names) {
        this.names = names;
    }

    @JsonCreator
    public NameValueExtractor(@JsonProperty("names") Map<String, String> names) {
        this(new ArrayList<>(names.values()));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback) {
        for (String name : names) {
            String value = span.getTags().get(name);
            if (StringUtils.hasText(value)) {
                callback.accept(span, value);
            }
        }
    }
}
