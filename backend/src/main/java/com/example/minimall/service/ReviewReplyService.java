package com.example.minimall.service;

import com.example.minimall.mapper.ReviewReplyMapper;
import com.example.minimall.model.ReviewReply;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 评价回复服务 */
@Service
public class ReviewReplyService {
    /** 评价回复 Mapper */
    private final ReviewReplyMapper reviewReplyMapper;

    public ReviewReplyService(ReviewReplyMapper reviewReplyMapper) {
        this.reviewReplyMapper = reviewReplyMapper;
    }

    /**
     * 创建评价回复（**事务**）
     * <p>自动写入 createdAt / updatedAt</p>
     *
     * @param reply 回复实体
     * @return 创建后的回复
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewReply createReply(ReviewReply reply) {
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        reviewReplyMapper.insert(reply);
        return reply;
    }

    /**
     * 查询某条评价的全部回复
     *
     * @param reviewId 评价 ID
     * @return 回复列表
     */
    public List<ReviewReply> getRepliesByReviewId(Long reviewId) {
        return reviewReplyMapper.selectByReviewId(reviewId);
    }

    /**
     * 批量查询多条评价的回复（**单次 SQL**）
     *
     * @param reviewIds 评价 ID 列表
     * @return 回复列表
     */
    public List<ReviewReply> getRepliesByReviewIds(List<Long> reviewIds) {
        return reviewReplyMapper.selectByReviewIds(reviewIds);
    }

    /**
     * 根据回复 ID 查询
     *
     * @param id 回复主键
     * @return 回复实体
     */
    public ReviewReply getReplyById(Long id) {
        return reviewReplyMapper.selectById(id);
    }

    /**
     * 更新回复内容（**事务**）
     *
     * @param id    回复主键
     * @param reply 新值（仅取 replyContent）
     * @return 更新后的回复实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewReply updateReply(Long id, ReviewReply reply) {
        ReviewReply existing = reviewReplyMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("回复不存在");
        }

        existing.setReplyContent(reply.getReplyContent());
        existing.setUpdatedAt(LocalDateTime.now());
        reviewReplyMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除回复（**事务**）
     *
     * @param id 回复主键
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long id) {
        reviewReplyMapper.deleteById(id);
    }
}
