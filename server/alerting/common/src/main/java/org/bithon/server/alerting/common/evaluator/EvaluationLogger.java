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

package org.bithon.server.alerting.common.evaluator;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.springframework.boot.logging.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Frank Chen
 * @date 15/2/24 10:58 am
 */
@Slf4j
public class EvaluationLogger {
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final IEvaluationLogWriter logWriter;

    public EvaluationLogger(IEvaluationLogWriter logWriter) {
        this.logWriter = logWriter;
    }

    public void info(String alertId,
                     String alertName,
                     Class<?> logClass,
                     String format,
                     Object... args) {
        info(alertId, alertName, logClass, StringUtils.format(format, args));
    }

    public void error(String ruleId,
                      String ruleName,
                      Class<?> logClass,
                      Throwable exception,
                      String messageFormat,
                      Object... args) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            exception.printStackTrace(pw);
        }
        log(ruleId, ruleName, LogLevel.ERROR, logClass, StringUtils.format(messageFormat, args) + "\nException: " + sw);
    }

    public void info(String ruleId, String ruleName, Class<?> logClass, String message) {
        log(ruleId, ruleName, LogLevel.INFO, logClass, message);
    }

    private void log(String ruleId, String ruleName, LogLevel level, Class<?> logClass, String message) {
        log.info("[{}] [{} {}]: {}",
                 logClass.getSimpleName(),
                 ruleId,
                 ruleName,
                 message);

        EvaluationLogEvent log = new EvaluationLogEvent();
        log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSequence(SEQUENCE.getAndIncrement());
        log.setLevel(level.name());
        log.setAlertId(ruleId);
        log.setClazz(logClass.getSimpleName());
        log.setMessage(message);

        this.logWriter.write(log);
    }
}
