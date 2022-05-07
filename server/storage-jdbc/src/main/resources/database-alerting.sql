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
USE
`bithon_codegen`;

DROP TABLE IF EXISTS `bithon_alert_object`;
CREATE TABLE `bithon_alert_object`
(
    `alert_id`      varchar(32)  NOT NULL COMMENT 'UUID',
    `alert_name`    varchar(128) NOT NULL DEFAULT '' COMMENT '',
    `app_name`      varchar(128) NOT NULL DEFAULT '' COMMENT '',
    `namespace`     varchar(64)  NOT NULL COMMENT 'namespace of application',
    `disabled`      tinyint(2) NOT NULL DEFAULT '0' COMMENT '',
    `deleted`       tinyint(2) NOT NULL DEFAULT '0' COMMENT '',
    `payload`       text COMMENT 'JSON formatted alert',
    `created_at`    timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '',
    `updated_at`    timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP (3) COMMENT '',
    `last_operator` varchar(64)  NOT NULL DEFAULT '' COMMENT '',
    UNIQUE `uq_alert_object_id` (`alert_id`),
    KEY             `idx_alert_object_app_name` (`app_name`),
    KEY             `idx_alert_object_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Alert';


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
    `timestamp`           timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP (3) COMMENT 'create timestamp',
    `notification_status` int(11) NOT NULL DEFAULT '0' COMMENT '-1:waiting ack，1:ACK',
    `notification_result` text COMMENT 'JSON formatted',
    KEY                   `idx_bithon_alert_record_id` (`record_id`),
    KEY                   `idx_bithon_alert_record_alert_id` (`alert_id`),
    KEY                   `idx_bithon_alert_record_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Alerting History Records';

DROP TABLE IF EXISTS `bithon_alert_state`;
CREATE TABLE `bithon_alert_state`
(
    `alert_id`       varchar(32) NOT NULL COMMENT '',
    `last_alert_at`  datetime    NOT NULL COMMENT '',
    `last_record_id` varchar(32) COMMENT 'The PK ID in bithon_alert_record table',
    UNIQUE KEY `uq_alert_id` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Alerting State';

DROP TABLE IF EXISTS `bithon_alert_changelog`;
CREATE TABLE `bithon_alert_changelog`
(
    `pk_id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'PK ID',
    `alert_id`           varchar(32)  NOT NULL DEFAULT '' COMMENT 'ID of Alert Object',
    `action`             varchar(32)  NOT NULL DEFAULT '' COMMENT '',
    `payload_before`     text COMMENT 'JSON formatted',
    `payload_after`      text COMMENT 'JSON formatted',
    `editor`             varchar(64)           DEFAULT NULL COMMENT '',
    `server_update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP (3) COMMENT '更新时间',
    PRIMARY KEY (`pk_id`),
    KEY                  `idx_alert_changelog_alert_id` (`alert_id`),
    KEY                  `idx_alert_changelog_server_create_time` (`server_update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Change logs of alert';

DROP TABLE IF EXISTS `bithon_alert_runlog`;
CREATE TABLE `bithon_alert_runlog`
(
    `timestamp` timestamp(3) NOT NULL COMMENT '',
    `alert_id`  varchar(32)  NOT NULL DEFAULT '' COMMENT 'Alert ID',
    `sequence`  bigint(20)   NOT NULL DEFAULT 0 COMMENT 'Used for ordering',
    `clazz`     varchar(128) NOT NULL DEFAULT '' COMMENT 'Logger Class',
    `message`   text COMMENT '',
    KEY         `idx_alert_runlog_timestamp` (`timestamp`),
    KEY         `idx_alert_runlog_alert_id` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Running logs of alert';
