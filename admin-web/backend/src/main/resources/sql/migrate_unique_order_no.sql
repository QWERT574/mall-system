-- ============================================================
-- 清理 orders 表 order_no 历史脏数据 + 加 UNIQUE 索引
-- 背景：
--   1) 9 条历史订单号被重复使用（每个号 3 次）
--   2) 20 条 NULL 早期订单
--   3) 当前无 UNIQUE 约束，存在并发风险（虽然生成算法带 6 位随机，理论上撞概率小）
-- 处理：
--   1) 把重复的 order_no 改名为 ORD-XXX-DUP-{id}（保留全部数据）
--   2) 20 条 NULL 保持 NULL（MySQL UNIQUE 允许多个 NULL）
--   3) 加 UNIQUE 索引 uk_order_no
-- ============================================================

-- 1. 把重复的 order_no 加后缀（保留最早的，把后两条改名为带 DUP）
--    这样：最早的 id 保留原 order_no，后面 2 条改名
UPDATE orders SET order_no = CONCAT(order_no, '-DUP', id)
WHERE id IN (
    61, 65,  -- ORD202604010001 的后两条
    62, 66,  -- ORD202604020001 的后两条
    63, 67   -- ORD202604030001 的后两条
);

-- 2. 加 UNIQUE 索引
ALTER TABLE orders ADD UNIQUE KEY uk_order_no (order_no);

-- 3. 验证
-- SHOW INDEX FROM orders WHERE Key_name = 'uk_order_no';
-- SELECT order_no, COUNT(*) FROM orders GROUP BY order_no HAVING COUNT(*) > 1;
