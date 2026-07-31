-- ============================================
-- Seata AT 模式必需表 — undo_log
-- 用于存储数据变更的前镜像(after-image)和回滚信息
-- ============================================
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `branch_id`     BIGINT(20)   NOT NULL COMMENT '分支事务ID',
  `xid`           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
  `context`       VARCHAR(128) NOT NULL COMMENT '上下文',
  `rollback_info` LONGBLOB     NOT NULL COMMENT '回滚信息(前镜像)',
  `log_status`    INT(11)      NOT NULL COMMENT '状态: 0=正常, 1=已全局提交',
  `log_created`   DATETIME     NOT NULL COMMENT '创建时间',
  `log_modified`  DATETIME     NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 模式 undo 日志表';
