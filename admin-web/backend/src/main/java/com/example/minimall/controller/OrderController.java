package com.example.minimall.controller;
import com.example.minimall.annotation.RateLimit;
import com.example.minimall.mapper.*;
import com.example.minimall.model.*;
import com.example.minimall.service.OrderService;
import com.example.minimall.enums.OrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单相关接口：创建、查询、支付、取消、确认、发货、删除订单等
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    /** 订单主表 Mapper */
    private final OrdersMapper ordersMapper;
    /** 订单业务服务 */
    private final OrderService orderService;
    /** 商品 Mapper（用于补充订单项中的商品信息） */
    private final ProductMapper productMapper;
    /** 订单项 Mapper */
    private final OrderItemMapper orderItemMapper;
    /** 用户 Mapper（用于关联买卖双方信息） */
    private final UserMapper userMapper;
    /** 支付记录 Mapper（用于返回订单支付流水） */
    private final PaymentMapper paymentMapper;

    public OrderController(OrdersMapper ordersMapper, OrderService orderService, ProductMapper productMapper, OrderItemMapper orderItemMapper, UserMapper userMapper, PaymentMapper paymentMapper){
        this.ordersMapper=ordersMapper;
        this.orderService=orderService;
        this.productMapper=productMapper;
        this.orderItemMapper=orderItemMapper;
        this.userMapper=userMapper;
        this.paymentMapper=paymentMapper;
    }

    // Create order
    /**
     * 创建订单：基于商品列表、收货地址与可选优惠券生成订单（带限流）
     */
    @RateLimit(limit = 5, timeout = 60)
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String,Object> body){
        try {
            String openid = (String) body.get("openid");
            List<Map<String,Object>> items = (List<Map<String,Object>>) body.get("items");
            Long userId = body.get("userId") != null ? Long.valueOf(String.valueOf(body.get("userId"))) : null;
            Long addressId = body.get("addressId") != null ? Long.valueOf(String.valueOf(body.get("addressId"))) : null;
            Long userCouponId = body.get("userCouponId") != null ? Long.valueOf(String.valueOf(body.get("userCouponId"))) : null;
            
            logger.info("Create order request: openid={}, userId={}, addressId={}, userCouponId={}, items={}", openid, userId, addressId, userCouponId, items);
            
            Orders order = orderService.createOrder(userId, addressId, items, userCouponId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", order.getId());
            result.put("orderSn", order.getOrderSn());
            result.put("totalPrice", order.getTotalPrice());
            result.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
            result.put("payAmount", order.getPayAmount());
            result.put("status", order.getStatus());
            result.put("createTime", order.getCreatedAt());
            return createSuccessResponse(result);
        } catch (Exception e) {
            logger.error("Create order failed", e);
            return createErrorResponse("Create order failed: " + e.getMessage());
        }
    }

    /**
     * 分页获取当前登录用户的订单列表，支持按状态和订单号关键字筛选
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword){
        try {
            String token = request.getHeader("Authorization");
            Long userId = null;

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    JwtUtil jwtUtil = new JwtUtil();
                    userId = jwtUtil.getUserIdFromToken(token);
                } catch (Exception e) {
                    logger.warn("Failed to extract userId from token: {}", e.getMessage());
                }
            }

            if (userId == null) {
                return createSuccessResponse(new ArrayList<>());
            }

            QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.orderByDesc("create_time");

            if (status != null) {
                queryWrapper.eq("status", status);
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.and(w -> w.like("order_no", keyword.trim()));
            }

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> pageQuery =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            com.baomidou.mybatisplus.core.metadata.IPage<Orders> pageResult =
                ordersMapper.selectPage(pageQuery, queryWrapper);

            List<Orders> orders = pageResult.getRecords();

            for (Orders order : orders) {
                QueryWrapper<OrderItem> query = new QueryWrapper<>();
                query.eq("order_id", order.getId());
                List<OrderItem> items = orderItemMapper.selectList(query);
                for (OrderItem item : items) {
                    if (item.getProductId() != null) {
                        Product p = productMapper.selectById(item.getProductId());
                        if (p != null) {
                            if (item.getProductImage() == null && p.getCover() != null) item.setProductImage(p.getCover());
                            if (p.getSellerId() != null) item.setSellerId(p.getSellerId());
                        }
                    }
                }
                order.setItems(items);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("list", orders);
            result.put("total", pageResult.getTotal());
            result.put("pages", pageResult.getPages());
            result.put("current", pageResult.getCurrent());

            return createSuccessResponse(result);
        } catch (Exception e) {
            logger.error("Get order list failed", e);
            return createErrorResponse("Get order list failed: " + e.getMessage());
        }
    }
    
    // 微信小程序获取订单 - 通过 openid
    /**
     * 微信小程序通过 openid 获取对应用户的全部订单列表
     */
    @GetMapping("/wx/list")
    public Map<String, Object> wxOrderList(@RequestParam String openid){
        try {
            logger.info("WeChat order list request, openid: {}", openid);
            
            // 根据 openid 查询用户
            User user = userMapper.selectByOpenid(openid);
            if (user == null) {
                logger.warn("User not found for openid: {}", openid);
                return createSuccessResponse(new ArrayList<>());
            }
            
            logger.info("Found user: {}", user.getId());
            
            // 根据 userId 查询订单
            List<Orders> orders = orderService.findByUserId(user.getId());
            
            logger.info("Prepare to return order list, count: {}", orders.size());
            
            // Add items to each order
            for (Orders order : orders) {
                QueryWrapper<OrderItem> query = new QueryWrapper<>();
                query.eq("order_id", order.getId());
                List<OrderItem> items = orderItemMapper.selectList(query);
                order.setItems(items);
            }
            
            return createSuccessResponse(orders);
        } catch (Exception e) {
            logger.error("WeChat get order list failed", e);
            return createErrorResponse("获取订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 管理员订单列表接口
     * 返回所有订单，同时包含买家信息和卖家信息
     */
    @GetMapping("/admin/list")
    public Map<String, Object> adminOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        try {
            logger.info("Admin order list request, page={}, size={}, status={}, orderNo={}", page, size, status, orderNo);

            QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");

            if (status != null) {
                queryWrapper.eq("status", status);
            }

            if (orderNo != null && !orderNo.trim().isEmpty()) {
                queryWrapper.like("order_no", orderNo.trim());
            }

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> pageQuery =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            com.baomidou.mybatisplus.core.metadata.IPage<Orders> pageResult =
                ordersMapper.selectPage(pageQuery, queryWrapper);

            List<Orders> orders = pageResult.getRecords();

            Set<Long> userIds = orders.stream()
                .map(Orders::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            Set<Long> orderIds = orders.stream()
                .map(Orders::getId)
                .collect(Collectors.toSet());

            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(userIds);
                userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            }

            Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
            Set<Long> sellerIds = new HashSet<>();

            if (!orderIds.isEmpty()) {
                QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
                itemQuery.in("order_id", orderIds);
                List<OrderItem> allItems = orderItemMapper.selectList(itemQuery);

                for (OrderItem item : allItems) {
                    orderItemsMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
                    sellerIds.add(item.getProductId());
                }
            }

            Map<Long, User> sellerMap = new HashMap<>();
            if (!sellerIds.isEmpty()) {
                QueryWrapper<Product> productQuery = new QueryWrapper<>();
                productQuery.in("id", sellerIds);
                productQuery.select("id", "seller_id");
                List<Product> products = productMapper.selectList(productQuery);

                Set<Long> sellerIdSet = products.stream()
                    .map(Product::getSellerId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                if (!sellerIdSet.isEmpty()) {
                    List<User> sellers = userMapper.selectBatchIds(sellerIdSet);
                    sellerMap = sellers.stream()
                        .collect(Collectors.toMap(User::getId, s -> s));
                }
            }

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Orders order : orders) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("id", order.getId());
                orderData.put("orderNo", order.getOrderSn());
                orderData.put("totalAmount", order.getTotalPrice());
                orderData.put("payAmount", order.getPayAmount());
                orderData.put("status", order.getStatus());
                orderData.put("payStatus", order.getPayStatus());
                orderData.put("consignee", order.getConsignee());
                orderData.put("phone", order.getPhone());
                orderData.put("province", order.getProvince());
                orderData.put("city", order.getCity());
                orderData.put("district", order.getDistrict());
                orderData.put("detail", order.getDetail());
                orderData.put("createdAt", order.getCreatedAt());
                orderData.put("remark", order.getRemark());

                User buyer = userMap.get(order.getUserId());
                if (buyer != null) {
                    Map<String, Object> buyerInfo = new HashMap<>();
                    buyerInfo.put("id", buyer.getId());
                    buyerInfo.put("username", buyer.getUsername());
                    buyerInfo.put("nickname", buyer.getNickname());
                    buyerInfo.put("phone", buyer.getPhone());
                    buyerInfo.put("avatar", buyer.getAvatar());
                    buyerInfo.put("userType", buyer.getUserType());
                    orderData.put("buyer", buyerInfo);
                }

                List<OrderItem> items = orderItemsMap.get(order.getId());
                if (items != null && !items.isEmpty()) {
                    List<Map<String, Object>> itemList = new ArrayList<>();
                    for (OrderItem item : items) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("id", item.getId());
                        itemData.put("productId", item.getProductId());
                        itemData.put("productName", item.getProductName());
                        itemData.put("specName", item.getSpecName());
                        itemData.put("price", item.getPrice());
                        itemData.put("quantity", item.getQuantity());

                        Product product = productMapper.selectById(item.getProductId());
                        if (product != null && product.getSellerId() != null) {
                            User seller = sellerMap.get(product.getSellerId());
                            if (seller != null) {
                                Map<String, Object> sellerInfo = new HashMap<>();
                                sellerInfo.put("id", seller.getId());
                                sellerInfo.put("username", seller.getUsername());
                                sellerInfo.put("nickname", seller.getNickname());
                                sellerInfo.put("phone", seller.getPhone());
                                sellerInfo.put("shopName", seller.getCompanyName());
                                sellerInfo.put("userType", seller.getUserType());
                                itemData.put("seller", sellerInfo);
                            }
                        }

                        itemList.add(itemData);
                    }
                    orderData.put("items", itemList);
                }

                resultList.add(orderData);
            }

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("records", resultList);
            pageInfo.put("total", pageResult.getTotal());
            pageInfo.put("current", pageResult.getCurrent());
            pageInfo.put("size", pageResult.getSize());

            logger.info("Admin order list return {} records", resultList.size());
            return createSuccessResponse(pageInfo);

        } catch (Exception e) {
            logger.error("Admin get order list failed", e);
            e.printStackTrace();
            return createErrorResponse("获取管理员订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 对指定订单进行支付（事务），可附带支付渠道等信息
     */
    @PostMapping("/pay/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> paymentInfo){
        try {
            logger.info("Pay order request: orderId={}, paymentInfo={}", id, paymentInfo);
            orderService.payOrder(id, paymentInfo);
            return createSuccessResponse("支付成功");
        } catch (Exception e) {
            logger.error("Pay order failed", e);
            return createErrorResponse("支付失败：" + e.getMessage());
        }
    }

    /**
     * 取消指定订单（事务）
     */
    @PostMapping("/cancel/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelOrder(@PathVariable Long id){
        try {
            logger.info("Cancel order request: orderId={}", id);
            Orders order = orderService.cancelOrder(id);
            return createSuccessResponse(order);
        } catch (Exception e) {
            logger.error("Cancel order failed", e);
            return createErrorResponse("取消订单失败：" + e.getMessage());
        }
    }

    /**
     * 用户确认收货（事务）
     */
    @PostMapping("/confirm/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmOrder(@PathVariable Long id){
        try {
            logger.info("Confirm order request: orderId={}", id);
            Orders order = orderService.confirmOrder(id);
            return createSuccessResponse(order);
        } catch (Exception e) {
            logger.error("Confirm order failed", e);
            return createErrorResponse("确认收货失败：" + e.getMessage());
        }
    }

    /**
     * 删除指定订单（事务，逻辑删除）
     */
    @PostMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteOrder(@PathVariable Long id){
        try {
            logger.info("Delete order request: orderId={}", id);
            orderService.deleteOrder(id);
            return createSuccessResponse("订单删除成功");
        } catch (Exception e) {
            logger.error("Delete order failed", e);
            return createErrorResponse("删除订单失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定订单的当前状态与状态文案（用于轮询）
     */
    @GetMapping("/status/{id}")
    public Map<String, Object> getOrderStatus(@PathVariable Long id){
        try {
            Orders order = orderService.findById(id);
            if (order == null) {
                return createErrorResponse("Order not found");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("id", order.getId());
            result.put("status", order.getStatus());
            result.put("statusText", OrderStatusEnum.getByCode(order.getStatus()).getDesc());
            result.put("payStatus", order.getPayStatus());
            return createSuccessResponse(result);
        } catch (Exception e) {
            logger.error("Get order status failed", e);
            return createErrorResponse("获取订单状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取指定商家对应商品产生的所有订单（商家后台订单列表）
     */
    @GetMapping("/seller/{sellerId}")
    public Map<String, Object> getOrdersBySellerId(@PathVariable Long sellerId){
        try {
            logger.info("Get orders by sellerId: {}", sellerId);
            
            // Query products by seller ID
            QueryWrapper<Product> productQuery = new QueryWrapper<>();
            productQuery.eq("seller_id", sellerId);
            List<Product> products = productMapper.selectList(productQuery);
            
            if (products.isEmpty()) {
                logger.info("No products found for sellerId: {}", sellerId);
                return createSuccessResponse(new ArrayList<>());
            }
            
            List<Long> productIds = products.stream()
                    .map(Product::getId)
                    .collect(java.util.stream.Collectors.toList());
            
            logger.info("Found {} products, productIds: {}", products.size(), productIds);
            
            // Query order items by product IDs
            QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
            orderItemQuery.in("product_id", productIds);
            List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);
            
            if (orderItems.isEmpty()) {
                logger.info("No order items found for these products");
                return createSuccessResponse(new ArrayList<>());
            }
            
            logger.info("Found {} order items", orderItems.size());
            
            // Get distinct order IDs
            List<Long> orderIds = orderItems.stream()
                    .map(OrderItem::getOrderId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
            logger.info("Found {} distinct orders", orderIds.size());
            
            // Query orders by order IDs
            QueryWrapper<Orders> orderQuery = new QueryWrapper<>();
            orderQuery.in("id", orderIds);
            orderQuery.orderByDesc("create_time");
            List<Orders> orders = ordersMapper.selectList(orderQuery);
            
            // Set items for each order
            for (Orders order : orders) {
                List<OrderItem> items = new ArrayList<>();
                for (OrderItem item : orderItems) {
                    if (item.getOrderId().equals(order.getId())) {
                        items.add(item);
                    }
                }
                order.setItems(items);
            }
            
            logger.info("Return {} orders", orders.size());
            return createSuccessResponse(orders);
        } catch (Exception e) {
            logger.error("Get orders by seller ID failed", e);
            return createErrorResponse("获取商家订单失败：" + e.getMessage());
        }
    }
    
    /**
     * 商家发货：仅对待发货状态订单有效，状态置为待收货（事务）
     */
    @PostMapping("/ship/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> shipOrder(@PathVariable Long id){
        try {
            logger.info("Ship order, id: {}", id);
            
            // Get order
            Orders order = ordersMapper.selectById(id);
            if (order == null) {
                return createErrorResponse("订单不存在");
            }
            
            // Check order status
            if (order.getStatus() != 1) {
                return createErrorResponse("订单状态不是待发货，无法发货");
            }
            
            // Update order status to 2 (pending receipt)
            order.setStatus(2);
            order.setUpdatedAt(java.time.LocalDateTime.now());
            ordersMapper.updateById(order);
            
            logger.info("Order shipped successfully, order id: {}", id);
            return createSuccessResponse("发货成功");
        } catch (Exception e) {
            logger.error("Ship order failed", e);
            return createErrorResponse("发货失败：" + e.getMessage());
        }
    }

    /**
     * 通过请求体通用地更新订单状态（兼容旧接口）
     */
    @PostMapping("/updateStatus")
    public Map<String, Object> updateOrderStatus(@RequestBody Map<String, Object> body) {
        try {
            Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
            Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;

            if (orderId == null || status == null) {
                return createErrorResponse("orderId和status不能为空");
            }

            Orders order = ordersMapper.selectById(orderId);
            if (order == null) return createErrorResponse("订单不存在");

            order.setStatus(status);
            order.setUpdatedAt(java.time.LocalDateTime.now());
            ordersMapper.updateById(order);

            logger.info("Order status updated: orderId={}, newStatus={}", orderId, status);
            return createSuccessResponse(order);
        } catch (Exception e) {
            logger.error("Update order status failed", e);
            return createErrorResponse("更新订单状态失败：" + e.getMessage());
        }
    }

    /**
     * REST 风格更新订单状态：路径中传订单 ID，请求体传新状态
     */
    @PutMapping("/{id}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
            if (status == null) return createErrorResponse("status不能为空");

            Orders order = ordersMapper.selectById(id);
            if (order == null) return createErrorResponse("订单不存在");

            order.setStatus(status);
            order.setUpdatedAt(java.time.LocalDateTime.now());
            ordersMapper.updateById(order);

            logger.info("Order status updated: orderId={}, newStatus={}", id, status);
            return createSuccessResponse(order);
        } catch (Exception e) {
            logger.error("Update order status failed", e);
            return createErrorResponse("更新订单状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单详情：含订单项、商品图片与最近一笔支付记录
     */
    @GetMapping("/detail/{id}")
    public Map<String, Object> getOrderDetail(@PathVariable Long id){
        try {
            logger.info("Get order detail, id: {}", id);
            
            // Get order
            Orders order = ordersMapper.selectById(id);
            if (order == null) {
                return createErrorResponse("订单不存在");
            }
            
            // Get order items
            QueryWrapper<OrderItem> query = new QueryWrapper<>();
            query.eq("order_id", id);
            List<OrderItem> items = orderItemMapper.selectList(query);
            for (OrderItem item : items) {
                if (item.getProductImage() == null && item.getProductId() != null) {
                    Product p = productMapper.selectById(item.getProductId());
                    if (p != null && p.getCover() != null) item.setProductImage(p.getCover());
                }
            }
            order.setItems(items);
            
            QueryWrapper<Payment> paymentQuery = new QueryWrapper<>();
            paymentQuery.eq("order_id", id).orderByDesc("created_at").last("LIMIT 1");
            Payment payment = paymentMapper.selectOne(paymentQuery);
            order.setPayment(payment);
            
            logger.info("Order detail retrieved successfully");
            return createSuccessResponse(order);
        } catch (Exception e) {
            logger.error("Get order detail failed", e);
            return createErrorResponse("获取订单详情失败：" + e.getMessage());
        }
    }

    // Build success response
    private Map<String, Object> createSuccessResponse(Object data){
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    // Build error response
    private Map<String, Object> createErrorResponse(String message){
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}