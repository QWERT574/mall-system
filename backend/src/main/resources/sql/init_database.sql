-- 乡村振兴农产品销售平台 - 完整数据库初始化脚本
-- 包含：基础表结构 + 扩展表结构 + 索引优化 + 初始化数据

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 基础表结构
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    openid VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    username VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    password VARCHAR(255) DEFAULT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    birthday DATE DEFAULT NULL COMMENT '生日',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    role_id BIGINT DEFAULT NULL COMMENT '角色ID',
    user_type INT DEFAULT 0 COMMENT '用户类型：0-普通用户，1-商品提供方，2-管理员',
    company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称（商品提供方）',
    company_address VARCHAR(255) DEFAULT NULL COMMENT '公司地址（商品提供方）',
    is_verified TINYINT DEFAULT 0 COMMENT '是否已认证：0-未认证，1-已认证',
    verified_at TIMESTAMP NULL DEFAULT NULL COMMENT '认证时间',
    verification_info TEXT COMMENT '认证信息',
    status TINYINT DEFAULT 1 COMMENT '账号状态：0-禁用，1-启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_openid (openid),
    INDEX idx_phone (phone),
    INDEX idx_username (username),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status),
    INDEX idx_is_verified (is_verified),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    level INT DEFAULT 1 COMMENT '分类层级',
    sort INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品分类关联表（ProductCategory 模型对应表，记录商品分类层级关系）
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    level INT DEFAULT 1 COMMENT '分类层级',
    icon VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类关联表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock INT DEFAULT 0 COMMENT '库存数量',
    description TEXT COMMENT '商品描述',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID（允许 NULL，配合 ON DELETE SET NULL）',
    seller_id BIGINT DEFAULT NULL COMMENT '商家ID',
    parent_category_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT fk_product_parent_category FOREIGN KEY (parent_category_id) REFERENCES category(id) ON DELETE SET NULL,
    INDEX idx_category_id (category_id),
    INDEX idx_parent_category_id (parent_category_id),
    INDEX idx_product_seller (seller_id),
    INDEX idx_product_category_price (category_id, price),
    INDEX idx_product_name (name),
    INDEX idx_product_stock (stock),
    FULLTEXT KEY ft_product_search (name, description) WITH PARSER ngram,
    FULLTEXT KEY ft_product_name (name) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

-- 商品规格表
CREATE TABLE IF NOT EXISTS product_spec (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_name VARCHAR(50) NOT NULL COMMENT '规格名称',
    description TEXT COMMENT '规格描述',
    price DECIMAL(10,2) NOT NULL COMMENT '规格价格',
    stock INT DEFAULT 0 COMMENT '规格库存',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '订单号（ORD+yyyyMMddHHmmss+6位随机）',
    openid VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    address_id BIGINT DEFAULT NULL COMMENT '地址ID',
    shipping_address_id BIGINT DEFAULT NULL COMMENT '收货地址ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) DEFAULT NULL COMMENT '实付金额',
    logistics_id BIGINT DEFAULT NULL COMMENT '物流ID',
    status INT DEFAULT 0 COMMENT '订单状态：0-待支付，1-待发货，2-已发货，3-已完成，4-已取消，5-已退款',
    pay_status INT DEFAULT 0 COMMENT '支付状态：0-未支付，1-已支付，2-支付失败',
    consignee VARCHAR(20) NOT NULL COMMENT '收货人',
    phone VARCHAR(20) NOT NULL COMMENT '收货电话',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    remark TEXT COMMENT '订单备注',
    user_coupon_id BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
    discount_amount DECIMAL(10,2) DEFAULT 0 COMMENT '优惠抵扣金额',
    total_price DECIMAL(10,2) DEFAULT NULL COMMENT '订单总价（兼容旧字段，业务用 total_amount）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（兼容旧字段，业务用 created_at）',
    pay_time TIMESTAMP DEFAULT NULL COMMENT '支付时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (address_id) REFERENCES shipping_address(id) ON DELETE SET NULL,
    FOREIGN KEY (shipping_address_id) REFERENCES shipping_address(id) ON DELETE SET NULL,
    FOREIGN KEY (logistics_id) REFERENCES logistics(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    -- 订单号唯一索引：保证业务层订单号不重复（UNIQUE 自动成为索引）
    -- 允许多个 NULL（MySQL 默认行为），但非 NULL 值必须唯一
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单项表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_id BIGINT DEFAULT NULL COMMENT '商品规格ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    spec_name VARCHAR(50) DEFAULT NULL COMMENT '规格名称',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    quantity INT NOT NULL COMMENT '购买数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (spec_id) REFERENCES product_spec(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 物流表
CREATE TABLE IF NOT EXISTS logistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    logistics_company VARCHAR(50) DEFAULT NULL COMMENT '物流公司',
    logistics_no VARCHAR(50) DEFAULT NULL COMMENT '物流单号',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '物流状态：pending-待发货，shipped-已发货，delivered-已送达，exception-异常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_logistics_no (logistics_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

-- 物流追踪表
CREATE TABLE IF NOT EXISTS logistics_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '追踪ID',
    logistics_id BIGINT NOT NULL COMMENT '物流ID',
    status VARCHAR(50) NOT NULL COMMENT '状态',
    description VARCHAR(255) NOT NULL COMMENT '描述',
    location VARCHAR(255) DEFAULT NULL COMMENT '位置',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (logistics_id) REFERENCES logistics(id) ON DELETE CASCADE,
    INDEX idx_logistics_id (logistics_id),
    INDEX idx_status (status),
    INDEX idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流追踪表';

-- 收货地址表
CREATE TABLE IF NOT EXISTS shipping_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) DEFAULT NULL COMMENT '地址名称（如：家、公司）',
    consignee VARCHAR(20) NOT NULL COMMENT '收货人',
    phone VARCHAR(20) NOT NULL COMMENT '收货电话',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 搜索历史表
CREATE TABLE IF NOT EXISTS search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '历史ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    keyword VARCHAR(100) NOT NULL COMMENT '搜索关键词',
    search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_search_time (search_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表';

-- ============================================
-- 2. 扩展表结构
-- ============================================

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '权限编码',
    description VARCHAR(255) DEFAULT NULL COMMENT '权限描述',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 商品提供方认证表
CREATE TABLE IF NOT EXISTS supplier_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '认证ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    company_name VARCHAR(100) NOT NULL COMMENT '公司名称',
    company_address VARCHAR(255) NOT NULL COMMENT '公司地址',
    business_license VARCHAR(255) NOT NULL COMMENT '营业执照图片URL',
    contact_person VARCHAR(20) NOT NULL COMMENT '联系人',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    status TINYINT DEFAULT 0 COMMENT '认证状态：0-待审核，1-已通过，2-已拒绝',
    reject_reason TEXT COMMENT '拒绝原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品提供方认证表';

-- 商品图片表
CREATE TABLE IF NOT EXISTS product_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片URL',
    sort INT DEFAULT 0 COMMENT '排序',
    is_cover TINYINT DEFAULT 0 COMMENT '是否封面：0-不是，1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- 商品标签表
CREATE TABLE IF NOT EXISTS product_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(20) NOT NULL UNIQUE COMMENT '标签名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签表';

-- 商品-标签关联表
CREATE TABLE IF NOT EXISTS product_tag_relation (
    product_id BIGINT NOT NULL COMMENT '商品ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES product_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品-标签关联表';

-- 购物车表
CREATE TABLE IF NOT EXISTS cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_id BIGINT DEFAULT NULL COMMENT '商品规格ID',
    -- 虚拟列：spec_id 为 NULL 时取 0，用于 UNIQUE 索引去重
    -- MySQL UNIQUE 索引对 NULL 不去重，因此引入该列解决 (user,product,NULL) 重复插入问题
    spec_id_dummy BIGINT GENERATED ALWAYS AS (COALESCE(spec_id, 0)) VIRTUAL COMMENT 'spec_id 兜底列（NULL→0），仅供 UNIQUE 索引',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    checked TINYINT DEFAULT 1 COMMENT '是否选中：0-未选中，1-选中',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (spec_id) REFERENCES product_spec(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product_spec (user_id, product_id, spec_id_dummy),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 支付表
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    payment_method INT NOT NULL COMMENT '支付方式：1-微信支付，2-支付宝支付',
    payment_no VARCHAR(100) DEFAULT NULL COMMENT '支付流水号',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status INT DEFAULT 0 COMMENT '支付状态：0-待支付，1-已支付，2-支付失败',
    pay_time TIMESTAMP DEFAULT NULL COMMENT '支付时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '支付备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_payment_no (payment_no),
    INDEX idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

-- 助农活动表
CREATE TABLE IF NOT EXISTS activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    title VARCHAR(100) NOT NULL COMMENT '活动标题',
    description TEXT NOT NULL COMMENT '活动描述',
    start_time TIMESTAMP NOT NULL COMMENT '活动开始时间',
    end_time TIMESTAMP NOT NULL COMMENT '活动结束时间',
    activity_type INT NOT NULL COMMENT '活动类型：1-大宗采购，2-农场参观，3-实地观光',
    location VARCHAR(255) NOT NULL COMMENT '活动地点',
    organizer VARCHAR(100) NOT NULL COMMENT '主办方',
    contact_person VARCHAR(20) NOT NULL COMMENT '联系人',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    max_participants INT DEFAULT 0 COMMENT '最大参与人数（0表示无限制）',
    current_participants INT DEFAULT 0 COMMENT '当前参与人数',
    status INT DEFAULT 0 COMMENT '活动状态：0-筹备中，1-进行中，2-已结束，3-已取消',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '活动封面图片',
    is_recommended TINYINT DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
    recommend_order INT DEFAULT 999 COMMENT '推荐排序（越小越靠前）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_activity_type (activity_type),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助农活动表';

-- 活动参与者表
CREATE TABLE IF NOT EXISTS activity_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参与者ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    participant_name VARCHAR(20) NOT NULL COMMENT '参与者姓名',
    participant_phone VARCHAR(20) NOT NULL COMMENT '参与者电话',
    status INT DEFAULT 0 COMMENT '参与状态：0-待审核，1-已通过，2-已拒绝',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_activity_id (activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与者表';

-- 活动图片表
CREATE TABLE IF NOT EXISTS activity_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片URL',
    sort INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
    INDEX idx_activity_id (activity_id),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动图片表';

-- 商品评价表
CREATE TABLE IF NOT EXISTS product_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    rating INT NOT NULL COMMENT '评分：1-5星',
    content TEXT NOT NULL COMMENT '评价内容',
    images TEXT COMMENT '评价图片（JSON格式）',
    status TINYINT DEFAULT 1 COMMENT '评价状态：0-已删除，1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 评价回复表
CREATE TABLE IF NOT EXISTS review_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回复ID',
    review_id BIGINT NOT NULL COMMENT '评价ID',
    reply_content TEXT NOT NULL COMMENT '回复内容',
    reply_type INT NOT NULL COMMENT '回复类型：1-商家回复，2-平台回复',
    reply_by BIGINT NOT NULL COMMENT '回复人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (review_id) REFERENCES product_review(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_by) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价回复表';

-- 售后服务表
CREATE TABLE IF NOT EXISTS after_sale_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '售后ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    service_type INT NOT NULL COMMENT '服务类型：1-退货，2-换货，3-维修，4-投诉',
    reason TEXT NOT NULL COMMENT '售后原因',
    images TEXT COMMENT '售后凭证图片（JSON格式）',
    status INT DEFAULT 0 COMMENT '售后状态：0-待处理，1-处理中，2-已解决，3-已关闭',
    service_result TEXT COMMENT '处理结果',
    refund_amount DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额',
    return_logistics VARCHAR(200) DEFAULT NULL COMMENT '退货物流单号',
    return_logistics_company VARCHAR(100) DEFAULT NULL COMMENT '退货物流公司',
    expect_complete_date DATETIME DEFAULT NULL COMMENT '预计完成时间',
    close_reason TEXT COMMENT '关闭原因',
    supplementary_evidence TEXT COMMENT '补充证据',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    processed_by BIGINT DEFAULT NULL COMMENT '处理人ID',
    processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_service_type (service_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后服务表';

-- 服务记录表
CREATE TABLE IF NOT EXISTS service_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    after_sale_id BIGINT NOT NULL COMMENT '售后ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operation_type INT NOT NULL COMMENT '操作类型：1-创建工单，2-分配处理人，3-处理中，4-已解决，5-已关闭',
    operation_content TEXT NOT NULL COMMENT '操作内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (after_sale_id) REFERENCES after_sale_service(id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_after_sale_id (after_sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务记录表';

-- AI服务记录表
CREATE TABLE IF NOT EXISTS ai_service_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    query TEXT NOT NULL COMMENT '用户查询内容',
    response TEXT NOT NULL COMMENT 'AI响应内容',
    service_type INT DEFAULT 0 COMMENT '服务类型：0-商品检索，1-智能推荐，2-常见问题，3-其他',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_service_type (service_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI服务记录表';

-- 用户偏好表
CREATE TABLE IF NOT EXISTS user_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '偏好ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    preference_data JSON NOT NULL COMMENT '偏好数据（JSON格式，包含用户喜欢的商品类别、品牌等）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 2.6 RAG知识库相关表
-- ============================================

-- 知识文档表（管理原始文档/知识条目）
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID',
    title VARCHAR(500) NOT NULL COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT '文档原始内容',
    source_type TINYINT DEFAULT 0 COMMENT '来源类型：0-手动录入 1-FAQ 2-历史对话 3-政策文档 4-商品说明 5-帮助文档',
    category VARCHAR(100) DEFAULT NULL COMMENT '知识分类（如：售后政策、物流规则、商品百科）',
    tags VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理 1-已向量化 2-已启用 3-已禁用',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    doc_meta VARCHAR(1000) DEFAULT NULL COMMENT '元数据JSON（作者、URL等）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_source_type (source_type),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识文档表';

-- 知识分块表（文档分块 + 向量存储）
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分块ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号（文档内有序）',
    content TEXT NOT NULL COMMENT '分块文本内容',
    embedding MEDIUMBLOB COMMENT '向量嵌入（float数组序列化）',
    embedding_model VARCHAR(100) DEFAULT NULL COMMENT '生成向量的模型名',
    embedding_dim INT DEFAULT NULL COMMENT '向量维度',
    token_count INT DEFAULT NULL COMMENT 'token数量',
    chunk_meta VARCHAR(1000) DEFAULT NULL COMMENT '分块元数据JSON',
    score_avg DECIMAL(10,6) DEFAULT NULL COMMENT '平均检索得分（用于质量评估）',
    hit_count INT DEFAULT 0 COMMENT '被检索命中次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (document_id) REFERENCES knowledge_document(id) ON DELETE CASCADE,
    INDEX idx_document_id (document_id),
    INDEX idx_hit_count (hit_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分块与向量存储表';

-- FAQ问答对表（结构化高频问题）
CREATE TABLE IF NOT EXISTS knowledge_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'FAQ ID',
    question TEXT NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '标准答案',
    question_embedding MEDIUMBLOB COMMENT '问题的向量嵌入',
    category VARCHAR(100) DEFAULT NULL COMMENT 'FAQ分类',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词（逗号分隔）',
    priority INT DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    embedding_model VARCHAR(100) DEFAULT NULL COMMENT '向量模型',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_hit_count (hit_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ结构化问答对表';

-- 对话会话表（多轮对话上下文管理）
CREATE TABLE IF NOT EXISTS conversation_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    session_token VARCHAR(64) NOT NULL UNIQUE COMMENT '会话令牌（前端持有）',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    service_type INT DEFAULT 1 COMMENT '服务类型：1-商品咨询 2-物流查询 3-售后咨询',
    title VARCHAR(255) DEFAULT NULL COMMENT '会话标题（取首条消息摘要）',
    message_count INT DEFAULT 0 COMMENT '消息总数',
    context_summary TEXT DEFAULT NULL COMMENT '对话上下文摘要（压缩历史）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-已关闭 1-活跃',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    INDEX idx_user_id (user_id),
    INDEX idx_session_token (session_token),
    INDEX idx_status (status),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI多轮对话会话表';

-- 对话消息表（记录每轮对话的完整信息，含RAG溯源）
CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    retrieved_chunks TEXT DEFAULT NULL COMMENT '检索到的知识分块JSON（含文档ID、内容、得分）',
    retrieved_faqs TEXT DEFAULT NULL COMMENT '命中的FAQ JSON',
    sources TEXT DEFAULT NULL COMMENT '知识来源溯源JSON（用于可解释性展示）',
    retrieval_score DECIMAL(10,6) DEFAULT NULL COMMENT '检索置信度',
    response_time_ms INT DEFAULT NULL COMMENT '响应耗时（毫秒）',
    token_count INT DEFAULT NULL COMMENT 'token数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (session_id) REFERENCES conversation_session(id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息与溯源表';

-- RAG评估记录表（用于效果对比与质量监控）
CREATE TABLE IF NOT EXISTS rag_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评估ID',
    query TEXT NOT NULL COMMENT '测试问题',
    rag_answer TEXT COMMENT 'RAG增强回答',
    baseline_answer TEXT COMMENT '基线回答（无RAG）',
    rag_response_time_ms INT DEFAULT NULL COMMENT 'RAG响应耗时',
    baseline_response_time_ms INT DEFAULT NULL COMMENT '基线响应耗时',
    rag_retrieval_time_ms INT DEFAULT NULL COMMENT '检索耗时',
    rag_source_count INT DEFAULT NULL COMMENT '检索到的来源数',
    accuracy_score DECIMAL(3,2) DEFAULT NULL COMMENT '准确率评分(0-1)',
    relevance_score DECIMAL(3,2) DEFAULT NULL COMMENT '相关性评分(0-1)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG效果评估记录表';

-- ============================================
-- 3. 初始化数据
-- ============================================

-- 初始化角色数据
INSERT IGNORE INTO role (id, name, description) VALUES 
(1, '管理员', '系统管理员，拥有所有权限'),
(2, '商品提供方', '商品提供方用户，可发布商品'),
(3, '普通用户', '普通购买用户');

-- 初始化权限数据
INSERT IGNORE INTO permission (id, name, code, description, parent_id) VALUES 
(1, '系统管理', 'system:manage', '系统管理权限', 0),
(2, '用户管理', 'user:manage', '用户管理权限', 1),
(3, '商品管理', 'product:manage', '商品管理权限', 1),
(4, '订单管理', 'order:manage', '订单管理权限', 1),
(5, '活动管理', 'activity:manage', '活动管理权限', 1),
(6, '评价管理', 'review:manage', '评价管理权限', 1),
(7, '售后管理', 'after_sale:manage', '售后管理权限', 1),
(8, '商品发布', 'product:publish', '商品发布权限', 3),
(9, '商品编辑', 'product:edit', '商品编辑权限', 3),
(10, '商品删除', 'product:delete', '商品删除权限', 3);

-- 初始化角色权限关联数据
INSERT IGNORE INTO role_permission (role_id, permission_id) VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(2, 3), (2, 8), (2, 9), (2, 10),
(3, 4);

-- 初始化系统配置数据
INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES 
('site_name', '乡村振兴农产品销售平台', '网站名称'),
('site_description', '助力乡村振兴，推广优质农产品', '网站描述'),
('ai_assistant_enabled', '1', '是否启用AI助手'),
('ai_api_key', '', 'AI服务API密钥'),
('ai_api_url', '', 'AI服务API地址');

-- 初始化商品标签数据
INSERT IGNORE INTO product_tag (id, name) VALUES 
(1, '有机'),
(2, '绿色'),
(3, '无公害'),
(4, '扶贫产品'),
(5, '地方特产'),
(6, '新鲜'),
(7, '干货'),
(8, '粮油');

-- 初始化商品分类数据
INSERT IGNORE INTO category (id, name, parent_id, sort) VALUES 
(1, '蔬菜', 0, 1),
(2, '水果', 0, 2),
(3, '肉类', 0, 3),
(4, '粮油', 0, 4),
(5, '干货', 0, 5),
(6, '禽蛋', 0, 6);

-- 测试活动数据（id=16 兜底；created_by 用 NULL——DataInitializer 启动会删除重建
-- admin(id=1)，若此处引用 admin 会被 ON DELETE CASCADE 级联删除导致活动丢失，
-- 与本地历史数据（created_by 全 NULL）保持一致）
INSERT IGNORE INTO activity (id, title, description, start_time, end_time, activity_type, location, organizer, contact_person, contact_phone, max_participants, current_participants, status, created_by) VALUES 
(16, '助农采摘节', '乡村振兴助农采摘活动', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 3, '示范农场', '村委会', '李主任', '13800138016', 100, 0, 1, NULL);

-- ============================================
-- 4. 初始化用户数据（重要！用于登录测试）
-- ============================================

-- 密码 123456 的 BCrypt 哈希值
-- 可通过 BCryptPasswordEncoder.encode("123456") 生成

-- 管理员账号 (user_type=2)
INSERT IGNORE INTO user (id, username, password, nickname, user_type, status) VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5mO', '系统管理员', 2, 1);

-- 关联管理员角色
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (1, 1);

-- 测试商家账号 (user_type=1)
INSERT IGNORE INTO user (id, username, password, nickname, phone, user_type, company_name, is_verified, status) VALUES 
(11, 'seller', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5mO', '绿色农场旗舰店', '13800138001', 1, '绿色农业发展有限公司', 1, 1);

-- 关联商家角色
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (11, 2);

-- 测试买家账号 (user_type=0)
INSERT IGNORE INTO user (id, username, password, nickname, phone, user_type, status) VALUES 
(101, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5mO', '张三', '13900139001', 0, 1);

-- 关联普通用户角色
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (101, 3);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 本地消息表（Outbox）— 保证订单消息可靠投递（C5）
-- 与业务事务同库写入，提交后异步发 MQ，失败由补偿任务重试
-- ============================================
CREATE TABLE IF NOT EXISTS message_outbox (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  biz_type        VARCHAR(64)  NOT NULL COMMENT '业务类型(ORDER_CREATED)',
  biz_id          BIGINT       NOT NULL COMMENT '业务ID(orderId)',
  topic           VARCHAR(128) NOT NULL COMMENT 'MQ topic',
  payload         TEXT         NOT NULL COMMENT '消息体JSON',
  status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待发送 1=已发送 2=已放弃',
  retry_count     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status_retry (status, next_retry_time),
  INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表(Outbox)';

-- ============================================
-- 优惠券表（payment-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '优惠券ID',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type INT NOT NULL COMMENT '类型：1-满减，2-折扣，3-无门槛',
    threshold DECIMAL(10,2) DEFAULT NULL COMMENT '使用门槛金额',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠金额/折扣率',
    total_count INT DEFAULT 0 COMMENT '发放总量',
    used_count INT DEFAULT 0 COMMENT '已使用数量',
    per_user_limit INT DEFAULT 1 COMMENT '每人限领数量',
    start_time TIMESTAMP DEFAULT NULL COMMENT '生效时间',
    end_time TIMESTAMP DEFAULT NULL COMMENT '失效时间',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    seller_id BIGINT DEFAULT NULL COMMENT '发放商家ID',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ============================================
-- 用户优惠券表（payment-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    status INT DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期',
    used_at TIMESTAMP DEFAULT NULL COMMENT '使用时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    INDEX idx_user_id (user_id),
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- ============================================
-- 折扣活动表（payment-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS discount_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    name VARCHAR(100) NOT NULL COMMENT '活动名称',
    type INT NOT NULL COMMENT '类型：1-折扣，2-满减',
    discount_rate DECIMAL(5,2) DEFAULT NULL COMMENT '折扣率（如0.85=85折）',
    threshold DECIMAL(10,2) DEFAULT NULL COMMENT '满减门槛',
    reduce_amount DECIMAL(10,2) DEFAULT NULL COMMENT '满减金额',
    start_time TIMESTAMP DEFAULT NULL COMMENT '开始时间',
    end_time TIMESTAMP DEFAULT NULL COMMENT '结束时间',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    seller_id BIGINT DEFAULT NULL COMMENT '商家ID',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣活动表';

-- ============================================
-- 折扣活动-商品关联表（payment-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS discount_activity_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    discount_price DECIMAL(10,2) DEFAULT NULL COMMENT '活动价格',
    INDEX idx_activity_id (activity_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣活动商品关联表';

-- ============================================
-- 聊天会话表（chat-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    seller_id BIGINT DEFAULT NULL COMMENT '商家ID',
    agent_id BIGINT DEFAULT NULL COMMENT '客服ID',
    product_id BIGINT DEFAULT NULL COMMENT '关联商品ID',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    status INT DEFAULT 1 COMMENT '状态：0-已关闭，1-活跃',
    session_type INT DEFAULT 0 COMMENT '会话类型',
    source VARCHAR(50) DEFAULT NULL COMMENT '来源',
    auto_reply_enabled INT DEFAULT 1 COMMENT '是否启用自动回复',
    user_unread INT DEFAULT 0 COMMENT '用户未读数',
    seller_unread INT DEFAULT 0 COMMENT '商家未读数',
    last_message_at TIMESTAMP DEFAULT NULL COMMENT '最后消息时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    closed_at TIMESTAMP DEFAULT NULL COMMENT '关闭时间',
    close_reason VARCHAR(255) DEFAULT NULL COMMENT '关闭原因',
    INDEX idx_user_id (user_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ============================================
-- 聊天消息表（chat-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_type INT NOT NULL COMMENT '发送者类型：0-用户，1-商家，2-客服',
    receiver_id BIGINT DEFAULT NULL COMMENT '接收者ID',
    content TEXT COMMENT '消息内容',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
    message_type INT DEFAULT 0 COMMENT '消息类型：0-文本，1-图片，2-文件',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小',
    related_order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    related_product_id BIGINT DEFAULT NULL COMMENT '关联商品ID',
    is_read INT DEFAULT 0 COMMENT '是否已读',
    read_at TIMESTAMP DEFAULT NULL COMMENT '已读时间',
    status INT DEFAULT 1 COMMENT '状态：0-已撤回，1-正常',
    delivered_at TIMESTAMP DEFAULT NULL COMMENT '送达时间',
    is_auto_reply INT DEFAULT 0 COMMENT '是否自动回复',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session_id (session_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ============================================
-- 聊天通知表（chat-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS chat_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_type INT DEFAULT 0 COMMENT '用户类型',
    session_id BIGINT DEFAULT NULL COMMENT '会话ID',
    message_id BIGINT DEFAULT NULL COMMENT '消息ID',
    type VARCHAR(50) DEFAULT NULL COMMENT '通知类型',
    title VARCHAR(100) DEFAULT NULL COMMENT '标题',
    content VARCHAR(500) DEFAULT NULL COMMENT '内容',
    is_read INT DEFAULT 0 COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天通知表';

-- ============================================
-- 售后聊天表（order-service/chat-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS after_sale_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    after_sale_id BIGINT NOT NULL COMMENT '售后单ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_type INT NOT NULL COMMENT '发送者类型',
    content TEXT COMMENT '消息内容',
    message_type INT DEFAULT 0 COMMENT '消息类型',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    target_type VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    INDEX idx_after_sale_id (after_sale_id),
    INDEX idx_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后聊天表';

-- ============================================
-- 管理员干预表（payment-service 域）
-- ============================================
CREATE TABLE IF NOT EXISTS admin_intervention (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '干预ID',
    order_id BIGINT DEFAULT NULL COMMENT '订单ID',
    product_id BIGINT DEFAULT NULL COMMENT '商品ID',
    seller_id BIGINT DEFAULT NULL COMMENT '商家ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id BIGINT DEFAULT NULL COMMENT '会话ID',
    aftersale_id BIGINT DEFAULT NULL COMMENT '售后单ID',
    issue_type VARCHAR(50) NOT NULL COMMENT '问题类型',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    description TEXT COMMENT '描述',
    evidence_images TEXT COMMENT '证据图片(JSON)',
    status INT DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-已解决',
    admin_id BIGINT DEFAULT NULL COMMENT '处理管理员ID',
    result VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    admin_remark VARCHAR(500) DEFAULT NULL COMMENT '管理员备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    processed_at TIMESTAMP DEFAULT NULL COMMENT '处理时间',
    INDEX idx_order_id (order_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员干预表';
