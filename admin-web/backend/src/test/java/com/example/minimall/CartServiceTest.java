package com.example.minimall;

import com.example.minimall.mapper.CartMapper;
import com.example.minimall.model.Cart;
import com.example.minimall.service.CartService;
import com.example.minimall.service.ProductService;
import com.example.minimall.service.ProductSpecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CartService.save() 单元测试（**原子性 save 测试**）
 * <p>
 * 覆盖场景：
 * <ol>
 *   <li>save() 改为调用 mapper.insertOrAccumulate（不再先查后写）</li>
 *   <li>传递的参数正确：userId, productId, specId, quantity, checked</li>
 *   <li>checked 为 null 时兜底为 0</li>
 *   <li>不会调用 selectOne（性能更好，无 N+1）</li>
 * </ol>
 * </p>
 */
@DisplayName("CartService.save() 原子性累加单元测试")
class CartServiceTest {

    private CartMapper cartMapper;
    private ProductService productService;
    private ProductSpecService productSpecService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartMapper = mock(CartMapper.class);
        productService = mock(ProductService.class);
        productSpecService = mock(ProductSpecService.class);
        cartService = new CartService(cartMapper, productService, productSpecService);
    }

    @Test
    @DisplayName("save() 调用 insertOrAccumulate（不再先查后写）")
    void shouldCallInsertOrAccumulate() {
        when(cartMapper.insertOrAccumulate(anyLong(), anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(1);

        Cart cart = new Cart();
        cart.setUserId(1L);
        cart.setProductId(100L);
        cart.setSpecId(200L);
        cart.setQuantity(3);
        cart.setChecked(1);
        cartService.save(cart);

        // 验证调用了新的 insertOrAccumulate
        verify(cartMapper, times(1)).insertOrAccumulate(1L, 100L, 200L, 3, 1);
        // 验证不再调用先查后写
        verify(cartMapper, never()).selectOne(any());
        verify(cartMapper, never()).selectList(any());
        verify(cartMapper, never()).updateById(any());
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    @DisplayName("checked 为 null 时兜底为 0")
    void shouldDefaultCheckedToZero() {
        when(cartMapper.insertOrAccumulate(anyLong(), anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(1);

        Cart cart = new Cart();
        cart.setUserId(1L);
        cart.setProductId(100L);
        cart.setSpecId(200L);
        cart.setQuantity(5);
        // 故意不设 checked
        cartService.save(cart);

        verify(cartMapper).insertOrAccumulate(1L, 100L, 200L, 5, 0);
    }

    @Test
    @DisplayName("specId 为 null 时也能正确传递（兼容无规格商品）")
    void shouldSupportNullSpecId() {
        when(cartMapper.insertOrAccumulate(anyLong(), anyLong(), isNull(), anyInt(), anyInt()))
                .thenReturn(1);

        Cart cart = new Cart();
        cart.setUserId(1L);
        cart.setProductId(100L);
        // 故意不设 specId（null）
        cart.setQuantity(2);
        cartService.save(cart);

        verify(cartMapper).insertOrAccumulate(eq(1L), eq(100L), isNull(), eq(2), anyInt());
    }

    @Test
    @DisplayName("cart.id / createdAt / updatedAt 字段被忽略（由 SQL 内部赋值）")
    void shouldIgnoreClientSideIdAndTimestamps() {
        when(cartMapper.insertOrAccumulate(anyLong(), anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(1);

        Cart cart = new Cart();
        cart.setId(999L);  // 客户端试图覆盖 id，应该被忽略
        cart.setUserId(1L);
        cart.setProductId(100L);
        cart.setSpecId(200L);
        cart.setQuantity(1);
        cartService.save(cart);

        // 用 ArgumentCaptor 验证：传给 Mapper 的参数只用了 userId/productId/specId/quantity/checked
        // id / createdAt / updatedAt 都没出现在调用里
        ArgumentCaptor<Long> userIdCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> productIdCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> specIdCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> quantityCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> checkedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cartMapper).insertOrAccumulate(
                userIdCap.capture(),
                productIdCap.capture(),
                specIdCap.capture(),
                quantityCap.capture(),
                checkedCap.capture()
        );

        assertEquals(1L, userIdCap.getValue());
        assertEquals(100L, productIdCap.getValue());
        assertEquals(200L, specIdCap.getValue());
        assertEquals(1, quantityCap.getValue());
        // checked 默认 0
        assertEquals(0, checkedCap.getValue());
    }
}
