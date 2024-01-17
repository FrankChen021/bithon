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

import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/1 20:56
 */
public class UrlUtils {
    public static Map<String, String> parseURLParameters(String uriText) {
        if (uriText == null) {
            return Collections.emptyMap();
        }

        URI uri;
        try {
            uri = new URI(uriText);
        } catch (URISyntaxException ignored) {
            return Collections.emptyMap();
        }

        String queryString = uri.getQuery();
        if (!StringUtils.hasText(queryString)) {
            return Collections.emptyMap();
        }

        Map<String, String> variables = new TreeMap<>();
        int tokenStart = 0;
        int tokenEnd = 0;
        do {
            tokenEnd = queryString.indexOf('=', tokenStart);
            if (tokenEnd > tokenStart) {
                // Find the parameter name
                String name = queryString.substring(tokenStart, tokenEnd);

                // +1 to skip the '='
                tokenStart = tokenEnd + 1;

                // Find the parameter value
                tokenEnd = queryString.indexOf('&', tokenStart);
                if (tokenEnd == -1) {
                    // If there's no '&' found, the whole is the value
                    variables.put(name, queryString.substring(tokenStart));
                } else {
                    // If there's a '&' found, get the substring as value
                    variables.put(name, queryString.substring(tokenStart, tokenEnd));
                }

                tokenStart = tokenEnd + 1;
            } else if (tokenEnd == tokenStart) {
                // extra '=' found, e.g: queryString equals to '='
                tokenStart = tokenEnd + 1;
            } else {
                // Not found '=', tokenEnd is -1,
            }
        } while (tokenEnd != -1);

        return variables;
    }
}
