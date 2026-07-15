package com.example.minimall;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.CategoryMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.model.Product;
import com.example.minimall.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CategoryService.hasProducts 单元测试
 * <p>
 * 覆盖场景：
 * <ol>
 *   <li>分类下没有商品 → false</li>
 *   <li>分类下有商品 → true</li>
 *   <li>验证 SQL 拼接：eq("category_id", id)</li>
 * </ol>
 * </p>
 */
@DisplayName("CategoryService.hasProducts() 单元测试")
class CategoryServiceTest {

    private CategoryMapper categoryMapper;
    private ProductMapper productMapper;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        // 用 Mockito 创建 mock（不依赖 Spring 上下文）
        categoryMapper = mock(CategoryMapper.class);
        productMapper = mock(ProductMapper.class);
        categoryService = new CategoryService(categoryMapper, productMapper);
    }

    @Test
    @DisplayName("分类下无商品时返回 false")
    void shouldReturnFalseWhenNoProducts() {
        // mock selectCount 返回 0
        when(productMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = categoryService.hasProducts(100L);

        assertFalse(result, "分类下无商品应返回 false");
        verify(productMapper, times(1)).selectCount(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("分类下有商品时返回 true")
    void shouldReturnTrueWhenHasProducts() {
        // mock selectCount 返回 5
        when(productMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);

        boolean result = categoryService.hasProducts(200L);

        assertTrue(result, "分类下有商品应返回 true");
        verify(productMapper, times(1)).selectCount(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("验证 SQL 条件：eq(\"category_id\", 999)")
    void shouldQueryByCorrectCategoryId() {
        when(productMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        categoryService.hasProducts(999L);

        // 捕获传入的 QueryWrapper 验证 SQL 条件
        ArgumentCaptor<QueryWrapper<Product>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(productMapper).selectCount(captor.capture());

        QueryWrapper<Product> qw = captor.getValue();
        String sqlSegment = qw.getSqlSegment();
        String paramStr = qw.getParamNameValuePairs().toString();
        System.out.println("SQL 片段: " + sqlSegment);
        System.out.println("参数:     " + paramStr);

        // MyBatis Plus 的 eq() 生成的 SQL 用 #{} 占位符
        // SQL 片段中 column = #{MPGENVAL1}，参数 Map 里 999 是 value
        assertTrue(sqlSegment.contains("category_id"),
                "SQL 应包含 category_id 字段");
        assertTrue(sqlSegment.contains("="),
                "SQL 应包含等号");
        assertTrue(paramStr.contains("999"),
                "参数应包含传入的 ID=999，实际: " + paramStr);
    }

    @Test
    @DisplayName("CategoryService 构造器能正确接收 ProductMapper（验证我的 TODO 修改）")
    void shouldInjectProductMapper() {
        // 显式调用构造器，不抛异常即通过
        CategoryService svc = new CategoryService(categoryMapper, productMapper);
        assertNotNull(svc);
    }
}
