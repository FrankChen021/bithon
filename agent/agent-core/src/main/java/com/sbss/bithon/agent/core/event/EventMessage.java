package com.sbss.bithon.agent.core.event;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:44 下午
 */
public class EventMessage {
    private final String messageType;
    private final Map<String, String> args;

    public EventMessage(String messageType, Map<String, String> args) {
        this.messageType = messageType;
        this.args = args;
    }

    public String getMessageType() {
        return messageType;
    }

    public Map<String, String> getArgs() {
        return args;
    }
}
