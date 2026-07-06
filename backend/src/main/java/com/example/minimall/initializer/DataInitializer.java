package com.example.minimall.initializer;

import com.example.minimall.mapper.CategoryMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.mapper.UserMapper;
import com.example.minimall.model.Category;
import com.example.minimall.model.Product;
import com.example.minimall.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统启动时数据初始化器。
 * <p>
 * 实现 {@link CommandLineRunner} 风格逻辑：
 * 1. 自动创建/补齐数据库表与字段；
 * 2. 初始化管理员/商家/买家等测试账号（密码经 BCrypt 加密）；
 * 3. 商品表为空时插入默认分类与商品；
 * 4. 修复微信登录遗留的 user_type 为空的用户。
 * </p>
 */
@Component
public class DataInitializer {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final DataSource dataSource;

    /**
     * 构造方法，注入相关 Mapper 与数据源。
     */
    public DataInitializer(ProductMapper productMapper, CategoryMapper categoryMapper, UserMapper userMapper, DataSource dataSource) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.dataSource = dataSource;
    }

    /**
     * 装配启动钩子：Spring Boot 启动完成后执行表结构与基础数据初始化。
     *
     * @return CommandLineRunner 任务体
     */
    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            try {
                // 0. 确保数据库表存在（自动建表）
                System.out.println("检查并创建必要的数据库表...");
                ensureTablesExist();

                // 1. 初始化用户数据（管理员、商家、买家）
                System.out.println("开始初始化用户数据...");
                initUsers();

                // 2. 检查是否需要初始化商品数据（只在商品表为空时才初始化）
                long productCount = productMapper.selectCount(null);
                if (productCount == 0) {
                    System.out.println("商品表为空，开始插入初始商品数据...");
                    insertCategories();
                    insertProducts();
                    System.out.println("商品数据初始化完成！");
                } else {
                    System.out.println("商品表已有 " + productCount + " 条记录，跳过商品初始化");
                }

                System.out.println("基础数据初始化完成！");

                // 修复userType为null的用户（微信登录创建的用户）
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    int updated = stmt.executeUpdate("UPDATE user SET user_type = 0 WHERE user_type IS NULL");
                    if (updated > 0) {
                        System.out.println("已修复 " + updated + " 个用户的userType（设为买家）");
                    }
                }
            } catch (Exception e) {
                System.out.println("数据初始化异常: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * 初始化/重建测试账号：管理员 admin、商家 seller、买家 user、testseller。
     */
    private void initUsers() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        try {
            // 强制删除旧的管理员账号（如果存在）
            User existingAdmin = userMapper.selectByUsername("admin");
            if (existingAdmin != null) {
                userMapper.deleteById(existingAdmin.getId());
                System.out.println("🗑️ 删除旧管理员账号: " + existingAdmin.getUsername());
            }
            
            // 创建管理员账号
            User adminUser = new User();
            adminUser.setId(1L);
            adminUser.setUsername("admin");
            String encodedPassword = passwordEncoder.encode("123456");
            System.out.println("🔐 管理员密码哈希: " + encodedPassword.substring(0, 20) + "...");
            adminUser.setPassword(encodedPassword);
            adminUser.setNickname("系统管理员");
            adminUser.setUserType(2); // 管理员
            adminUser.setStatus(1);
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());
            int result = userMapper.insert(adminUser);
            System.out.println("✅ 创建管理员账号: admin / 123456 (影响行数: " + result + ", ID: " + adminUser.getId() + ")");
            
            // 验证插入结果
            User verifyAdmin = userMapper.selectByUsername("admin");
            if (verifyAdmin != null) {
                System.out.println("✅ 验证成功 - 管理员账号已存在: " + verifyAdmin.getUsername());
            } else {
                System.out.println("❌ 验证失败 - 管理员账号未找到!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 创建管理员账号失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            // 创建商家账号
            User sellerUser = userMapper.selectByUsername("seller");
            if (sellerUser == null) {
                sellerUser = new User();
                sellerUser.setId(11L);
                sellerUser.setUsername("seller");
                sellerUser.setPassword(passwordEncoder.encode("123456"));
                sellerUser.setNickname("绿色农场旗舰店");
                sellerUser.setPhone("13800138001");
                sellerUser.setUserType(1); // 商家
                sellerUser.setCompanyName("绿色农业发展有限公司");
                sellerUser.setIsVerified(1);
                sellerUser.setStatus(1);
                sellerUser.setCreatedAt(LocalDateTime.now());
                sellerUser.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(sellerUser);
                System.out.println("✅ 创建商家账号: seller / 123456");
            } else {
                System.out.println("ℹ️ 商家账号已存在: " + sellerUser.getUsername());
            }
            
            // 创建普通用户
            User normalUser = userMapper.selectByUsername("user");
            if (normalUser == null) {
                normalUser = new User();
                normalUser.setId(101L);
                normalUser.setUsername("user");
                normalUser.setPassword(passwordEncoder.encode("123456"));
                normalUser.setNickname("张三");
                normalUser.setPhone("13900139001");
                normalUser.setUserType(0); // 普通用户
                normalUser.setStatus(1);
                normalUser.setCreatedAt(LocalDateTime.now());
                normalUser.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(normalUser);
                System.out.println("✅ 创建买家账号: user / 123456");
            } else {
                System.out.println("ℹ️ 买家账号已存在: " + normalUser.getUsername());
            }
        } catch (Exception e) {
            System.err.println("❌ 创建测试用户失败: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (userMapper.selectByUsername("testseller") == null) {
                User testSellerUser = new User();
                testSellerUser.setUsername("testseller");
                testSellerUser.setPassword(passwordEncoder.encode("123456"));
                testSellerUser.setNickname("测试商家");
                testSellerUser.setPhone("13700137001");
                testSellerUser.setUserType(1);
                testSellerUser.setCompanyName("测试商家店铺");
                testSellerUser.setCompanyAddress("测试地址");
                testSellerUser.setIsVerified(1);
                testSellerUser.setStatus(1);
                testSellerUser.setCreatedAt(LocalDateTime.now());
                testSellerUser.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(testSellerUser);
                System.out.println("✅ 创建测试商家账号: testseller / 123456");
            }
        } catch (Exception e) {
            System.err.println("❌ 创建testseller账号失败: " + e.getMessage());
        }
    }

    /**
     * 插入商品分类根节点：水果、蔬菜、蛋类、粮油、坚果、蜂蜜。
     */
    private void insertCategories() {
        // 插入几个基础分类
        Category fruitCategory = new Category();
        fruitCategory.setName("水果");
        fruitCategory.setParentId(0L);
        fruitCategory.setSort(1);
        fruitCategory.setIsDeleted(0);
        fruitCategory.setCreatedAt(LocalDateTime.now());
        fruitCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(fruitCategory);
        System.out.println("插入分类: 水果 (ID: " + fruitCategory.getId() + ")");

        Category vegetableCategory = new Category();
        vegetableCategory.setName("蔬菜");
        vegetableCategory.setParentId(0L);
        vegetableCategory.setSort(2);
        vegetableCategory.setIsDeleted(0);
        vegetableCategory.setCreatedAt(LocalDateTime.now());
        vegetableCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(vegetableCategory);
        System.out.println("插入分类: 蔬菜 (ID: " + vegetableCategory.getId() + ")");

        Category eggCategory = new Category();
        eggCategory.setName("蛋类");
        eggCategory.setParentId(0L);
        eggCategory.setSort(3);
        eggCategory.setIsDeleted(0);
        eggCategory.setCreatedAt(LocalDateTime.now());
        eggCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(eggCategory);
        System.out.println("插入分类: 蛋类 (ID: " + eggCategory.getId() + ")");

        Category grainCategory = new Category();
        grainCategory.setName("粮油");
        grainCategory.setParentId(0L);
        grainCategory.setSort(4);
        grainCategory.setIsDeleted(0);
        grainCategory.setCreatedAt(LocalDateTime.now());
        grainCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(grainCategory);
        System.out.println("插入分类: 粮油 (ID: " + grainCategory.getId() + ")");

        Category nutCategory = new Category();
        nutCategory.setName("坚果");
        nutCategory.setParentId(0L);
        nutCategory.setSort(5);
        nutCategory.setIsDeleted(0);
        nutCategory.setCreatedAt(LocalDateTime.now());
        nutCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(nutCategory);
        System.out.println("插入分类: 坚果 (ID: " + nutCategory.getId() + ")");

        Category honeyCategory = new Category();
        honeyCategory.setName("蜂蜜");
        honeyCategory.setParentId(0L);
        honeyCategory.setSort(6);
        honeyCategory.setIsDeleted(0);
        honeyCategory.setCreatedAt(LocalDateTime.now());
        honeyCategory.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(honeyCategory);
        System.out.println("插入分类: 蜂蜜 (ID: " + honeyCategory.getId() + ")");
    }

    /**
     * 插入示例商品数据，绑定到 testseller 商家并关联分类。
     */
    private void insertProducts() {
        // 先获取所有分类
        List<Category> categories = categoryMapper.selectList(null);
        
        // 创建分类名称到 ID 的映射
        Map<String, Long> categoryIdMap = new HashMap<>();
        for (Category category : categories) {
            categoryIdMap.put(category.getName(), category.getId());
            System.out.println("分类映射：" + category.getName() + " -> " + category.getId());
        }
        
        // 获取测试商家 ID（testseller）
        Long testSellerId = 11L;
        System.out.println("使用测试商家 ID: " + testSellerId);
        
        // 定义不同分类的商品
        // 格式：[名称，图片，价格，库存，描述，分类名称]
        String[][] allProducts = {
            // 水果类商品
            {"有机苹果", "/images/product1.jpg", "128.00", "100", "精选有机苹果，新鲜采摘，无农药无化肥，口感清脆香甜", "水果"},
            {"香蕉礼盒", "/images/product2.jpg", "68.00", "200", "新鲜香蕉礼盒，成熟度适中，果肉细腻，香甜可口", "水果"},
            {"橙子", "/images/product3.jpg", "158.00", "50", "新鲜橙子，皮薄多汁，富含维生素C，酸甜可口", "水果"},
            {"草莓", "/images/product4.jpg", "48.00", "150", "新鲜草莓，色泽鲜艳，果肉饱满，香甜多汁", "水果"},
            {"葡萄", "/images/product5.jpg", "98.00", "80", "新鲜葡萄，颗粒饱满，口感甜美，富含多种营养元素", "水果"},
            {"西瓜", "/images/product6.jpg", "78.00", "70", "新鲜西瓜，皮薄肉厚，汁多味甜，消暑解渴", "水果"},
            {"梨", "/images/product7.jpg", "58.00", "90", "新鲜梨，果肉细腻，清甜多汁，润肺止咳", "水果"},
            
            // 蔬菜类商品
            {"有机蔬菜礼盒", "/images/product8.jpg", "128.00", "100", "精选有机蔬菜组合，包含多种时令蔬菜", "蔬菜"},
            {"有机西红柿", "/images/product9.jpg", "38.00", "150", "有机种植西红柿，色泽红艳，酸甜可口", "蔬菜"},
            {"新鲜生菜", "/images/product10.jpg", "18.00", "200", "新鲜采摘生菜，叶片鲜嫩，适合凉拌和沙拉", "蔬菜"},
            
            // 蛋类商品
            {"生态土鸡蛋", "/images/product11.jpg", "68.00", "200", "散养土鸡蛋，蛋黄饱满，营养丰富", "蛋类"},
            {"有机柴鸡蛋", "/images/product12.jpg", "88.00", "150", "有机认证柴鸡蛋，无抗生素，无激素", "蛋类"},
            
            // 粮油类商品
            {"农家小米", "/images/product13.jpg", "48.00", "150", "农家种植小米，颗粒饱满，口感香甜", "粮油"},
            {"有机大米", "/images/product14.jpg", "128.00", "100", "有机认证大米，口感软糯，营养丰富", "粮油"},
            {"特级面粉", "/images/product15.jpg", "78.00", "180", "特级小麦面粉，适合制作各种面食", "粮油"},
            
            // 坚果类商品
            {"核桃礼盒", "/images/product16.jpg", "98.00", "80", "精选优质核桃，皮薄肉厚", "坚果"},
            {"开心果", "/images/product17.jpg", "128.00", "100", "精选开心果，颗粒饱满，口感香脆", "坚果"},
            
            // 蜂蜜类商品
            {"野生蜂蜜", "/images/product18.jpg", "158.00", "50", "纯天然野生蜂蜜，采集自深山野花", "蜂蜜"},
            {"槐花蜜", "/images/product19.jpg", "128.00", "80", "优质槐花蜜，色泽透明，口感清甜", "蜂蜜"}
        };

        // 插入所有商品，为每个商品设置正确的categoryId
        for (String[] p : allProducts) {
            Product product = new Product();
            product.setName(p[0]);
            product.setCover(p[1]);
            product.setPrice(new BigDecimal(p[2]));
            product.setStock(Integer.parseInt(p[3]));
            product.setDescription(p[4]);
            product.setSellerId(testSellerId);
            
            // 根据分类名称获取实际的 categoryId
            String categoryName = p[5];
            Long categoryId = categoryIdMap.get(categoryName);
            if (categoryId != null) {
                product.setCategoryId(categoryId);
                System.out.println("为商品" + p[0] + "设置分类ID: " + categoryId + " (" + categoryName + ")");
            } else {
                System.out.println("警告: 未找到分类" + categoryName + "，商品" + p[0] + "将不设置分类ID");
            }
            
            productMapper.insert(product);
            System.out.println("插入商品：" + p[0] + " (seller_id: " + testSellerId + ")");
        }
    }

    /**
     * 确保必要的数据库表存在
     * 如果表不存在则自动创建
     */
    private void ensureTablesExist() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 创建售后服务表
            String createAfterSaleTableSQL =
                "CREATE TABLE IF NOT EXISTS after_sale_service (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "order_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "product_id BIGINT NOT NULL," +
                "service_type INT NOT NULL," +
                "reason TEXT NOT NULL," +
                "images TEXT," +
                "status INT DEFAULT 0," +
                "service_result TEXT," +
                "refund_amount DECIMAL(10,2) DEFAULT NULL," +
                "return_logistics VARCHAR(200) DEFAULT NULL," +
                "return_logistics_company VARCHAR(100) DEFAULT NULL," +
                "expect_complete_date DATETIME DEFAULT NULL," +
                "close_reason TEXT," +
                "supplementary_evidence TEXT," +
                "contact_phone VARCHAR(20) DEFAULT NULL," +
                "processed_by BIGINT DEFAULT NULL," +
                "processed_at DATETIME DEFAULT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createAfterSaleTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_as_order_id ON after_sale_service(order_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_as_user_id ON after_sale_service(user_id)");

            // 为已存在的表添加缺失字段（兼容旧数据）
            try {
                java.sql.ResultSet asCols = statement.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'after_sale_service'");
                java.util.Set<String> asColSet = new java.util.HashSet<>();
                while (asCols.next()) asColSet.add(asCols.getString("COLUMN_NAME").toLowerCase());
                if (!asColSet.contains("refund_amount")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN refund_amount DECIMAL(10,2) DEFAULT NULL");
                if (!asColSet.contains("return_logistics")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN return_logistics VARCHAR(200) DEFAULT NULL");
                if (!asColSet.contains("return_logistics_company")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN return_logistics_company VARCHAR(100) DEFAULT NULL");
                if (!asColSet.contains("expect_complete_date")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN expect_complete_date DATETIME DEFAULT NULL");
                if (!asColSet.contains("close_reason")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN close_reason TEXT");
                if (!asColSet.contains("supplementary_evidence")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN supplementary_evidence TEXT");
                if (!asColSet.contains("contact_phone")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN contact_phone VARCHAR(20) DEFAULT NULL");
                if (!asColSet.contains("processed_by")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN processed_by BIGINT DEFAULT NULL");
                if (!asColSet.contains("processed_at")) statement.execute("ALTER TABLE after_sale_service ADD COLUMN processed_at DATETIME DEFAULT NULL");
                System.out.println("✅ after_sale_service 表字段检查/补充完成");
            } catch (Exception e) {
                System.out.println("⚠️ 补充after_sale_service表字段时出错: " + e.getMessage());
            }

            System.out.println("✅ after_sale_service 表检查完成");

            // 创建服务记录表
            String createServiceRecordTableSQL =
                "CREATE TABLE IF NOT EXISTS service_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "after_sale_id BIGINT NOT NULL," +
                "operator_id BIGINT NOT NULL," +
                "operation_type INT NOT NULL," +
                "operation_content TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createServiceRecordTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sr_after_sale_id ON service_record(after_sale_id)");
            System.out.println("✅ service_record 表检查完成");

            // 创建管理员介入表
            String createInterventionTableSQL =
                "CREATE TABLE IF NOT EXISTS admin_intervention (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "seller_id BIGINT," +
                "order_id BIGINT," +
                "product_id BIGINT," +
                "session_id BIGINT," +
                "aftersale_id BIGINT," +
                "issue_type VARCHAR(50) NOT NULL," +
                "title VARCHAR(200) NOT NULL," +
                "description TEXT," +
                "status INT DEFAULT 0," +
                "admin_id BIGINT," +
                "result TEXT," +
                "admin_remark TEXT," +
                "evidence_images TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "processed_at TIMESTAMP NULL" +
                ")";
            statement.execute(createInterventionTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ai_user_id ON admin_intervention(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ai_seller_id ON admin_intervention(seller_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ai_status ON admin_intervention(status)");
            System.out.println("✅ admin_intervention 表检查完成");

            // 创建聊天会话表
            String createChatSessionTableSQL =
                "CREATE TABLE IF NOT EXISTS chat_session (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "seller_id BIGINT NOT NULL," +
                "product_id BIGINT," +
                "order_id BIGINT," +
                "status INT DEFAULT 0," +
                "user_unread INT DEFAULT 0," +
                "seller_unread INT DEFAULT 0," +
                "last_message TEXT," +
                "last_message_at TIMESTAMP NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createChatSessionTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cs_user_id ON chat_session(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cs_seller_id ON chat_session(seller_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cs_status ON chat_session(status)");
            System.out.println("✅ chat_session 表检查完成");

            try {
                java.sql.ResultSet csCols = statement.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_session'");
                java.util.Set<String> csColSet = new java.util.HashSet<>();
                while (csCols.next()) csColSet.add(csCols.getString("COLUMN_NAME").toLowerCase());
                if (!csColSet.contains("session_type")) statement.execute("ALTER TABLE chat_session ADD COLUMN session_type INT DEFAULT 1");
                if (!csColSet.contains("source")) statement.execute("ALTER TABLE chat_session ADD COLUMN source VARCHAR(50)");
                if (!csColSet.contains("auto_reply_enabled")) statement.execute("ALTER TABLE chat_session ADD COLUMN auto_reply_enabled INT DEFAULT 1");
                if (!csColSet.contains("agent_id")) statement.execute("ALTER TABLE chat_session ADD COLUMN agent_id BIGINT");
                if (!csColSet.contains("closed_at")) statement.execute("ALTER TABLE chat_session ADD COLUMN closed_at TIMESTAMP NULL");
                if (!csColSet.contains("close_reason")) statement.execute("ALTER TABLE chat_session ADD COLUMN close_reason TEXT");
                System.out.println("✅ chat_session 表字段检查/补充完成");
            } catch (Exception e) {
                System.out.println("⚠️ 补充chat_session表字段时出错: " + e.getMessage());
            }

            // 创建聊天消息表
            String createChatMessageTableSQL =
                "CREATE TABLE IF NOT EXISTS chat_message (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "session_id BIGINT NOT NULL," +
                "sender_id BIGINT NOT NULL," +
                "sender_type INT NOT NULL," +
                "receiver_id BIGINT," +
                "content TEXT NOT NULL," +
                "image_url VARCHAR(500)," +
                "message_type INT DEFAULT 1," +
                "related_order_id BIGINT," +
                "related_product_id BIGINT," +
                "is_read INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createChatMessageTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cm_session_id ON chat_message(session_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cm_sender_id ON chat_message(sender_id)");
            System.out.println("✅ chat_message 表检查完成");

            String createChatNotificationTableSQL =
                "CREATE TABLE IF NOT EXISTS chat_notification (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "user_type INT DEFAULT 1," +
                "session_id BIGINT," +
                "message_id BIGINT," +
                "type VARCHAR(50) NOT NULL," +
                "title VARCHAR(200)," +
                "content TEXT," +
                "is_read INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createChatNotificationTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cn_user_id ON chat_notification(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cn_is_read ON chat_notification(is_read)");
            System.out.println("✅ chat_notification 表检查完成");

            try {
                java.sql.ResultSet cmCols = statement.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_message'");
                java.util.Set<String> cmColSet = new java.util.HashSet<>();
                while (cmCols.next()) cmColSet.add(cmCols.getString("COLUMN_NAME").toLowerCase());
                if (!cmColSet.contains("status")) statement.execute("ALTER TABLE chat_message ADD COLUMN status INT DEFAULT 1");
                if (!cmColSet.contains("delivered_at")) statement.execute("ALTER TABLE chat_message ADD COLUMN delivered_at TIMESTAMP NULL");
                if (!cmColSet.contains("read_at")) statement.execute("ALTER TABLE chat_message ADD COLUMN read_at TIMESTAMP NULL");
                if (!cmColSet.contains("is_auto_reply")) statement.execute("ALTER TABLE chat_message ADD COLUMN is_auto_reply INT DEFAULT 0");
                if (!cmColSet.contains("file_name")) statement.execute("ALTER TABLE chat_message ADD COLUMN file_name VARCHAR(200)");
                if (!cmColSet.contains("file_size")) statement.execute("ALTER TABLE chat_message ADD COLUMN file_size BIGINT");
                System.out.println("✅ chat_message 表字段检查/补充完成");
            } catch (Exception e) {
                System.out.println("⚠️ 补充chat_message表字段时出错: " + e.getMessage());
            }

            // 创建商品表
            String createProductTableSQL =
                "CREATE TABLE IF NOT EXISTS product (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(200) NOT NULL," +
                "cover VARCHAR(500)," +
                "price DECIMAL(10,2) NOT NULL," +
                "stock INT DEFAULT 0," +
                "description TEXT," +
                "category_id BIGINT," +
                "seller_id BIGINT" +
                ")";
            statement.execute(createProductTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_p_category_id ON product(category_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_p_seller_id ON product(seller_id)");
            System.out.println("✅ product 表检查完成");

            // 创建分类表
            String createCategoryTableSQL =
                "CREATE TABLE IF NOT EXISTS category (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(50) NOT NULL," +
                "parent_id BIGINT DEFAULT 0," +
                "sort INT DEFAULT 0," +
                "is_deleted INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createCategoryTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_c_parent_id ON category(parent_id)");
            System.out.println("✅ category 表检查完成");

            // 创建用户表
            String createUserTableSQL =
                "CREATE TABLE IF NOT EXISTS user (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "openid VARCHAR(100)," +
                "nickname VARCHAR(100)," +
                "avatar VARCHAR(500)," +
                "phone VARCHAR(20)," +
                "username VARCHAR(50) NOT NULL," +
                "password VARCHAR(200) NOT NULL," +
                "role_id BIGINT," +
                "user_type INT DEFAULT 0," +
                "company_name VARCHAR(200)," +
                "company_address VARCHAR(500)," +
                "is_verified INT DEFAULT 0," +
                "verified_at TIMESTAMP NULL," +
                "verification_info TEXT," +
                "status INT DEFAULT 1," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createUserTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_u_username ON user(username)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_u_user_type ON user(user_type)");
            System.out.println("✅ user 表检查完成");

            // 创建优惠券表
            String createCouponTableSQL =
                "CREATE TABLE IF NOT EXISTS coupon (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(200) NOT NULL," +
                "type INT NOT NULL," +
                "threshold DECIMAL(10,2) DEFAULT 0," +
                "discount_value DECIMAL(10,2) NOT NULL," +
                "total_count INT DEFAULT 0," +
                "used_count INT DEFAULT 0," +
                "per_user_limit INT DEFAULT 1," +
                "start_time TIMESTAMP NULL," +
                "end_time TIMESTAMP NULL," +
                "status INT DEFAULT 1," +
                "seller_id BIGINT," +
                "description TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createCouponTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_c_status ON coupon(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_c_seller_id ON coupon(seller_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_c_end_time ON coupon(end_time)");
            System.out.println("✅ coupon 表检查完成");

            // 创建用户优惠券表
            String createUserCouponTableSQL =
                "CREATE TABLE IF NOT EXISTS user_coupon (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "coupon_id BIGINT NOT NULL," +
                "status INT DEFAULT 0," +
                "used_at TIMESTAMP NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createUserCouponTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_uc_user_id ON user_coupon(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_uc_coupon_id ON user_coupon(coupon_id)");
            System.out.println("✅ user_coupon 表检查完成");

            // 创建打折活动表
            String createDiscountActivityTableSQL =
                "CREATE TABLE IF NOT EXISTS discount_activity (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(200) NOT NULL," +
                "type INT NOT NULL," +
                "discount_rate DECIMAL(5,2)," +
                "threshold DECIMAL(10,2)," +
                "reduce_amount DECIMAL(10,2)," +
                "start_time TIMESTAMP NULL," +
                "end_time TIMESTAMP NULL," +
                "status INT DEFAULT 1," +
                "seller_id BIGINT," +
                "description TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createDiscountActivityTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_da_status ON discount_activity(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_da_seller_id ON discount_activity(seller_id)");
            System.out.println("✅ discount_activity 表检查完成");

            // 创建打折活动商品表
            String createDiscountActivityProductTableSQL =
                "CREATE TABLE IF NOT EXISTS discount_activity_product (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "activity_id BIGINT NOT NULL," +
                "product_id BIGINT NOT NULL," +
                "discount_price DECIMAL(10,2) NOT NULL" +
                ")";
            statement.execute(createDiscountActivityProductTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_dap_activity_id ON discount_activity_product(activity_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_dap_product_id ON discount_activity_product(product_id)");
            System.out.println("✅ discount_activity_product 表检查完成");

            // 创建活动表
            String createActivityTableSQL =
                "CREATE TABLE IF NOT EXISTS activity (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "title VARCHAR(200) NOT NULL," +
                "description TEXT," +
                "start_time DATETIME," +
                "end_time DATETIME," +
                "activity_type INT DEFAULT 1," +
                "location VARCHAR(200)," +
                "organizer VARCHAR(100)," +
                "contact_person VARCHAR(50)," +
                "contact_phone VARCHAR(20)," +
                "max_participants INT DEFAULT 0," +
                "current_participants INT DEFAULT 0," +
                "status INT DEFAULT 0," +
                "cover_image VARCHAR(500)," +
                "created_by BIGINT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "images TEXT," +
                "is_recommended INT DEFAULT 0," +
                "recommend_order INT DEFAULT 0" +
                ")";
            statement.execute(createActivityTableSQL);
            System.out.println("✅ activity 表检查完成");

            // 创建活动参与者表
            String createActivityParticipantSQL =
                "CREATE TABLE IF NOT EXISTS activity_participant (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "activity_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "participant_name VARCHAR(100)," +
                "participant_phone VARCHAR(20)," +
                "status INT DEFAULT 0," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createActivityParticipantSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ap_activity_id ON activity_participant(activity_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ap_user_id ON activity_participant(user_id)");
            System.out.println("✅ activity_participant 表检查完成");

            String createSearchHistoryTableSQL =
                "CREATE TABLE IF NOT EXISTS search_history (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "keyword VARCHAR(100) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            statement.execute(createSearchHistoryTableSQL);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sh_user_id ON search_history(user_id)");
            System.out.println("✅ search_history 表检查完成");

            // 为 orders 表添加优惠券相关字段
            try {
                statement.execute("ALTER TABLE orders ADD COLUMN user_coupon_id BIGINT NULL");
                statement.execute("ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10,2) DEFAULT 0");
                System.out.println("✅ orders 表优惠券字段添加完成");
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate column")) {
                    System.out.println("✅ orders 表优惠券字段已存在");
                } else {
                    System.err.println("⚠️ orders 表添加字段失败: " + e.getMessage());
                }
            }

            // 初始化示例优惠券数据
            initCoupons(statement);

            // 初始化示例优惠活动数据
            initDiscountActivities(statement);

            // 初始化示例助农活动数据
            initActivities(statement);

            System.out.println("📊 所有必要的数据库表已就绪！");

        } catch (Exception e) {
            System.err.println("⚠️ 创建数据库表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 插入示例优惠券数据（仅在 coupon 表为空时执行）。
     */
    private void initCoupons(Statement statement) {
        try {
            Long count = 0L;
            try {
                java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM coupon");
                if (rs.next()) count = rs.getLong(1);
                rs.close();
            } catch (Exception ignored) {}

            if (count == 0) {
                statement.execute("INSERT INTO coupon (name, type, threshold, discount_value, total_count, used_count, per_user_limit, start_time, end_time, status, description) VALUES " +
                    "('满50减10', 1, 50.00, 10.00, 500, 0, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '全场通用满减券')," +
                    "('满100减25', 1, 100.00, 25.00, 300, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '大额满减优惠')," +
                    "('9折优惠券', 2, 0, 9.00, 200, 0, 2, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, '全场9折')," +
                    "('满200减50', 1, 200.00, 50.00, 100, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '超值满减券')," +
                    "('8.5折优惠', 2, 0, 8.50, 150, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, '限时折扣优惠券')," +
                    "('新用户满30减5', 1, 30.00, 5.00, 999, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1, '新用户专享满减券')");
                System.out.println("✅ 示例优惠券数据初始化完成");
            } else {
                System.out.println("✅ 优惠券数据已存在，跳过初始化");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 初始化优惠券数据失败: " + e.getMessage());
        }
    }

    /**
     * 插入示例优惠活动数据及其关联商品折扣价（仅在 discount_activity 为空时执行）。
     */
    private void initDiscountActivities(Statement statement) {
        try {
            Long count = 0L;
            try {
                java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM discount_activity");
                if (rs.next()) count = rs.getLong(1);
                rs.close();
            } catch (Exception ignored) {}

            if (count == 0) {
                statement.execute("INSERT INTO discount_activity (name, type, threshold, reduce_amount, start_time, end_time, status, seller_id, description) VALUES " +
                    "('新品限时秒杀', 3, 0, 20.00, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 11, '精选新品，限时秒杀优惠')," +
                    "('满减大促', 1, 100.00, 30.00, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, 11, '满100减30，超值优惠')," +
                    "('会员专享折扣', 2, 0, 8.50, NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, 11, '全场商品8.5折')");

                java.sql.ResultSet rs = statement.executeQuery("SELECT id FROM discount_activity ORDER BY id LIMIT 3");
                int idx = 0;
                while (rs.next() && idx < 3) {
                    long activityId = rs.getLong(1);
                    long productId = (idx * 5) + 1;
                    double originalPrice = 128.00 + (idx * 30);
                    double discountPrice = originalPrice * (0.85 - idx * 0.05);

                    statement.execute(String.format(
                        "INSERT INTO discount_activity_product (activity_id, product_id, discount_price) VALUES (%d, %d, %.2f)",
                        activityId, productId, discountPrice));
                    idx++;
                }
                rs.close();

                System.out.println("✅ 示例优惠活动数据初始化完成: 3 个活动");
            } else {
                System.out.println("✅ 优惠活动数据已存在，跳过初始化");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 初始化优惠活动数据失败: " + e.getMessage());
        }
    }

    /**
     * 插入示例助农活动数据（仅在 activity 表为空时执行）。
     */
    private void initActivities(Statement statement) {
        try {
            Long count = 0L;
            try {
                java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM activity");
                if (rs.next()) count = rs.getLong(1);
                rs.close();
            } catch (Exception ignored) {}

            if (count == 0) {
                statement.execute("INSERT INTO activity (title, description, start_time, end_time, activity_type, location, organizer, contact_person, contact_phone, max_participants, current_participants, status, cover_image, created_by, is_recommended, recommend_order, images, created_at, updated_at) VALUES " +
                    "('爱心助农·苹果采摘', '春季助农活动，体验采摘乐趣，帮助果农增收。参与即可获得新鲜采摘的有机苹果一份！', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '绿色农场', '乡农优选平台', '李经理', '13800138001', 100, 45, 1, '/images/product1.jpg', 1, 1, 1, '/images/product1.jpg,/images/product2.jpg', NOW(), NOW())," +
                    "'乡村振兴·蜂蜜品鉴', '深入蜂场，了解蜂蜜制作过程，品尝纯天然野生蜂蜜。每份报名费将捐赠给当地贫困农户。', NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 2, '深山养蜂基地', '乡农优选平台', '王站长', '13900139002', 50, 28, 1, '/images/honey.jpg', 1, 1, 2, '/images/honey.jpg,/images/walnut.jpg', NOW(), NOW())," +
                    "('有机蔬菜种植体验', '走进有机农场，亲手种植有机蔬菜，学习现代农业知识。适合亲子家庭参与！', NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, '有机蔬菜基地', '乡农优选平台', '张园长', '13700137003', 80, 62, 1, '/images/vegetable.jpg', 1, 1, 3, '/images/vegetable.jpg,/images/egg.jpg', NOW(), NOW())," +
                    "('开心果园开放日', '参观现代化果园，了解果树栽培技术，现场品尝多种时令水果，购买优惠农产品。', NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, '开心果园', '乡农优选平台', '赵经理', '13600136004', 120, 89, 1, '/images/product17.jpg', 1, 1, 4, '/images/product17.jpg,/images/product4.jpg', NOW(), NOW())");
                System.out.println("✅ 示例助农活动数据初始化完成: 4 个活动");
            } else {
                System.out.println("✅ 助农活动数据已存在，跳过初始化");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 初始化助农活动数据失败: " + e.getMessage());
        }
    }

}
