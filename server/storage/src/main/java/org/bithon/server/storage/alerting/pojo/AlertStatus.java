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

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/9 21:09
 */
public enum AlertStatus {
    /**
     * PENDING for evaluation
     */
    PENDING(0),

    /**
     * An alert is firing
     */
    FIRING(10),

    /**
     * A fired alert is resolved
     */
    RESOLVED(20);

    private final int statusCode;

    AlertStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public static AlertStatus fromCode(int statusCode) {
        for (AlertStatus status : AlertStatus.values()) {
            if (status.statusCode == statusCode) {
                return status;
            }
        }
        return null;
    }

    public int statusCode() {
        return statusCode;
    }
}
