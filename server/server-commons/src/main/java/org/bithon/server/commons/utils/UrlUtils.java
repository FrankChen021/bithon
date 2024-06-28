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

package org.bithon.server.commons.utils;

import org.bithon.component.commons.utils.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/1 20:56
 */
public class UrlUtils {

    public static Map<String, String> parseURLParameters(String url) {
        return parseURLParameters(url, null);
    }

    public static Map<String, String> parseURLParameters(String url, Set<String> retainParameters) {
        if (StringUtils.isEmpty(url)) {
            return Collections.emptyMap();
        }

        // Locate the start of query string
        int queryParameterIndex = url.indexOf('?');
        if (queryParameterIndex < 0) {
            return Collections.emptyMap();
        }

        final Map<String, String> parsed = new TreeMap<>();
        StringUtils.extractKeyValueParis(url, queryParameterIndex + 1, "&", "=", (k, v) -> {
            if (retainParameters == null || retainParameters.isEmpty() || retainParameters.contains(k)) {
                parsed.put(k, v);
            }
        });
        return parsed;
    }

    /**
     * An in place replacement of value of a given parameter in one URL
     * <pre>
     * For example, for the given inputs as follows:
     *   url: http://localhost/?p1=a&p2=b
     *   parameter: p1
     *   replacement: HIDDEN
     * </pre>
     * This function returns: http://localhost/?p1=HIDDEN&p2=b
     */
    public static String sanitize(String url, String parameter, String replacement) {
        // Locate the start of query string
        int queryParameterIndex = url.indexOf('?');
        if (queryParameterIndex < 0) {
            return url;
        }

        int parameterIndex = queryParameterIndex + 1;
        do {
            // Locate the start of given parameter name
            parameterIndex = url.indexOf(parameter, parameterIndex);
            if (parameterIndex == -1) {
                break;
            }

            parameterIndex += parameter.length();

            // After the parameter name, it should be the '='
            // To make the function robust, we skip any spaces before the '=' character
            while (parameterIndex < url.length() && url.charAt(parameterIndex) == ' ') {
                parameterIndex++;
            }

            if (parameterIndex < url.length()) {
                // A '=' is expected.
                // If it's not, the current match might be a substring, we need to continue to search the left characters
                if (url.charAt(parameterIndex) == '=') {
                    if (parameterIndex + 1 == url.length()) {
                        // A string like 'p1&p2=' where we're searching 'p2'
                        return url;
                    }

                    int splitterIndex = url.indexOf('&', parameterIndex);
                    if (parameterIndex + 1 == splitterIndex) {
                        // No value for the parameter
                        return url;
                    }

                    String before = url.substring(0, parameterIndex);
                    return before + "=" + replacement + (splitterIndex > 0 ? url.substring(splitterIndex) : "");
                }
            }
        } while (parameterIndex > 0 && parameterIndex < url.length());

        return url;
    }
}
