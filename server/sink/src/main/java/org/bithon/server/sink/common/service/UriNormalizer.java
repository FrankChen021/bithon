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

package org.bithon.server.sink.common.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.server.commons.matcher.IMatcher;
import org.bithon.server.commons.matcher.StringRegexMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/12 9:59 下午
 */
@Service
public class UriNormalizer {

    private final RuleConfigs configs;

    public UriNormalizer() {
        this.configs = new RuleConfigs();
        this.configs.setGlobalRules(new UriNormalizationRuleConfig());
        this.configs.setApplicationRules(new HashMap<>());
        this.configs.globalRules.partRules.add(new UriPattern(new StringRegexMatcher("[0-9]+"), "*"));
        this.configs.globalRules.partRules.add(new UriPattern(new StringRegexMatcher(
            "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"), "*"));
        this.configs.globalRules.partRules.add(new UriPattern(new StringRegexMatcher("[0-9a-f]{32}"), "*"));
    }

    public NormalizedResult normalize(String applicationName, String path) {
        if (path == null) {
            return new NormalizedResult(false, null);
        }

        // strip parameters
        boolean striped = false;
        int paramStartIndex = path.indexOf('?');
        if (paramStartIndex > 0) {
            path = path.substring(0, paramStartIndex);
            striped = true;
        }

        //
        // 先使用domain的规则进行处理
        //
        if (applicationName != null && this.configs.getApplicationRules().containsKey(applicationName)) {
            path = normalize(this.configs.getApplicationRules().get(applicationName), path).getUri();
        }

        //
        // 再使用全局规则进行处理
        //
        NormalizedResult result = normalize(this.configs.getGlobalRules(), path);
        if (striped && !result.isNormalized()) {
            result.setNormalized(true);
        }
        return result;
    }

    private NormalizedResult normalize(UriNormalizationRuleConfig matchers, String path) {
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        //
        // 使用Path匹配
        //
        NormalizedResult result = new NormalizedResult(false, path);
        if (!CollectionUtils.isEmpty(matchers.getPathRules())) {
            for (UriPattern pattern : matchers.getPathRules()) {
                if (pattern.getMatcher().matches(path)) {
                    result.setNormalized(true);
                    result.setUri(pattern.getReplacement());
                    return result;
                }
            }
        }

        //
        // 使用Parts规则匹配
        //
        return normalize(matchers.getPartRules(), path);
    }

    private NormalizedResult normalize(List<UriPattern> partRules, String path) {
        if (CollectionUtils.isEmpty(partRules)) {
            return new NormalizedResult(false, path);
        }

        boolean normalized = false;
        String[] parts = path.split("/");
        List<String> normalizedParts = new ArrayList<>();
        for (String part : parts) {
            boolean appended = false;
            for (UriPattern partRule : partRules) {
                if (partRule.getMatcher().matches(part)) {
                    normalizedParts.add(partRule.getReplacement());
                    appended = true;
                    normalized = true;
                    break;
                }
            }
            if (!appended) {
                normalizedParts.add(part);
            }
        }

        if (normalizedParts.isEmpty()) {
            return new NormalizedResult(false, path);
        }

        // compress more starts into one
        int i = normalizedParts.size() - 1;
        while (i >= 0 && "*".equals(normalizedParts.get(i))) {
            i--;
        }
        if (i < normalizedParts.size() - 2) {
            normalizedParts = normalizedParts.subList(0, i + 1);
            normalizedParts.add("**");
        }

        return new NormalizedResult(normalized, String.join("/", normalizedParts));
    }

    @Data
    @AllArgsConstructor
    public static class UriPattern {
        private IMatcher matcher;
        private String replacement;
    }

    @Data
    public static class UriNormalizationRuleConfig {
        private List<UriPattern> pathRules = new ArrayList<>();
        private List<UriPattern> partRules = new ArrayList<>();
        private List<IMatcher> filters = new ArrayList<>();
    }

    @Data
    public static class RuleConfigs {
        private UriNormalizationRuleConfig globalRules;
        private Map<String, UriNormalizationRuleConfig> applicationRules;
    }

    @Data
    @AllArgsConstructor
    public static class NormalizedResult {
        private boolean normalized;
        private String uri;
    }
}
