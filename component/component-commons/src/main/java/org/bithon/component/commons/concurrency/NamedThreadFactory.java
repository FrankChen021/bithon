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

package org.bithon.component.commons.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-21:46
 */
public class NamedThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean isDaemon;

    private NamedThreadFactory(String namePrefix, boolean isDaemon) {
        final SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
        this.isDaemon = isDaemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread t = new Thread(group, r, namePrefix + "-" + threadNumber.getAndIncrement(), 0);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        t.setDaemon(isDaemon);
        return t;
    }

    /**
     * Define two thread factories for creating daemon and non-daemon threads
     * This helps us to use forbidden-apis-maven-plugin to detect improper usage of these factories
     */
    public static ThreadFactory nonDaemonThreadFactory(String namePrefix) {
        return new NamedThreadFactory(namePrefix, false);
    }

    public static ThreadFactory daemonThreadFactory(String namePrefix) {
        return new NamedThreadFactory(namePrefix, true);
    }
}
