-- ============================================================
-- 清理 orders 表的孤儿列 order_sn 与孤儿索引 idx_order_sn
-- 背景：
--   1) order_sn 列在 Java 实体中已重命名为 orderSn（@TableField("order_no")）
--   2) Java 代码 0 引用 order_sn（5 个文件全是 orderSn 字段）
--   3) 现有 61 条数据 order_sn 全为 NULL
--   4) idx_order_sn 索引没有任何查询使用
-- 作用：精简表结构，避免面试官/答辩时被问到"两个订单号列是干嘛的"
-- ============================================================

-- 1. 备份当前数据（可选，保险起见）
-- CREATE TABLE orders_backup_20260609 AS SELECT * FROM orders;

-- 2. 删除孤儿索引
ALTER TABLE orders DROP INDEX idx_order_sn;

-- 3. 删除孤儿列
ALTER TABLE orders DROP COLUMN order_sn;

-- 4. 验证
-- DESCRIBE orders;
-- SHOW INDEX FROM orders;
