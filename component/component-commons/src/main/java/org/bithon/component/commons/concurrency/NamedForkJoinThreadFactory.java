package org.bithon.component.commons.concurrency;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frank Chen
 * @date 13/4/22 6:25 PM
 */
public class NamedForkJoinThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public NamedForkJoinThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix + "-";
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName(namePrefix + threadNumber.getAndIncrement());
        return thread;
    }
}
