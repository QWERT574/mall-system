package com.example.minimall.initializer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * chat-service 启动时数据初始化器。
 *
 * <p><b>重构说明</b>：原 chat-service 的 DataInitializer 完整复制了原单体后端的初始化逻辑
 * （管理员/商家/买家账号、商品分类/商品、优惠券、活动等），属于跨域数据初始化。
 * 微服务拆分后，这些数据应由 user-service / product-service 各自负责初始化，
 * chat-service 只需保证聊天领域相关的表存在即可。</p>
 *
 * <p>当前职责仅一项：自动创建/补齐聊天相关的数据库表与字段，包括
 * chat_session / chat_message / chat_notification / admin_intervention /
 * service_record / after_sale_chat 等聊天领域表。</p>
 */
@Component
public class DataInitializer {

    private final DataSource dataSource;

    public DataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 装配启动钩子：Spring Boot 启动完成后执行聊天相关表结构初始化。
     *
     * @return CommandLineRunner 任务体
     */
    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            try {
                System.out.println("[chat-service] 检查并创建聊天相关数据库表...");
                ensureChatTablesExist();
                System.out.println("[chat-service] 聊天数据表初始化完成！");
            } catch (Exception e) {
                System.err.println("[chat-service] 数据初始化异常: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    /**
     * 确保聊天相关的数据库表存在，并对已存在的表补齐后续新增字段。
     */
    private void ensureChatTablesExist() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 1. 管理员介入表
            statement.execute(
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
                ")");
            createIndexIfNotExists(statement, "idx_ai_user_id", "admin_intervention", "user_id");
            createIndexIfNotExists(statement, "idx_ai_seller_id", "admin_intervention", "seller_id");
            createIndexIfNotExists(statement, "idx_ai_status", "admin_intervention", "status");
            System.out.println("✅ admin_intervention 表检查完成");

            // 2. 聊天会话表
            statement.execute(
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
                ")");
            createIndexIfNotExists(statement, "idx_cs_user_id", "chat_session", "user_id");
            createIndexIfNotExists(statement, "idx_cs_seller_id", "chat_session", "seller_id");
            createIndexIfNotExists(statement, "idx_cs_status", "chat_session", "status");
            patchChatSessionColumns(statement);
            System.out.println("✅ chat_session 表检查完成");

            // 3. 聊天消息表
            statement.execute(
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
                ")");
            createIndexIfNotExists(statement, "idx_cm_session_id", "chat_message", "session_id");
            createIndexIfNotExists(statement, "idx_cm_sender_id", "chat_message", "sender_id");
            patchChatMessageColumns(statement);
            System.out.println("✅ chat_message 表检查完成");

            // 4. 聊天通知表
            statement.execute(
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
                ")");
            createIndexIfNotExists(statement, "idx_cn_user_id", "chat_notification", "user_id");
            createIndexIfNotExists(statement, "idx_cn_is_read", "chat_notification", "is_read");
            System.out.println("✅ chat_notification 表检查完成");

            // 5. 服务记录表（售后/客服流程操作记录）
            statement.execute(
                "CREATE TABLE IF NOT EXISTS service_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "after_sale_id BIGINT NOT NULL," +
                "operator_id BIGINT NOT NULL," +
                "operation_type INT NOT NULL," +
                "operation_content TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            createIndexIfNotExists(statement, "idx_sr_after_sale_id", "service_record", "after_sale_id");
            System.out.println("✅ service_record 表检查完成");

            // 6. 售后聊天表
            statement.execute(
                "CREATE TABLE IF NOT EXISTS after_sale_chat (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "after_sale_id BIGINT NOT NULL," +
                "sender_id BIGINT NOT NULL," +
                "sender_type INT NOT NULL," +
                "receiver_id BIGINT," +
                "content TEXT," +
                "image_url VARCHAR(500)," +
                "is_read INT DEFAULT 0," +
                "read_at TIMESTAMP NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            createIndexIfNotExists(statement, "idx_asc_after_sale_id", "after_sale_chat", "after_sale_id");
            System.out.println("✅ after_sale_chat 表检查完成");

        } catch (Exception e) {
            System.err.println("⚠️ 创建聊天数据库表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MySQL 兼容的"不存在则建索引"辅助方法。
     *
     * <p>MySQL 不支持 {@code CREATE INDEX IF NOT EXISTS} 语法（仅 MariaDB 支持），
     * 故直接执行 {@code CREATE INDEX}，当索引已存在时捕获 SQL 错误码 1061
     * (ER_DUP_KEYNAME) 并忽略。</p>
     *
     * @param statement JDBC Statement
     * @param indexName 索引名
     * @param table     表名
     * @param column    列名（或列表达式）
     */
    private void createIndexIfNotExists(Statement statement, String indexName, String table, String column) {
        try {
            statement.execute("CREATE INDEX " + indexName + " ON " + table + "(" + column + ")");
        } catch (java.sql.SQLException e) {
            // MySQL error 1061 = ER_DUP_KEYNAME (索引已存在)，属预期情况，忽略
            if (e.getErrorCode() != 1061) {
                System.out.println("⚠️ 创建索引 " + indexName + " 失败: " + e.getMessage());
            }
        }
    }

    /** 为已存在的 chat_session 表补充后续新增字段 */
    private void patchChatSessionColumns(Statement statement) {
        try {
            java.sql.ResultSet rs = statement.executeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_session'");
            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString(1).toLowerCase());
            rs.close();
            if (!cols.contains("session_type")) statement.execute("ALTER TABLE chat_session ADD COLUMN session_type INT DEFAULT 1");
            if (!cols.contains("source")) statement.execute("ALTER TABLE chat_session ADD COLUMN source VARCHAR(50)");
            if (!cols.contains("auto_reply_enabled")) statement.execute("ALTER TABLE chat_session ADD COLUMN auto_reply_enabled INT DEFAULT 1");
            if (!cols.contains("agent_id")) statement.execute("ALTER TABLE chat_session ADD COLUMN agent_id BIGINT");
            if (!cols.contains("closed_at")) statement.execute("ALTER TABLE chat_session ADD COLUMN closed_at TIMESTAMP NULL");
            if (!cols.contains("close_reason")) statement.execute("ALTER TABLE chat_session ADD COLUMN close_reason TEXT");
            System.out.println("✅ chat_session 表字段检查/补充完成");
        } catch (Exception e) {
            System.out.println("⚠️ 补充chat_session表字段时出错: " + e.getMessage());
        }
    }

    /** 为已存在的 chat_message 表补充后续新增字段 */
    private void patchChatMessageColumns(Statement statement) {
        try {
            java.sql.ResultSet rs = statement.executeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_message'");
            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString(1).toLowerCase());
            rs.close();
            if (!cols.contains("status")) statement.execute("ALTER TABLE chat_message ADD COLUMN status INT DEFAULT 1");
            if (!cols.contains("delivered_at")) statement.execute("ALTER TABLE chat_message ADD COLUMN delivered_at TIMESTAMP NULL");
            if (!cols.contains("read_at")) statement.execute("ALTER TABLE chat_message ADD COLUMN read_at TIMESTAMP NULL");
            if (!cols.contains("is_auto_reply")) statement.execute("ALTER TABLE chat_message ADD COLUMN is_auto_reply INT DEFAULT 0");
            if (!cols.contains("file_name")) statement.execute("ALTER TABLE chat_message ADD COLUMN file_name VARCHAR(200)");
            if (!cols.contains("file_size")) statement.execute("ALTER TABLE chat_message ADD COLUMN file_size BIGINT");
            System.out.println("✅ chat_message 表字段检查/补充完成");
        } catch (Exception e) {
            System.out.println("⚠️ 补充chat_message表字段时出错: " + e.getMessage());
        }
    }

}
