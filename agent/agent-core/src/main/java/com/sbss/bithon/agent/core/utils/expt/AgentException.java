package com.sbss.bithon.agent.core.utils.expt;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/18 8:42 下午
 */
public class AgentException extends RuntimeException {
    public AgentException(String message, Throwable e) {
        super(message, e);
    }

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String format, Object... args) {
        super(String.format(format, args));
    }
}
