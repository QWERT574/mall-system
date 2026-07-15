package com.example.minimall.controller;

import com.example.minimall.common.Result;
import com.example.minimall.mapper.*;
import com.example.minimall.model.*;
import com.example.minimall.service.SystemConfigService;
import com.example.minimall.vo.SystemConfigVO;
import static com.example.minimall.vo.Converters.convert;
import static com.example.minimall.vo.Converters.convertList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理控制器
 * 提供仪表盘统计、系统配置等管理功能
 */
@RestController
@RequestMapping("/api/system")
public class SystemController extends BaseController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取仪表盘统计数据
     * 返回商品总数、订单总数、用户总数、总销售额等关键指标
     *
     * @return 统计数据Map
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 1. 商品总数
            List<Product> allProducts = productMapper.selectList(null);
            int totalProducts = allProducts != null ? allProducts.size() : 0;
            stats.put("totalProducts", totalProducts);

            // 2. 用户总数
            List<User> allUsers = userMapper.selectList(null);
            int totalUsers = allUsers != null ? allUsers.size() : 0;
            stats.put("totalUsers", totalUsers);

            // 3. 订单总数
            List<Orders> allOrders = ordersMapper.selectList(null);
            int totalOrders = allOrders != null ? allOrders.size() : 0;
            stats.put("totalOrders", totalOrders);

            // 4. 总销售额（只统计已完成的订单）
            BigDecimal totalSales = BigDecimal.ZERO;
            int completedOrders = 0;
            int pendingOrders = 0;
            int shippingOrders = 0;

            if (allOrders != null) {
                for (Orders order : allOrders) {
                    Integer orderStatus = order.getStatus();
                    if (orderStatus != null && order.getTotalPrice() != null) {
                        if (orderStatus == 1 || orderStatus == 2) {
                            totalSales = totalSales.add(order.getTotalPrice());
                            completedOrders++;
                        } else if (orderStatus == 0) {
                            pendingOrders++;
                        } else if (orderStatus == 4 || orderStatus == 5) {
                            shippingOrders++;
                        }
                    }
                }
            }

            stats.put("totalSales", totalSales);
            stats.put("completedOrders", completedOrders);
            stats.put("pendingOrders", pendingOrders);
            stats.put("shippingOrders", shippingOrders);

            // 5. 商家数量（userType=1）
            int sellerCount = 0;
            int buyerCount = 0;
            int adminCount = 0;
            if (allUsers != null) {
                for (User user : allUsers) {
                    if (user.getUserType() != null) {
                        if (user.getUserType() == 1) sellerCount++;
                        else if (user.getUserType() == 0) buyerCount++;
                        else if (user.getUserType() == 2) adminCount++;
                    }
                }
            }

            stats.put("sellerCount", sellerCount);
            stats.put("buyerCount", buyerCount);
            stats.put("adminCount", adminCount);

            // 6. 趋势数据（简化实现，实际项目中应查询历史数据计算）
            // 这里返回模拟趋势数据，后续可优化为真实统计
            stats.put("productTrend", totalProducts > 0 ? 12.5 : 0);  // 较上周增长
            stats.put("orderTrend", totalOrders > 0 ? 8.3 : 0);
            stats.put("userTrend", totalUsers > 0 ? 15.2 : 0);
            stats.put("salesTrend", totalSales.compareTo(BigDecimal.ZERO) > 0 ? 23.7 : 0);

            return success(stats);

        } catch (Exception e) {
            e.printStackTrace();
            return error("获取仪表盘统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取仪表盘图表统计数据
     * 返回销售趋势、订单状态分布、商品分类统计等图表数据
     */
    @GetMapping("/dashboard/chart")
    public Result<Map<String, Object>> getDashboardChartData() {
        try {
            Map<String, Object> chartData = new HashMap<>();

            // 1. 销售趋势数据（最近7天）
            List<Map<String, Object>> salesTrend = getSalesTrendData();
            chartData.put("salesTrend", salesTrend);

            // 2. 订单状态分布
            Map<String, Object> orderStatusDist = getOrderStatusDistribution();
            chartData.put("orderStatusDist", orderStatusDist);

            // 3. 商品分类销量统计
            List<Map<String, Object>> categoryStats = getCategoryStatistics();
            chartData.put("categoryStats", categoryStats);

            // 4. 热销商品TOP10
            List<Map<String, Object>> topProducts = getTopProductsData();
            chartData.put("topProducts", topProducts);

            return success(chartData);

        } catch (Exception e) {
            e.printStackTrace();
            return error("获取图表统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近7天的销售趋势数据
     */
    private List<Map<String, Object>> getSalesTrendData() {
        List<Map<String, Object>> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd"));
            String dayName = date.format(DateTimeFormatter.ofPattern("EEE"));

            // 查询该日期的订单数据
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            QueryWrapper<Orders> query = new QueryWrapper<>();
            query.ge("create_time", startOfDay);
            query.lt("create_time", endOfDay);
            List<Orders> dayOrders = ordersMapper.selectList(query);

            // 计算销售额和订单数
            BigDecimal dailySales = BigDecimal.ZERO;
            int orderCount = dayOrders.size();
            for (Orders order : dayOrders) {
                if (order.getTotalPrice() != null) {
                    dailySales = dailySales.add(order.getTotalPrice());
                }
            }

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("dayName", dayName);
            dayData.put("sales", dailySales);
            dayData.put("orderCount", orderCount);
            trendData.add(dayData);
        }

        return trendData;
    }

    /**
     * 获取订单状态分布数据
     */
    private Map<String, Object> getOrderStatusDistribution() {
        Map<String, Object> distribution = new LinkedHashMap<>();

        List<Orders> allOrders = ordersMapper.selectList(null);

        // 统计各状态订单数量
        Map<String, Integer> statusCount = new LinkedHashMap<>();
        statusCount.put("已完成", 0);
        statusCount.put("已发货", 0);
        statusCount.put("待发货", 0);
        statusCount.put("待支付", 0);
        statusCount.put("已取消", 0);

        for (Orders order : allOrders) {
            Integer orderStatus = order.getStatus();
            if (orderStatus != null) {
                switch (orderStatus) {
                    case 0:
                        statusCount.put("待发货", statusCount.get("待发货") + 1);
                        break;
                    case 1:
                    case 2:
                        statusCount.put("已完成", statusCount.get("已完成") + 1);
                        break;
                    case 4:
                    case 5:
                        statusCount.put("已发货", statusCount.get("已发货") + 1);
                        break;
                    case 3:
                    case 6:
                        statusCount.put("已取消", statusCount.get("已取消") + 1);
                        break;
                    default:
                        statusCount.put("待支付", statusCount.get("待支付") + 1);
                        break;
                }
            }
        }

        // 转换为列表格式
        List<Map<String, Object>> dataList = new ArrayList<>();
        String[] colors = {"#67C23A", "#409EFF", "#E6A23C", "#909399", "#F56C6C"};

        for (int i = 0; i < statusCount.size(); i++) {
            Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) statusCount.entrySet().toArray()[i];
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            Map<String, String> itemStyle = new HashMap<>();
            itemStyle.put("color", colors[i]);
            item.put("itemStyle", itemStyle);
            dataList.add(item);
        }

        distribution.put("data", dataList);
        distribution.put("total", allOrders.size());

        return distribution;
    }

    /**
     * 获取商品分类销量统计
     */
    private List<Map<String, Object>> getCategoryStatistics() {
        List<Map<String, Object>> categoryStats = new ArrayList<>();

        // 查询所有分类
        List<Category> allCategories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = allCategories.stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

        // 查询所有商品及其分类
        List<Product> allProducts = productMapper.selectList(null);
        Map<Long, Long> productCategoryMap = allProducts.stream()
            .filter(p -> p.getCategoryId() != null)
            .collect(Collectors.toMap(Product::getId, Product::getCategoryId));

        // 查询所有订单项，统计每个分类的销量
        List<OrderItem> allItems = orderItemMapper.selectList(null);
        Map<Long, Integer> categorySalesMap = new LinkedHashMap<>();

        for (OrderItem item : allItems) {
            Long productId = item.getProductId();
            Long categoryId = productCategoryMap.get(productId);
            if (categoryId != null) {
                categorySalesMap.merge(categoryId, item.getQuantity(), Integer::sum);
            }
        }

        // 转换为图表数据
        for (Map.Entry<Long, Integer> entry : categorySalesMap.entrySet()) {
            Long categoryId = entry.getKey();
            String categoryName = categoryNameMap.getOrDefault(categoryId, "其他");
            Integer sales = entry.getValue();

            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", categoryName);
            item.put("sales", sales);
            categoryStats.add(item);
        }

        return categoryStats;
    }

    private List<Map<String, Object>> getTopProductsData() {
        List<Map<String, Object>> topProducts = new ArrayList<>();
        List<OrderItem> allItems = orderItemMapper.selectList(null);

        Map<Long, Integer> productSalesMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> productAmountMap = new LinkedHashMap<>();

        for (OrderItem item : allItems) {
            Long pid = item.getProductId();
            productSalesMap.merge(pid, item.getQuantity() != null ? item.getQuantity() : 0, Integer::sum);
            BigDecimal amount = item.getPrice() != null ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0)) : BigDecimal.ZERO;
            productAmountMap.merge(pid, amount, BigDecimal::add);
        }

        List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(productSalesMap.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int limit = Math.min(10, sorted.size());
        for (int i = 0; i < limit; i++) {
            Long pid = sorted.get(i).getKey();
            Product product = productMapper.selectById(pid);
            Map<String, Object> item = new HashMap<>();
            item.put("productId", pid);
            item.put("productName", product != null ? product.getName() : "商品" + pid);
            item.put("cover", product != null ? product.getCover() : null);
            item.put("sales", productSalesMap.getOrDefault(pid, 0));
            item.put("totalAmount", productAmountMap.getOrDefault(pid, BigDecimal.ZERO));
            topProducts.add(item);
        }

        return topProducts;
    }

    @GetMapping("/config/list")
    public Result<List<SystemConfigVO>> listConfigs() {
        return success(convertList(systemConfigService.listAll(), SystemConfigVO::new));
    }

    @GetMapping("/config/{key}")
    public Result<SystemConfigVO> getConfig(@PathVariable String key) {
        SystemConfig config = systemConfigService.findByKey(key);
        if (config == null) return error("配置项不存在");
        return success(convert(config, SystemConfigVO::new));
    }

    @PutMapping("/config/{id}")
    public Result<String> updateConfig(@PathVariable Long id, @RequestBody SystemConfig config) {
        if (config.getConfigKey() == null || config.getConfigValue() == null) {
            return error("配置键和值不能为空");
        }
        systemConfigService.save(config);
        return success("更新成功");
    }

    @GetMapping("/ai/config")
    public Result<Map<String, Object>> getAiConfig() {
        Map<String, Object> config = new HashMap<>();
        String[] aiKeys = {"ai_api_key", "ai_base_url", "ai_model_name", "ai_max_tokens", "ai_temperature", "ai_enabled"};
        String[] defaultValues = {"", "https://api.deepseek.com", "deepseek-chat", "2000", "0.7", "true"};
        for (int i = 0; i < aiKeys.length; i++) {
            String value = systemConfigService.getConfigValue(aiKeys[i]);
            config.put(aiKeys[i].substring(3), value != null ? value : defaultValues[i]);
        }
        return success(config);
    }

    @PutMapping("/ai/config")
    public Result<String> updateAiConfig(@RequestBody Map<String, Object> body) {
        body.forEach((k, v) -> {
            String configKey = "ai_" + k;
            if (v != null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(configKey);
                config.setConfigValue(v.toString());
                systemConfigService.save(config);
            }
        });
        return success("AI配置更新成功");
    }
}
