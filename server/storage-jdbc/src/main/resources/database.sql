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

CREATE DATABASE IF NOT EXISTS `bithon_codegen` DEFAULT CHARSET utf8mb4;
USE `bithon_codegen`;

-- 应用
DROP TABLE IF EXISTS `bithon_application`;

DROP TABLE IF EXISTS `bithon_application_instance`;
CREATE TABLE `bithon_application_instance`
(
    `timestamp`        TIMESTAMP(3)     NOT NULL COMMENT'update time',
    `appName` varchar(128) NOT NULL,
    `appType` varchar(64)  NOT NULL,
    `instanceName`    varchar(64)  NOT NULL,
    KEY `idx_app_instance_timestamp` (`timestamp`), # Use a unique index name because some db like H2 rejects duplicated name
    KEY `idx_app_instance_name` (`appName`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用';

DROP TABLE IF EXISTS `bithon_agent_setting`;
CREATE TABLE `bithon_agent_setting`
(
    `timestamp`   TIMESTAMP    NOT NULL COMMENT 'Created Timestamp',
    `appName`     varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
    `settingName` varchar(64)  NOT NULL DEFAULT '' COMMENT '配置名称',
    `setting`      TEXT COMMENT '设置',
    `updatedAt`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `key_appName` (`appName`, `settingName`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置';

DROP TABLE IF EXISTS `bithon_trace_span`;
CREATE TABLE `bithon_trace_span`
(
    `timestamp`     TIMESTAMP              NOT NULL COMMENT 'Milli Seconds',
    `appName`      VARCHAR(64)            NOT NULL COMMENT '',
    `instanceName` VARCHAR(64)            NOT NULL COMMENT '',
    `name`          VARCHAR(64)            NOT NULL COMMENT '',
    `clazz`         varchar(128)           NOT NULL COMMENT '',
    `method`        VARCHAR(128)           NOT NULL COMMENT '',
    `traceId`       VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `spanId`        VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `parentSpanId`  VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `kind`          VARCHAR(64)            NOT NULL DEFAULT '' COMMENT '',
    `costTimeMs`    BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Milli Second',
    `startTimeUs`   BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Micro Second',
    `endTimeUs`     BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Micro Second',
    `tags`          TEXT COMMENT '',
    KEY `idx_timestamp` (`timestamp`),
    KEY `idx_app_name` (`appName`),
    KEY `idx_instanceName` (`instanceName`),
    UNIQUE `idx_key` (`traceId`, `spanId`),
    KEY `idx_parentSpanId` (`parentSpanId`),
    KEY `idx_start_time` (`startTimeUs`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_trace_mapping`;
CREATE TABLE `bithon_trace_mapping`
(
    `timestamp`     TIMESTAMP             NOT NULL COMMENT 'Milli Seconds',
    `user_tx_id`    VARCHAR(64)           NOT NULL COMMENT 'user side transaction id',
    `trace_id`      VARCHAR(64)           NOT NULL COMMENT 'trace id in bithon',
    KEY `idx_trace_mapping_id` (`user_tx_id`, `trace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_event`;
CREATE TABLE `bithon_event`
(
    `timestamp`     TIMESTAMP(3) NOT NULL COMMENT 'reported Timestamp',
    `appName`      VARCHAR(64)  NOT NULL COMMENT '',
    `instanceName` VARCHAR(64)  NOT NULL COMMENT '',
    `type`          VARCHAR(64)  NOT NULL COMMENT 'type of event',
    `arguments`     TEXT COMMENT 'JSON formatted Map<String, String>',
    KEY `idx_event_timestamp` (`timestamp`),
    KEY `idx_event_appName` (`appName`),
    KEY `idx_event_instanceName` (`instanceName`),
    KEY `idx_event_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='';
