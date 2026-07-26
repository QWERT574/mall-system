package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.model.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品评价表 Mapper，对应 product_review 表
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
    /** 根据商品 ID 查询评价列表 */
    List<ProductReview> selectByProductId(Long productId);

    /** 根据用户 ID 查询评价列表 */
    List<ProductReview> selectByUserId(Long userId);

    /** 分页查询商品评价 */
    IPage<ProductReview> selectByProductIdPage(Page<ProductReview> page, @Param("productId") Long productId);

    /** 带评分、图片筛选的分页查询商品评价 */
    IPage<ProductReview> selectByProductIdPageWithFilter(Page<ProductReview> page,
                                                          @Param("productId") Long productId,
                                                          @Param("minRating") Integer minRating,
                                                          @Param("maxRating") Integer maxRating,
                                                          @Param("hasImage") Boolean hasImage);

    /** 查询订单下某商品是否已评价 */
    ProductReview selectByOrderIdAndProductId(@Param("orderId") Long orderId, @Param("productId") Long productId);

    /** 分页查询商家收到的评价 */
    IPage<ProductReview> selectBySellerIdPage(Page<ProductReview> page,
                                               @Param("sellerId") Long sellerId,
                                               @Param("keyword") String keyword);

    /** 查询商家收到的评价（不分页） */
    List<ProductReview> selectBySellerId(@Param("sellerId") Long sellerId);
}
