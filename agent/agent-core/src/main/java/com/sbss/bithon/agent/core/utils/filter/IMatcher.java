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

package com.sbss.bithon.agent.core.utils.filter;

import shaded.com.fasterxml.jackson.annotation.JsonSubTypes;
import shaded.com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import shaded.com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 9:16 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @Type(name = "in", value = InCollectionMatcher.class),
    @Type(name = "contains", value = StringContainsMatcher.class),
    @Type(name = "startwith", value = StringPrefixMatcher.class),
    @Type(name = "endwith", value = StringSuffixMatcher.class)
})
public interface IMatcher {
    boolean matches(Object input);
}
