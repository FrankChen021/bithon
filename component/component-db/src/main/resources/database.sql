--
--    Copyright 2020 bithon.cn
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

CREATE TABLE `bithon_application` (
  `id` bigint(20) NOT NULL COMMENT '唯一编号',
  `name` varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
  `env` varchar(64) NOT NULL DEFAULT '' COMMENT '环境',
  `group` varchar(64) NOT NULL DEFAULT '' COMMENT '分组',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_name`(`name`, `env`),
  KEY `idx_group`(`group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用';

DROP TABLE IF EXISTS `bithon_metadata`;

CREATE TABLE `bithon_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '唯一编号',
  `name` varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
  `type` varchar(64) NOT NULL DEFAULT '' COMMENT '环境',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_name` (`name`,`type`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COMMENT='应用';

DROP TABLE IF EXISTS `bithon_agent_setting`;
CREATE TABLE `bithon_agent_setting` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '唯一编号',
  `app_name` varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
  `setting_name` varchar(64) NOT NULL DEFAULT '' COMMENT '配置名称',
  `setting` TEXT COMMENT '设置',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_appName` (`app_name`, `setting_name`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COMMENT='配置';

DROP TABLE IF EXISTS `bithon_trace_span`;
CREATE TABLE `bithon_trace_span` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_name` VARCHAR(64) NOT NULL COMMENT '',
  `instance_name` VARCHAR(64) NOT NULL COMMENT '',
  `name` VARCHAR(64) NOT NULL COMMENT '',
  `clazz` varchar(128) NOT NULL COMMENT '',
  `method` VARCHAR(128) NOT NULL COMMENT '',
  `traceId` VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
  `spanId` VARCHAR(64) DEFAULT '' NOT NULL COMMENT '',
  `parentSpanId` VARCHAR(64)  DEFAULT '' NOT NULL COMMENT '',
  `kind` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '',
  `costTime` BIGINT NOT NULL DEFAULT 0 COMMENT '',
  `tags` TEXT COMMENT '',
  `timestamp` TIMESTAMP NOT NULL COMMENT 'Milli Seconds',
  PRIMARY KEY `idx_id` (`id`),
  KEY `idx_app_name` (`app_name`),
  KEY `idx_instanceName` (`instance_name`),
  UNIQUE `idx_key` (`traceId`, `spanId`),
  KEY `idx_parentSpanId` (`parentSpanId`)
);

DROP TABLE IF EXISTS `bithon_event`;
CREATE TABLE `bithon_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_name` VARCHAR(64) NOT NULL COMMENT '',
  `instance_name` VARCHAR(64) NOT NULL COMMENT '',
  `type` VARCHAR(64) NOT NULL COMMENT 'type of event',
  `arguments` TEXT COMMENT 'JSON formatted Map<String, String>',
  `timestamp` TIMESTAMP NOT NULL COMMENT 'reported Timestamp',
  PRIMARY KEY `idx_id` (`id`),
  KEY `idx_appName` (`app_name`),
  KEY `idx_instanceName` (`instance_name`),
  KEY `idx_type` (`type`),
  KEY `idx_timestamp` (`timestamp`)
)ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COMMENT='';

DROP TABLE IF EXISTS `bithon_metric_dimension`;
CREATE TABLE `bithon_metric_dimension` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `data_source` VARCHAR(64) NOT NULL COMMENT '',
  `dimension_name` VARCHAR(64) NOT NULL COMMENT '',
  `dimension_value` VARCHAR(256) NOT NULL COMMENT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '',
  PRIMARY KEY `idx_id` (`id`),
  UNIQUE `idx_dimension` (`data_source`, `dimension_name`, `dimension_value`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COMMENT='Metric Dimension';

DROP TABLE IF EXISTS `bithon_application_topo`;
CREATE TABLE `bithon_application_topo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `src_endpoint` VARCHAR(64) NOT NULL COMMENT '',
  `src_endpoint_type` VARCHAR(16) NOT NULL COMMENT '0:Application; 1:MySQL; 2:REDIS; 3:Mongo;...',
  `dst_endpoint` VARCHAR(256) NOT NULL COMMENT '',
  `dst_endpoint_type` VARCHAR(16) NOT NULL COMMENT '0:Application; 1:MySQL 2:REDIS; 3:Mongo;...',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '',
  PRIMARY KEY `idx_id` (`id`),
  UNIQUE `idx_topo` (`src_endpoint`, `src_endpoint_type`, `dst_endpoint`, `dst_endpoint_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COMMENT='TOPO';
