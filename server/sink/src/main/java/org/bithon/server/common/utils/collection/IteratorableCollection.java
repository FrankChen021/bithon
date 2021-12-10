package org.bithon.server.common.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Frank Chen
 * @date 10/12/21 6:48 PM
 */
public class IteratorableCollection<E> implements Iterator<E> {
    private final List<E> collections = new ArrayList<>();
    private final Iterator<E> delegate;
    private boolean end;

    public static <E> IteratorableCollection<E> of(Iterator<E> delegate) {
        return new IteratorableCollection<>(delegate);
    }

    private IteratorableCollection(Iterator<E> delegate) {
        this.delegate = delegate;
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
    public Collection<E> toCollection() {
        if (end) {
            return collections;
        }
        while(hasNext()) {
            next();
        }
        return collections;
    }
}
