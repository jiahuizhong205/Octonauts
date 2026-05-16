-- 1. 创建并使用系统专属数据库
CREATE DATABASE IF NOT EXISTS `campushub_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `campushub_db`;

-- 2. 清理历史表结构（注意顺序：先删有关联外键逻辑的子表，再删父表）
DROP TABLE IF EXISTS `biz_evaluation`;
DROP TABLE IF EXISTS `biz_order`;
DROP TABLE IF EXISTS `biz_requirement`;
DROP TABLE IF EXISTS `sys_user`;

-- 3. 创建核心表

-- 用户基础信息表
CREATE TABLE `sys_user` (
  `user_id` BIGINT NOT NULL COMMENT '用户唯一标识，雪花算法生成',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '加盐哈希密码',
  `phone_encrypted` VARCHAR(255) NOT NULL COMMENT '加密后的手机号',
  `student_id` VARCHAR(32) NOT NULL COMMENT '学号',
  `campus` VARCHAR(64) DEFAULT NULL COMMENT '校区',
  `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用积分，初始100',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

-- 需求信息表
CREATE TABLE `biz_requirement` (
  `req_id` BIGINT NOT NULL COMMENT '需求唯一标识',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `title` VARCHAR(128) NOT NULL COMMENT '需求标题',
  `description` TEXT NOT NULL COMMENT '需求详情描述',
  `budget` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '预算金额，精确到分',
  `type` VARCHAR(32) NOT NULL COMMENT '需求分类枚举值',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '需求状态：PENDING, ACCEPTED, COMPLETED, CANCELED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`req_id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='需求信息表';

-- 交易订单表
CREATE TABLE `biz_order` (
  `order_id` BIGINT NOT NULL COMMENT '订单唯一标识',
  `req_id` BIGINT NOT NULL COMMENT '关联的需求ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接单者用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '交易最终金额',
  `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '订单状态：IN_PROGRESS, TO_CONFIRM, COMPLETED, CANCELED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接单/创建时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '订单完成时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_req_id` (`req_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单表';

-- 订单评价表
CREATE TABLE `biz_evaluation` (
  `eval_id` BIGINT NOT NULL COMMENT '评价唯一标识',
  `order_id` BIGINT NOT NULL COMMENT '关联的订单ID',
  `reviewer_id` BIGINT NOT NULL COMMENT '评价方用户ID',
  `target_id` BIGINT NOT NULL COMMENT '被评价方用户ID',
  `star` TINYINT NOT NULL COMMENT '星级评分(1-5)',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '文字评价内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`eval_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单评价表';