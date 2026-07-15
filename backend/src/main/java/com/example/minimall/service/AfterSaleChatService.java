package com.example.minimall.service;

import com.example.minimall.model.AfterSaleChat;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 售后服务聊天记录服务
 */
@Service
public interface AfterSaleChatService {
    
    /**
     * 发送消息
     * @param chat 聊天记录
     * @return 保存后的聊天记录
     */
    AfterSaleChat sendMessage(AfterSaleChat chat);
    
    /**
     * 根据售后服务ID查询聊天记录
     * @param afterSaleId 售后服务ID
     * @return 聊天记录列表
     */
    List<AfterSaleChat> getChatByAfterSaleId(Long afterSaleId);
    
    /**
     * 更新聊天记录为已读状态
     * @param afterSaleId 售后服务ID
     * @param receiverType 接收者类型
     * @return 更新结果
     */
    int updateChatReadStatus(Long afterSaleId, Integer receiverType);
    
    /**
     * 查询未读消息数量
     * @param afterSaleId 售后服务ID
     * @param receiverType 接收者类型
     * @return 未读消息数量
     */
    int getUnreadCount(Long afterSaleId, Integer receiverType);
    
    /**
     * 删除聊天记录
     * @param id 聊天记录ID
     * @return 删除结果
     */
    int deleteChat(Long id);
}