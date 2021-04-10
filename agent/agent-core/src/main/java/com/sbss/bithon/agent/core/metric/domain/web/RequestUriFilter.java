/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.metric.domain.web;

import com.sbss.bithon.agent.core.utils.filter.InCollectionMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * TODO: move out of this package
 *
 * @author frankchen
 */
public class RequestUriFilter {
    private InCollectionMatcher suffixMatcher;

    public RequestUriFilter() {
        this("html", "js", "css", "jpg", "gif", "png", "swf", "ttf", "ico", "woff", "woff2", "eot", "svg");
    }

    public RequestUriFilter(String... suffix) {
        suffixMatcher = new InCollectionMatcher(Arrays.stream(suffix).collect(Collectors.toSet()));
    }

    public boolean isFiltered(String uri) {
        if (uri == null) {
            return false;
        }

        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return suffixMatcher.matches(suffix);
    }
}
