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

package org.bithon.component.logging.adaptor.log4j2;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.LoggingLevel;

import java.security.AccessController;
import java.security.PrivilegedAction;

import static org.bithon.component.logging.AbstractLogAdaptor.EXCEPTION_MESSAGE;

class Log4j2LogAdaptor extends ExtendedLoggerWrapper implements ILogAdaptor {

    private static final boolean VARARGS_ONLY;

    static {
        // check if current log4j2 supports vargs
        VARARGS_ONLY = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            try {
                Logger.class.getMethod("debug", String.class, Object.class);
                return false;
            } catch (NoSuchMethodException ignore) {
                // Log4J2 version too old.
                return true;
            } catch (SecurityException ignore) {
                // We could not detect the version so we will use Log4J2 if its on the classpath.
                return false;
            }
        });
    }

    Log4j2LogAdaptor(Logger logger) {
        super((ExtendedLogger) logger, logger.getName(), logger.getMessageFactory());
        if (VARARGS_ONLY) {
            throw new UnsupportedOperationException("Log4J2 version mismatch");
        }
    }

    private static Level toLevel(LoggingLevel level) {
        switch (level) {
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.DEBUG;
            case WARN:
                return Level.WARN;
            case ERROR:
                return Level.ERROR;
            case TRACE:
                return Level.TRACE;
            default:
                throw new Error();
        }
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public void trace(Throwable t) {
        log(Level.TRACE, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void debug(Throwable t) {
        log(Level.DEBUG, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void info(Throwable t) {
        log(Level.INFO, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void warn(Throwable t) {
        log(Level.WARN, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void error(Throwable t) {
        log(Level.ERROR, EXCEPTION_MESSAGE, t);
    }

    @Override
    public boolean isEnabled(LoggingLevel level) {
        return isEnabled(toLevel(level));
    }

    @Override
    public void log(LoggingLevel level, String msg) {
        log(toLevel(level), msg);
    }

    @Override
    public void log(LoggingLevel level, String format, Object arg) {
        log(toLevel(level), format, arg);
    }

    @Override
    public void log(LoggingLevel level, String format, Object argA, Object argB) {
        log(toLevel(level), format, argA, argB);
    }

    @Override
    public void log(LoggingLevel level, String format, Object... arguments) {
        log(toLevel(level), format, arguments);
    }

    @Override
    public void log(LoggingLevel level, String msg, Throwable t) {
        log(toLevel(level), msg, t);
    }

    @Override
    public void log(LoggingLevel level, Throwable t) {
        log(toLevel(level), EXCEPTION_MESSAGE, t);
    }
}
