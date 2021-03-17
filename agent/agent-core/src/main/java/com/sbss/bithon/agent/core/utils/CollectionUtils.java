package com.sbss.bithon.agent.core.utils;

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

    public static boolean isEmpty(Object[] objs) {
        return objs == null || objs.length == 0;
    }
}
