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

DROP TABLE IF EXISTS `bithon_jvm_metrics`;
CREATE TABLE `bithon_jvm_metrics` (
  `timestamp` TIMESTAMP NOT NULL,
  `appName` VARCHAR(64) NOT NULL COMMENT '',
  `instanceName` VARCHAR(64) NOT NULL COMMENT '',
  `processCpuLoad` DOUBLE DEFAULT 0 COMMENT '',
  `instanceUpTime` BIGINT NOT NULL DEFAULT 0 COMMENT '',
  `instanceStartTime` BIGINT NOT NULL DEFAULT 0 COMMENT '',
  `heap` BIGINT NOT NULL DEFAULT 0 COMMENT '-Xmx',
  `heapInit` BIGINT NOT NULL DEFAULT 0 COMMENT '-Xms',
  `heapUsed` BIGINT NOT NULL DEFAULT 0 COMMENT '',
  `heapCommitted` BIGINT NOT NULL DEFAULT 0 COMMENT '可使用的内存大小，包括used',
  `peakThreads` BIGINT NOT NULL DEFAULT 0 COMMENT 'number of peak threads',
  `daemonThreads` BIGINT NOT NULL DEFAULT 0 COMMENT 'number of daemon threads',
  `totalThreads` BIGINT NOT NULL DEFAULT 0 COMMENT 'number of total threads',
  `activeThreads` BIGINT NOT NULL DEFAULT 0 COMMENT 'number of active threads',

  UNIQUE KEY `idx_key` (`timestamp`, `appName`, `instanceName`)
);

DROP TABLE IF EXISTS `bithon_web_request_metrics`;
CREATE TABLE `bithon_web_request_metrics` (
  `timestamp` TIMESTAMP NOT NULL,
  `appName` VARCHAR(64) NOT NULL COMMENT '',
  `instanceName` VARCHAR(64) NOT NULL COMMENT '',
  `uri` VARCHAR(255) NOT NULL COMMENT '',
  `costTime` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `requestCount` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `errorCount` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `count4xx` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `count5xx` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `requestByteSize` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `responseByteSize` BIGINT DEFAULT 0 NOT NULL COMMENT '',

  UNIQUE KEY `idx_key` (`timestamp`, `appName`, `instanceName`, `uri`)
);

DROP TABLE IF EXISTS `bithon_web_server_metrics`;
CREATE TABLE `bithon_web_server_metrics` (
  `timestamp` TIMESTAMP NOT NULL,
  `appName` VARCHAR(64) NOT NULL COMMENT '',
  `instanceName` VARCHAR(64) NOT NULL COMMENT '',
  `connectionCount` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `maxConnections` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `activeThreads` BIGINT DEFAULT 0 NOT NULL COMMENT '',
  `maxThreads` BIGINT DEFAULT 0 NOT NULL COMMENT '',

  UNIQUE KEY `idx_key` (`timestamp`, `appName`, `instanceName`)
);

DROP TABLE IF EXISTS `bithon_trace_span`;
CREATE TABLE `bithon_trace_span` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `appName` VARCHAR(64) NOT NULL COMMENT '',
  `instanceName` VARCHAR(64) NOT NULL COMMENT '',
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
  KEY `idx_appName` (`appName`),
  KEY `idx_instanceName` (`instanceName`),
  UNIQUE `idx_key` (`traceId`, `spanId`),
  KEY `idx_parentSpanId` (`parentSpanId`)
);


DROP TABLE IF EXISTS `bithon_event`;
CREATE TABLE `bithon_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `appName` VARCHAR(64) NOT NULL COMMENT '',
  `instanceName` VARCHAR(64) NOT NULL COMMENT '',
  `type` VARCHAR(64) NOT NULL COMMENT 'type of event',
  `arguments` TEXT COMMENT 'JSON formatted Map<String, String>',
  `timestamp` TIMESTAMP NOT NULL COMMENT 'reported Timestamp',
  PRIMARY KEY `idx_id` (`id`),
  KEY `idx_appName` (`appName`),
  KEY `idx_instanceName` (`instanceName`),
  KEY `idx_type` (`type`),
  KEY `idx_timestamp` (`timestamp`)
);
