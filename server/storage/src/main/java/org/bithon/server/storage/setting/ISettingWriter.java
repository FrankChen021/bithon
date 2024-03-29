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

/**
 * @author Frank Chen
 * @date 26/1/24 2:16 pm
 */
public interface ISettingWriter {
    void addSetting(String app, String env, String name, String value, String format);

    void deleteSetting(String app, String env, String name);

    void updateSetting(String appName, String env, String name, String value, String format);
}
