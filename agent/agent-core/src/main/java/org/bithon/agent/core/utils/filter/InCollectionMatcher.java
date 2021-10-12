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

package org.bithon.agent.core.utils.filter;

import shaded.com.fasterxml.jackson.annotation.JsonCreator;
import shaded.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 9:28 下午
 */
public class InCollectionMatcher implements IMatcher {

    public static final String TYPE = "in";

    private final Set<String> collection;

    @JsonCreator
    public InCollectionMatcher(@JsonProperty("collection") Collection<String> collection) {
        this.collection = new HashSet<>(collection);
    }

    @Override
    public String toString() {
        return "InCollectionMatcher{" +
               "collection=" + collection +
               '}';
    }

    @Override
    public boolean matches(Object input) {
        return input != null && collection.contains(input);
    }
}
