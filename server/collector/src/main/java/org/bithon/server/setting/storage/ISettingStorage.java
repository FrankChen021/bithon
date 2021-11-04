package org.bithon.server.setting.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Frank Chen
 * @date 4/11/21 3:14 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISettingStorage {
    void initialize();

    ISettingReader createReader();
}
