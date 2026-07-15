package com.example.minimall.controller;

import com.example.minimall.mapper.OrderItemMapper;
import com.example.minimall.mapper.OrdersMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.mapper.ProductSpecMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.model.OrderItem;
import com.example.minimall.model.Orders;
import com.example.minimall.model.Product;
import com.example.minimall.model.ProductSpec;
import com.example.minimall.service.ProductService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 商品相关接口：商品 CRUD、搜索、详情、规格、库存、商家维度查询与统计等
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {
    /** 商品业务服务 */
    private final ProductService productService;
    /** 商品 Mapper */
    private final ProductMapper productMapper;
    /** 商品规格 Mapper */
    private final ProductSpecMapper productSpecMapper;
    /** 订单项 Mapper（用于统计与关联） */
    private final OrderItemMapper orderItemMapper;
    /** 订单 Mapper（用于商家仪表盘统计） */
    private final OrdersMapper ordersMapper;
    /** 原生 JDBC 操作（用于搜索历史读写） */
    private final JdbcTemplate jdbcTemplate;
    
    public ProductController(ProductService productService, ProductMapper productMapper, ProductSpecMapper productSpecMapper, OrderItemMapper orderItemMapper, OrdersMapper ordersMapper, JdbcTemplate jdbcTemplate){
        this.productService = productService;
        this.productMapper = productMapper;
        this.productSpecMapper = productSpecMapper;
        this.orderItemMapper = orderItemMapper;
        this.ordersMapper = ordersMapper;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // 创建成功响应
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }
    
    // 构建错误响应
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
    
    // 商品相关接口
    /**
     * 分页获取商品列表，可按分类筛选；同时对封面图做兼容处理
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer page, 
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId){ 
        // 创建分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        // 执行分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<Product> resultPage;
        if (categoryId != null) {
            resultPage = productService.pageByCategoryId(mpPage, categoryId);
        } else {
            resultPage = productService.page(mpPage);
        }
        
        List<Product> products = resultPage.getRecords();
        // 为每个产品添加完整的图片 URL
        products.forEach(product -> {
            String cover = product.getCover();
            // 如果没有图片或图片为空，或者图片是相对路径且不包含 /images/，使用默认图片
            if (cover == null || cover.isEmpty()) {
                product.setCover("/images/product-default.jpg");
            }
            // 如果是相对路径但不包含 /images/，说明是旧数据，使用默认图片
            else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                // 这些是旧数据，图片文件不存在，使用默认图片
                product.setCover("/images/product-default.jpg");
            }
            // 如果是 /images/ 开头的相对路径，保持不变
            else if (cover.startsWith("/images/")) {
                product.setCover(cover);
            }
            // 如果是外部 URL，保持不变
            else if (cover.startsWith("http")) {
                product.setCover(cover);
            }
        });
        
        // 构建分页响应
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("records", products);
        pageInfo.put("total", resultPage.getTotal());
        pageInfo.put("size", resultPage.getSize());
        pageInfo.put("current", resultPage.getCurrent());
        pageInfo.put("pages", resultPage.getPages());
        
        return createSuccessResponse(pageInfo);
    }
    
    /**
     * 商品关键字搜索，支持分类、价格区间与排序
     */
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam String keyword, 
            @RequestParam(defaultValue = "0") Long userId, 
            @RequestParam(defaultValue = "1") Integer page, 
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder){ 
        // 创建分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        // 执行分页搜索
        com.baomidou.mybatisplus.core.metadata.IPage<Product> resultPage = productService.searchPageWithFilters(mpPage, keyword, categoryId, minPrice, maxPrice, sortBy, sortOrder);
        
        List<Product> products = resultPage.getRecords();
        // 为每个产品添加完整的图片 URL
        products.forEach(product -> {
            String cover = product.getCover();
            // 如果没有图片或图片为空，使用默认图片
            if (cover == null || cover.isEmpty()) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
            else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是 /images/ 开头的相对路径，保持不变
            else if (cover.startsWith("/images/")) {
                product.setCover(cover);
            }
            // 如果是外部 URL，保持不变
            else if (cover.startsWith("http")) {
                product.setCover(cover);
            }
        });
        
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("records", products);
        pageInfo.put("total", resultPage.getTotal());
        pageInfo.put("size", resultPage.getSize());
        pageInfo.put("current", resultPage.getCurrent());
        pageInfo.put("pages", resultPage.getPages());
        
        return createSuccessResponse(pageInfo);
    }
    
    /**
     * 根据商品 ID 获取商品详情，并对封面图做兼容处理
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id){
        try {
            Product product = productService.findById(id);
            
            // 检查商品是否存在
            if (product == null) {
                return createErrorResponse("商品不存在");
            }
            
            // 为产品添加完整的图片 URL
            String cover = product.getCover();
            // 如果没有图片或图片为空，使用默认图片
            if (cover == null || cover.isEmpty()) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
            else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是 /images/ 开头的相对路径，保持不变
            else if (cover.startsWith("/images/")) {
                product.setCover(cover);
            }
            // 如果是外部 URL，保持不变
            else if (cover.startsWith("http")) {
                product.setCover(cover);
            }
            return createSuccessResponse(product);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商品详情失败：" + e.getMessage());
        }
    }
    
    // 获取商品规格列表
    /**
     * 获取指定商品下的所有规格信息
     */
    @GetMapping("/{id}/specs")
    public Map<String, Object> getSpecs(@PathVariable Long id){
        List<ProductSpec> specs = productSpecMapper.selectByProductId(id); 
        return createSuccessResponse(specs);
    }
    
    // 根据分类ID获取商品列表
    /**
     * 根据分类 ID 分页获取该分类下的商品
     */
    @GetMapping("/category/{id}")
    public Map<String, Object> listByCategoryId(
            @PathVariable Long id, 
            @RequestParam(defaultValue = "1") Integer page, 
            @RequestParam(defaultValue = "20") Integer pageSize) { 
        // 创建分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        // 执行分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<Product> resultPage = productService.pageByCategoryId(mpPage, id);
        
        List<Product> products = resultPage.getRecords();
        // 为每个产品添加完整的图片 URL
        products.forEach(product -> {
            String cover = product.getCover();
            // 如果没有图片或图片为空，使用默认图片
            if (cover == null || cover.isEmpty()) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
            else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                product.setCover("/images/product-default.svg");
            }
            // 如果是 /images/ 开头的相对路径，保持不变
            else if (cover.startsWith("/images/")) {
                product.setCover(cover);
            }
            // 如果是外部 URL，保持不变
            else if (cover.startsWith("http")) {
                product.setCover(cover);
            }
        });
        
        // 构建分页响应
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("records", products);
        pageInfo.put("total", resultPage.getTotal());
        pageInfo.put("size", resultPage.getSize());
        pageInfo.put("current", resultPage.getCurrent());
        pageInfo.put("pages", resultPage.getPages());
        
        return createSuccessResponse(pageInfo);
    }
    
    /**
     * 创建一个商品
     */
    @PostMapping("/create")
    public Map<String, Object> create(@Valid @RequestBody Product product) {
        try {
            productService.save(product);
            return createSuccessResponse(product);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新指定 ID 的商品
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        try {
            product.setId(id);
            productService.save(product);
            return createSuccessResponse(product);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据 ID 删除商品
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除商品
     */
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {
                productService.delete(id);
            }
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("批量删除商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新商品库存数量
     */
    @PostMapping("/{id}/update-stock")
    public Map<String, Object> updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer stock = request.get("stock");
            productService.updateStock(id, stock);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新库存失败: " + e.getMessage());
        }
    }
    
    // 根据标签ID获取商品
    /**
     * 根据标签 ID 获取关联商品列表
     */
    @GetMapping("/tag/{tagId}")
    public Map<String, Object> getByTagId(@PathVariable Long tagId) {
        try {
            List<Product> products = productService.findByTagId(tagId);
            // 为每个产品添加完整的图片URL
            products.forEach(product -> {
                String cover = product.getCover();
                // 如果没有图片或图片为空，使用默认图片
                if (cover == null || cover.isEmpty()) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
                else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是 /images/ 开头的相对路径，保持不变
                else if (cover.startsWith("/images/")) {
                    product.setCover(cover);
                }
            });
            return createSuccessResponse(products);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商品失败: " + e.getMessage());
        }
    }
    
    // 根据商家ID获取商品列表，支持分页
    /**
     * 分页获取指定商家下的商品
     */
    @GetMapping("/seller/{sellerId}")
    public Map<String, Object> getProductsBySellerId(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            // 创建分页查询
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
            // 执行分页查询
            com.baomidou.mybatisplus.core.metadata.IPage<Product> resultPage = productService.pageBySellerId(mpPage, sellerId);
            
            List<Product> products = resultPage.getRecords();
            // 为每个产品添加完整的图片URL
            products.forEach(product -> {
                String cover = product.getCover();
                // 如果没有图片或图片为空，使用默认图片
                if (cover == null || cover.isEmpty()) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
                else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是 /images/ 开头的相对路径，保持不变
                else if (cover.startsWith("/images/")) {
                    product.setCover(cover);
                }
            });
            
            // 构建分页响应
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("records", products);
            pageInfo.put("total", resultPage.getTotal());
            pageInfo.put("size", resultPage.getSize());
            pageInfo.put("current", resultPage.getCurrent());
            pageInfo.put("pages", resultPage.getPages());
            
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商家商品列表失败: " + e.getMessage());
        }
    }
    
    // 根据商家ID获取所有商品（不分页）
    @GetMapping("/seller/{sellerId}/all")
    public Map<String, Object> getAllProductsBySellerId(@PathVariable Long sellerId) {
        try {
            List<Product> products = productService.findBySellerId(sellerId);
            // 为每个产品添加完整的图片URL
            products.forEach(product -> {
                String cover = product.getCover();
                // 如果没有图片或图片为空，使用默认图片
                if (cover == null || cover.isEmpty()) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是相对路径但不包含 /images/ 或 /uploads/，说明是旧数据，使用默认图片
                else if (cover.startsWith("/") && !cover.contains("/images/") && !cover.contains("/uploads/")) {
                    product.setCover("/images/product-default.svg");
                }
                // 如果是 /images/ 开头的相对路径，保持不变
                else if (cover.startsWith("/images/")) {
                    product.setCover(cover);
                }
            });
            return createSuccessResponse(products);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商家商品列表失败: " + e.getMessage());
        }
    }
    
    
    // 获取分类统计信息
    @GetMapping("/category/statistics")
    public Map<String, Object> getCategoryStatistics(@RequestParam String categoryIds) {
        try {
            String[] idArray = categoryIds.split(",");
            List<Long> ids = new java.util.ArrayList<>();
            for (String idStr : idArray) {
                try {
                    ids.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    return createErrorResponse("分类ID格式错误: " + idStr);
                }
            }
            
            if (ids.isEmpty()) {
                return createErrorResponse("分类ID不能为空");
            }
            
            List<Map<String, Object>> statistics = productService.getCategoryStatistics(ids);
            return createSuccessResponse(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取分类统计信息失败: " + e.getMessage());
        }
    }
    
    // 获取分类比较数据
    @GetMapping("/category/compare")
    public Map<String, Object> getCategoryComparison(@RequestParam String categoryIds) {
        try {
            String[] idArray = categoryIds.split(",");
            List<Long> ids = new java.util.ArrayList<>();
            for (String idStr : idArray) {
                try {
                    ids.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    return createErrorResponse("分类ID格式错误: " + idStr);
                }
            }
            
            if (ids.isEmpty()) {
                return createErrorResponse("分类ID不能为空");
            }
            
            if (ids.size() < 2) {
                return createErrorResponse("至少需要选择2个分类进行比较");
            }
            
            Map<String, Object> comparison = productService.getCategoryComparison(ids);
            return createSuccessResponse(comparison);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取分类比较数据失败: " + e.getMessage());
        }
    }
    
    // 获取商家仪表盘数据
    @GetMapping("/seller/{sellerId}/dashboard")
    public Map<String, Object> getSellerDashboard(@PathVariable Long sellerId) {
        try {
            QueryWrapper<Product> productQuery = new QueryWrapper<>();
            productQuery.eq("seller_id", sellerId);
            int productCount = productMapper.selectCount(productQuery).intValue();

            List<Product> products = productMapper.selectList(productQuery);
            int orderCount = 0;
            BigDecimal totalSales = BigDecimal.ZERO;
            int pendingOrders = 0;
            List<Orders> sellerOrders = new ArrayList<>();

            if (!products.isEmpty()) {
                List<Long> productIds = products.stream()
                        .map(Product::getId)
                        .collect(java.util.stream.Collectors.toList());

                QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
                orderItemQuery.in("product_id", productIds);
                List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);

                if (!orderItems.isEmpty()) {
                    List<Long> orderIds = orderItems.stream()
                            .map(OrderItem::getOrderId)
                            .distinct()
                            .collect(java.util.stream.Collectors.toList());

                    orderCount = orderIds.size();
                    List<Orders> fetchedOrders = ordersMapper.selectBatchIds(orderIds);
                    sellerOrders = fetchedOrders != null ? fetchedOrders : new ArrayList<>();
                    for (Orders order : sellerOrders) {
                        if (order.getTotalPrice() != null) {
                            totalSales = totalSales.add(order.getTotalPrice());
                        }
                        if (order.getStatus() == 1) {
                            pendingOrders++;
                        }
                    }
                }
            }

            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("totalProducts", productCount);
            dashboardData.put("totalOrders", orderCount);
            dashboardData.put("totalRevenue", totalSales);
            dashboardData.put("pendingOrders", pendingOrders);

            dashboardData.put("salesTrend", getSalesTrend(sellerOrders));
            dashboardData.put("orderStatusDist", getOrderStatusDist(sellerOrders));

            return createSuccessResponse(dashboardData);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商家仪表盘数据失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> getSalesTrend(List<Orders> orders) {
        List<Map<String, Object>> trend = new ArrayList<>();
        if (orders == null || orders.isEmpty()) {
            LocalDate today = LocalDate.now();
            String[] dayNames = {"周一","周二","周三","周四","周五","周六","周日"};
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                Map<String, Object> day = new HashMap<>();
                day.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
                day.put("dayName", dayNames[date.getDayOfWeek().getValue() - 1]);
                day.put("sales", BigDecimal.ZERO);
                day.put("orderCount", 0);
                trend.add(day);
            }
            return trend;
        }

        LocalDate today = LocalDate.now();
        String[] dayNames = {"周一","周二","周三","周四","周五","周六","周日"};

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            BigDecimal daySales = BigDecimal.ZERO;
            int dayOrderCount = 0;
            for (Orders o : orders) {
                if (o.getCreatedAt() != null) {
                    LocalDateTime orderTime = o.getCreatedAt();
                    if (!orderTime.isBefore(start) && orderTime.isBefore(end)) {
                        if (o.getTotalPrice() != null) daySales = daySales.add(o.getTotalPrice());
                        dayOrderCount++;
                    }
                }
            }

            Map<String, Object> day = new HashMap<>();
            day.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            day.put("dayName", dayNames[date.getDayOfWeek().getValue() - 1]);
            day.put("sales", daySales);
            day.put("orderCount", dayOrderCount);
            trend.add(day);
        }
        return trend;
    }

    private Map<String, Object> getOrderStatusDist(List<Orders> orders) {
        Map<Integer, Integer> countMap = new LinkedHashMap<>();
        countMap.put(0, 0);
        countMap.put(1, 0);
        countMap.put(2, 0);
        countMap.put(3, 0);
        countMap.put(4, 0);
        if (orders != null) {
            for (Orders o : orders) {
                if (o != null && o.getStatus() != null) {
                    countMap.merge(o.getStatus(), 1, Integer::sum);
                }
            }
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        String[] labels = {"待发货","待付款","已完成","已取消","已发货"};
        String[] colors = {"#f59e0b","#ef4444","#10b981","#9ca3af","#3b82f6"};
        int[] statusKeys = {0, 3, 2, 6, 4};

        for (int i = 0; i < labels.length; i++) {
            int cnt = countMap.getOrDefault(statusKeys[i], 0);
            if (cnt > 0 || true) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", labels[i]);
                item.put("value", cnt);
                Map<String, String> style = new HashMap<>();
                style.put("color", colors[i]);
                item.put("itemStyle", style);
                dataList.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", dataList);
        result.put("total", orders != null ? orders.size() : 0);
        return result;
    }

    @GetMapping("/search-history")
    public Map<String, Object> getSearchHistory(@RequestParam Long userId) {
        try {
            List<String> keywords = jdbcTemplate.queryForList(
                "SELECT keyword FROM search_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 20",
                String.class, userId);
            return createSuccessResponse(keywords);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取搜索历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/search-history/clear")
    public Map<String, Object> clearSearchHistory(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            if (userId == null) {
                return createErrorResponse("userId不能为空");
            }
            jdbcTemplate.update("DELETE FROM search_history WHERE user_id = ?", userId);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("清除搜索历史失败: " + e.getMessage());
        }
    }
}
