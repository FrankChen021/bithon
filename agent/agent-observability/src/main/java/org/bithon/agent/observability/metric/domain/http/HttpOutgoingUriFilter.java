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

package org.bithon.agent.observability.metric.domain.http;

import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/30 22:06
 */
@ConfigurationProperties(path = "agent.plugin.http.outgoing.filter.uri")
public class HttpOutgoingUriFilter {
    @JsonProperty
    private Set<String> suffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(","))
                                         .map(x -> x.trim().toLowerCase(Locale.ENGLISH))
                                         .collect(Collectors.toSet());

    public Set<String> getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(Set<String> s) {
        suffixes = s;
    }
}
