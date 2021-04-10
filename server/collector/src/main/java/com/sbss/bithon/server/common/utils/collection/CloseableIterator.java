package com.sbss.bithon.server.common.utils.collection;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * @author frankchen
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
    default <R> CloseableIterator<R> map(Function<T, R> mapFunction) {
        final CloseableIterator<T> delegate = this;

        return new CloseableIterator<R>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return mapFunction.apply(delegate.next());
            }

            @Override
            public void close() throws IOException {
                delegate.close();
            }
        };
    }

    default <R> CloseableIterator<R> flatMap(Function<T, CloseableIterator<R>> function) {
        final CloseableIterator<T> delegate = this;

        return new CloseableIterator<R>() {
            CloseableIterator<R> iterator = findNextIteratorIfNecessary();

            @Nullable
            private CloseableIterator<R> findNextIteratorIfNecessary() {
                while ((iterator == null || !iterator.hasNext()) && delegate.hasNext()) {
                    if (iterator != null) {
                        try {
                            iterator.close();
                            iterator = null;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    iterator = function.apply(delegate.next());
                    if (iterator.hasNext()) {
                        return iterator;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return iterator != null && iterator.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    return iterator.next();
                } finally {
                    findNextIteratorIfNecessary();
                }
            }

            @Override
            public void close() throws IOException {
                delegate.close();
                if (iterator != null) {
                    iterator.close();
                    iterator = null;
                }
            }
        };
    }
}
