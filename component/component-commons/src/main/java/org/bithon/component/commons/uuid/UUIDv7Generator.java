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

package org.bithon.component.commons.uuid;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The Unix Epoch time-based UUID specified in RFC 9562.
 * <p>
 * This class is adapted from <a href="https://github.com/f4b6a3/uuid-creator">UUID-Creator</a> under its MIT license.
 *
 * @author frank.chen021@outlook.com
 * @date 14/5/24 9:30 pm
 */
public abstract class UUIDv7Generator {

    /**
     * add 2^48 to `rand_b`
     */
    public static final int INCREMENT_TYPE_DEFAULT = 0;

    /**
     * just add 1 to `rand_b`
     */
    public static final int INCREMENT_TYPE_PLUS_1 = 1;

    /**
     * add a random n to `rand_b`, where 1 <= n <= 2^32
     */
    public static final int INCREMENT_TYPE_PLUS_N = 2;

    private static final long INCREMENT_MAX_DEFAULT = 0xffffffffL; // 2^32-1

    // Used to preserve monotonicity when the system clock is
    // adjusted by NTP after a small clock drift or when the
    // system clock jumps back by 1 second due to leap second.
    private static final long CLOCK_DRIFT_TOLERANCE = 10_000;

    private static final long VERSION_MASK = 0x000000000000f000L;
    private static final long VARIANT_MASK = 0xc000000000000000L;
    private static final long LOWER_16_MASK = 0x000000000000ffffL;
    private static final long UPPER_16_MASK = 0xffff000000000000L;
    protected static final long OVERFLOW = 0x0000000000000000L;

    public static UUIDv7Generator create(int type) {
        switch (type) {
            case INCREMENT_TYPE_PLUS_1:
                return new Plus1Function();
            case INCREMENT_TYPE_PLUS_N:
                return new PlusNFunction(INCREMENT_MAX_DEFAULT);
            case INCREMENT_TYPE_DEFAULT:
            default:
                return new DefaultFunction();
        }
    }

    protected long msb = 0L; // most significant bits
    protected long lsb = 0L; // least significant bits

    protected final ReentrantLock lock = new ReentrantLock();

    private UUIDv7Generator() {
        reset(getCurrentTimestamp());
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a time-ordered unique identifier (UUIDv7).
     *
     * @return a UUIDv7
     */
    public UUID generate() {
        lock.lock();
        try {
            final long lastTime = this.time();
            final long time = getCurrentTimestamp();

            // Check if the current time is the same as the previous time or has moved
            // backwards after a small system clock adjustment or after a leap second.
            // Drift tolerance = (previous_time - 10s) < current_time <= previous_time
            if ((time > lastTime - CLOCK_DRIFT_TOLERANCE) && (time <= lastTime)) {
                increment();
            } else {
                reset(time);
            }

            return new UUID(this.msb, this.lsb);

        } finally {
            lock.unlock();
        }
    }

    // to be implemented
    protected abstract void increment();

    private long time() {
        return this.msb >>> 16;
    }

    private void reset(final long time) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.msb = (time << 16) | (rnd.nextLong() & LOWER_16_MASK);
        this.lsb = rnd.nextLong();
    }

    private static final class DefaultFunction extends UUIDv7Generator {

        @Override
        protected void increment() {

            // add 2^48 to rand_b
            this.lsb = (this.lsb & UPPER_16_MASK);
            this.lsb = (this.lsb | VARIANT_MASK) + (1L << 48);

            if (this.lsb == OVERFLOW) {
                // add 1 to rand_a if rand_b overflows
                this.msb = (this.msb | VERSION_MASK) + 1L;
            }

            this.lsb |= ThreadLocalRandom.current().nextLong() & (~UPPER_16_MASK);
        }
    }

    private static final class Plus1Function extends UUIDv7Generator {

        @Override
        protected void increment() {
            // add 1 to rand_b
            this.lsb = (this.lsb | VARIANT_MASK) + 1L;

            if (this.lsb == OVERFLOW) {
                // add 1 to rand_a if rand_b overflows
                this.msb = (this.msb | VERSION_MASK) + 1L;
            }
        }
    }

    private static final class PlusNFunction extends UUIDv7Generator {

        private final long incrementMax;

        public PlusNFunction(long incrementMax) {
            this.incrementMax = incrementMax;
        }

        @Override
        protected void increment() {
            long val;
            if (incrementMax == INCREMENT_MAX_DEFAULT) {
                // return n, where 1 <= n <= 2^32
                val = (ThreadLocalRandom.current().nextLong() >>> 32) + 1;
            } else {
                final long positive = 0x7fffffffffffffffL;
                // return n, where 1 <= n <= incrementMax
                val = ((ThreadLocalRandom.current().nextLong() & positive) % incrementMax) + 1;
            }

            // add a random n to rand_b, where 1 <= n <= incrementMax
            this.lsb = (this.lsb | VARIANT_MASK) + val;

            if (this.lsb == OVERFLOW) {
                // add 1 to rand_a if rand_b overflows
                this.msb = (this.msb | VERSION_MASK) + 1L;
            }
        }
    }
}
