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

import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;

import java.util.List;

/**
 * A logger for context logs generated during evaluation of each alert
 * <p>
 * The lifetime of each logger starts from the starting point of evaluation of an alert, and ends at the evaluation.
 * So for the implementation, no need to consider race condition across multiple threads.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/1/26
 */
public interface IEvaluationLogWriter extends AutoCloseable {

    void setInstance(String instance);

    void write(EvaluationLogEvent logEvent);

    void write(List<EvaluationLogEvent> logs);

    /**
     * override to ignore the checked exception
     */
    void close();
}
