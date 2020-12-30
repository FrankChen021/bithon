package com.sbss.bithon.collector.common.service;

import com.sbss.bithon.collector.common.matcher.IStringMatcher;
import com.sbss.bithon.collector.common.matcher.RegexMatcher;
import lombok.AllArgsConstructor;
import lombok.Data;
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

    @Data
    @AllArgsConstructor
    static public class UriPattern {
        private IStringMatcher matcher;
        private String replacement;
    }

    @Data
    static public class UriNormalizationRuleConfig {
        private List<UriPattern> pathRules = new ArrayList<>();
        private List<UriPattern> partRules = new ArrayList<>();
        private List<IStringMatcher> filters = new ArrayList<>();
    }

    @Data
    static public class RuleConfigs {
        private UriNormalizationRuleConfig globalRules;
        private Map<String, UriNormalizationRuleConfig> instanceRules;
    }

    @Data
    @AllArgsConstructor
    static public class NormalizedResult {
        private boolean normalized;
        private String uri;
    }

    private final RuleConfigs configs;

    public UriNormalizer() {
        this.configs = new RuleConfigs();
        this.configs.setGlobalRules(new UriNormalizationRuleConfig());
        this.configs.setInstanceRules(new HashMap<>());
        this.configs.globalRules.partRules.add(new UriPattern(new RegexMatcher("[0-9]+"), "*"));
        this.configs.globalRules.partRules.add(new UriPattern(new RegexMatcher("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"), "*"));
        this.configs.globalRules.partRules.add(new UriPattern(new RegexMatcher("[0-9a-f]{32}"), "*"));
    }

    public NormalizedResult normalize(String instanceName, String path) {
        if (path == null) {
            return new NormalizedResult(false, null);
        }

        //
        // 先使用domain的规则进行处理
        //
        if (instanceName != null && this.configs.getInstanceRules().containsKey(instanceName)) {
            path = normalize(this.configs.getInstanceRules().get(instanceName), path).getUri();
        }

        //
        // 再使用全局规则进行处理
        //
        return normalize(this.configs.getGlobalRules(), path);
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
                if (partRule.getMatcher().matches(path)) {
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
}
