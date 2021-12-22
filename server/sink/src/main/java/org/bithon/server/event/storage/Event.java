package org.bithon.server.event.storage;

import lombok.Data;

/**
 * @author Frank Chen
 * @date 22/12/21 11:26 AM
 */
@Data
public class Event {
    private long timestamp;
    private String application;
    private String instance;
    private String type;
    private String args;
}
