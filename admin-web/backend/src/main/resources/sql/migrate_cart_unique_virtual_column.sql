-- ============================================================
-- 购物车表：修复 UNIQUE 索引对 NULL 不去重的问题
-- 业务说明：MySQL UNIQUE 索引允许多个 NULL 值同时存在，
--           导致 (user_id=1, product_id=1, spec_id=NULL) 可被多次插入。
-- 修复方案：增加一个生成列 spec_id_dummy（NULL 自动转 0），
--           UNIQUE 索引改为基于该生成列。
-- ============================================================

-- 1. 清理重复行（按 user_id+product_id+spec_id 分组，保留最早 id）
DELETE c1 FROM cart c1
INNER JOIN cart c2
WHERE c1.id > c2.id
  AND c1.user_id = c2.user_id
  AND c1.product_id = c2.product_id
  AND ((c1.spec_id IS NULL AND c2.spec_id IS NULL) OR c1.spec_id = c2.spec_id);

-- 2. 删除旧 UNIQUE 索引
ALTER TABLE cart DROP INDEX uk_user_product_spec;

-- 3. 增加虚拟列：NULL 自动转 0（spec_id 业务范围 1+ 不会冲突）
ALTER TABLE cart
    ADD COLUMN spec_id_dummy BIGINT GENERATED ALWAYS AS (COALESCE(spec_id, 0)) VIRTUAL
    COMMENT 'spec_id 兜底列（NULL→0），供 UNIQUE 索引使用';

-- 4. 加新 UNIQUE 索引（基于虚拟列）
ALTER TABLE cart ADD UNIQUE KEY uk_user_product_spec (user_id, product_id, spec_id_dummy);

-- 5. 验证
-- SHOW INDEX FROM cart WHERE Key_name = 'uk_user_product_spec';
