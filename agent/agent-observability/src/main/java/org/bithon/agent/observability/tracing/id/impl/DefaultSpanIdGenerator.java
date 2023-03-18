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

package org.bithon.agent.observability.tracing.id.impl;

import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/7 10:37 下午
 */
public class DefaultSpanIdGenerator implements ISpanIdGenerator {
    private final long processId;
    private final AtomicInteger counter;

    public DefaultSpanIdGenerator() {
        // get the process id
        long id = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

        // we only use the lower 16 bits
        id &= 0xFFFF;

        //
        // make sure the id has 4 digits in HEX to ensure the final span id has 16 digits in HEX
        // and 0x1000 is the lowest number that has 4 digits in HEX
        //
        if (id < 0x1000) {
            id += ThreadLocalRandom.current().nextInt(0x1000, 0xE000);
        }

        // this id takes the first 4 digits of total 16 digits
        this.processId = id << 48;

        // integer is enough even though it may overflow in a foreseeable time
        // the final span id has a time part which guarantees the value to be different after overflow
        this.counter = new AtomicInteger(0);
    }

    @Override
    public String newSpanId() {
        return Long.toHexString(processId | (((System.nanoTime() & 0xFFFF) << 32)) | counter.getAndIncrement());
    }
}
