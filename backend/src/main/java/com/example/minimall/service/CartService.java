package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.CartMapper;
import com.example.minimall.model.Cart;
import com.example.minimall.model.Product;
import com.example.minimall.model.ProductSpec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 购物车服务，负责购物车项的增删改查及与商品/规格的关联 */
@Service
public class CartService {
    /** 购物车 Mapper */
    private final CartMapper mapper;
    /** 商品服务 */
    private final ProductService productService;
    /** 商品规格服务 */
    private final ProductSpecService productSpecService;

    public CartService(CartMapper mapper, ProductService productService, ProductSpecService productSpecService) {
        this.mapper = mapper;
        this.productService = productService;
        this.productSpecService = productSpecService;
    }

    /**
     * 根据用户 ID 查询购物车列表（含商品和规格详情）
     * <p>
     * 流程：
     * <ol>
     *   <li>查 cart 表拉出该用户全部购物车项</li>
     *   <li>批量提取 productId / specId</li>
     *   <li>批量取商品 / 规格信息（避免 N+1 查询）</li>
     *   <li>合并后返回 List&lt;Map&gt;：每条记录含购物车基础字段 + 商品名/封面/价格/库存 + 规格名/价格/库存</li>
     *   <li>价格 / 库存以规格为准（若规格存在），否则用商品本身</li>
     * </ol>
     * </p>
     *
     * @param userId 用户 ID
     * @return 购物车视图列表（Map 形式，方便 Controller 直接序列化）
     */
    public List<Map<String, Object>> findByUserId(Long userId) {
        // 1. 查询所有购物车项
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Cart> cartItems = mapper.selectList(queryWrapper);
        
        // 2. 如果购物车为空，直接返回空列表
        if (cartItems == null || cartItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 提取所有商品ID和规格ID
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        List<Long> specIds = cartItems.stream().filter(item -> item.getSpecId() != null).map(Cart::getSpecId).distinct().collect(Collectors.toList());
        
        // 4. 批量获取商品信息和规格信息
        Map<Long, Product> productMap = new java.util.HashMap<>();
        for (Long productId : productIds) {
            Product product = productService.findById(productId);
            if (product != null) {
                productMap.put(productId, product);
            }
        }
        
        Map<Long, ProductSpec> specMap = new java.util.HashMap<>();
        for (Long specId : specIds) {
            ProductSpec spec = productSpecService.findById(specId);
            if (spec != null) {
                specMap.put(specId, spec);
            }
        }
        
        // 5. 合并商品信息和规格信息到购物车数据中
        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart cart : cartItems) {
            Map<String, Object> cartMap = new java.util.HashMap<>();
            
            // 6. 添加购物车基本信息
            cartMap.put("id", cart.getId());
            cartMap.put("userId", cart.getUserId());
            cartMap.put("productId", cart.getProductId());
            cartMap.put("specId", cart.getSpecId());
            cartMap.put("quantity", cart.getQuantity());
            cartMap.put("checked", cart.getChecked());
            cartMap.put("createdAt", cart.getCreatedAt());
            cartMap.put("updatedAt", cart.getUpdatedAt());
            
            // 7. 添加商品信息
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                cartMap.put("name", product.getName());
                cartMap.put("cover", product.getCover());
                cartMap.put("price", product.getPrice());
                cartMap.put("stock", product.getStock());
            }
            
            // 8. 添加规格信息
            ProductSpec spec = specMap.get(cart.getSpecId());
            if (spec != null) {
                cartMap.put("specName", spec.getSpecName());
                // 如果规格有单独的价格，使用规格价格，否则使用商品价格
                if (spec.getPrice() != null) {
                    cartMap.put("price", spec.getPrice());
                }
                // 如果规格有单独的库存，使用规格库存，否则使用商品库存
                if (spec.getStock() != null) {
                    cartMap.put("stock", spec.getStock());
                }
            }
            
            result.add(cartMap);
        }
        
        return result;
    }

    /**
     * 根据购物车项 ID 查询
     *
     * @param id 购物车项主键
     * @return 购物车实体，未找到返回 null
     */
    public Cart findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 保存购物车项（**原子性查重+累加/新增**）
     * <p>
     * 唯一性维度：{userId, productId, specId}。
     * 通过 cart 表的 UNIQUE 索引 + INSERT ... ON DUPLICATE KEY UPDATE 一次 SQL 完成，
     * 无需事务、无需先查后写，并发安全。
     * </p>
     * <p>
     * 调用方传入的 id / createdAt / updatedAt 字段会被忽略（由 SQL 内部赋值）。
     * </p>
     *
     * @param cart 待保存购物车项（userId / productId / specId / quantity / checked 必填）
     */
    public void save(Cart cart) {
        // 一条 SQL 完成"查重+累加/插入"，由数据库 UNIQUE 索引保证原子性
        // 比起"先查后写"省一次 IO，且彻底避免并发重复插入问题
        mapper.insertOrAccumulate(
                cart.getUserId(),
                cart.getProductId(),
                cart.getSpecId(),
                cart.getQuantity(),
                cart.getChecked() == null ? 0 : cart.getChecked()
        );
    }

    // 更新购物车项数量
    public void updateQuantity(Long id, Integer quantity) {
        Cart cart = mapper.selectById(id);
        if (cart != null) {
            cart.setQuantity(quantity);
            mapper.updateById(cart);
        }
    }

    // 更新购物车项选中状态
    public void updateChecked(Long id, Integer checked) {
        Cart cart = mapper.selectById(id);
        if (cart != null) {
            cart.setChecked(checked);
            mapper.updateById(cart);
        }
    }

    // 删除购物车项
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /**
     * 清空指定用户的购物车（**真实删除**）
     * <p>一般在提交订单完成后被调用</p>
     *
     * @param userId 用户 ID
     */
    public void clearByUserId(Long userId) {
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        mapper.delete(queryWrapper);
    }

    /**
     * 查询用户已选中的购物车项（含商品和规格信息）
     * <p>提交订单前会调用此方法，只结算 checked=1 的项</p>
     *
     * @param userId 用户 ID
     * @return 已选中的购物车视图列表
     */
    public List<Map<String, Object>> findCheckedByUserId(Long userId) {
        // 1. 查询所有选中的购物车项
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("checked", 1);
        List<Cart> cartItems = mapper.selectList(queryWrapper);
        
        // 2. 如果没有选中的购物车项，直接返回空列表
        if (cartItems == null || cartItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 提取所有商品ID和规格ID
        List<Long> productIds = cartItems.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        List<Long> specIds = cartItems.stream().filter(item -> item.getSpecId() != null).map(Cart::getSpecId).distinct().collect(Collectors.toList());
        
        // 4. 批量获取商品信息和规格信息
        Map<Long, Product> productMap = new java.util.HashMap<>();
        for (Long productId : productIds) {
            Product product = productService.findById(productId);
            if (product != null) {
                productMap.put(productId, product);
            }
        }
        
        Map<Long, ProductSpec> specMap = new java.util.HashMap<>();
        for (Long specId : specIds) {
            ProductSpec spec = productSpecService.findById(specId);
            if (spec != null) {
                specMap.put(specId, spec);
            }
        }
        
        // 5. 合并商品信息和规格信息到购物车数据中
        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart cart : cartItems) {
            Map<String, Object> cartMap = new java.util.HashMap<>();
            
            // 6. 添加购物车基本信息
            cartMap.put("id", cart.getId());
            cartMap.put("userId", cart.getUserId());
            cartMap.put("productId", cart.getProductId());
            cartMap.put("specId", cart.getSpecId());
            cartMap.put("quantity", cart.getQuantity());
            cartMap.put("checked", cart.getChecked());
            cartMap.put("createdAt", cart.getCreatedAt());
            cartMap.put("updatedAt", cart.getUpdatedAt());
            
            // 7. 添加商品信息
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                cartMap.put("name", product.getName());
                cartMap.put("cover", product.getCover());
                cartMap.put("price", product.getPrice());
                cartMap.put("stock", product.getStock());
            }
            
            // 8. 添加规格信息
            ProductSpec spec = specMap.get(cart.getSpecId());
            if (spec != null) {
                cartMap.put("specName", spec.getSpecName());
                // 如果规格有单独的价格，使用规格价格，否则使用商品价格
                if (spec.getPrice() != null) {
                    cartMap.put("price", spec.getPrice());
                }
                // 如果规格有单独的库存，使用规格库存，否则使用商品库存
                if (spec.getStock() != null) {
                    cartMap.put("stock", spec.getStock());
                }
            }
            
            result.add(cartMap);
        }
        
        return result;
    }
}
