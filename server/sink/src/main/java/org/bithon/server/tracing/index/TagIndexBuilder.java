package org.bithon.server.tracing.index;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 3/3/22 12:06 PM
 */
public class TagIndexBuilder {
    /**
     * use TraceConfig so that if the configuration changes dynamically, the latest configuration can be used immediately.
     */
    private final TraceConfig config;

    public TagIndexBuilder(TraceConfig config) {
        this.config = config;
    }

    public List<TagIndex> build(Collection<TraceSpan> spans) {
        if (this.config.getTagIndex() == null || spans.isEmpty()) {
            return Collections.emptyList();
        }

        List<TagIndex> indexes = new ArrayList<>();

        List<String> names = this.config.getTagIndex().getNames();
        for (TraceSpan span : spans) {
            for (String name : names) {
                String value = span.getTag(name);
                if (!StringUtils.hasText(value)) {
                    continue;
                }
                indexes.add(TagIndex.builder()
                                    .timestamp(span.getStartTime() / 1000)
                                    .application(span.getAppName())
                                    .traceId(span.getTraceId())
                                    .name(name)
                                    .value(value)
                                    .build());
            }
        }

        return indexes;
    }
}
