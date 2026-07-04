package com.example.minimall.service;

import com.example.minimall.config.TestConfig;
import com.example.minimall.mapper.*;
import com.example.minimall.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseTableFunctionTest {

    @Autowired private UserService userService;
    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CartService cartService;
    @Autowired private CouponService couponService;
    @Autowired private ProductSpecService productSpecService;
    @Autowired private ActivityImageService activityImageService;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private PermissionService permissionService;
    @Autowired private ProductMapper productMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private OrdersMapper ordersMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private CouponMapper couponMapper;
    @Autowired private UserMapper userMapper;

    // ===== User 表测试 =====

    @Test
    @Order(1)
    void testUserFindAll() {
        List<User> users = userService.findAll();
        assertNotNull(users);
        assertTrue(users.size() > 0);
    }

    @Test
    @Order(2)
    void testUserFindById() {
        User user = userService.findById(1L);
        assertNotNull(user);
        assertEquals(1L, user.getId());
    }

    @Test
    @Order(3)
    void testUserSave() {
        User user = new User();
        user.setUsername("test_user_" + System.currentTimeMillis());
        user.setPhone("1990000" + System.currentTimeMillis() % 10000);
        user.setNickname("测试用户");
        user.setUserType(0);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);
        assertNotNull(user.getId());
    }

    @Test
    @Order(4)
    void testUserDelete() {
        User user = new User();
        user.setUsername("del_user_" + System.currentTimeMillis());
        user.setPhone("1980000" + System.currentTimeMillis() % 10000);
        user.setNickname("待删除用户");
        user.setUserType(0);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);
        Long userId = user.getId();
        userService.delete(userId);
        assertNull(userService.findById(userId));
    }

    @Test
    @Order(5)
    void testUserAssignRole() {
        assertDoesNotThrow(() -> userService.assignRole(1L, 3L));
    }

    @Test
    @Order(6)
    void testUserFindAllRoles() {
        List<Role> roles = userService.findAllRoles();
        assertNotNull(roles);
        assertTrue(roles.size() >= 3);
    }

    // ===== Product 表测试 =====

    @Test
    @Order(10)
    void testProductListAll() {
        List<Product> products = productService.listAll();
        assertNotNull(products);
        assertTrue(products.size() > 0);
    }

    @Test
    @Order(11)
    void testProductFindById() {
        List<Product> products = productService.listAll();
        if (!products.isEmpty()) {
            Product product = productService.findById(products.get(0).getId());
            assertNotNull(product);
        }
    }

    @Test
    @Order(12)
    void testProductSave() {
        Product product = new Product();
        product.setName("测试商品_" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("29.90"));
        product.setStock(100);
        product.setCover("test.jpg");
        productService.save(product);
        assertNotNull(product.getId());
    }

    @Test
    @Order(13)
    void testProductSearch() {
        List<Product> results = productService.search("大米");
        assertNotNull(results);
    }

    @Test
    @Order(14)
    void testProductUpdateStock() {
        List<Product> products = productService.listAll();
        if (!products.isEmpty()) {
            Product p = products.get(0);
            int originalStock = p.getStock() != null ? p.getStock() : 0;
            productService.updateStock(p.getId(), originalStock + 10);
            Product updated = productService.findById(p.getId());
            assertEquals(originalStock + 10, updated.getStock());
        }
    }

    @Test
    @Order(15)
    void testProductDelete() {
        Product product = new Product();
        product.setName("待删除商品_" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("9.90"));
        product.setStock(10);
        productService.save(product);
        Long pid = product.getId();
        productService.delete(pid);
        assertNull(productService.findById(pid));
    }

    // ===== Category 表测试 =====

    @Test
    @Order(20)
    void testCategoryListAll() {
        List<Category> categories = categoryService.listAll();
        assertNotNull(categories);
        assertTrue(categories.size() > 0);
    }

    @Test
    @Order(21)
    void testCategorySave() {
        Category category = new Category();
        category.setName("测试分类_" + System.currentTimeMillis());
        category.setParentId(0L);
        category.setLevel(1);
        category.setSort(99);
        category.setStatus(1);
        category.setIsDeleted(0);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryService.save(category);
        assertNotNull(category.getId());
    }

    @Test
    @Order(22)
    void testCategoryListTop() {
        List<Category> tops = categoryService.listTopCategories();
        assertNotNull(tops);
    }

    // ===== Cart 表测试 =====

    @Test
    @Order(30)
    void testCartAdd() {
        List<Product> products = productService.listAll();
        if (products.isEmpty()) return;
        Cart cart = new Cart();
        cart.setUserId(1L);
        cart.setProductId(products.get(0).getId());
        cart.setQuantity(2);
        cart.setChecked(1);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        assertDoesNotThrow(() -> cartService.save(cart));
    }

    @Test
    @Order(31)
    void testCartFindByUserId() {
        List<Map<String, Object>> items = cartService.findByUserId(1L);
        assertNotNull(items);
    }

    // ===== Coupon 表测试 =====

    @Test
    @Order(40)
    void testCouponGetAvailable() {
        assertDoesNotThrow(() -> couponService.getAvailableCoupons(1, 10));
    }

    @Test
    @Order(41)
    void testCouponCreate() {
        Coupon coupon = new Coupon();
        coupon.setName("测试优惠券");
        coupon.setType(1);
        coupon.setThreshold(new BigDecimal("50.00"));
        coupon.setDiscountValue(new BigDecimal("10.00"));
        coupon.setTotalCount(100);
        coupon.setUsedCount(0);
        coupon.setPerUserLimit(1);
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(30));
        coupon.setStatus(1);
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        assertDoesNotThrow(() -> couponService.createCoupon(coupon));
    }

    // ===== ProductSpec 表测试 =====

    @Test
    @Order(50)
    void testProductSpecSave() {
        List<Product> products = productService.listAll();
        if (products.isEmpty()) return;
        ProductSpec spec = new ProductSpec();
        spec.setProductId(products.get(0).getId());
        spec.setSpecName("5kg装");
        spec.setPrice(new BigDecimal("49.90"));
        spec.setStock(50);
        assertDoesNotThrow(() -> productSpecService.save(spec));
    }

    // ===== ActivityImage 表测试（新增Service） =====

    @Test
    @Order(60)
    void testActivityImageSave() {
        ActivityImage image = new ActivityImage();
        image.setActivityId(16L);
        image.setImageUrl("/uploads/activity/test.jpg");
        image.setSort(1);
        image.setCreatedAt(LocalDateTime.now());
        assertDoesNotThrow(() -> activityImageService.save(image));
    }

    @Test
    @Order(61)
    void testActivityImageFindByActivityId() {
        List<ActivityImage> images = activityImageService.findByActivityId(16L);
        assertNotNull(images);
    }

    @Test
    @Order(62)
    void testActivityImageSaveNullActivityId() {
        ActivityImage image = new ActivityImage();
        image.setImageUrl("/test.jpg");
        assertThrows(IllegalArgumentException.class, () -> activityImageService.save(image));
    }

    // ===== SystemConfig 表测试（新增Service） =====

    @Test
    @Order(70)
    void testSystemConfigListAll() {
        List<SystemConfig> configs = systemConfigService.listAll();
        assertNotNull(configs);
    }

    @Test
    @Order(71)
    void testSystemConfigSave() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test_key_" + System.currentTimeMillis());
        config.setConfigValue("test_value");
        config.setDescription("测试配置");
        assertDoesNotThrow(() -> systemConfigService.save(config));
    }

    @Test
    @Order(72)
    void testSystemConfigSaveNullKey() {
        SystemConfig config = new SystemConfig();
        config.setConfigValue("value");
        assertThrows(IllegalArgumentException.class, () -> systemConfigService.save(config));
    }

    @Test
    @Order(73)
    void testSystemConfigGetConfigValue() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test_get_key_" + System.currentTimeMillis());
        config.setConfigValue("hello_world");
        systemConfigService.save(config);
        String value = systemConfigService.getConfigValue(config.getConfigKey());
        assertEquals("hello_world", value);
    }

    // ===== Permission 表测试（新增Service） =====

    @Test
    @Order(80)
    void testPermissionListAll() {
        List<Permission> permissions = permissionService.listAll();
        assertNotNull(permissions);
    }

    @Test
    @Order(81)
    void testPermissionSave() {
        Permission perm = new Permission();
        perm.setName("测试权限");
        perm.setCode("test:perm_" + System.currentTimeMillis());
        perm.setDescription("测试用权限");
        perm.setParentId(0L);
        assertDoesNotThrow(() -> permissionService.save(perm));
    }

    @Test
    @Order(82)
    void testPermissionSaveNullName() {
        Permission perm = new Permission();
        perm.setCode("test:code");
        assertThrows(IllegalArgumentException.class, () -> permissionService.save(perm));
    }

    @Test
    @Order(83)
    void testPermissionAssignToRole() {
        assertDoesNotThrow(() -> permissionService.assignPermissionsToRole(1L, Arrays.asList(1L, 2L)));
    }

    // ===== 数据一致性验证 =====

    @Test
    @Order(90)
    void testOrderUserConsistency() {
        List<Orders> orders = ordersMapper.selectList(null);
        for (Orders order : orders) {
            if (order.getUserId() != null) {
                User user = userService.findById(order.getUserId());
                assertNotNull(user, "订单user_id=" + order.getUserId() + "在user表中不存在");
            }
        }
    }

    @Test
    @Order(91)
    void testProductCategoryConsistency() {
        List<Product> products = productMapper.selectList(null);
        Set<Long> categoryIds = new HashSet<>();
        for (Category c : categoryService.listAll()) {
            categoryIds.add(c.getId());
        }
        for (Product p : products) {
            if (p.getCategoryId() != null) {
                assertTrue(categoryIds.contains(p.getCategoryId()),
                    "商品id=" + p.getId() + "的category_id不存在");
            }
        }
    }

    @Test
    @Order(92)
    void testOrderAmountNonNegative() {
        List<Orders> orders = ordersMapper.selectList(null);
        for (Orders order : orders) {
            if (order.getTotalPrice() != null) {
                assertTrue(order.getTotalPrice().compareTo(BigDecimal.ZERO) >= 0,
                    "订单id=" + order.getId() + "金额为负数");
            }
        }
    }

    @Test
    @Order(93)
    void testProductPriceNonNegative() {
        List<Product> products = productMapper.selectList(null);
        for (Product p : products) {
            if (p.getPrice() != null) {
                assertTrue(p.getPrice().compareTo(BigDecimal.ZERO) >= 0,
                    "商品id=" + p.getId() + "价格为负数");
            }
        }
    }

    @Test
    @Order(94)
    void testProductStockNonNegative() {
        List<Product> products = productMapper.selectList(null);
        for (Product p : products) {
            if (p.getStock() != null) {
                assertTrue(p.getStock() >= 0, "商品id=" + p.getId() + "库存为负数");
            }
        }
    }

    @Test
    @Order(95)
    void testCouponDiscountPositive() {
        List<Coupon> coupons = couponMapper.selectList(null);
        for (Coupon c : coupons) {
            if (c.getDiscountValue() != null) {
                assertTrue(c.getDiscountValue().compareTo(BigDecimal.ZERO) > 0,
                    "优惠券id=" + c.getId() + "折扣值应为正数");
            }
        }
    }

    @Test
    @Order(96)
    void testAllTablesAccessible() {
        assertDoesNotThrow(() -> userMapper.selectList(null));
        assertDoesNotThrow(() -> productMapper.selectList(null));
        assertDoesNotThrow(() -> ordersMapper.selectList(null));
        assertDoesNotThrow(() -> categoryMapper.selectList(null));
        assertDoesNotThrow(() -> cartMapper.selectList(null));
        assertDoesNotThrow(() -> couponMapper.selectList(null));
    }
}
