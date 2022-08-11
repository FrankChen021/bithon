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

package org.bithon.agent.core.dispatcher;


import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.channel.IMessageChannel;
import org.bithon.agent.core.dispatcher.channel.IMessageChannelFactory;
import org.bithon.agent.core.dispatcher.config.DispatcherConfig;
import org.bithon.agent.core.dispatcher.task.BlockingQueue;
import org.bithon.agent.core.dispatcher.task.DispatchTask;
import org.bithon.agent.core.dispatcher.task.IMessageQueue;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class Dispatcher {
    private static final ILogAdaptor log = LoggerFactory.getLogger(Dispatcher.class);

    private final String dispatcherName;
    private final IMessageConverter messageConverter;
    private final IMessageChannel messageChannel;
    private final DispatcherConfig dispatcherConfig;
    private int appPort;
    private DispatchTask task;

    Dispatcher(String dispatcherName,
               AppInstance appInstance,
               DispatcherConfig dispatcherConfig) throws Exception {
        this.dispatcherName = dispatcherName;
        this.dispatcherConfig = dispatcherConfig;

        //
        // create dispatcher instance by config
        //
        IMessageChannelFactory factory = createDispatcherFactory(dispatcherConfig);
        Method createMethod = IMessageChannelFactory.class.getMethod("create" + capitalize(dispatcherName) + "Channel",
                                                                     DispatcherConfig.class);
        this.messageChannel = (IMessageChannel) createMethod.invoke(factory, dispatcherConfig);
        this.messageConverter = factory.createMessageConverter();

        if (appInstance.getPort() == 0) {
            appInstance.addListener(this::startTask);
        }
        startTask(appInstance.getPort());
    }

    public boolean isReady() {
        return task != null && task.canAccept();
    }

    public void onReady(Consumer<Dispatcher> listener) {
        AgentContext.getInstance().getAppInstance().addListener(port -> listener.accept(this));
    }

    public IMessageConverter getMessageConverter() {
        return this.messageConverter;
    }

    /**
     * will be replaced by {@link #send(Collection)} once underlying send method on channel is refactor to have the same interface
     */
    public void sendMessage(Object message) {
        if (task != null && message != null) {
            task.accept(message);
        }
    }

    public void send(Collection<Object> message) {
        if (task != null && message != null) {
            task.accept(message);
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
        return (IMessageChannelFactory) Class.forName(config.getClient().getFactory())
                                             .getDeclaredConstructor()
                                             .newInstance();
    }

    private synchronized void startTask(int port) {
        if (appPort != 0 || port == 0) {
            return;
        }
        this.appPort = port;

        log.info("Application port updated to {}, {} will soon be at work",
                 port,
                 this.dispatcherName);

        task = new DispatchTask(dispatcherName,
                                createQueue(dispatcherConfig),
                                dispatcherConfig,
                                messageChannel::sendMessage);
    }

    private IMessageQueue createQueue(DispatcherConfig config) {
        return new BlockingQueue(config.getQueueSize());
    }

    public void shutdown() {
        log.info("Shutting down dispatcher task [{}]...", dispatcherName);
        if (task != null) {
            task.stop();
        }

        // stop underlying message channel
        log.info("Closing message channel [{}]...", dispatcherName);
        try {
            messageChannel.close();
        } catch (Exception ignored) {
        }
    }
}
