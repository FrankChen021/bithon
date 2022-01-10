package org.bithon.server.tracing.sanitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.sink.TraceSpan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 10/1/22 2:28 PM
 */
@Slf4j
public class SanitizerFactory {
    private final ObjectMapper objectMapper;
    private final Map<String, ISanitizer> applicationSanitizers;
    private final ISanitizer globalSanitizer;

    public SanitizerFactory(ObjectMapper objectMapper, TraceConfig traceConfig) {
        this.objectMapper = objectMapper;
        this.applicationSanitizers = new HashMap<>();
        if ( traceConfig.getApplicationSanitizer() != null) {
            traceConfig.getApplicationSanitizer().forEach((application, config) -> {
                ISanitizer sanitizer = getSanitizer(config);
                if (sanitizer != null) {
                    applicationSanitizers.put(application, sanitizer);
                }
            });
        }
        this.globalSanitizer = traceConfig.getGlobalSanitizer() == null ? null : getSanitizer(traceConfig.getGlobalSanitizer());
    }

    private ISanitizer getSanitizer(SanitizerConfig config) {
        try {
            //flatten the configuration
            Map<String, Object> map = new HashMap<>();
            map.put("type", config.getType());
            map.putAll(config.getArgs());
            String json = objectMapper.writeValueAsString(map);

            return objectMapper.readValue(json, ISanitizer.class);
        } catch (IOException e) {
            log.error("Unable to create extractor for type " + config.getType(), e);
            return null;
        }
    }

    public void sanitize(IteratorableCollection<TraceSpan> spans) {
        if (applicationSanitizers.isEmpty() && globalSanitizer == null) {
            return;
        }
        while (spans.hasNext()) {
            sanitize(spans.next());
        }
    }

    private void sanitize(TraceSpan span) {
        if (span.getAppName() != null) {
            ISanitizer sanitizer = applicationSanitizers.get(span.getAppName());
            if (sanitizer != null) {
                sanitizer.sanitize(span);
            }
        }
        if(globalSanitizer != null) {
            globalSanitizer.sanitize(span);
        }
    }
}
