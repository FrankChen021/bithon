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

package org.bithon.server.storage.setting;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 4/11/21 3:15 pm
 */
public interface ISettingReader {

    @Data
    class SettingEntry {
        private String appName;
        private String environment;
        private String name;
        private String value;
        private String format;
        private Timestamp createdAt;
        private Timestamp updatedAt;
    }

    List<SettingEntry> getSettings();
    List<SettingEntry> getSettings(String appName);
    List<SettingEntry> getSettings(String appName, String env);

    SettingEntry getSetting(String appName, String env, String setting);
}
