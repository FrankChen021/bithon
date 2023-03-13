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

package org.bithon.server.storage.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.common.IExpirable;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.common.IStorage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:17 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IEventStorage extends IStorage, IExpirable {

    default String getName() {
        return "event";
    }

    default void initialize() {
    }

    IEventWriter createWriter();

    IEventReader createReader();

    IExpirationRunnable getExpirationRunnable();
}
