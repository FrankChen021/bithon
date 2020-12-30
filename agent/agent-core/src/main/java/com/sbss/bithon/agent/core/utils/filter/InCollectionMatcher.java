package com.sbss.bithon.agent.core.utils.filter;

import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 9:28 下午
 */
public class InCollectionMatcher implements IMatcher {

    private Set<String> collection;

    public InCollectionMatcher(Set<String> collection) {
        this.collection = collection;
    }

    @Override
    public boolean matches(Object input) {
        return input != null && collection.contains(input);
    }
}
