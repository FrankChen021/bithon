package com.sbss.bithon.agent.dispatcher;

import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frankchen
 * @Date 2020-05-18 16:27:24
 */
public class WebContainer {

    private static final Logger log = LoggerFactory.getLogger(WebContainer.class);

    public enum ContainerType {
        TOMCAT,
        JETTY,
        NETTY,
        UNDERTOW
    }

    public static interface IContainerListener {
        void onStart(ContainerType type, int port);
    }

    private static ContainerType containerType;

    public static ContainerType getType() {
        return containerType;
    }

    private static List<IContainerListener> listeners = new ArrayList<>();

    public static void notifyStarted(ContainerType type, int port) {
        containerType = type;
        for (IContainerListener listener : listeners) {
            try {
                listener.onStart(type, port);
            } catch (Exception e) {
                log.error("web container notify failed", e);
            }
        }
    }

    public static void addListener(IContainerListener listener) {
        listeners.add(listener);
    }
}
