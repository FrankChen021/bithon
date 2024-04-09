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
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.common.IStorage;
import org.bithon.server.storage.common.expiration.IExpirable;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Frank Chen
 * @date 21/3/22 11:56 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IAlertRecordStorage extends IStorage, IExpirable {

    int STATUS_CODE_UNCHECKED = -1;
    int STATUS_CODE_CHECKED = 1;

    Timestamp getLastAlert(String alertId);

    void addAlertRecord(AlertRecordObject record);

    ListResult<AlertRecordObject> getAlertRecords(String alertId, int pageNumber, int pageSize);

    AlertRecordObject getAlertRecord(String id);

    List<AlertRecordObject> getRecordsByNotificationStatus(int statusCode);

    void setNotificationResult(String id, int statusCode, String status);

    void initialize();

    void updateAlertStatus(String id, AlertStatus alertStatus);
}
