/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.core.utils.time;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:13 下午
 */
public class Clock {
    final long millis;
    final long nanos;

    public Clock() {
        this.millis = System.currentTimeMillis();
        this.nanos = System.nanoTime();
    }

    public long currentMicroseconds() {
        return (System.nanoTime() - this.nanos) / 1000L + this.millis * 1000;
    }

    public long currentMilliseconds() {
        return this.millis;
    }

    @Override
    public String toString() {
        return "Clock{millis=" + this.millis + ", nanos=" + this.nanos + "}";
    }
}
