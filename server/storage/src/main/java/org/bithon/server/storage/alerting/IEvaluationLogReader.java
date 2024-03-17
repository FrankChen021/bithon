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

import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;

import java.util.List;

/**
 * @author Frank Chen
 * @date 20/3/22 6:44 PM
 */
public interface IEvaluationLogReader {
    List<EvaluationLogEvent> getLogs(String alertId, TimeSpan start, TimeSpan end);
}
