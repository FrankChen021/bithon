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

package org.bithon.server.storage.alerting.pojo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 10/4/22 6:06 PM
 */
@Data
public class AlertStorageObject {
    private String id;
    private String name;
    private String appName;
    private String namespace;
    private boolean disabled;
    private boolean deleted;

    /**
     * {@link AlertStorageObjectPayload}
     */
    private AlertStorageObjectPayload payload;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String lastOperator;
}
