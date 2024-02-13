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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.utils.UrlUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.Collections;
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
    /**
     * key - the attribute name in the 'tags'
     * val - the parameter name
     * <p>
     * For example:
     * key: http.uri
     * val: password
     * This means sanitize the 'password' parameter on the 'http.uri' attribute
     */
    private final Map<String, String> sensitiveParameters;

    /**
     * NOTE: the ctor is passed from configuration which are deserialized from the application yml
     * The default deserialization treats the list as a LinkedHashMap, so we have to define the ctor as the map
     */
    @JsonCreator
    public UrlSanitizeTransformer(@JsonProperty("where") String where,
                                  @JsonProperty("sensitiveParameters") Map<String, String> sensitiveParameters) {
        super(where);
        this.sensitiveParameters = sensitiveParameters == null ? Collections.emptyMap() : sensitiveParameters;
    }

    @Override
    protected void sanitize(IInputRow inputRow) {
        TraceSpan span = (TraceSpan) inputRow;
        for (Map.Entry<String, String> entry : this.sensitiveParameters.entrySet()) {
            String attribName = entry.getKey();
            String parameterName = entry.getValue();

            String attribVal = span.getTag(attribName);
            if (StringUtils.hasText(attribVal)) {
                span.setTag(attribName, UrlUtils.sanitize(attribVal, parameterName, "*HIDDEN*"));
            }
        }
    }
}
