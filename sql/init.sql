-- 设备表
CREATE TABLE IF NOT EXISTS `device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `name` varchar(50) NOT NULL COMMENT '设备名称',
  `ip` varchar(20) NOT NULL UNIQUE COMMENT '管理IP',
  `vendor` varchar(20) NOT NULL COMMENT '厂商（华为/思科/H3C）',
  `model` varchar(30) NOT NULL COMMENT '设备型号',
  `protocol` varchar(10) DEFAULT 'ssh' COMMENT '采集协议',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password_enc` varchar(100) NOT NULL COMMENT '加密后的密码',
  `status` varchar(10) DEFAULT 'offline' COMMENT '在线状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 巡检报告表
CREATE TABLE IF NOT EXISTS `inspect_report` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `device_id` int(11) NOT NULL COMMENT '设备ID',
  `inspect_time` datetime NOT NULL COMMENT '巡检时间',
  `health_score` int(3) NOT NULL COMMENT '健康评分(0-100)',
  `abnormal` text COMMENT '异常信息',
  `repair_result` text COMMENT '修复结果',
  `log_hash` varchar(64) COMMENT '区块链存证哈希',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`device_id`) REFERENCES `device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表（审计用）
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` int(11) NOT NULL COMMENT '操作用户ID',
  `device_id` int(11) NOT NULL COMMENT '设备ID',
  `op_type` varchar(20) NOT NULL COMMENT '操作类型',
  `op_content` text COMMENT '操作内容',
  `op_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `result` varchar(10) DEFAULT 'success' COMMENT '操作结果',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户表（权限控制）
CREATE TABLE IF NOT EXISTS `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '用户名',
  `password_hash` varchar(100) NOT NULL COMMENT '密码哈希',
  `role` varchar(20) DEFAULT 'operator' COMMENT '角色：admin/operator/readonly',
  `email` varchar(100) COMMENT '邮箱（用于告警）',
  `phone` varchar(20) COMMENT '手机号（用于告警）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务调度表（定时巡检）
CREATE TABLE IF NOT EXISTS `inspect_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `name` varchar(100) NOT NULL COMMENT '任务名称',
  `device_ids` text COMMENT '设备ID列表（JSON数组）',
  `cron_expr` varchar(50) NOT NULL COMMENT 'Cron表达式（如：0 3 * * * 表示每天3点）',
  `trigger_type` varchar(20) DEFAULT 'schedule' COMMENT '触发类型：schedule（定时）/trigger（触发式）',
  `trigger_condition` text COMMENT '触发条件（JSON，如：{"metric":"cpu_usage","threshold":80}）',
  `auto_repair` tinyint(1) DEFAULT 0 COMMENT '是否自动修复',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_run_time` datetime COMMENT '最后执行时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 告警配置表
CREATE TABLE IF NOT EXISTS `alert_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `name` varchar(100) NOT NULL COMMENT '告警规则名称',
  `device_id` int(11) COMMENT '设备ID（NULL表示所有设备）',
  `metric` varchar(50) NOT NULL COMMENT '监控指标（cpu_usage/memory_usage/interface_down等）',
  `threshold` decimal(10,2) NOT NULL COMMENT '告警阈值',
  `comparison` varchar(10) DEFAULT '>' COMMENT '比较符：>/</=',
  `channels` text COMMENT '告警渠道（JSON数组：["email","wechat","sms"]）',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`device_id`) REFERENCES `device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 告警记录表
CREATE TABLE IF NOT EXISTS `alert_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `alert_config_id` int(11) NOT NULL COMMENT '告警配置ID',
  `device_id` int(11) NOT NULL COMMENT '设备ID',
  `metric_value` decimal(10,2) NOT NULL COMMENT '指标值',
  `alert_level` varchar(10) DEFAULT 'warning' COMMENT '告警等级：critical/warning/info',
  `message` text COMMENT '告警消息',
  `channels` text COMMENT '发送渠道',
  `send_status` varchar(20) DEFAULT 'pending' COMMENT '发送状态：pending/success/failed',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`alert_config_id`) REFERENCES `alert_config`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`device_id`) REFERENCES `device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 设备分组表
CREATE TABLE IF NOT EXISTS `device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `name` varchar(50) NOT NULL COMMENT '分组名称',
  `description` text COMMENT '分组描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 设备分组关联表
CREATE TABLE IF NOT EXISTS `device_group_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `group_id` int(11) NOT NULL COMMENT '分组ID',
  `device_id` int(11) NOT NULL COMMENT '设备ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_device` (`group_id`, `device_id`),
  FOREIGN KEY (`group_id`) REFERENCES `device_group`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`device_id`) REFERENCES `device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化测试数据（eNSP虚拟设备）
INSERT INTO `device` (`name`, `ip`, `vendor`, `model`, `username`, `password_enc`)
VALUES 
('华为AR1000路由器', '192.168.31.100', '华为', 'AR1000', 'admin', 'gAAAAABk...'), -- 替换为实际加密后的密码
('思科2800路由器', '192.168.31.101', '思科', '2800', 'cisco', 'gAAAAABk...'),
('华为S5700交换机', '192.168.31.102', '华为', 'S5700', 'admin', 'gAAAAABk...'),
('华为USG6000防火墙', '192.168.31.103', '华为', 'USG6000', 'admin', 'gAAAAABk...');

-- 初始化默认管理员用户（密码：admin123，实际使用时需要加密）
INSERT INTO `user` (`username`, `password_hash`, `role`, `email`)
VALUES 
('admin', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqJ5q5q5q5', 'admin', 'admin@example.com'),
('operator', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqJ5q5q5q5', 'operator', 'operator@example.com');
