package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Cart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 购物车表 Mapper，对应 cart 表
 * <p>
 * 业务唯一性：{userId, productId, specId}
 * 数据库约束：UNIQUE KEY uniq_user_product_spec (user_id, product_id, spec_id)
 * </p>
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 原子性"查重+累加/新增"SQL
     * <p>
     * 行为：
     * <ul>
     *   <li>(user_id, product_id, spec_id) 不存在 → 插入新行</li>
     *   <li>(user_id, product_id, spec_id) 已存在 → quantity 累加，updated_at 刷新</li>
     * </ul>
     * 由数据库 UNIQUE 索引保证并发安全，无需事务。
     * </p>
     * <p>
     * 注：项目使用 MySQL 8.0.3（较老），不支持 MySQL 8.0.20+ 的 AS new_row 别名语法，
     * 因此采用 VALUES() 函数（自 MySQL 8.0.20 起被标记为 deprecated，但旧版仍可正常工作）。
     * 升级到 8.0.20+ 后建议改为：AS new ON DUPLICATE KEY UPDATE col = new.col
     * </p>
     *
     * @param cart 购物车实体（id / createdAt / updatedAt 字段会被忽略）
     * @return 影响行数：1=新增 2=累加（MySQL ON DUPLICATE 特性）
     */
    @Insert("INSERT INTO cart (user_id, product_id, spec_id, quantity, checked, created_at, updated_at) " +
            "VALUES (#{userId}, #{productId}, #{specId}, #{quantity}, #{checked}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "quantity = quantity + VALUES(quantity), " +
            "updated_at = NOW()")
    int insertOrAccumulate(@Param("userId") Long userId,
                           @Param("productId") Long productId,
                           @Param("specId") Long specId,
                           @Param("quantity") Integer quantity,
                           @Param("checked") Integer checked);
}
