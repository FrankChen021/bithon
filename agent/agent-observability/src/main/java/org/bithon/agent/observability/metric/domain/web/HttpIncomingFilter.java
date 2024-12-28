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

package org.bithon.agent.observability.metric.domain.web;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.observability.utils.filter.IMatcher;
import org.bithon.agent.observability.utils.filter.StringSuffixMatcher;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A filter that determines whether a request should be excluded for metrics and tracing
 *
 * @author frankchen
 */
public class HttpIncomingFilter {

    @ConfigurationProperties(path = "agent.plugin.http.incoming.filter.uri")
    public static class UriFilterConfiguration {
        @JsonProperty
        private String suffixes = ".html,.js,.css,.jpg,.gif,.png,.swf,.ttf,.ico,.woff,.woff2,.eot,.svg,.map";

        @JsonProperty
        private String endpoints = "";

        public String getSuffixes() {
            return suffixes;
        }

        public void setSuffixes(String s) {
            suffixes = s;
        }

        public String getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(String endpoints) {
            this.endpoints = endpoints;
        }
    }

    @ConfigurationProperties(path = "agent.plugin.http.incoming.filter.user-agent")
    public static class UserAgentFilterConfiguration {
        @JsonProperty
        private List<IMatcher> matchers = Collections.emptyList();

        public List<IMatcher> getMatchers() {
            return matchers;
        }
    }

    private final Set<String> dotSuffix;
    private final Set<String> endpoints;
    private final List<IMatcher> uriMatchers;
    private final UserAgentFilterConfiguration userAgentConfig;

    public HttpIncomingFilter() {
        uriMatchers = new ArrayList<>();
        {
            UriFilterConfiguration uriFilterConfig = ConfigurationManager.getInstance()
                                                                         .getConfig(UriFilterConfiguration.class);

            endpoints = new HashSet<>();
            endpoints.addAll(Arrays.asList(uriFilterConfig.getEndpoints().split(",")));

            dotSuffix = new HashSet<>();
            for (String suffix : uriFilterConfig.getSuffixes().split(",")) {
                if (suffix.startsWith(".")) {
                    // merge suffix together
                    dotSuffix.add(suffix);
                } else {
                    uriMatchers.add(new StringSuffixMatcher(suffix));
                }
            }
        }

        userAgentConfig = ConfigurationManager.getInstance()
                                              .getConfig(UserAgentFilterConfiguration.class);
    }

    public boolean shouldBeExcluded(String uri, String userAgent) {
        if (uri != null) {
            for (String endpoint : endpoints) {
                if (endpoint.equals(uri)) {
                    return true;
                }
            }
            if (!dotSuffix.isEmpty()) {
                int dotIndex = uri.lastIndexOf(".");
                if (dotIndex >= 0) {
                    String suffix = uri.substring(dotIndex).toLowerCase(Locale.ENGLISH);
                    if (dotSuffix.contains(suffix)) {
                        return true;
                    }
                }
            }
            for (IMatcher matcher : uriMatchers) {
                if (matcher.matches(uri)) {
                    return true;
                }
            }
        }


        if (userAgent != null) {
            for (IMatcher matcher : userAgentConfig.getMatchers()) {
                if (matcher.matches(userAgent)) {
                    return true;
                }
            }
        }

        return false;
    }
}
