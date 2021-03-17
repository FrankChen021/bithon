package com.sbss.bithon.agent.core.metrics.web;

import com.sbss.bithon.agent.core.utils.filter.InCollectionMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
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
