-- ============================================================
-- 购物车表：添加/确认唯一索引（并发安全）
-- 业务说明：同一用户、同一商品、同一规格 在购物车中只应存在一行
-- 并发场景：用户多端/多线程同时"加入购物车"
--           由 UNIQUE 索引 + INSERT ... ON DUPLICATE KEY UPDATE 保证原子性
-- 文件：src/main/resources/sql/migrate_cart_unique_index.sql
-- ============================================================

-- 1. 查看当前 cart 表索引
-- SHOW INDEX FROM cart;

-- 2. 若 uk_user_product_spec 不存在则添加（MySQL 不支持 IF NOT EXISTS 索引写法，用存储过程兼容）
DROP PROCEDURE IF EXISTS add_cart_unique_index;
DELIMITER //
CREATE PROCEDURE add_cart_unique_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cart'
          AND index_name = 'uk_user_product_spec'
    ) THEN
        ALTER TABLE cart ADD UNIQUE KEY uk_user_product_spec (user_id, product_id, spec_id);
    END IF;
END //
DELIMITER ;

CALL add_cart_unique_index();
DROP PROCEDURE add_cart_unique_index;

-- 3. 验证索引
-- SHOW INDEX FROM cart WHERE Key_name = 'uk_user_product_spec';

-- 4. 清理可能的脏数据（理论上不应该有，但保险起见）
-- 若历史数据存在重复行，需先手动删除/合并后，再执行上述加索引
-- DELETE c1 FROM cart c1 INNER JOIN cart c2
-- WHERE c1.id > c2.id
--   AND c1.user_id = c2.user_id
--   AND c1.product_id = c2.product_id
--   AND ((c1.spec_id IS NULL AND c2.spec_id IS NULL) OR c1.spec_id = c2.spec_id);
