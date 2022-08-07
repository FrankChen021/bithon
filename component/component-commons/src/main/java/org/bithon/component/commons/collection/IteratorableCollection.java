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

package org.bithon.component.commons.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 10/12/21 6:48 PM
 */
public class IteratorableCollection<E> implements Iterator<E> {
    private final List<E> collections = new ArrayList<>();
    private final Iterator<E> delegate;
    private boolean end;

    protected IteratorableCollection(Iterator<E> delegate) {
        this.delegate = delegate;
    }

    public static <E> IteratorableCollection<E> of(Iterator<E> delegate) {
        return new IteratorableCollection<>(delegate);
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = delegate.hasNext();
        if (!hasNext) {
            end = true;
        }
        return hasNext;
    }

    @Override
    public E next() {
        E e = delegate.next();
        collections.add(e);
        return e;
    }

    /**
     * call this method will reset the iterator to the end
     */
    public List<E> toCollection() {
        if (end) {
            return collections;
        }
        while (hasNext()) {
            next();
        }
        return collections;
    }
}
