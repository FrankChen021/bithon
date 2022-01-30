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

package org.bithon.agent.core.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 3:18 下午
 */
public class CollectionUtils {

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> list) {
        return !isEmpty(list);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Object[] objs) {
        return objs == null || objs.length == 0;
    }
}
