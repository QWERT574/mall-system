package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评价回复表 Mapper，对应 review_reply 表
 */
@Mapper
public interface ReviewReplyMapper extends BaseMapper<ReviewReply> {
    /** 根据评价 ID 查询回复列表（按时间倒序） */
    @Select("SELECT * FROM review_reply WHERE review_id = #{reviewId} ORDER BY created_at DESC")
    List<ReviewReply> selectByReviewId(Long reviewId);

    /** 批量查询评价的回复 */
    @Select("<script>SELECT * FROM review_reply WHERE review_id IN " +
            "<foreach item='id' collection='reviewIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY created_at DESC</script>")
    List<ReviewReply> selectByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
