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

package org.bithon.server.storage.common.expiration;

/**
 *
 * If implemented, no need to do explicitly register to be scheduled.
 * For each storage object, it's a Spring Bean that can be obtained through {@link org.springframework.context.ApplicationContext}.
 * The {@link ExpirationScheduler} automatically find all expirable storage to schedule the expiration.
 *
 * @author frank chen
 */
public interface IExpirable {
    IExpirationRunnable getExpirationRunnable();
}
