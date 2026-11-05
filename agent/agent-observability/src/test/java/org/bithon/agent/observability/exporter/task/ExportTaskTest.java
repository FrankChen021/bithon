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

package org.bithon.agent.observability.exporter.task;

import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExportTaskTest {

    private static final int FLUSH_TIME = 10_000;

    private ExporterConfig createConfig(int batchSize) {
        ExporterConfig config = new ExporterConfig();
        config.setBatchSize(batchSize);
        config.setFlushTime(FLUSH_TIME);
        return config;
    }

    private void addMessages(List<Object> exported, Object message) {
        synchronized (exported) {
            if (message instanceof Collection) {
                exported.addAll((Collection<?>) message);
            } else {
                exported.add(message);
            }
        }
    }

    private void testStopDoesNotWaitForFlushTime(int batchSize) throws InterruptedException {
        List<Object> exported = new ArrayList<>();
        ExportTask task = new ExportTask("test",
                                         new BlockingQueue(),
                                         createConfig(batchSize),
                                         (message) -> addMessages(exported, message));

        // Make sure the send thread is waiting on the queue
        Thread.sleep(200);

        task.accept(1);
        task.accept(2);

        long start = System.currentTimeMillis();
        task.stop();
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed < FLUSH_TIME / 2, "stop() took " + elapsed + "ms");

        List<Object> sorted = new ArrayList<>(exported);
        sorted.sort(null);
        Assertions.assertEquals(Arrays.asList(1, 2), sorted);

        // No more messages are accepted after stop
        task.accept(3);
        Assertions.assertFalse(task.canAccept());
        Assertions.assertEquals(2, exported.size());
    }

    @Test
    public void testStopWithoutBatch() throws InterruptedException {
        testStopDoesNotWaitForFlushTime(0);
    }

    @Test
    public void testStopWithBatch() throws InterruptedException {
        testStopDoesNotWaitForFlushTime(100);
    }

    @Test
    public void testStopIdleTask() {
        List<Object> exported = Collections.synchronizedList(new ArrayList<>());
        ExportTask task = new ExportTask("test",
                                         new BlockingQueue(),
                                         createConfig(100),
                                         exported::add);

        long start = System.currentTimeMillis();
        task.stop();
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed < FLUSH_TIME / 2, "stop() took " + elapsed + "ms");
    }
}
