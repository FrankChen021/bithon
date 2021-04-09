package com.sbss.bithon.server.common.utils.collection;


import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

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