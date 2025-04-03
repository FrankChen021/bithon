--
--    Copyright 2020 bithon.org
--
--    Licensed under the Apache License, Version 2.0 (the "License");
--    you may not use this file except in compliance with the License.
--    You may obtain a copy of the License at
--
--        http://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.
--

CREATE
DATABASE IF NOT EXISTS `bithon_codegen` DEFAULT CHARSET utf8mb4;
USE `bithon_codegen`;

DROP TABLE IF EXISTS `bithon_alert_object`;
CREATE TABLE `bithon_alert_object`
(
    `alert_id`      varchar(32)  NOT NULL COMMENT 'UUID',
    `alert_name`    varchar(128) NOT NULL DEFAULT '' COMMENT '',
    `app_name`      varchar(128) NOT NULL DEFAULT '' COMMENT '',
    `namespace`     varchar(64)  NOT NULL COMMENT 'namespace of application',
    `disabled`      int          NOT NULL COMMENT '',
    `deleted`       int          NOT NULL COMMENT '',
    `payload`       text COMMENT 'JSON formatted alert',
    `created_at`    timestamp(3) NOT NULL COMMENT '',
    `updated_at`    timestamp(3) NOT NULL COMMENT '',
    `last_operator` varchar(64)  NOT NULL DEFAULT '' COMMENT '',
    UNIQUE `uq_alert_object_id` (`alert_id`),
    KEY             `idx_alert_object_app_name` (`app_name`),
    KEY             `idx_alert_object_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alert Rules';


DROP TABLE IF EXISTS `bithon_alert_record`;
CREATE TABLE `bithon_alert_record`
(
    `record_id`           varchar(32)  NOT NULL COMMENT '',
    `alert_id`            varchar(32)  NOT NULL COMMENT '',
    `alert_name`          varchar(128) NOT NULL DEFAULT '' COMMENT 'alert name',
    `app_name`            varchar(128) NOT NULL DEFAULT '' COMMENT 'application name',
    `namespace`           varchar(64)  NOT NULL COMMENT '',
    `payload`             text COMMENT 'JSON formatted alert object',
    `data_source`         text COMMENT 'JSON formatted data source configuration',
    `created_at`          timestamp(3) NOT NULL COMMENT 'create timestamp',
    `notification_status` int(11) NOT NULL DEFAULT 0 COMMENT '-1:waiting ack，1:ACK',
    `notification_result` text COMMENT 'JSON formatted',
    KEY                   `idx_bithon_alert_record_id` (`record_id`),
    KEY                   `idx_bithon_alert_record_alert_id` (`alert_id`),
    KEY                   `idx_bithon_alert_record_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alerting History Records';

DROP TABLE IF EXISTS `bithon_alert_state`;
CREATE TABLE `bithon_alert_state`
(
    `alert_id`          varchar(32) NOT NULL COMMENT '',
    `alert_status`      int(11) NOT NULL COMMENT 'See the AlertStatus enum',
    `last_alert_at`     datetime    NOT NULL COMMENT '',
    `last_record_id`    varchar(32) COMMENT 'The PK ID in bithon_alert_record table',
    `last_evaluated_at` datetime    NOT NULL COMMENT 'The last time the alert is evaluated',
    `update_at`         datetime    NOT NULL COMMENT 'when the record is updated',
    `payload`           text COMMENT 'JSON formatted runtime info. See AlertStateObject$Payload to know more',
    UNIQUE KEY `uq_alert_id` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alerting State';

DROP TABLE IF EXISTS `bithon_alert_change_log`;
CREATE TABLE `bithon_alert_change_log`
(
    `alert_id`       varchar(32)  NOT NULL COMMENT 'ID of Alert Object',
    `action`         varchar(32)  NOT NULL COMMENT '',
    `payload_before` text COMMENT 'JSON formatted',
    `payload_after`  text COMMENT 'JSON formatted',
    `editor`         varchar(64) DEFAULT NULL COMMENT '',
    `created_at`     timestamp(3) NOT NULL COMMENT 'Create timestamp',
    KEY              `idx_alert_change_log_alert_id` (`alert_id`),
    KEY              `idx_alert_change_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Change logs of alert';

DROP TABLE IF EXISTS `bithon_alert_evaluation_log`;
CREATE TABLE `bithon_alert_evaluation_log`
(
    `timestamp` timestamp(3) NOT NULL COMMENT '',
    `alert_id`  varchar(32)  NOT NULL DEFAULT '' COMMENT 'Alert ID',
    `sequence`  bigint(20)   NOT NULL DEFAULT 0 COMMENT 'Used for ordering',
    `instance`  varchar(32)  NOT NULL COMMENT 'The instance that runs the evaluation',
    `level`     varchar(16)  NOT NULL DEFAULT '' COMMENT 'Logger Level: INFO, WARN, ERROR',
    `clazz`     varchar(128) NOT NULL DEFAULT '' COMMENT 'Logger Class',
    `message`   text COMMENT '',
    KEY         `bithon_alert_evaluation_log_timestamp` (`timestamp`),
    KEY         `bithon_alert_evaluation_log_timestamp_id` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Evaluation logs of alert';

DROP TABLE IF EXISTS `bithon_alert_notification_channel`;
CREATE TABLE `bithon_alert_notification_channel`
(
    `name`       varchar(64)  NOT NULL,
    `type`       varchar(16)  NOT NULL,
    `payload`    text         NOT NULL COMMENT 'channel payload',
    `created_at` timestamp(3) NOT NULL COMMENT 'create time',
    `updated_at` timestamp(3) NOT NULL COMMENT 'update time',
    UNIQUE KEY `alert_notification_channel_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alert Notification channels';
