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

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frank Chen
 * @date 26/3/24 3:25 pm
 */
public class RateLimitLogger implements Logger {
    static class Config {
        private final long max;

        long lastAccessTime;
        AtomicInteger count = new AtomicInteger(0);

        Config(long max) {
            this.max = max;
        }
    }

    private final Logger delegation;
    private final Config[] configPerLevel = new Config[5];

    public RateLimitLogger(Logger delegation) {
        this.delegation = delegation;
    }

    public RateLimitLogger config(Level level, int countPerSecond) {
        int index = toIndex(level);
        configPerLevel[index] = new Config(countPerSecond);
        return this;
    }

    private int toIndex(Level level) {
        if (level == Level.DEBUG) {
            return 0;
        } else if (level == Level.TRACE) {
            return 1;
        } else if (level == Level.INFO) {
            return 2;
        } else if (level == Level.WARN) {
            return 3;
        } else if (level == Level.ERROR) {
            return 4;
        } else {
            throw new RuntimeException("Invalid level");
        }
    }

    private boolean isRateLimited(Level level) {
        int index = toIndex(level);
        if (configPerLevel[index] == null) {
            return false;
        }

        long nowSecond = System.currentTimeMillis() / 1000;

        synchronized (configPerLevel[index]) {
            Config config = this.configPerLevel[index];
            long cnt;
            if (nowSecond != config.lastAccessTime) {
                // reset counter
                config.lastAccessTime = nowSecond;
                config.count.set(1);
                cnt = 1;
            } else {
                cnt = config.count.incrementAndGet();
            }
            return cnt > config.max;
        }
    }

    @Override
    public String getName() {
        return delegation.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegation.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegation.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (isRateLimited(Level.TRACE)) {
            return;
        }

        delegation.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegation.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegation.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (isRateLimited(Level.DEBUG)) {
            return;
        }

        delegation.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegation.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(format, arg);

    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(format, arg1, arg2);

    }

    @Override
    public void info(String format, Object... arguments) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(format, arguments);

    }

    @Override
    public void info(String msg, Throwable t) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(msg, t);

    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegation.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (isRateLimited(Level.INFO)) {
            return;
        }

        delegation.info(marker, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegation.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegation.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (isRateLimited(Level.WARN)) {
            return;
        }

        delegation.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegation.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegation.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (isRateLimited(Level.ERROR)) {
            return;
        }

        delegation.error(marker, msg, t);
    }
}
