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

package org.bithon.server.storage.jdbc.alerting;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 12/11/21 4:31 pm
 */
@Slf4j
public class JdbcLogWriter implements IEvaluationLogWriter {

    private final DSLContext dslContext;

    public JdbcLogWriter(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void write(EvaluationLogEvent logEvent) {
        write(Collections.singletonList(logEvent));
    }

    @Override
    public void write(List<EvaluationLogEvent> logs) {
        if (logs.isEmpty()) {
            return;
        }

        BatchBindStep step = dslContext.batch(dslContext.insertInto(Tables.BITHON_ALERT_EVALUATION_LOG,
                                                                    Tables.BITHON_ALERT_EVALUATION_LOG.TIMESTAMP,
                                                                    Tables.BITHON_ALERT_EVALUATION_LOG.ALERT_ID,
                                                                    Tables.BITHON_ALERT_EVALUATION_LOG.SEQUENCE,
                                                                    Tables.BITHON_ALERT_EVALUATION_LOG.CLAZZ,
                                                                    Tables.BITHON_ALERT_EVALUATION_LOG.MESSAGE)
                                                        .values((LocalDateTime) null,
                                                                null,
                                                                null,
                                                                null,
                                                                null
                                                               ));

        int fieldLength = Tables.BITHON_ALERT_EVALUATION_LOG.CLAZZ.getDataType().length();
        for (EvaluationLogEvent log : logs) {
            String clazz = log.getClazz();
            if (log.getClazz().length() > fieldLength) {
                // ensure the text is within the size limit of this field
                clazz = clazz.substring(clazz.length() - fieldLength);
            }

            step.bind(log.getTimestamp(),
                      log.getAlertId(),
                      log.getSequence(),
                      clazz,
                      log.getMessage());
        }
        step.execute();
    }

    @Override
    public void close() {
    }
}
