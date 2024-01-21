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

package org.bithon.server.storage.alerting;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.common.IStorage;
import org.bithon.server.storage.common.expiration.IExpirable;

/**
 * @author Frank Chen
 * @date 19/3/22 12:47 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IEvaluationLogStorage extends IStorage, IExpirable {

    void initialize();

    IEvaluationLogWriter createWriter();

    IEvaluationLogReader createReader();
}
