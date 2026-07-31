package com.example.minimall.controller;

import com.example.minimall.annotation.RateLimit;
import com.example.minimall.common.Result;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatNotification;
import com.example.minimall.model.ChatSession;
import com.example.minimall.vo.ChatSessionVO;
import com.example.minimall.vo.ChatMessageVO;
import com.example.minimall.vo.ChatNotificationVO;
import static com.example.minimall.vo.Converters.convert;
import static com.example.minimall.vo.Converters.convertList;
import com.example.minimall.service.AdminInterventionService;
import com.example.minimall.service.ChatService;
import com.example.minimall.service.ChatMonitorService;
import com.example.minimall.service.ChatNotificationService;
import com.example.minimall.service.RedisChatService;
import com.example.minimall.utils.JwtUtil;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.mapper.ChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 聊天主控：会话/消息/在线状态/通知等用户端接口 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    /** 聊天业务服务 */
    @Autowired
    private ChatService chatService;

    /** JWT 工具 */
    @Autowired
    private JwtUtil jwtUtil;

    /** Redis 缓存聊天服务 */
    @Autowired
    private RedisChatService redisChatService;

    /** 管理员介入业务服务 */
    @Autowired
    private AdminInterventionService adminInterventionService;

    /** 聊天会话 Mapper */
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    /** 聊天消息 Mapper */
    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /** WebSocket 消息推送模板 */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /** 聊天监控服务 */
    @Autowired
    private ChatMonitorService chatMonitorService;

    /** 聊天通知服务 */
    @Autowired
    private ChatNotificationService chatNotificationService;

    /** 获取或创建与卖家的聊天会话 */
    @PostMapping("/session")
    public Result<ChatSessionVO> getOrCreateSession(
            @RequestParam Long sellerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long orderId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error("请先登录");

        ChatSession session = chatService.getOrCreateSession(userId, sellerId, productId, orderId);
        return Result.success(convert(session, ChatSessionVO::new));
    }

    /** 获取当前登录用户的所有聊天会话 */
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getUserSessions(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error("请先登录");

        List<ChatSession> sessions = chatService.getUserSessions(userId);
        return Result.success(convertList(sessions, ChatSessionVO::new));
    }

    // 别名接口,兼容小程序/旧代码: GET /api/chat/session/list
    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> getUserSessionListAlias(HttpServletRequest request) {
        return getUserSessions(request);
    }

    /** 获取指定会话详情 */
    @GetMapping("/sessions/{id}")
    public Result<ChatSessionVO> getSessionDetail(@PathVariable Long id) {
        ChatSession session = chatService.getSessionDetail(id);
        if (session == null) return Result.error("会话不存在");
        return Result.success(convert(session, ChatSessionVO::new));
    }

    /** 用户主动关闭会话 */
    @PutMapping("/session/{id}/close")
    public Result<Void> closeSession(@PathVariable Long id, @RequestParam(defaultValue = "user_close") String reason) {
        chatService.closeSession(id, reason);
        return Result.success(null);
    }

    /** 分页/增量获取会话消息 */
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMessageVO>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<ChatMessage> messages;
        if (afterId > 0) {
            messages = chatService.getMessagesAfterId(sessionId, afterId);
        } else {
            messages = chatService.getMessagesPaged(sessionId, page, size);
        }
        return Result.success(convertList(messages, ChatMessageVO::new));
    }

    // 别名: GET /api/chat/message?sessionId=xxx (兼容小程序)
    @GetMapping("/message")
    public Result<List<ChatMessageVO>> getMessagesByQuery(
            @RequestParam Long sessionId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return getMessages(sessionId, afterId, page, size);
    }

    /** 检查目标用户/客服/商家的在线状态 */
    @GetMapping("/status/{targetId}")
    public Result<Map<String, Object>> checkOnlineStatus(@PathVariable Long targetId,
                                                          @RequestParam(defaultValue = "user") String type) {
        boolean online;
        if ("seller".equals(type)) {
            online = redisChatService.isSellerOnline(targetId);
        } else if ("agent".equals(type)) {
            online = redisChatService.isAgentOnline(targetId);
        } else {
            online = redisChatService.isUserOnline(targetId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isOnline", online);
        return Result.success(result);
    }

    /** 获取当前用户的离线消息（拉取后清除） */
    @GetMapping("/offline/messages")
     public Result<List<ChatMessageVO>> getOfflineMessages(HttpServletRequest request) {
         Long userId = getUserId(request);
         if (userId == null) return Result.error("请先登录");
         List<ChatMessage> messages = redisChatService.getOfflineMessages(userId);
         if (messages != null && !messages.isEmpty()) {
             redisChatService.clearOfflineMessages(userId);
         }
         return Result.success(convertList(messages, ChatMessageVO::new));
    }

    /** 将会话内消息标记为已读 */
    @PostMapping("/read")
     public Result<Void> markAsRead(@RequestParam Long sessionId, HttpServletRequest request) {
         Long userId = getUserId(request);
         if (userId == null) return Result.error(401, "请先登录");
         ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) return Result.error(404, "会话不存在");
        int callerRole = 3;
        if (session.getUserId().equals(userId)) {
            callerRole = 1;
        } else if (session.getSellerId() != null && session.getSellerId().equals(userId)) {
            callerRole = 2;
        }
        chatService.markMessagesAsRead(sessionId, userId, callerRole);
        return Result.success(null);
     }

    /** 获取当前商家名下的所有聊天会话 */
    @GetMapping("/seller/sessions")
     public Result<List<ChatSessionVO>> getSellerSessions(HttpServletRequest request) {
         Long sellerId = getUserId(request);
         if (sellerId == null) {
             return Result.error(401, "请先登录");
         }
         List<ChatSession> sessions = chatSessionMapper.selectBySellerId(sellerId);
         return Result.success(convertList(sessions, ChatSessionVO::new));
     }
 
    /** 管理员在会话中发送消息（带离线存储与通知） */
    @PostMapping("/send")
     public Result<ChatMessageVO> adminSendMessage(
             @RequestParam Long sessionId,
             @RequestParam String content,
             HttpServletRequest request) {
         Long adminId = getUserId(request);
         if (adminId == null) {
             return Result.error(401, "请先登录");
         }
         ChatSession session = chatSessionMapper.selectById(sessionId);
         if (session == null) {
             return Result.error(404, "会话不存在");
         }
         ChatMessage message = chatService.sendTextMessage(
                sessionId, adminId, 3, session.getSellerId(),
                content, session.getOrderId(), session.getProductId());

        redisChatService.updateSessionActiveTime(sessionId);
        redisChatService.cacheLastMessage(sessionId, content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content);

        boolean userOnline = redisChatService.isUserOnline(session.getUserId());
        if (!userOnline) {
            redisChatService.saveOfflineMessage(session.getUserId(), message);
        }
        redisChatService.incrementUnreadCount(sessionId, session.getUserId());

        boolean sellerOnline = redisChatService.isSellerOnline(session.getSellerId());
        if (!sellerOnline) {
            redisChatService.saveOfflineMessage(session.getSellerId(), message);
        }

        try {
            messagingTemplate.convertAndSend("/topic/chat/session/" + sessionId, message);
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, message);
        } catch (Exception e) {
            logger.warn("WebSocket推送消息失败: {}", e.getMessage());
        }

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_message");
            notification.put("sessionId", sessionId);
            notification.put("content", content != null && content.length() > 100 ? content.substring(0, 100) : content);
            notification.put("senderType", 3);
            notification.put("createdAt", message.getCreatedAt().toString());
            messagingTemplate.convertAndSendToUser(String.valueOf(session.getUserId()), "/queue/notifications", notification);
            if (session.getSellerId() != null) {
                messagingTemplate.convertAndSendToUser(String.valueOf(session.getSellerId()), "/queue/notifications", notification);
            }
        } catch (Exception e) {
            logger.warn("推送消息通知失败: {}", e.getMessage());
        }

        return Result.success(convert(message, ChatMessageVO::new));
    }

    /** 关闭指定会话（仅修改状态，不删除） */
    @PostMapping("/close/{id}")
     public Result<Void> closeSession(@PathVariable Long id) {
         ChatSession session = chatSessionMapper.selectById(id);
         if (session == null) {
             return Result.error(404, "会话不存在");
         }
         session.setStatus(2);
         session.setClosedAt(LocalDateTime.now());
         chatSessionMapper.updateById(session);
         return Result.success(null);
     }
 
    /** 统计当前活跃（未关闭）会话数 */
    @GetMapping("/active-count")
    public Result<Integer> getActiveCount() {
         QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
         wrapper.in("status", 0, 1);
         Long count = chatSessionMapper.selectCount(wrapper);
         return Result.success(count.intValue());
     }

    /** 获取当前用户或商家的未读消息总数 */
    @GetMapping("/unread/count")
    public Result<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error("请先登录");

        String userType = request.getHeader("User-Type");
        List<ChatSession> sessions;
        if (userType != null && Integer.parseInt(userType) == 2) {
            sessions = chatSessionMapper.selectBySellerId(userId);
        } else {
            sessions = chatService.getUserSessions(userId);
        }

        int totalUnread = 0;
        for (ChatSession session : sessions) {
            int redisUnread = redisChatService.getUnreadCount(session.getId(), userId);
            if (redisUnread > 0) {
                totalUnread += redisUnread;
            } else {
                if (userType != null && Integer.parseInt(userType) == 2) {
                    totalUnread += session.getSellerUnread() != null ? session.getSellerUnread() : 0;
                } else {
                    totalUnread += session.getUserUnread() != null ? session.getUserUnread() : 0;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalUnread", totalUnread);
        return Result.success(result);
    }

    /** 标记当前用户/客服/商家上线 */
    @PostMapping("/online")
     public Result<Void> setOnline(HttpServletRequest request) {
         Long userId = getUserId(request);
         if (userId == null) return Result.error("请先登录");
         String userType = request.getHeader("User-Type");
         if (userType != null) {
             int ut = Integer.parseInt(userType);
             if (ut == 2) {
                 redisChatService.sellerOnline(userId);
             } else if (ut == 3) {
                 redisChatService.agentOnline(userId);
             } else {
                 redisChatService.userOnline(userId);
             }
         } else {
             redisChatService.userOnline(userId);
         }
         return Result.success(null);
     }
 
    /** 标记当前用户/客服/商家下线 */
    @PostMapping("/offline")
     public Result<Void> setOffline(HttpServletRequest request) {
         Long userId = getUserId(request);
         if (userId == null) return Result.error("请先登录");
         String userType = request.getHeader("User-Type");
         if (userType != null) {
             int ut = Integer.parseInt(userType);
             if (ut == 2) {
                 redisChatService.sellerOffline(userId);
             } else if (ut == 3) {
                 redisChatService.agentOffline(userId);
             } else {
                 redisChatService.userOffline(userId);
             }
         } else {
             redisChatService.userOffline(userId);
         }
         return Result.success(null);
     }

    /** 用户/商家发送聊天消息并触发实时推送/离线存储/通知 */
    @RateLimit(limit = 20, timeout = 10)
    @PostMapping("/message")
    public Result<ChatMessageVO> sendMessage(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String imageUrl,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        // 兼容: query 优先,否则从 body 取 (小程序用 JSON body)
        if (body != null) {
            if (sessionId == null && body.get("sessionId") != null) {
                sessionId = Long.valueOf(body.get("sessionId").toString());
            }
            if ((content == null || content.isEmpty()) && body.get("content") != null) {
                content = body.get("content").toString();
            }
            if ((imageUrl == null || imageUrl.isEmpty()) && body.get("imageUrl") != null) {
                imageUrl = body.get("imageUrl").toString();
            }
        }
        if (sessionId == null) {
            return Result.error(400, "sessionId 不能为空");
        }
        if (content == null || content.isEmpty()) {
            return Result.error(400, "content 不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "会话不存在");
        }
        
        Integer senderType = 1;
        Long receiverId = session.getSellerId();
        if (session.getSellerId() != null && session.getSellerId().equals(userId)) {
            senderType = 2;
            receiverId = session.getUserId();
        }
        String userTypeStr = request.getHeader("User-Type");
        if (userTypeStr != null) {
            try {
                int userType = Integer.parseInt(userTypeStr);
                if (userType == 3) {
                    senderType = 3;
                    receiverId = session.getUserId();
                }
            } catch (NumberFormatException ignored) {}
        }
        
        ChatMessage message = chatService.sendTextMessage(
                sessionId, userId, senderType, receiverId,
                content, session.getOrderId(), session.getProductId());
        chatMonitorService.recordMessageSent();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            message.setImageUrl(imageUrl);
        }

        redisChatService.updateSessionActiveTime(sessionId);
        redisChatService.cacheLastMessage(sessionId, content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content);

        boolean receiverOnline;
        if (senderType == 1) {
            receiverOnline = redisChatService.isSellerOnline(session.getSellerId());
            if (!receiverOnline) {
                redisChatService.saveOfflineMessage(session.getSellerId(), message);
            } else {
                message.setStatus(2);
                message.setDeliveredAt(LocalDateTime.now());
                message.setUpdatedAt(LocalDateTime.now());
                chatMessageMapper.updateById(message);
                chatMonitorService.recordMessageDelivered();
            }
            redisChatService.incrementUnreadCount(sessionId, session.getSellerId());
        } else if (senderType == 2) {
            receiverOnline = redisChatService.isUserOnline(session.getUserId());
            if (!receiverOnline) {
                redisChatService.saveOfflineMessage(session.getUserId(), message);
            } else {
                message.setStatus(2);
                message.setDeliveredAt(LocalDateTime.now());
                message.setUpdatedAt(LocalDateTime.now());
                chatMessageMapper.updateById(message);
                chatMonitorService.recordMessageDelivered();
            }
            redisChatService.incrementUnreadCount(sessionId, session.getUserId());
        } else if (senderType == 3) {
            boolean buyerOnline = redisChatService.isUserOnline(session.getUserId());
            boolean sellerOnline = redisChatService.isSellerOnline(session.getSellerId());
            message.setStatus(2);
            message.setDeliveredAt(LocalDateTime.now());
            message.setUpdatedAt(LocalDateTime.now());
            chatMessageMapper.updateById(message);
            chatMonitorService.recordMessageDelivered();
            if (!buyerOnline) redisChatService.saveOfflineMessage(session.getUserId(), message);
            if (!sellerOnline) redisChatService.saveOfflineMessage(session.getSellerId(), message);
            redisChatService.incrementUnreadCount(sessionId, session.getUserId());
            redisChatService.incrementUnreadCount(sessionId, session.getSellerId());
        }

        try {
            messagingTemplate.convertAndSend("/topic/chat/session/" + sessionId, message);
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, message);
        } catch (Exception e) {
            logger.warn("WebSocket推送消息失败: {}", e.getMessage());
        }

        try {
            if (senderType == 1) {
                Long targetId = session.getAgentId() != null ? session.getAgentId() : session.getSellerId();
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "new_message");
                notification.put("sessionId", sessionId);
                notification.put("content", content != null && content.length() > 100 ? content.substring(0, 100) : content);
                notification.put("senderType", senderType);
                notification.put("createdAt", message.getCreatedAt().toString());
                messagingTemplate.convertAndSendToUser(String.valueOf(targetId), "/queue/notifications", notification);
            } else if (senderType == 2) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "new_message");
                notification.put("sessionId", sessionId);
                notification.put("content", content != null && content.length() > 100 ? content.substring(0, 100) : content);
                notification.put("senderType", senderType);
                notification.put("createdAt", message.getCreatedAt().toString());
                messagingTemplate.convertAndSendToUser(String.valueOf(session.getUserId()), "/queue/notifications", notification);
            } else if (senderType == 3) {
                Map<String, Object> buyerNotification = new HashMap<>();
                buyerNotification.put("type", "new_message");
                buyerNotification.put("sessionId", sessionId);
                buyerNotification.put("content", content != null && content.length() > 100 ? content.substring(0, 100) : content);
                buyerNotification.put("senderType", senderType);
                buyerNotification.put("createdAt", message.getCreatedAt().toString());
                messagingTemplate.convertAndSendToUser(String.valueOf(session.getUserId()), "/queue/notifications", buyerNotification);
                if (session.getSellerId() != null) {
                    Map<String, Object> sellerNotification = new HashMap<>(buyerNotification);
                    messagingTemplate.convertAndSendToUser(String.valueOf(session.getSellerId()), "/queue/notifications", sellerNotification);
                }
            }
        } catch (Exception e) {
            logger.warn("推送消息通知失败: {}", e.getMessage());
        }

        if (senderType == 1) {
            Long targetId = session.getAgentId() != null ? session.getAgentId() : session.getSellerId();
            chatNotificationService.notifyNewMessage(message, targetId, 2);
        } else if (senderType == 2) {
            chatNotificationService.notifyNewMessage(message, session.getUserId(), 1);
        } else if (senderType == 3) {
            chatNotificationService.notifyNewMessage(message, session.getUserId(), 1);
            if (session.getSellerId() != null) {
                chatNotificationService.notifyNewMessage(message, session.getSellerId(), 2);
            }
        }

        return Result.success(convert(message, ChatMessageVO::new));
    }

    /** 用户在聊天中申请客服介入 */
    @PostMapping("/request-intervention")
    public Result<Map<String, Object>> requestIntervention(
            @RequestParam Long sessionId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String evidenceImages,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "会话不存在");
        }
        AdminIntervention intervention = new AdminIntervention();
        intervention.setOrderId(session.getOrderId());
        intervention.setProductId(session.getProductId());
        intervention.setSellerId(session.getSellerId());
        intervention.setUserId(userId);
        intervention.setSessionId(sessionId);
        intervention.setIssueType("chat_dispute");
        intervention.setTitle("聊天纠纷");
        intervention.setDescription(reason != null ? reason : "用户请求客服介入");
        intervention.setEvidenceImages(evidenceImages);
        intervention.setStatus(0);
        intervention.setCreatedAt(LocalDateTime.now());
        intervention.setUpdatedAt(LocalDateTime.now());
        adminInterventionService.createIntervention(intervention);
        Map<String, Object> result = new HashMap<>();
        result.put("interventionId", intervention.getId());
        result.put("status", intervention.getStatus());
        return Result.success(result);
    }

    /** 标记单条消息为已送达 */
    @PutMapping("/message/{messageId}/delivered")
    public Result<Void> markDelivered(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) return Result.error(404, "消息不存在");
        if (message.getStatus() == null || message.getStatus() < 2) {
            message.setStatus(2);
            message.setDeliveredAt(LocalDateTime.now());
            message.setUpdatedAt(LocalDateTime.now());
            chatMessageMapper.updateById(message);
        }
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("type", "message_status");
            statusUpdate.put("messageId", messageId);
            statusUpdate.put("status", 2);
            statusUpdate.put("deliveredAt", message.getDeliveredAt() != null ? message.getDeliveredAt().toString() : null);
            messagingTemplate.convertAndSend("/topic/chat/session/" + message.getSessionId(), statusUpdate);
            messagingTemplate.convertAndSend("/topic/chat/" + message.getSessionId(), statusUpdate);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getSenderId()), "/queue/notifications", statusUpdate);
        } catch (Exception e) {
            logger.warn("推送消息投递状态失败: {}", e.getMessage());
        }
        return Result.success(null);
    }

    /** 标记单条消息为已读 */
    @PutMapping("/message/{messageId}/read")
    public Result<Void> markMessageRead(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) return Result.error(404, "消息不存在");
        if (message.getIsRead() == null || message.getIsRead() == 0) {
            message.setIsRead(1);
            message.setReadAt(LocalDateTime.now());
            message.setStatus(3);
            message.setUpdatedAt(LocalDateTime.now());
            chatMessageMapper.updateById(message);
        }
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("type", "message_status");
            statusUpdate.put("messageId", messageId);
            statusUpdate.put("status", 3);
            statusUpdate.put("readAt", message.getReadAt() != null ? message.getReadAt().toString() : null);
            messagingTemplate.convertAndSend("/topic/chat/session/" + message.getSessionId(), statusUpdate);
            messagingTemplate.convertAndSend("/topic/chat/" + message.getSessionId(), statusUpdate);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getSenderId()), "/queue/notifications", statusUpdate);
        } catch (Exception e) {
            logger.warn("推送消息已读状态失败: {}", e.getMessage());
        }
        return Result.success(null);
    }

    /** 批量标记消息为已送达 */
    @PutMapping("/messages/delivered/batch")
    public Result<Void> markBatchDelivered(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        List<Long> messageIds = body.get("messageIds");
        if (messageIds == null || messageIds.isEmpty()) return Result.success(null);
        LocalDateTime now = LocalDateTime.now();
        for (Long msgId : messageIds) {
            try {
                ChatMessage message = chatMessageMapper.selectById(msgId);
                if (message != null && (message.getStatus() == null || message.getStatus() < 2)) {
                    message.setStatus(2);
                    message.setDeliveredAt(now);
                    message.setUpdatedAt(now);
                    chatMessageMapper.updateById(message);
                }
            } catch (Exception e) {
                logger.warn("批量标记投递失败, messageId={}: {}", msgId, e.getMessage());
            }
        }
        return Result.success(null);
    }

    /** 分页获取当前用户的消息通知 */
    @GetMapping("/notifications")
    public Result<List<ChatNotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        List<ChatNotification> notifications = chatNotificationService.getUserNotifications(userId, page, size);
        return Result.success(convertList(notifications, ChatNotificationVO::new));
    }

    /** 获取当前用户未读通知数 */
    @GetMapping("/notifications/unread-count")
    public Result<Map<String, Object>> getUnreadNotificationCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        int count = chatNotificationService.getUnreadCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /** 标记单条通知为已读 */
    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        chatNotificationService.markAsRead(id);
        return Result.success(null);
    }

    /** 标记当前用户所有通知为已读 */
    @PutMapping("/notifications/read-all")
    public Result<Void> markAllNotificationsRead(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "请先登录");
        chatNotificationService.markAllAsRead(userId);
        return Result.success(null);
    }

    /** 获取聊天系统监控健康度 */
    @GetMapping("/monitor/health")
    public Result<Map<String, Object>> getMonitorHealth() {
        return Result.success(chatMonitorService.getHealth());
    }

    /** 获取聊天系统监控指标 */
    @GetMapping("/monitor/metrics")
    public Result<Map<String, Object>> getMonitorMetrics() {
        return Result.success(chatMonitorService.getMetrics());
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.getUserIdFromToken(authHeader.substring(7));
            } catch (Exception e) {
                logger.error("Failed to get userId from token", e);
            }
        }
        return null;
    }
}
