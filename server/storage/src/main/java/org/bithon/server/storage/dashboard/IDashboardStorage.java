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

package org.bithon.server.storage.dashboard;

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
