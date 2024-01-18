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

package org.bithon.component.commons.utils;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 25/7/23 9:34 am
 */
public class RetryUtils {
    public static <T> T retry(Callable<T> callable,
                              Predicate<Exception> retryPredicate,
                              int maxTries,
                              Duration backoff) throws Exception {
        Exception lastException = null;

        for (int i = 0; i < maxTries; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                if (!retryPredicate.test(e)) {
                    throw e;
                }

                lastException = e;
            }

            if (i < maxTries - 1) {
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw new RuntimeException(StringUtils.format("Exception after retry %d times", maxTries), lastException);
    }

    public static void retry(Runnable runnable, Predicate<Exception> retryPredicate, int maxTries, Duration backoff) throws Exception {
        Exception lastException = null;

        for (int i = 0; i < maxTries; i++) {
            try {
                runnable.run();
                return;
            } catch (Exception e) {
                if (!retryPredicate.test(e)) {
                    throw e;
                }

                lastException = e;
            }

            if (i < maxTries - 1) {
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw lastException;
    }
}
