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

package org.bithon.agent.plugin.thread.jdk.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/9/7 16:15
 */
public class ThreadPoolNameExtractorTest {

    @Test
    public void testStripSuffix() {
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task-", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task- ", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task - ", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task - %d", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task - %d ", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task - %n ", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("task", ThreadPoolNameExtractor.stripSuffix("task . %s ", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("DiskUsage-/mnt/disk/9", ThreadPoolNameExtractor.stripSuffix("DiskUsage-/mnt/disk/9/%n", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("async-process-thread", ThreadPoolNameExtractor.stripSuffix("async-process-thread-%s", ThreadPoolNameExtractor.SUFFIX_LIST));
        Assertions.assertEquals("Datanode ReportManager Thread", ThreadPoolNameExtractor.stripSuffix("Datanode ReportManager Thread - %d", ThreadPoolNameExtractor.SUFFIX_LIST));
    }
}
