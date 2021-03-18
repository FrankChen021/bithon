package com.sbss.bithon.agent.core.dispatcher;


import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannelFactory;
import com.sbss.bithon.agent.core.dispatcher.task.DispatchTask;
import com.sbss.bithon.agent.core.dispatcher.task.FileQueueImpl;
import com.sbss.bithon.agent.core.dispatcher.task.IMessageQueue;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class Dispatcher {
    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private final String agentPath;
    private final String dispatcherName;
    private final String appName;
    private final IMessageConverter messageConverter;
    private final IMessageChannel dispatcher;
    private final DispatcherConfig dispatcherConfig;
    private int appPort;
    private DispatchTask task;

    Dispatcher(String dispatcherName,
               String agentPath,
               AppInstance appInstance,
               DispatcherConfig dispatcherConfig) throws Exception {
        this.dispatcherName = dispatcherName;
        this.agentPath = agentPath;
        this.dispatcherConfig = dispatcherConfig;
        this.appName = appInstance.getAppName();

        //
        // create dispatcher instance by config
        //
        IMessageChannelFactory factory = createDispatcherFactory(dispatcherConfig);
        Method createMethod = IMessageChannelFactory.class.getMethod("create" + capitalize(dispatcherName) + "Channel",
                                                                     DispatcherConfig.class);
        this.dispatcher = (IMessageChannel) createMethod.invoke(factory, dispatcherConfig);
        this.messageConverter = factory.createMessageConverter();

        if (appInstance.getPort() == 0) {
            appInstance.addListener(this::startTask);
        }
        startTask(appInstance.getPort());
    }

    public boolean isReady() {
        return task != null;
    }

    public IMessageConverter getMessageConverter() {
        return this.messageConverter;
    }

    public void sendMessage(Object message) {
        if (task != null && message != null) {
            task.sendMessage(message);
        }
    }

    private String capitalize(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return (char) (s.charAt(0) - 'a' + 'A') + s.substring(1);
        } else {
            return s;
        }
    }

    private IMessageChannelFactory createDispatcherFactory(DispatcherConfig config) throws Exception {
        return (IMessageChannelFactory) Class.forName(config.getClient().getFactory()).newInstance();
    }

    synchronized private void startTask(int port) {
        if (appPort != 0 || port == 0) {
            return;
        }
        this.appPort = port;

        log.info("Application port updated to {}, {} will soon be at work",
                 port,
                 this.dispatcherName);

        task = new DispatchTask(dispatcherName,
                                createQueue(),
                                dispatcherConfig,
                                dispatcher::sendMessage);
    }

    private IMessageQueue createQueue() {
        try {
            return new FileQueueImpl(agentPath
                                     + separator
                                     + AgentContext.TMP_DIR
                                     + separator
                                     + dispatcherName
                                     + separator
                                     + appName,
                                     String.valueOf(appPort));
        } catch (IOException e) {

            System.exit(0);
            return null;
        }
    }
}
