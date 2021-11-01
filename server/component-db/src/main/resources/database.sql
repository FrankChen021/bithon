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
    `timestamp`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `application_name` varchar(128) NOT NULL,
    `application_type` varchar(64)  NOT NULL,
    `instance_name`    varchar(64)  NOT NULL,
    KEY `idx_timestamp` (`timestamp`),
    KEY `idx_app_name` (`application_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用';

DROP TABLE IF EXISTS `bithon_agent_setting`;
CREATE TABLE `bithon_agent_setting`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '唯一编号',
    `app_name`     varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
    `setting_name` varchar(64)  NOT NULL DEFAULT '' COMMENT '配置名称',
    `setting`      TEXT COMMENT '设置',
    `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `key_appName` (`app_name`, `setting_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 100000
  DEFAULT CHARSET = utf8mb4 COMMENT ='配置';

DROP TABLE IF EXISTS `bithon_trace_span`;
CREATE TABLE `bithon_trace_span`
(
    `timestamp`     TIMESTAMP              NOT NULL COMMENT 'Milli Seconds',
    `app_name`      VARCHAR(64)            NOT NULL COMMENT '',
    `instance_name` VARCHAR(64)            NOT NULL COMMENT '',
    `name`          VARCHAR(64)            NOT NULL COMMENT '',
    `clazz`         varchar(128)           NOT NULL COMMENT '',
    `method`        VARCHAR(128)           NOT NULL COMMENT '',
    `traceId`       VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `spanId`        VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `parentSpanId`  VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
    `kind`          VARCHAR(64)            NOT NULL DEFAULT '' COMMENT '',
    `costTime`      BIGINT                 NOT NULL DEFAULT 0 COMMENT '',
    `tags`          TEXT COMMENT '',
    KEY `idx_timestamp` (`timestamp`),
    KEY `idx_app_name` (`app_name`),
    KEY `idx_instanceName` (`instance_name`),
    UNIQUE `idx_key` (`traceId`, `spanId`),
    KEY `idx_parentSpanId` (`parentSpanId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `bithon_event`;
CREATE TABLE `bithon_event`
(
    `timestamp`     TIMESTAMP(3) NOT NULL COMMENT 'reported Timestamp',
    `app_name`      VARCHAR(64)  NOT NULL COMMENT '',
    `instance_name` VARCHAR(64)  NOT NULL COMMENT '',
    `type`          VARCHAR(64)  NOT NULL COMMENT 'type of event',
    `arguments`     TEXT COMMENT 'JSON formatted Map<String, String>',
    KEY `idx_timestamp` (`timestamp`),
    KEY `idx_appName` (`app_name`),
    KEY `idx_instanceName` (`instance_name`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='';
