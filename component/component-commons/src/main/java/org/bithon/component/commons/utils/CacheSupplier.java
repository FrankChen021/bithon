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

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/10 21:38
 */
public class CacheSupplier<T> {
    private final Supplier<T> supplier;
    private volatile T object;

    public static <T> CacheSupplier<T> of(Supplier<T> supplier) {
        return new CacheSupplier<>(supplier);
    }

    private CacheSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        T tmp = object;
        if (tmp == null) {
            synchronized (this) {
                tmp = object;
                if (tmp == null) {
                    object = tmp = supplier.get();
                }
            }
        }
        return tmp;
    }
}
