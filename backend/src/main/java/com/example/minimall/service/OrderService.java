package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.*;
import com.example.minimall.model.*;
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

/** 订单业务服务，负责订单创建、支付、取消、确认、自动关单等 */
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    /** 订单 Mapper */
    private final OrdersMapper ordersMapper;
    /** 商品 Mapper */
    private final ProductMapper productMapper;
    /** 订单明细 Mapper */
    private final OrderItemMapper orderItemMapper;
    /** 商品规格 Mapper */
    private final ProductSpecMapper productSpecMapper;
    /** 物流 Mapper */
    private final LogisticsMapper logisticsMapper;
    /** 收货地址 Mapper */
    private final ShippingAddressMapper shippingAddressMapper;
    /** 支付记录 Mapper */
    private final PaymentMapper paymentMapper;
    /** 用户 Mapper */
    private final UserMapper userMapper;
    /** 优惠券服务 */
    private final CouponService couponService;

    public OrderService(OrdersMapper ordersMapper, ProductMapper productMapper, OrderItemMapper orderItemMapper, ProductSpecMapper productSpecMapper, LogisticsMapper logisticsMapper, ShippingAddressMapper shippingAddressMapper, PaymentMapper paymentMapper, UserMapper userMapper, CouponService couponService) {
        this.ordersMapper = ordersMapper;
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
        this.productSpecMapper = productSpecMapper;
        this.logisticsMapper = logisticsMapper;
        this.shippingAddressMapper = shippingAddressMapper;
        this.paymentMapper = paymentMapper;
        this.userMapper = userMapper;
        this.couponService = couponService;
    }

    /**
     * 创建订单（不带优惠券的便捷重载）
     *
     * @param userId    用户 ID
     * @param addressId 收货地址 ID
     * @param items     商品列表，每个 Map 包含 productId、quantity、可选 specId
     * @return 创建成功的订单实体（含自增 ID、订单号、应付金额）
     */
    @Transactional(rollbackFor = Exception.class, propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public Orders createOrder(Long userId, Long addressId, List<Map<String, Object>> items) {
        return createOrder(userId, addressId, items, null);
    }

    /**
     * 创建订单（带优惠券的完整流程）
     * <p>
     * 核心业务：校验商品/规格 → 扣减库存（防超卖）→ 计算金额 → 套用优惠券 → 写订单/订单项。
     * 整个流程在 @Transactional 事务中，任一步失败全部回滚。
     * </p>
     *
     * @param userId       用户 ID
     * @param addressId    收货地址 ID（可空，会用默认地址兜底）
     * @param items        商品列表，每个 Map 包含 productId、quantity、可选 specId
     * @param userCouponId 用户优惠券 ID（可空）
     * @return 创建成功的订单实体
     * @throws IllegalArgumentException 参数非法 / 商品不存在 / 库存不足 / 规格不匹配
     */
    @Transactional(rollbackFor = Exception.class, propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public Orders createOrder(Long userId, Long addressId, List<Map<String, Object>> items, Long userCouponId) {
        logger.info("Start creating order, userId: {}, addressId: {}, userCouponId: {}, items: {}", userId, addressId, userCouponId, items);
        
        try {
            // Check parameters
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Order items cannot be empty");
            }

            // Create order
            Orders order = new Orders();
            order.setUserId(userId);
            
            // Get user info
            User user = null;
            if (userId != null) {
                logger.info("Get user info by userId: {}", userId);
                user = userMapper.selectById(userId);
                logger.info("Got user info: {}", user);
            }
            
            // Handle address info
            ShippingAddress shippingAddress = null;
            if (user != null && addressId != null) {
                logger.info("Get address by addressId: {}", addressId);
                shippingAddress = shippingAddressMapper.selectById(addressId);
                logger.info("Got address: {}", shippingAddress);
            }
            
            // Set address to order
            if (shippingAddress != null) {
                logger.info("Set address info to order");
                order.setConsignee(shippingAddress.getConsignee());
                order.setPhone(shippingAddress.getPhone());
                order.setProvince(shippingAddress.getProvince());
                order.setCity(shippingAddress.getCity());
                order.setDistrict(shippingAddress.getDistrict());
                order.setDetail(shippingAddress.getDetail());
            } else {
                // Use default address
                logger.warn("No address found, using default address");
                order.setConsignee("Default Consignee");
                order.setPhone("13800000000");
                order.setProvince("Default Province");
                order.setCity("Default City");
                order.setDistrict("Default District");
                order.setDetail("Default Detail");
            }
            
            // Set order basic info
            order.setStatus(0); // 0: pending payment
            order.setPayStatus(0); // 0: unpaid
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            // Generate order number
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String dateTimeStr = now.format(formatter);
            String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
            String orderSn = "ORD" + dateTimeStr + randomStr;
            order.setOrderSn(orderSn);
            logger.info("Generated order number: {}", orderSn);

            // Calculate total price
            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

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

                // Check quantity
                if (qty <= 0) {
                    throw new IllegalArgumentException("Item quantity must be greater than 0");
                }

                // Get product info
                logger.info("Query product info, productId: {}", pid);
                Product product = productMapper.selectById(pid);
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

                // Handle spec
                if (specId != null && specId > 0) {
                    // Query spec info
                    logger.info("Query spec info, specId: {}", specId);
                    ProductSpec spec = productSpecMapper.selectById(specId);
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

                    // Deduct spec stock
                    logger.info("Deduct spec stock, specId: {}, currentStock: {}, quantity: {}", specId, spec.getStock(), qty);
                    int updatedRows = productSpecMapper.updateStockById(specId, spec.getStock(), qty);
                    if (updatedRows == 0) {
                        throw new IllegalArgumentException("Spec stock insufficient: " + spec.getSpecName());
                    }
                    logger.info("Deduct spec stock success, updatedRows: {}", updatedRows);

                    itemPrice = spec.getPrice();
                    orderItem.setSpecId(specId);
                    orderItem.setSpecName(spec.getSpecName());
                } else {
                    // Check product stock
                    if (product.getStock() < qty) {
                        throw new IllegalArgumentException("Product stock insufficient: " + product.getName());
                    }

                    // Deduct product stock
                    logger.info("Deduct product stock, productId: {}, currentStock: {}, quantity: {}", pid, product.getStock(), qty);
                    int updatedRows = productMapper.updateStockById(pid, product.getStock(), qty);
                    if (updatedRows == 0) {
                        throw new IllegalArgumentException("Product stock insufficient: " + product.getName());
                    }
                    logger.info("Deduct product stock success, updatedRows: {}", updatedRows);

                    itemPrice = product.getPrice();
                    orderItem.setSpecId(null);
                    orderItem.setSpecName("");
                }

                orderItem.setPrice(itemPrice);
                orderItems.add(orderItem);

                // Calculate total
                total = total.add(itemPrice.multiply(new BigDecimal(qty)));
                logger.info("Current item total: {}, accumulated order total: {}", itemPrice, total);
            }

            order.setTotalPrice(total);
            logger.info("Order total: {}", total);

            // Apply coupon discount
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (userCouponId != null && userCouponId > 0) {
                try {
                    discountAmount = couponService.calculateDiscount(userCouponId, total);
                    if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                        couponService.useCoupon(userCouponId);
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

            // Save order
            logger.info("Saving order, order: {}", order);
            ordersMapper.insert(order);
            logger.info("Order saved successfully, orderId: {}", order.getId());

            // Save order items
            logger.info("Start saving order items, count: {}", orderItems.size());
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
                logger.info("Saving order item: {}", orderItem);
                orderItemMapper.insert(orderItem);
                logger.info("Order item saved successfully");
            }

            logger.info("Order created successfully, orderSn: {}", order.getOrderSn());
            return order;
        } catch (Exception e) {
            logger.error("Create order failed", e);
            throw e;
        }
    }

    /** 根据 ID 查询订单 */
    public Orders findById(Long id) {
        return ordersMapper.selectById(id);
    }

    // Find by userId
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

    private void populateItemImages(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;
        for (OrderItem item : items) {
            if (item.getProductImage() == null && item.getProductId() != null) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null && product.getCover() != null) {
                    item.setProductImage(product.getCover());
                }
            }
        }
    }

    // Pay order
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id, Map<String, Object> paymentInfo) {
        // 1. 取订单
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        // 2. ★ 状态校验：只有 status=0（待支付）才能付
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("Order status does not allow payment");
        }

        // 3. 改订单状态：0→1，pay_status：0→1
        order.setStatus(1);
        order.setPayStatus(1);
        ordersMapper.updateById(order);
        // 4. 保存支付记录
        if (paymentInfo != null) {
            Payment payment = new Payment();
            payment.setOrderId(id);
            payment.setAmount(order.getTotalPrice());
            payment.setStatus(1);
            payment.setPayTime(LocalDateTime.now());
            // 4.1 解析支付方式
            Object paymentMethodObj = paymentInfo.get("paymentMethod");
            if (paymentMethodObj != null) {
                String methodStr = paymentMethodObj.toString();
                if ("wechat".equals(methodStr)) {
                    payment.setPaymentMethod(1);
                } else if ("alipay".equals(methodStr)) {
                    payment.setPaymentMethod(2);
                } else if ("bank".equals(methodStr)) {
                    payment.setPaymentMethod(3);
                } else {
                    payment.setPaymentMethod(0);
                }
            }
            // 4.2 记录交易号（前端传来的模拟号）
            Object transactionIdObj = paymentInfo.get("transactionId");
            if (transactionIdObj != null) {
                payment.setPaymentNo(transactionIdObj.toString());
            }
            // 4.3 备注
            Object remarkObj = paymentInfo.get("remark");
            if (remarkObj != null) {
                payment.setRemark(remarkObj.toString());
            }
            
            paymentMapper.insert(payment);
        }

        logger.info("Order paid successfully, orderId: {}", id);
    }

    // Cancel order
    @Transactional(rollbackFor = Exception.class)
    public Orders cancelOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("Only pending payment orders can be cancelled");
        }

        // Restore stock
        List<OrderItem> items = orderItemMapper.selectByOrderId(id);
        for (OrderItem item : items) {
            if (item.getSpecId() != null && item.getSpecId() > 0) {
                // Restore spec stock
                ProductSpec spec = productSpecMapper.selectById(item.getSpecId());
                if (spec != null) {
                    productSpecMapper.updateStockById(item.getSpecId(), spec.getStock(), -item.getQuantity());
                }
            } else {
                // Restore product stock
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    productMapper.updateStockById(item.getProductId(), product.getStock(), -item.getQuantity());
                }
            }
        }

        // Update order status
        order.setStatus(4);
        order.setPayStatus(0);
        ordersMapper.updateById(order);

        logger.info("Order cancelled successfully, orderId: {}", id);
        return order;
    }

    // Confirm order
    @Transactional(rollbackFor = Exception.class)
    public Orders confirmOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() != 2) {
            throw new IllegalArgumentException("Only shipped orders can be confirmed");
        }

        // Update order status
        order.setStatus(3);
        ordersMapper.updateById(order);

        // Update logistics status
        if (order.getLogisticsId() != null) {
            Logistics logistics = logisticsMapper.selectById(order.getLogisticsId());
            if (logistics != null) {
                logistics.setStatus(3); // Delivered
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
        
        // Only allow deleting cancelled or completed orders
        if (order.getStatus() != 4 && order.getStatus() != 3) {
            throw new IllegalArgumentException("Only cancelled or completed orders can be deleted");
        }
        
        // Delete payment records
        QueryWrapper<Payment> paymentQuery = new QueryWrapper<>();
        paymentQuery.eq("order_id", id);
        paymentMapper.delete(paymentQuery);
        
        // Delete order items
        QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
        orderItemQuery.eq("order_id", id);
        orderItemMapper.delete(orderItemQuery);
        
        // Delete logistics
        if (order.getLogisticsId() != null) {
            logisticsMapper.deleteById(order.getLogisticsId());
        }
        
        // Delete order
        ordersMapper.deleteById(id);
        
        logger.info("Order deleted successfully, orderId: {}", id);
    }

    /**
     * 商家分页查询自己的订单
     * <p>
     * 实现思路：先查出该商家所有商品 → 用商品 ID 集合查到所有订单项 → 用订单项里的订单 ID 去重后查订单。
     * </p>
     *
     * @param page    分页对象
     * @param sellerId 商家（用户）ID
     * @return 商家名下的订单分页结果
     */
    public IPage<Orders> pageBySellerId(Page<Orders> page, Long sellerId) {
        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        productQuery.eq("seller_id", sellerId);
        List<Product> products = productMapper.selectList(productQuery);
        
        if (products.isEmpty()) {
            return new Page<>();
        }
        
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toList());
        
        QueryWrapper<OrderItem> orderItemQuery = new QueryWrapper<>();
        orderItemQuery.in("product_id", productIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemQuery);
        
        if (orderItems.isEmpty()) {
            return new Page<>();
        }
        
        List<Long> orderIds = orderItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
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
}
