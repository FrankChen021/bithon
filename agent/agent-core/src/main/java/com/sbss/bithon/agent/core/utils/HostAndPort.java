package com.sbss.bithon.agent.core.utils;


/**
 * @author frankchen
 */
public class HostAndPort {
    public static final int NO_PORT = -1;

    public static String of(String host, int port) {
        return of(host, port, NO_PORT);
    }

    public static String of(String host, int port, int noPort) {
        if (noPort == port) {
            return host;
        }
        return host + ':' + port;
    }
}
