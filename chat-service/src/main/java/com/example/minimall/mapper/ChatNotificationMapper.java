package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ChatNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 聊天通知表 Mapper，对应 chat_notification 表
 */
@Mapper
public interface ChatNotificationMapper extends BaseMapper<ChatNotification> {
    /** 分页查询指定用户的聊天通知（按时间倒序） */
    @Select("SELECT * FROM chat_notification WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatNotification> selectByUserId(Long userId, int limit, int offset);

    /** 统计指定用户的未读通知数量 */
    @Select("SELECT COUNT(*) FROM chat_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(Long userId);
}
