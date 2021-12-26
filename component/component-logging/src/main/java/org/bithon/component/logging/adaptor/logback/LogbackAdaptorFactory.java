package org.bithon.component.logging.adaptor.logback;

import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.ILogAdaptorFactory;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * @author Frank Chen
 * @date 26/12/21 6:35 PM
 */
public class LogbackAdaptorFactory implements ILogAdaptorFactory {

    @Override
    public ILogAdaptor newLogger(String name) {
        return new LogbackLogAdaptor(StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(name));
    }
}
