package com.example.minimall.service.impl;

import com.example.minimall.mapper.AfterSaleChatMapper;
import com.example.minimall.model.AfterSaleChat;
import com.example.minimall.service.AfterSaleChatService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后服务聊天记录服务实现
 */
@Service
public class AfterSaleChatServiceImpl implements AfterSaleChatService {

    /** 售后聊天Mapper */
    private final AfterSaleChatMapper afterSaleChatMapper;

    public AfterSaleChatServiceImpl(AfterSaleChatMapper afterSaleChatMapper) {
        this.afterSaleChatMapper = afterSaleChatMapper;
    }

    @Override
    /**
     * 发送并保存一条售后聊天记录
     * <p>自动设置 isRead=false、createdAt/updatedAt</p>
     *
     * @param chat 聊天实体
     * @return 保存后的实体
     */
    public AfterSaleChat sendMessage(AfterSaleChat chat) {
        // 设置默认值
        chat.setIsRead(false);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        // 保存聊天记录
        afterSaleChatMapper.insert(chat);

        return chat;
    }

    @Override
    /**
     * 获取指定售后单的所有聊天记录
     *
     * @param afterSaleId 售后单 ID
     * @return 聊天记录列表
     */
    public List<AfterSaleChat> getChatByAfterSaleId(Long afterSaleId) {
        return afterSaleChatMapper.selectByAfterSaleId(afterSaleId);
    }

    @Override
    /**
     * 将指定售后单下发给指定接收方的未读消息全部置为已读
     *
     * @param afterSaleId  售后单 ID
     * @param receiverType 接收方类型
     * @return 受影响行数
     */
    public int updateChatReadStatus(Long afterSaleId, Integer receiverType) {
        return afterSaleChatMapper.updateChatReadStatus(afterSaleId, receiverType);
    }

    @Override
    /**
     * 统计指定售后单下某接收方的未读消息数
     *
     * @param afterSaleId  售后单 ID
     * @param receiverType 接收方类型
     * @return 未读消息数
     */
    public int getUnreadCount(Long afterSaleId, Integer receiverType) {
        return afterSaleChatMapper.selectUnreadCount(afterSaleId, receiverType);
    }

    @Override
    /**
     * 删除单条售后聊天记录
     *
     * @param id 聊天记录 ID
     * @return 受影响行数
     */
    public int deleteChat(Long id) {
        return afterSaleChatMapper.deleteById(id);
    }
}