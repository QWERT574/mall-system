package com.example.minimall.controller;

import com.example.minimall.model.AfterSaleChat;
import com.example.minimall.service.AfterSaleChatService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 售后服务聊天控制器
 */
@RestController
@RequestMapping("/api/aftersale/chat")
public class AfterSaleChatController {
    
    private final AfterSaleChatService afterSaleChatService;
    
    public AfterSaleChatController(AfterSaleChatService afterSaleChatService) {
        this.afterSaleChatService = afterSaleChatService;
    }
    
    /**
     * 发送消息
     * @param chat 聊天记录
     * @return 响应结果
     */
    @PostMapping
    public Map<String, Object> sendMessage(@RequestBody AfterSaleChat chat) {
        try {
            AfterSaleChat savedChat = afterSaleChatService.sendMessage(chat);
            return createSuccessResponse(savedChat);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取聊天记录
     * @param afterSaleId 售后服务ID
     * @return 响应结果
     */
    @GetMapping("/{afterSaleId}")
    public Map<String, Object> getChatMessages(@PathVariable Long afterSaleId) {
        try {
            List<AfterSaleChat> chats = afterSaleChatService.getChatByAfterSaleId(afterSaleId);
            return createSuccessResponse(chats);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取聊天记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新消息为已读
     * @param afterSaleId 售后服务ID
     * @param receiverType 接收者类型
     * @return 响应结果
     */
    @PutMapping("/{afterSaleId}/read")
    public Map<String, Object> markAsRead(@PathVariable Long afterSaleId, 
                                          @RequestParam Integer receiverType) {
        try {
            int result = afterSaleChatService.updateChatReadStatus(afterSaleId, receiverType);
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新消息状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取未读消息数量
     * @param afterSaleId 售后服务ID
     * @param receiverType 接收者类型
     * @return 响应结果
     */
    @GetMapping("/{afterSaleId}/unread-count")
    public Map<String, Object> getUnreadCount(@PathVariable Long afterSaleId, 
                                             @RequestParam Integer receiverType) {
        try {
            int count = afterSaleChatService.getUnreadCount(afterSaleId, receiverType);
            return createSuccessResponse(count);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取未读消息数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除消息
     * @param id 消息ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMessage(@PathVariable Long id) {
        try {
            int result = afterSaleChatService.deleteChat(id);
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建成功响应
     * @param data 响应数据
     * @return 响应结果
     */
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }
    
    /**
     * 构建错误响应
     * @param message 错误消息
     * @return 响应结果
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}