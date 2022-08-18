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

package org.bithon.server.commons.time;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Frank Chen
 * @date 18/8/22 3:49 pm
 */
public class TimeSpanTest {

    @Test
    public void testBefore() {

        TimeSpan span = new TimeSpan(System.currentTimeMillis());
        Assert.assertEquals(span.getMilliseconds() - 10, span.before(10, TimeUnit.MILLISECONDS).getMilliseconds());

        Assert.assertEquals(span.getMilliseconds() - 1000, span.before(1, TimeUnit.SECONDS).getMilliseconds());

        Assert.assertEquals(span.getMilliseconds() - 60_000, span.before(1, TimeUnit.MINUTES).getMilliseconds());

        Assert.assertEquals(span.getMilliseconds() - 3600_000, span.before(1, TimeUnit.HOURS).getMilliseconds());

        Assert.assertEquals(span.getMilliseconds() - 24 * 3600_000, span.before(1, TimeUnit.DAYS).getMilliseconds());
    }
}
