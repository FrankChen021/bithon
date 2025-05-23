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
     * The initial status of an alert, ready for evaluation
     */
    READY(0) {
        @Override
        public boolean canTransitTo(AlertStatus newStatus) {
            return (newStatus == PENDING || newStatus == ALERTING);
        }
    },

    /**
     * The rule is evaluated as true, but wait for more evaluation to fire
     */
    PENDING(5) {
        @Override
        public boolean canTransitTo(AlertStatus newStatus) {
            return newStatus == ALERTING || newStatus == RESOLVED;
        }
    },

    /**
     * The alert has been triggered
     */
    ALERTING(10) {
        @Override
        public boolean canTransitTo(AlertStatus newStatus) {
            return newStatus == SUPPRESSING || newStatus == RESOLVED;
        }
    },

    /**
     * The alert has been triggered, but the notification is suppressed during the silence period
     */
    SUPPRESSING(15) {
        @Override
        public boolean canTransitTo(AlertStatus newStatus) {
            return newStatus == ALERTING || newStatus == RESOLVED;
        }
    },

    /**
     * A fired alert is resolved
     */
    RESOLVED(20) {
        @Override
        public boolean canTransitTo(AlertStatus newStatus) {
            return newStatus == PENDING
                   || newStatus == ALERTING
                   || newStatus == SUPPRESSING;
        }
    };

    private final int statusCode;

    AlertStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * check if the status can transit to the new status.
     * the old status and the new status SHOULD NOT be the same.
     */
    public abstract boolean canTransitTo(AlertStatus newStatus);

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
