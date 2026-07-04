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
    sort INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    stock INT DEFAULT 0 COMMENT '库存数量',
    sales INT DEFAULT 0 COMMENT '销量',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    images TEXT COMMENT '商品图片（JSON格式）',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    is_featured TINYINT DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    FOREIGN KEY (supplier_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_price (price),
    INDEX idx_sales (sales),
    INDEX idx_name (name),
    INDEX idx_supplier_id (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品规格表
CREATE TABLE IF NOT EXISTS product_spec (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_name VARCHAR(50) NOT NULL COMMENT '规格名称',
    spec_value VARCHAR(255) NOT NULL COMMENT '规格值',
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
    created_by BIGINT NOT NULL COMMENT '创建人ID',
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
