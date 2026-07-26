package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.feign.PaymentFeignClient;
import com.example.minimall.feign.ProductFeignClient;
import com.example.minimall.feign.UserFeignClient;
import com.example.minimall.mapper.OrderItemMapper;
import com.example.minimall.mapper.OrdersMapper;
import com.example.minimall.mapper.LogisticsMapper;
import com.example.minimall.model.*;
import com.example.minimall.mq.OrderMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单业务服务，负责订单创建、支付、取消、确认、自动关单等。
 *
 * <p><b>架构说明（C2 跨域访问策略 — Feign 远程调用）</b><br>
 * 本服务只持有订单领域的 Mapper（orders / order_item / logistics）。
 * 跨域数据（user / shipping_address / product / product_spec / coupon / payment）
 * 统一通过 Feign 客户端调用对应服务的内部 API（/api/internal/**）获取。
 *
 * <p><b>事务与一致性</b>：跨服务写操作（库存扣减）无法参与本地事务，采用
 * try-catch + 补偿回滚模式 —— 订单写入失败时，已扣减的库存通过 Feign 回滚。
 */
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    /** 订单 Mapper */
    private final OrdersMapper ordersMapper;
    /** 订单明细 Mapper */
    private final OrderItemMapper orderItemMapper;
    /** 物流 Mapper */
    private final LogisticsMapper logisticsMapper;
    /** 商品服务 Feign 客户端（商品/规格/库存/优惠券） */
    private final ProductFeignClient productFeignClient;
    /** 用户服务 Feign 客户端（用户/收货地址） */
    private final UserFeignClient userFeignClient;
    /** 支付服务 Feign 客户端（支付记录） */
    private final PaymentFeignClient paymentFeignClient;
    /** 订单消息生产者（本地消息表 + 补偿重试，C5） */
    private final OrderMessageProducer orderMessageProducer;

    public OrderService(OrdersMapper ordersMapper,
                        OrderItemMapper orderItemMapper,
                        LogisticsMapper logisticsMapper,
                        ProductFeignClient productFeignClient,
                        UserFeignClient userFeignClient,
                        PaymentFeignClient paymentFeignClient,
                        OrderMessageProducer orderMessageProducer) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.logisticsMapper = logisticsMapper;
        this.productFeignClient = productFeignClient;
        this.userFeignClient = userFeignClient;
        this.paymentFeignClient = paymentFeignClient;
        this.orderMessageProducer = orderMessageProducer;
    }

    /**
     * 创建订单（不带优惠券的便捷重载）
     *
     * @param userId    用户 ID
     * @param addressId 收货地址 ID
     * @param items     商品列表，每个 Map 包含 productId、quantity、可选 specId
     * @return 创建成功的订单实体（含自增 ID、订单号、应付金额）
     */
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(Long userId, Long addressId, List<Map<String, Object>> items) {
        return createOrder(userId, addressId, items, null);
    }

    /**
     * 创建订单（带优惠券的完整流程）
     * <p>
     * 核心业务：校验商品/规格 → 扣减库存（防超卖，Feign 远程）→ 计算金额 → 套用优惠券 → 写订单/订单项。
     * 库存扣减为远程调用，订单写入失败时通过补偿回滚已扣减库存。
     * </p>
     *
     * @param userId       用户 ID
     * @param addressId    收货地址 ID（可空，会用默认地址兜底）
     * @param items        商品列表，每个 Map 包含 productId、quantity、可选 specId
     * @param userCouponId 用户优惠券 ID（可空）
     * @return 创建成功的订单实体
     * @throws IllegalArgumentException 参数非法 / 商品不存在 / 库存不足 / 规格不匹配
     */
    @Transactional(rollbackFor = Exception.class)
    @com.alibaba.csp.sentinel.annotation.SentinelResource(
            value = "createOrder",
            blockHandlerClass = com.example.minimall.config.SentinelFallbackHandler.class,
            blockHandler = "createOrderBlocked",
            fallbackClass = com.example.minimall.config.SentinelFallbackHandler.class,
            fallback = "createOrderFallback")
    public Orders createOrder(Long userId, Long addressId, List<Map<String, Object>> items, Long userCouponId) {
        logger.info("Start creating order, userId: {}, addressId: {}, userCouponId: {}, items: {}", userId, addressId, userCouponId, items);

        // 检查参数
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        // 创建订单
        Orders order = new Orders();
        order.setUserId(userId);

        // 跨域读 user-service 域（Feign）
        User user = null;
        if (userId != null) {
            logger.info("Get user info by userId: {}", userId);
            user = userFeignClient.getUser(userId);
            logger.info("Got user info: {}", user);
        }

        // 处理收货地址（Feign 调 user-service）
        ShippingAddress shippingAddress = null;
        if (user != null && addressId != null) {
            logger.info("Get address by addressId: {}", addressId);
            shippingAddress = userFeignClient.getAddress(addressId);
            logger.info("Got address: {}", shippingAddress);
        }

        // 设置收货地址到订单
        if (shippingAddress != null) {
            logger.info("Set address info to order");
            order.setConsignee(shippingAddress.getConsignee());
            order.setPhone(shippingAddress.getPhone());
            order.setProvince(shippingAddress.getProvince());
            order.setCity(shippingAddress.getCity());
            order.setDistrict(shippingAddress.getDistrict());
            order.setDetail(shippingAddress.getDetail());
        } else {
            // 使用默认地址兜底
            logger.warn("No address found, using default address");
            order.setConsignee("Default Consignee");
            order.setPhone("13800000000");
            order.setProvince("Default Province");
            order.setCity("Default City");
            order.setDistrict("Default District");
            order.setDetail("Default Detail");
        }

        // 设置订单基础信息
        order.setStatus(0); // 0: 待付款
        order.setPayStatus(0); // 0: 未支付
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 生成订单号
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTimeStr = now.format(formatter);
        String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
        String orderSn = "ORD" + dateTimeStr + randomStr;
        order.setOrderSn(orderSn);
        logger.info("Generated order number: {}", orderSn);

        // 计算总价 + 扣减库存（Feign 远程，带补偿回滚）
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        // 已扣减库存记录，用于订单写入失败时补偿回滚
        List<StockDeduction> deductions = new ArrayList<>();

        try {
            for (Map<String, Object> itemMap : items) {
                logger.info("Processing item: {}", itemMap);
                Long pid = Long.valueOf(String.valueOf(itemMap.get("productId")));
                Integer qty = Integer.valueOf(String.valueOf(itemMap.get("quantity")));
                Long specId = null;
                Object specIdObj = itemMap.get("specId");
                if (specIdObj != null && !specIdObj.toString().isEmpty() && !"null".equals(specIdObj.toString())) {
                    specId = Long.valueOf(String.valueOf(specIdObj));
                }
                logger.info("Processing item: productId={}, quantity={}, specId={}", pid, qty, specId);

                // 检查数量
                if (qty <= 0) {
                    throw new IllegalArgumentException("Item quantity must be greater than 0");
                }

                // 获取商品信息（Feign）
                logger.info("Query product info, productId: {}", pid);
                Product product = productFeignClient.getProduct(pid);
                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + pid);
                }
                logger.info("Got product info: {}", product);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getCover());
                orderItem.setQuantity(qty);

                BigDecimal itemPrice;

                // 处理规格
                if (specId != null && specId > 0) {
                    // 查询规格信息（Feign）
                    logger.info("Query spec info, specId: {}", specId);
                    ProductSpec spec = productFeignClient.getSpec(specId);
                    if (spec == null) {
                        throw new IllegalArgumentException("Spec not found: " + specId);
                    }
                    logger.info("Got spec info: {}", spec);
                    if (!spec.getProductId().equals(pid)) {
                        throw new IllegalArgumentException("Spec does not match product");
                    }
                    if (spec.getStock() < qty) {
                        throw new IllegalArgumentException("Spec stock insufficient: " + spec.getSpecName());
                    }

                    // 扣减规格库存（Feign 远程，SQL 带乐观校验 stock >= decrease）
                    logger.info("Deduct spec stock, specId: {}, currentStock: {}, quantity: {}", specId, spec.getStock(), qty);
                    boolean ok = productFeignClient.updateSpecStock(specId, qty);
                    if (!ok) {
                        throw new IllegalArgumentException("Spec stock insufficient: " + spec.getSpecName());
                    }
                    logger.info("Deduct spec stock success, specId: {}", specId);
                    deductions.add(new StockDeduction(pid, specId, qty));

                    itemPrice = spec.getPrice();
                    orderItem.setSpecId(specId);
                    orderItem.setSpecName(spec.getSpecName());
                } else {
                    // 检查商品库存
                    if (product.getStock() < qty) {
                        throw new IllegalArgumentException("Product stock insufficient: " + product.getName());
                    }

                    // 跨域扣减 product-service 域库存（Feign 远程，SQL 带乐观校验 stock >= decrease）
                    logger.info("Deduct product stock, productId: {}, currentStock: {}, quantity: {}", pid, product.getStock(), qty);
                    boolean ok = productFeignClient.updateStock(pid, qty);
                    if (!ok) {
                        throw new IllegalArgumentException("Product stock insufficient: " + product.getName());
                    }
                    logger.info("Deduct product stock success, productId: {}", pid);
                    deductions.add(new StockDeduction(pid, null, qty));

                    itemPrice = product.getPrice();
                    orderItem.setSpecId(null);
                    orderItem.setSpecName("");
                }

                orderItem.setPrice(itemPrice);
                orderItems.add(orderItem);

                // 累计总价
                total = total.add(itemPrice.multiply(new BigDecimal(qty)));
                logger.info("Current item total: {}, accumulated order total: {}", itemPrice, total);
            }

            order.setTotalPrice(total);
            logger.info("Order total: {}", total);

            // 套用优惠券（Feign 调 product-service）
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (userCouponId != null && userCouponId > 0) {
                try {
                    discountAmount = productFeignClient.calculateCouponDiscount(userCouponId, total);
                    if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                        productFeignClient.useCoupon(userCouponId);
                        order.setUserCouponId(userCouponId);
                        order.setDiscountAmount(discountAmount);
                        BigDecimal payAmount = total.subtract(discountAmount);
                        if (payAmount.compareTo(BigDecimal.ZERO) < 0) payAmount = BigDecimal.ZERO;
                        order.setPayAmount(payAmount);
                        logger.info("Coupon discount applied: {}, final amount: {}", discountAmount, payAmount);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to apply coupon: {}", e.getMessage());
                }
            }
            if (order.getPayAmount() == null) {
                order.setPayAmount(total);
            }

            // 保存订单
            logger.info("Saving order, order: {}", order);
            ordersMapper.insert(order);
            logger.info("Order saved successfully, orderId: {}", order.getId());

            // 保存订单项
            logger.info("Start saving order items, count: {}", orderItems.size());
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
                logger.info("Saving order item: {}", orderItem);
                orderItemMapper.insert(orderItem);
                logger.info("Order item saved successfully");
            }

            // 设置订单项到订单对象（供消息体使用）
            order.setItems(orderItems);
            // 异步发送订单创建消息（本地消息表保证最终一致性，失败由补偿任务重试，C5）
            try {
                orderMessageProducer.sendOrderCreated(order);
            } catch (Exception e) {
                logger.warn("发送订单消息失败,补偿任务会重试: orderId={}", order.getId(), e);
            }
            logger.info("Order created successfully, orderSn: {}", order.getOrderSn());
            return order;
        } catch (Exception e) {
            logger.error("Create order failed, compensating stock deductions", e);
            // 补偿回滚：订单写入失败，恢复已扣减的库存（Feign 远程，负数 decrease = 回滚）
            compensateStock(deductions);
            throw e;
        }
    }

    /** 根据 ID 查询订单 */
    public Orders findById(Long id) {
        return ordersMapper.selectById(id);
    }

    /** 根据 userId 查询订单 */
    public List<Orders> findByUserId(Long userId) {
        return ordersMapper.selectByUserId(userId);
    }

    /** 查询订单及明细 */
    public Orders findByIdWithItems(Long id) {
        Orders order = ordersMapper.selectOrderWithItems(id);

        if (order != null) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(id);
            populateItemImages(items);
            order.setItems(items);

            if (order.getTotalPrice() == null && items != null && !items.isEmpty()) {
                BigDecimal total = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    if (item.getPrice() != null && item.getQuantity() != null) {
                        total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                    }
                }
                order.setTotalPrice(total);
            }
        }

        return order;
    }

    /** 补充订单项商品图片（Feign 调 product-service） */
    private void populateItemImages(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;
        for (OrderItem item : items) {
            if (item.getProductImage() == null && item.getProductId() != null) {
                Product product = productFeignClient.getProduct(item.getProductId());
                if (product != null && product.getCover() != null) {
                    item.setProductImage(product.getCover());
                }
            }
        }
    }

    /** 取消订单（事务内回滚库存） */
    @Transactional(rollbackFor = Exception.class)
    public Orders cancelOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("Only pending payment orders can be cancelled");
        }

        // 恢复库存（Feign 远程，负数 decrease = 回滚）
        List<OrderItem> items = orderItemMapper.selectByOrderId(id);
        for (OrderItem item : items) {
            try {
                if (item.getSpecId() != null && item.getSpecId() > 0) {
                    productFeignClient.updateSpecStock(item.getSpecId(), -item.getQuantity());
                } else {
                    productFeignClient.updateStock(item.getProductId(), -item.getQuantity());
                }
            } catch (Exception e) {
                logger.warn("Restore stock failed, productId={}, specId={}, qty={}",
                        item.getProductId(), item.getSpecId(), item.getQuantity(), e);
            }
        }

        // 更新订单状态
        order.setStatus(4);
        order.setPayStatus(0);
        ordersMapper.updateById(order);

        logger.info("Order cancelled successfully, orderId: {}", id);
        return order;
    }

    /** 确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public Orders confirmOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() != 2) {
            throw new IllegalArgumentException("Only shipped orders can be confirmed");
        }

        // 更新订单状态
        order.setStatus(3);
        ordersMapper.updateById(order);

        // 更新物流状态
        if (order.getLogisticsId() != null) {
            Logistics logistics = logisticsMapper.selectById(order.getLogisticsId());
            if (logistics != null) {
                logistics.setStatus(3); // 已送达
                logisticsMapper.updateById(logistics);
            }
        }

        logger.info("Order confirmed successfully, orderId: {}", id);
        return order;
    }

    /** 删除订单（仅限已取消或已完成） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        // 仅允许删除已取消或已完成的订单
        if (order.getStatus() != 4 && order.getStatus() != 3) {
            throw new IllegalArgumentException("Only cancelled or completed orders can be deleted");
        }

        // 删除支付记录（Feign 调 payment-service）
        try {
            paymentFeignClient.deletePaymentsByOrderId(id);
        } catch (Exception e) {
            logger.warn("Delete payment records failed, orderId={}", id, e);
        }

        // 删除订单项
        QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
        orderItemQuery.eq("order_id", id);
        orderItemMapper.delete(orderItemQuery);

        // 删除物流
        if (order.getLogisticsId() != null) {
            logisticsMapper.deleteById(order.getLogisticsId());
        }

        // 删除订单
        ordersMapper.deleteById(id);

        logger.info("Order deleted successfully, orderId: {}", id);
    }

    /**
     * 商家分页查询自己的订单
     * <p>
     * 实现思路：先通过 Feign 查出该商家所有商品 → 用商品 ID 集合查到所有订单项 →
     * 用订单项里的订单 ID 去重后查订单。
     * </p>
     *
     * @param page     分页对象
     * @param sellerId 商家（用户）ID
     * @return 商家名下的订单分页结果
     */
    public IPage<Orders> pageBySellerId(Page<Orders> page, Long sellerId) {
        // 跨域查商品（Feign 调 product-service）
        List<Product> products = productFeignClient.listProductsBySeller(sellerId);

        if (products == null || products.isEmpty()) {
            return new Page<>();
        }

        List<Long> productIds = new ArrayList<>();
        for (Product p : products) {
            productIds.add(p.getId());
        }

        QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
        orderItemQuery.in("product_id", productIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);

        if (orderItems.isEmpty()) {
            return new Page<>();
        }

        List<Long> orderIds = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (!orderIds.contains(item.getOrderId())) {
                orderIds.add(item.getOrderId());
            }
        }

        QueryWrapper<Orders> orderQuery = new QueryWrapper<>();
        orderQuery.in("id", orderIds);
        return ordersMapper.selectPage(page, orderQuery);
    }

    /** 定时自动取消超时未支付订单 */
    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCancelTimeoutOrders() {
        logger.info("Start auto cancelling timeout orders");

        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);
        QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0)
                .lt("created_at", timeoutTime);

        List<Orders> timeoutOrders = ordersMapper.selectList(queryWrapper);

        for (Orders order : timeoutOrders) {
            try {
                cancelOrder(order.getId());
                logger.info("Auto cancelled timeout order, orderId: {}", order.getId());
            } catch (Exception e) {
                logger.error("Auto cancel order failed, orderId: {}", order.getId(), e);
            }
        }

        logger.info("Auto cancel timeout orders completed, cancelled count: {}", timeoutOrders.size());
    }

    // ======================== 内部辅助 ========================

    /** 已扣减库存记录（用于补偿回滚） */
    private static class StockDeduction {
        final Long productId;
        final Long specId;
        final Integer quantity;
        StockDeduction(Long productId, Long specId, Integer quantity) {
            this.productId = productId;
            this.specId = specId;
            this.quantity = quantity;
        }
    }

    /** 补偿回滚已扣减的库存（订单写入失败时调用） */
    private void compensateStock(List<StockDeduction> deductions) {
        for (StockDeduction d : deductions) {
            try {
                if (d.specId != null) {
                    productFeignClient.updateSpecStock(d.specId, -d.quantity);
                } else {
                    productFeignClient.updateStock(d.productId, -d.quantity);
                }
                logger.info("Compensated stock restore: productId={}, specId={}, qty={}",
                        d.productId, d.specId, d.quantity);
            } catch (Exception ex) {
                logger.error("Compensate stock restore FAILED (manual intervention needed): productId={}, specId={}, qty={}",
                        d.productId, d.specId, d.quantity, ex);
            }
        }
    }
}
