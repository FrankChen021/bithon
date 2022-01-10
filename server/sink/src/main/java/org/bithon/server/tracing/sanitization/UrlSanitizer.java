package org.bithon.server.tracing.sanitization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.server.tracing.sink.TraceSpan;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * The tracing at the agent side will catch information from the uri and user specified header.
 * But there might be some sensitive information in the information above for some specific applications
 *
 * So, this class is used to sanitize the sensitive information according to user's configuration.
 *
 * Currently, only sanitizing on the 'uri' parameter is supported.
 *
 * @author Frank Chen
 * @date 10/1/22 1:38 PM
 */
public class UrlSanitizer implements ISanitizer {
    private final Collection<String> sensitiveParameters;

    /**
     * NOTE: the ctor is passed from configuration which are deserialized from the application yml
     * The default deserialization treats the list as a LinkedHashMap, so we have to define the ctor as the map
     */
    @JsonCreator
    public UrlSanitizer(@JsonProperty("sensitiveParameters") Map<String, String> sensitiveParameters) {
        this.sensitiveParameters= new ArrayList<>(sensitiveParameters.values());
    }

    @Override
    public void sanitize(TraceSpan span) {
        boolean sanitized = false;

        Map<String, String> parameters = span.getURLParameters();
        for (String sensitiveParameter : sensitiveParameters) {
            if (parameters.containsKey(sensitiveParameter)) {
                parameters.put(sensitiveParameter, "***MASKED***");
                sanitized = true;
            }
        }
        if (!sanitized) {
            return;
        }

        // write back the query parameters to uri
        try {
            URI uri = new URI(span.getTag("uri"));

            StringBuilder query = new StringBuilder();
            parameters.forEach((key, val) -> {
                query.append(key);
                query.append("=");
                query.append(val);
                query.append("&");
            });
            query.deleteCharAt(query.length() - 1);

            URI modified = new URI(uri.getScheme(),
                                   uri.getUserInfo(),
                                   uri.getHost(),
                                   uri.getPort(),
                                   uri.getPath(),
                                   query.toString(),
                                   uri.getFragment());

            span.setTag("uri", modified.toString());
        } catch (URISyntaxException ignored) {
        }
    }
}
