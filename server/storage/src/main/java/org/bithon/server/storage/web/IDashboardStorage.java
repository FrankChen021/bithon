package org.bithon.server.storage.web;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;
import java.util.List;

/**
 * @author Frank Chen
 * @date 19/8/22 12:38 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IDashboardStorage {

    /**
     * get changed dashboard after given timestamp
     */
    List<Dashboard> getDashboard(long afterTimestamp);

    String put(String name, String payload);

    void putIfNotExist(String name, String payload) throws IOException;

    void initialize();
}