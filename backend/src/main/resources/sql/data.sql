-- 为商品添加分类信息
UPDATE product SET category_id = FLOOR(RAND() * 8) + 1, parent_category_id = FLOOR(RAND() * 8) + 1;