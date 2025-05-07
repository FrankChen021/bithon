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


import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 6/5/25 9:27 pm
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

    static <E, R> CloseableIterator<R> transform(Iterator<E> iterator, Function<E, R> mapFunction, AutoCloseable closeable) {
        return new CloseableIterator<R>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return mapFunction.apply(iterator.next());
            }

            @Override
            public void close() throws IOException {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            }
        };
    }
}
