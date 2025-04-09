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

package org.bithon.component.commons.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    public static <E> Set<E> emptyOrOriginal(Set<E> collection) {
        return collection == null ? Collections.emptySet() : collection;
    }

    public static <E> List<E> emptyOrOriginal(List<E> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

    public static boolean isArrayEqual(Object[] a, Object[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    public static <T> List<T> slice(List<T> list, int index) {
        if (index < 0 || index >= list.size()) {
            return Collections.emptyList();
        }
        return list.subList(index, list.size());
    }

    public static <T> List<T> slice(List<T> list, int index, int length) {
        if (index < 0 || index >= list.size()) {
            return Collections.emptyList();
        }
        if (length <= 0) {
            return Collections.emptyList();
        }
        if (index + length > list.size()) {
            return Collections.emptyList();
        }
        return list.subList(index, index + length);
    }

}
