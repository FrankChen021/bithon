package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.tracing.sink.TraceSpan;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 10/12/21 3:13 PM
 */
public class URLParameterExtractor implements ITraceMappingExtractor {

    private final List<String> parameters;

    @JsonCreator
    public URLParameterExtractor(@JsonProperty("parameters") List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<TraceMapping> extract(TraceSpan span) {
        List<TraceMapping> mappings = new ArrayList<>();

        String uriText = span.getTags().get("uri");
        if (!StringUtils.hasText(uriText)) {
            return mappings;
        }

        try {
            URI uri = new URI(uriText);

            Map<String, String> variables = parseQuery(uri.getQuery());

            for (String parameter : this.parameters) {
                String userTxId = variables.get(parameter);
                if (!StringUtils.hasText(userTxId)) {
                    continue;
                }

                mappings.add(new TraceMapping(span.getStartTime() / 1000, //to milli second
                                              userTxId,
                                              span.getTraceId()));
            }

        } catch (URISyntaxException e) {
            return mappings;
        }

        return mappings;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> variables = new HashMap<>();
        int fromIndex = 0;
        int toIndex = 0;
        while (toIndex != -1) {
            String name;
            String value;
            toIndex = query.indexOf('=', fromIndex);
            if (toIndex - fromIndex > 1) {
                name = query.substring(fromIndex, toIndex);
                fromIndex = toIndex + 1;
                toIndex = query.indexOf('&', fromIndex);
                if (toIndex == -1) {
                    value = query.substring(fromIndex);
                } else {
                    value = query.substring(fromIndex, toIndex);
                }
                variables.put(name, value);
                fromIndex = toIndex + 1;
            } else {
                fromIndex = query.indexOf('&', toIndex) + 1;
            }
        }
        return variables;
    }
}
