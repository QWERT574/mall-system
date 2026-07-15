package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.model.Category;
import com.example.minimall.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品业务服务
 * <p>
 * 提供商品的增删改查、搜索、分页、加权随机排序、库存维护、分类与商家维度查询等能力。
 * </p>
 */
@Service
public class ProductService {
    /** 商品 Mapper，封装 MyBatis-Plus 数据访问 */
    private final ProductMapper mapper;
    /** 商品图片服务，用于商品图片的增删与查询 */
    private final ProductImageService imageService;
    /** 商品标签服务，用于维护商品与标签的关联 */
    private final ProductTagService tagService;
    /** 商品规格服务，用于维护商品规格信息 */
    private final ProductSpecService specService;
    /** 分类服务，用于获取分类层级关系 */
    private final CategoryService categoryService;

    public ProductService(ProductMapper mapper, ProductImageService imageService, ProductTagService tagService, ProductSpecService specService, CategoryService categoryService){
        this.mapper=mapper;
        this.imageService = imageService;
        this.tagService = tagService;
        this.specService = specService;
        this.categoryService = categoryService;
    }

    /** 查询全部商品（无过滤条件） */
    public List<Product> listAll(){return mapper.selectList(null);}

    /** 根据主键 ID 查询单个商品 */
    public Product findById(Long id){return mapper.selectById(id);}

    /** 根据关键字搜索商品（由 Mapper 自定义 SQL 实现） */
    public List<Product> search(String keyword){return mapper.selectByKeyword(keyword);}

    /**
     * 分页查询商品（**按销量加权随机排序**，本项目核心算法）
     * <p>
     * 流程：
     * <ol>
     *   <li>拉取全量商品 + 销量（一次 IO）</li>
     *   <li>不放回加权随机抽样：销量大的商品被抽中概率高</li>
     *   <li>总销量为 0 时退化为纯随机，避免新商品永远没机会被推荐</li>
     *   <li>手动按 page 参数切片返回</li>
     * </ol>
     * 移除缓存注解以保证每次返回结果不同（推荐需要差异化）。
     * </p>
     *
     * @param page 分页对象（current、size）
     * @return 加权随机排序后的商品分页
     */
    public IPage<Product> page(Page<Product> page){
        // 1. 拉取全量商品 + 销量
        List<Product> allProducts = mapper.selectProductWithSales();

        // 2. 兜底：没有带销量的商品时用基础查询
        if (allProducts == null || allProducts.isEmpty()) {
            allProducts = mapper.selectList(null);
            // 销量默认 0，确保加权算法不出现 NPE
            allProducts.forEach(product -> product.setSales(0));
        }

        // 3. 加权随机排序（基于销量）
        List<Product> weightedRandomProducts = new ArrayList<>();
        // 临时列表：每次抽中后移除，实现"不放回"抽样
        List<Product> tempProducts = new ArrayList<>(allProducts);

        while (!tempProducts.isEmpty()) {
            // 3.1 计算当前剩余商品的销量总和（总权重）
            int totalWeight = tempProducts.stream()
                    .mapToInt(Product::getSales)
                    .sum();

            // 3.2 边界处理：所有商品销量都为 0 → 退化为纯随机
            if (totalWeight == 0) {
                int randomIndex = (int) (Math.random() * tempProducts.size());
                weightedRandomProducts.add(tempProducts.remove(randomIndex));
                continue;
            }

            // 3.3 加权抽样：在 [0, totalWeight) 上取随机数，落到销量区间上
            double random = Math.random() * totalWeight;
            int currentWeight = 0;
            for (int i = 0; i < tempProducts.size(); i++) {
                Product product = tempProducts.get(i);
                currentWeight += product.getSales();
                if (random < currentWeight) {
                    // 命中：从临时列表移除，追加到结果集
                    weightedRandomProducts.add(tempProducts.remove(i));
                    break;
                }
            }
        }

        // 4. 手动分页（因为 MyBatis 分页已被加权逻辑接管）
        int start = (int) (page.getCurrent() - 1) * (int) page.getSize();
        int end = Math.min(start + (int) page.getSize(), weightedRandomProducts.size());
        List<Product> pagedProducts = weightedRandomProducts.subList(start, end);

        // 5. 构造分页结果对象
        Page<Product> resultPage = new Page<>(page.getCurrent(), page.getSize());
        resultPage.setRecords(pagedProducts);
        resultPage.setTotal(weightedRandomProducts.size());
        resultPage.setPages((long) Math.ceil((double) weightedRandomProducts.size() / page.getSize()));

        return resultPage;
    }

    /**
     * 按关键字分页搜索商品（商品名模糊匹配）
     *
     * @param page    分页对象
     * @param keyword 搜索关键字
     * @return 匹配的商品分页
     */
    public IPage<Product> searchPage(Page<Product> page, String keyword){
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword);
        return mapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件过滤分页查询（关键字 + 分类 + 价格区间 + 排序）
     *
     * @param page      分页对象
     * @param keyword   关键字（商品名模糊匹配，可空）
     * @param categoryId 分类 ID（可空）
     * @param minPrice  最低价（可空）
     * @param maxPrice  最高价（可空）
     * @param sortBy    排序字段：price / sales / stock / 默认 id
     * @param sortOrder 排序方向：asc / desc（仅 price、stock 生效）
     * @return 商品分页
     */
    public IPage<Product> searchPageWithFilters(Page<Product> page, String keyword, Long categoryId, Double minPrice, Double maxPrice, String sortBy, String sortOrder){
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("name", keyword);
        }

        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }

        if (minPrice != null) {
            queryWrapper.ge("price", minPrice);
        }

        if (maxPrice != null) {
            queryWrapper.le("price", maxPrice);
        }

        // 排序：price 支持升降序，sales 只支持降序（销量越高越靠前）
        if ("price".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc("price");
            } else {
                queryWrapper.orderByDesc("price");
            }
        } else if ("sales".equals(sortBy)) {
            queryWrapper.orderByDesc("sales");
        } else if ("stock".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc("stock");
            } else {
                queryWrapper.orderByDesc("stock");
            }
        } else {
            // 默认按 ID 倒序
            queryWrapper.orderByDesc("id");
        }

        return mapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据分类 ID 列出该分类下所有商品
     *
     * @param categoryId 分类 ID
     * @return 商品列表
     */
    public List<Product> listByCategoryId(Long categoryId){
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId);
        return mapper.selectList(queryWrapper);
    }

    /**
     * 根据分类 ID 分页查询商品
     * <p>
     * 智能处理一级分类 vs 子分类：
     * <ul>
     *   <li>一级分类：合并自身 + 所有子分类的商品</li>
     *   <li>子分类：仅查该分类商品</li>
     * </ul>
     * </p>
     *
     * @param page       分页对象
     * @param categoryId 分类 ID
     * @return 商品分页
     */
    public IPage<Product> pageByCategoryId(Page<Product> page, Long categoryId){
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        // 1. 查询分类详情，判断层级
        Category category = categoryService.findById(categoryId);

        if (category != null) {
            // 2. 顶级分类（parentId == 0）→ 自身 + 所有子分类
            if (category.getParentId() == 0) {
                List<Category> subCategories = categoryService.listByParentId(categoryId);
                // 收集所有分类 ID
                List<Long> categoryIds = new ArrayList<>();
                categoryIds.add(categoryId); // 自身

                if (subCategories != null && !subCategories.isEmpty()) {
                    for (Category subCat : subCategories) {
                        categoryIds.add(subCat.getId());
                    }
                }

                // 用 IN 查询所有分类下的商品
                queryWrapper.in("category_id", categoryIds);
            } else {
                // 3. 子分类 → 直接查该分类
                queryWrapper.eq("category_id", categoryId);
            }
        } else {
            // 4. 分类不存在 → 返回空（用 -1 永远查不到）
            queryWrapper.eq("category_id", -1);
        }

        return mapper.selectPage(page, queryWrapper);
    }

    /**
     * 保存商品（新增或更新）
     * <p>
     * 通过 ID 是否为空判断新增/更新。同时清空商品缓存。
     * </p>
     *
     * @param product 商品实体
     */
    @CacheEvict(value = "products", allEntries = true)
    public void save(Product product) {
        if (product.getId() == null) {
            mapper.insert(product);
        } else {
            mapper.updateById(product);
        }
    }

    /**
     * 根据商家 ID 查询其所有商品
     *
     * @param sellerId 商家 ID
     * @return 该商家名下的商品列表
     */
    public List<Product> findBySellerId(Long sellerId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId);
        return mapper.selectList(queryWrapper);
    }

    /**
     * 根据商家 ID 分页查询其商品
     *
     * @param page     分页对象
     * @param sellerId 商家 ID
     * @return 商品分页
     */
    public IPage<Product> pageBySellerId(Page<Product> page, Long sellerId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId);
        return mapper.selectPage(page, queryWrapper);
    }

    /**
     * 删除商品（级联删除关联数据）
     * <p>
     * 同时删除：商品图片、标签关联、规格、商品本身
     * </p>
     *
     * @param id 商品 ID
     */
    public void delete(Long id) {
        // 级联删除：图片、标签、规格、商品
        imageService.deleteByProductId(id);
        tagService.deleteByProductId(id);
        specService.deleteByProductId(id);
        mapper.deleteById(id);
    }

    /**
     * 根据标签 ID 查询关联商品（由 Mapper 自定义 SQL 实现）
     *
     * @param tagId 标签 ID
     * @return 该标签下的商品列表
     */
    public List<Product> findByTagId(Long tagId) {
        return mapper.selectByTagId(tagId);
    }

    /**
     * 直接设置商品库存为指定值
     *
     * @param id    商品 ID
     * @param stock 新库存值
     */
    public void updateStock(Long id, Integer stock) {
        Product product = mapper.selectById(id);
        if (product != null) {
            product.setStock(stock);
            mapper.updateById(product);
        }
    }

    /**
     * 减少商品库存（下单时使用）
     *
     * @param id       商品 ID
     * @param quantity 减少数量
     */
    public void decreaseStock(Long id, Integer quantity) {
        Product product = mapper.selectById(id);
        if (product != null && product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity);
            mapper.updateById(product);
        }
    }

    /**
     * 增加商品库存（取消订单/退货时使用）
     *
     * @param id       商品 ID
     * @param quantity 增加数量
     */
    public void increaseStock(Long id, Integer quantity) {
        Product product = mapper.selectById(id);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            mapper.updateById(product);
        }
    }

    /**
     * 获取指定分类集合的统计信息
     *
     * @param categoryIds 分类 ID 列表
     * @return 每个分类的商品数 / 总销量 / 平均价格等
     */
    public java.util.List<java.util.Map<String, Object>> getCategoryStatistics(java.util.List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return mapper.selectCategoryStatistics(categoryIds);
    }

    /**
     * 获取分类对比数据（用于管理后台报表）
     *
     * @param categoryIds 分类 ID 列表
     * @return 对比 Map：含 categories、totalCategories、totalProducts、overallAvgPrice、overallTotalSales
     */
    public java.util.Map<String, Object> getCategoryComparison(java.util.List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            java.util.Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("categories", new java.util.ArrayList<>());
            emptyResult.put("totalCategories", 0);
            emptyResult.put("totalProducts", 0);
            emptyResult.put("overallAvgPrice", 0);
            emptyResult.put("overallTotalSales", 0);
            return emptyResult;
        }
        return mapper.selectCategoryComparison(categoryIds);
    }
}
