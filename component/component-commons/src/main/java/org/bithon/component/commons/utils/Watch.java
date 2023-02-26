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

import java.util.concurrent.Callable;

/**
 * @author Frank Chen
 * @date 26/2/23 5:23 pm
 */
public class Watch<V> {

    private final V result;
    private long duration;

    public Watch(Callable<V> callable) {
        long start = System.currentTimeMillis();
        try {
            this.result = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.duration = System.currentTimeMillis() - start;
        }
    }

    public Watch(Runnable runnable) {
        long start = System.currentTimeMillis();
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.duration = System.currentTimeMillis() - start;
        }
        this.result = null;
    }

    public V getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }
}
