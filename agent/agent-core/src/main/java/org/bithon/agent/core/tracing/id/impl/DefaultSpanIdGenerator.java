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

package org.bithon.agent.core.tracing.id.impl;

import org.bithon.agent.core.tracing.id.ISpanIdGenerator;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/7 10:37 下午
 */
public class DefaultSpanIdGenerator implements ISpanIdGenerator {
    private final long base;
    private final AtomicInteger counter;

    public DefaultSpanIdGenerator() {
        long processId = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        base = (System.nanoTime() << 48) | (processId << 32);
        counter = new AtomicInteger(1);
    }

    @Override
    public String newSpanId() {
        return String.format(Locale.ENGLISH, "%16x", base | counter.getAndIncrement());
    }
}
