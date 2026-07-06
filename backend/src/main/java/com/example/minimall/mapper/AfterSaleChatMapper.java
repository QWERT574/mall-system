package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.AfterSaleChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后服务聊天记录 Mapper，对应 after_sale_chat 表
 */
@Mapper
public interface AfterSaleChatMapper extends BaseMapper<AfterSaleChat> {

    /** 根据售后服务 ID 查询聊天记录 */
    List<AfterSaleChat> selectByAfterSaleId(@Param("afterSaleId") Long afterSaleId);

    /** 将指定接收者的聊天记录更新为已读 */
    int updateChatReadStatus(@Param("afterSaleId") Long afterSaleId, @Param("receiverType") Integer receiverType);

    /** 查询未读消息数量 */
    int selectUnreadCount(@Param("afterSaleId") Long afterSaleId, @Param("receiverType") Integer receiverType);
}