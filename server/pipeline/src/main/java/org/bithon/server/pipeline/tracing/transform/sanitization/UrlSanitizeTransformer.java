/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.pipeline.tracing.transform.sanitization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.tracing.TraceSpan;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * The tracing at the agent side will catch information from the uri and user-specified header.
 * But there might be some sensitive information in the information above for some specific applications
 * <p>
 * So, this class is used to sanitize the sensitive information according to user's configuration.
 * <p>
 * Currently, only sanitizing on the 'uri' parameter is supported.
 *
 * @author frank.chen021@outlook.com
 * @date 10/1/22 1:38 PM
 */
@JsonTypeName("url-sanitize-transform")
public class UrlSanitizeTransformer extends AbstractSanitizer {
    private final Collection<String> sensitiveParameters;

    /**
     * NOTE: the ctor is passed from configuration which are deserialized from the application yml
     * The default deserialization treats the list as a LinkedHashMap, so we have to define the ctor as the map
     */
    @JsonCreator
    public UrlSanitizeTransformer(@JsonProperty("where") String where,
                                  @JsonProperty("sensitiveParameters") Map<String, String> sensitiveParameters) {
        super(where);
        this.sensitiveParameters = new ArrayList<>(sensitiveParameters.values());
    }

    @Override
    protected void sanitize(IInputRow inputRow) {
        TraceSpan span = (TraceSpan) inputRow;

        boolean sanitized = false;

        Map<String, String> parameters = span.getUriParameters();
        for (String sensitiveParameter : sensitiveParameters) {
            if (parameters.containsKey(sensitiveParameter)) {
                parameters.put(sensitiveParameter, "***HIDDEN***");
                sanitized = true;
            }
        }
        if (!sanitized) {
            return;
        }

        // write back the query parameters to uri
        String uriText = span.getTag(Tags.Http.URL);
        if (StringUtils.isBlank(uriText)) {
            // compatibility
            uriText = span.getTag("uri");
        }
        if (StringUtils.isBlank(uriText)) {
            return;
        }

        try {
            URI uri = new URI(uriText);

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

            // for backward compatibility
            span.getTags().remove("uri");
            span.setTag(Tags.Http.URL, modified.toString());
        } catch (URISyntaxException ignored) {
        }
    }
}
