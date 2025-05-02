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
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.alerting.pojo.ListRuleDTO;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.OrderBy;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Frank Chen
 * @date 21/3/22 11:53 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IAlertObjectStorage {

    List<AlertStorageObject> getRuleListByTime(Timestamp start, Timestamp end);

    boolean existRuleById(String alertId);

    boolean existRuleByName(String name);

    AlertStorageObject getRuleById(String ruleId);

    default void createRule(AlertStorageObject alert, String operator) {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        createRule(alert, operator, ts, ts);
    }

    void createRule(AlertStorageObject alert, String operator, Timestamp createTimestamp, Timestamp updateTimestamp);

    boolean updateRule(AlertStorageObject oldObject, AlertStorageObject newObject, String operator);

    boolean disableRule(String alertId, String operator);

    boolean enableRule(String alertId, String operator);

    boolean deleteRule(String alertId, String operator);

    void addChangelog(String alertId,
                      ObjectAction action,
                      String operator,
                      String beforeJsonString,
                      String afterJsonString);

    int getRuleListSize(String appName, String ruleName);

    List<ListRuleDTO> getRuleList(String folder,
                                  String appName,
                                  String ruleName,
                                  OrderBy orderBy,
                                  Limit limit);

    ListResult<AlertChangeLogObject> getChangeLogs(String alertId, Integer pageNumber, Integer pageSize);

    <T> T executeTransaction(Callable<T> runnable);

    void initialize();
}
