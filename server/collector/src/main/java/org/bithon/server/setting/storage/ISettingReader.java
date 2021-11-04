package org.bithon.server.setting.storage;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 4/11/21 3:15 pm
 */
public interface ISettingReader {
    Map<String, String> getSettings(String appName, long since);
}
