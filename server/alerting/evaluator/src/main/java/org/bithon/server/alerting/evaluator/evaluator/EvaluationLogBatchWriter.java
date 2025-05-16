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

package org.bithon.server.alerting.evaluator.evaluator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Frank Chen
 * @date 15/2/24 10:29 am
 */
@Slf4j
public class EvaluationLogBatchWriter implements IEvaluationLogWriter {
    @Getter
    private final IEvaluationLogWriter delegate;
    private final Duration flushInterval;
    private final int batchSize;
    private final FlushTask flushTask;
    private List<EvaluationLogEvent> logs = new ArrayList<>();

    class FlushTask extends PeriodicTask {

        public FlushTask() {
            super("alert-eval-log-flush", flushInterval, false);
        }

        @Override
        protected void onRun() {
            flush();
        }

        @Override
        protected void onException(Exception e) {
        }
    }

    public EvaluationLogBatchWriter(IEvaluationLogWriter delegate,
                                    Duration flushInterval,
                                    int batchSize) {
        this.delegate = delegate;
        this.flushInterval = flushInterval;
        this.batchSize = batchSize;
        this.flushTask = new FlushTask();
    }

    @Override
    public void setInstance(String instance) {
        this.delegate.setInstance(instance);
    }

    @Override
    public void write(EvaluationLogEvent logEvent) {
        synchronized (this) {
            this.logs.add(logEvent);
        }
        if (this.logs.size() > this.batchSize) {
            flush();
        }
    }

    @Override
    public void write(List<EvaluationLogEvent> logs) {
        synchronized (this) {
            this.logs.addAll(logs);
        }
        if (this.logs.size() > this.batchSize) {
            flush();
        }
    }

    public void start() {
        this.flushTask.start();
    }

    public boolean isStarted() {
        return this.flushTask.isRunning();
    }

    @Override
    public void close() {
        flushTask.stop();

        // Make sure all cached contents have been flushed
        flush();
    }

    private void flush() {
        if (this.logs.isEmpty()) {
            return;
        }

        // Swap the object for flushing
        List<EvaluationLogEvent> flushLogs;
        synchronized (this) {
            flushLogs = this.logs;
            this.logs = new ArrayList<>(this.batchSize);
        }

        // Double check
        if (flushLogs.isEmpty()) {
            return;
        }
        try {
            log.info("Flushing [{}] evaluation logs into storage...", flushLogs.size());
            this.delegate.write(flushLogs);
        } catch (Exception e) {
            log.error("Exception when flushing metrics into storage", e);
        }
    }
}
