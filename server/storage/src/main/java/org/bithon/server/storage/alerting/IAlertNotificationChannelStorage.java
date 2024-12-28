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

package org.bithon.server.storage.alerting;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.OrderBy;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:40
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IAlertNotificationChannelStorage {

    @Data
    @Builder
    class GetChannelRequest {
        private String name;
        private long since;
        private OrderBy orderBy;
        private Limit limit;
    }

    List<NotificationChannelObject> getChannels(GetChannelRequest request);
    int getChannelsSize(GetChannelRequest request);

    void initialize();

    void createChannel(String type, String name, String props);

    void deleteChannel(String name);

    boolean exists(String name);

    NotificationChannelObject getChannel(String name);

    /**
     * Update channel by name
     */
    boolean updateChannel(NotificationChannelObject old, String props);
}
