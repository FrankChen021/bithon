package com.sbss.bithon.server.common.utils.collection;


/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/8 22:09
 */
public interface SizedIterator<T> extends CloseableIterator<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }
}
