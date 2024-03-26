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

package org.bithon.server.commons.logging;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import static org.mockito.Mockito.times;

/**
 * @author Frank Chen
 * @date 26/3/24 9:09 pm
 */
public class RateLimitLoggerTest {

    @Test
    public void testRateLimit() throws InterruptedException {
        Logger logger = Mockito.mock(Logger.class);
        RateLimitLogger rateLimitLogger = new RateLimitLogger(logger).config(Level.INFO, 1);

        // Since the rate is 1 per second, several consecutive calls to the info will be limited
        rateLimitLogger.info("msg1");
        rateLimitLogger.info("msg2");
        rateLimitLogger.info("msg3");
        Mockito.verify(logger, times(1)).info("msg1");
        Mockito.verify(logger, times(0)).info("msg2");
        Mockito.verify(logger, times(0)).info("msg3");

        // Sleep for 1 second to wait for the new counter
        Thread.sleep(1100);
        rateLimitLogger.info("msg4");
        rateLimitLogger.info("msg5");
        rateLimitLogger.info("msg6");
        Mockito.verify(logger, times(1)).info("msg4");
        Mockito.verify(logger, times(0)).info("msg5");
        Mockito.verify(logger, times(0)).info("msg6");

        // debug is not configured
        rateLimitLogger.debug("d1");
        rateLimitLogger.debug("d2");
        rateLimitLogger.debug("d3");
        Mockito.verify(logger, times(1)).debug("d1");
        Mockito.verify(logger, times(1)).debug("d2");
        Mockito.verify(logger, times(1)).debug("d3");
    }
}
