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

package org.bithon.server.alerting.manager.biz;

import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.model.GetEvaluationLogsResponse;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogReader;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/26
 */
@Service
@Conditional(ManagerModuleEnabler.class)
public class EvaluationLogService {

    private final IEvaluationLogReader logReader;

    public EvaluationLogService(IEvaluationLogStorage logStorage) {
        this.logReader = logStorage.createReader();
    }

    public GetEvaluationLogsResponse getEvaluationLogs(String alertId,
                                                       TimeSpan startTimestamp,
                                                       TimeSpan endTimestamp) {
        List<EvaluationLogEvent> logs = logReader.getLogs(alertId,
                                                          startTimestamp,
                                                          endTimestamp);

        return new GetEvaluationLogsResponse(logs.size(), logs);
    }
}
