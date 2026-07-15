-- 创建分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
    sort INT DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort (sort),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 为商品表添加分类字段
ALTER TABLE product ADD COLUMN category_id BIGINT COMMENT '分类ID' AFTER `description`;
ALTER TABLE product ADD COLUMN parent_category_id BIGINT COMMENT '父分类ID' AFTER `category_id`;

-- 添加外键约束
ALTER TABLE product ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
ALTER TABLE product ADD CONSTRAINT fk_product_parent_category FOREIGN KEY (parent_category_id) REFERENCES category(id) ON DELETE SET NULL;

-- 添加索引
ALTER TABLE product ADD INDEX idx_category_id (category_id);
ALTER TABLE product ADD INDEX idx_parent_category_id (parent_category_id);

-- 初始化分类数据，使用INSERT IGNORE避免重复插入
INSERT IGNORE INTO category (name, parent_id, sort) VALUES 
('蔬菜', 0, 1),
('水果', 0, 2),
('肉类', 0, 3),
('蛋类', 0, 4),
('粮油', 0, 5),
('干货', 0, 6),
('饮料', 0, 7),
('零食', 0, 8);