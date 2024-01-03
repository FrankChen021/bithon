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

DROP DATABASE bithon_codegen;
CREATE DATABASE IF NOT EXISTS `bithon_codegen` DEFAULT CHARSET utf8mb4;
USE `bithon_codegen`;

DROP TABLE IF EXISTS `bithon_application_instance`;
CREATE TABLE `bithon_application_instance`
(
    `timestamp`        TIMESTAMP(3)     NOT NULL COMMENT'update time',
    `appName` varchar(128) NOT NULL,
    `appType` varchar(64)  NOT NULL,
    `instanceName`    varchar(64)  NOT NULL,
    KEY `idx_app_instance_timestamp` (`timestamp`) COMMENT 'clickouse: minmax',
    UNIQUE `uq_name_type_instance` (`appName`, `appType`, `instanceName`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- mapping between application and metric
DROP TABLE IF EXISTS `bithon_meta_application_metric_map`;
CREATE TABLE `bithon_meta_application_metric_map`
(
    `timestamp`   TIMESTAMP(3)     NOT NULL COMMENT'update time',
    `application` varchar(128) NOT NULL,
    `schema` VARCHAR(64) NOT NULL COMMENT 'name in bithon_metric_schema',
    KEY `idx_meta_application_metric_map` (`application`, `schema`),
    KEY `idx_meta_application_metric_map_timestamp` (`timestamp`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_meta_schema`;
CREATE TABLE `bithon_meta_schema`
(
    `timestamp`    TIMESTAMP(3) NOT NULL COMMENT 'Created Timestamp',
    `name`         VARCHAR(64)  NOT NULL COMMENT 'Schema Name',
    `schema`       TEXT NOT NULL COMMENT 'Schema in JSON',
    `signature`    VARCHAR(250) NOT NULL COMMENT 'Signature of schema field, currently SHA256 is applied',
    UNIQUE `idx_meta_schema_name` (`name`),
    KEY `idx_meta_schema_timestamp` (`timestamp`) COMMENT 'clickhouse: minmax'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='';

-- Agent Configuration
DROP TABLE IF EXISTS `bithon_agent_setting`;
CREATE TABLE `bithon_agent_setting`
(
    `timestamp`   TIMESTAMP    NOT NULL COMMENT 'Created Timestamp',
    `appName`     varchar(128) NOT NULL COMMENT '',
    `settingName` varchar(64)  NOT NULL COMMENT '',
    `setting`      TEXT COMMENT '设置',
    `updatedAt`   datetime     NOT NULL COMMENT '',
    UNIQUE KEY `key_appName` (`appName`, `settingName`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_trace_span`;
CREATE TABLE `bithon_trace_span`
(
    `timestamp`     TIMESTAMP              NOT NULL COMMENT 'Milli Seconds',
    `appName`      VARCHAR(64)             NOT NULL COMMENT '',
    `instanceName` VARCHAR(64)             NOT NULL COMMENT '',
    `name`          VARCHAR(64)            NOT NULL COMMENT '',
    `clazz`         varchar(128)           NOT NULL COMMENT '',
    `method`        VARCHAR(128)           NOT NULL COMMENT '',
    `traceId`       VARCHAR(64)            NOT NULL COMMENT '',
    `spanId`        VARCHAR(64)            NOT NULL COMMENT '',
    `parentSpanId`  VARCHAR(64)            NOT NULL COMMENT '',
    `kind`          VARCHAR(64)            NOT NULL COMMENT '',
    `costTimeMs`    BIGINT                 NOT NULL COMMENT 'Micro Second, suffix is wrong',
    `startTimeUs`   BIGINT                 NOT NULL COMMENT 'Micro Second',
    `endTimeUs`     BIGINT                 NOT NULL COMMENT 'Micro Second',
    `tags`          TEXT COMMENT 'Kept for compatibility',
    `attributes`          TEXT COMMENT '',
    `normalizedUrl` VARCHAR(255) NOT NULL COMMENT '',
    `status`        VARCHAR(32)  NOT NULL COMMENT '',
    KEY `idx_ts_1_traceId` (`traceId`), -- NOTE: jOOQ generates indexes in the alphabetic order not the declaration order
    KEY `idx_ts_2_timestamp` (`timestamp`),
    KEY `idx_ts_3_app_name` (`appName`),
    KEY `idx_ts_4_instanceName` (`instanceName`),
    KEY `idx_ts_5_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- A view that only contains the entry of a trace
DROP TABLE IF EXISTS `bithon_trace_span_summary`;
CREATE TABLE `bithon_trace_span_summary`
(
    `timestamp`     TIMESTAMP              NOT NULL COMMENT 'Milli Seconds',
    `appName`       VARCHAR(64)            NOT NULL COMMENT '',
    `instanceName`  VARCHAR(64)            NOT NULL COMMENT '',
    `name`          VARCHAR(64)            NOT NULL COMMENT '',
    `clazz`         varchar(128)           NOT NULL COMMENT '',
    `method`        VARCHAR(128)           NOT NULL COMMENT '',
    `traceId`       VARCHAR(64)            NOT NULL COMMENT '',
    `spanId`        VARCHAR(64)            NOT NULL COMMENT '',
    `parentSpanId`  VARCHAR(64)            NOT NULL COMMENT '',
    `kind`          VARCHAR(64)            NOT NULL COMMENT '',
    `costTimeMs`    BIGINT                 NOT NULL COMMENT 'Milli Second',
    `startTimeUs`   BIGINT                 NOT NULL COMMENT 'Micro Second',
    `endTimeUs`     BIGINT                 NOT NULL COMMENT 'Micro Second',
    `tags`          TEXT COMMENT 'Kept for compatibility',
    `attributes`     TEXT COMMENT '',
    `normalizedUrl` VARCHAR(255) NOT NULL COMMENT '',
    `status`        VARCHAR(32)  NOT NULL COMMENT '',
    KEY `idx_tss_1_timestamp` (`timestamp`),
    KEY `idx_tss_2_app_name` (`appName`),
    KEY `idx_tss_3_instanceName` (`instanceName`),
    KEY `idx_tss_4_traceId` (`traceId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- An inverted index for tags
DROP TABLE IF EXISTS `bithon_trace_span_tag_index`;
CREATE TABLE `bithon_trace_span_tag_index`
(
    `timestamp`      TIMESTAMP   NOT NULL COMMENT 'Milli Seconds',
    `f1`             VARCHAR(64) DEFAULT '' COMMENT 'tag value1',
    `f2`             VARCHAR(64) DEFAULT '' COMMENT 'tag value2',
    `f3`             VARCHAR(64) DEFAULT '' COMMENT 'tag value3',
    `f4`             VARCHAR(64) DEFAULT '' COMMENT 'tag value4',
    `f5`             VARCHAR(64) DEFAULT '' COMMENT 'tag value5',
    `f6`             VARCHAR(64) DEFAULT '' COMMENT 'tag value6',
    `f7`             VARCHAR(64) DEFAULT '' COMMENT 'tag value7',
    `f8`             VARCHAR(64) DEFAULT '' COMMENT 'tag value8',
    `f9`             VARCHAR(64) DEFAULT '' COMMENT 'tag value9',
    `f10`            VARCHAR(64) DEFAULT '' COMMENT 'tag value10',
    `f11`            VARCHAR(64) DEFAULT '' COMMENT 'tag value11',
    `f12`            VARCHAR(64) DEFAULT '' COMMENT 'tag value12',
    `f13`            VARCHAR(64) DEFAULT '' COMMENT 'tag value13',
    `f14`            VARCHAR(64) DEFAULT '' COMMENT 'tag value14',
    `f15`            VARCHAR(64) DEFAULT '' COMMENT 'tag value15',
    `f16`            VARCHAR(64) DEFAULT '' COMMENT 'tag value16',
    `traceId`        VARCHAR(64) NOT NULL COMMENT '',
    KEY `idx_tsti_timestamp` (`timestamp`) COMMENT ''
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_trace_mapping`;
CREATE TABLE `bithon_trace_mapping`
(
    `timestamp`     TIMESTAMP             NOT NULL COMMENT 'Milli Seconds',
    `user_tx_id`    VARCHAR(64)           NOT NULL COMMENT 'user side transaction id',
    `trace_id`      VARCHAR(64)           NOT NULL COMMENT 'trace id in bithon',
    KEY `idx_trace_mapping_user_tx_id` (`user_tx_id`)
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
    KEY `idx_event_1_timestamp` (`timestamp`),
    KEY `idx_event_appName` (`appName`),
    KEY `idx_event_instanceName` (`instanceName`),
    KEY `idx_event_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='';

DROP TABLE IF EXISTS `bithon_web_dashboard`;
CREATE TABLE `bithon_web_dashboard`
(
    `timestamp`    TIMESTAMP(3) NOT NULL COMMENT 'Created Timestamp',
    `name`         VARCHAR(64)  NOT NULL COMMENT 'Name',
    `payload`      TEXT NOT NULL COMMENT 'Schema in JSON',
    `signature`    VARCHAR(250) NOT NULL COMMENT 'Signature of payload field, currently SHA256 is applied',
    `deleted`   INT NOT NULL COMMENT '',
    UNIQUE `idx_web_dashboard_name` (`name`),
    KEY `idx_web_dashboard_timestamp` (`timestamp`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='';

DROP TABLE IF EXISTS `bithon_metrics_baseline`;
CREATE TABLE `bithon_metrics_baseline`
(
    `date`         VARCHAR(10) NOT NULL COMMENT 'On which day the metrics will be kept.In the format of yyyy-MM-dd',
    `keep_days`    INT NOT NULL COMMENT 'How many days the metrics will be kept. If 0, the metrics will be kept forever ',
    `create_time`  TIMESTAMP(3) NOT NULL COMMENT 'Created Timestamp',
    UNIQUE `bithon_metrics_baseline_date` (`date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='This table keeps the date when the metrics will be kept for ever';