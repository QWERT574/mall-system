package com.example.minimall.controller;

import com.example.minimall.common.Result;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.service.ChatService;
import com.example.minimall.service.RedisChatService;
import com.example.minimall.vo.ChatMessageVO;
import com.example.minimall.vo.ChatSessionVO;
import org.springframework.beans.factory.annotation.Autowired;

import static com.example.minimall.vo.Converters.convert;
import static com.example.minimall.vo.Converters.convertList;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理员端客服聊天会话管理接口 */
@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    /** 聊天业务服务 */
    @Autowired
    private ChatService chatService;

    /** 聊天会话 Mapper */
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    /** Redis 缓存聊天服务 */
    @Autowired
    private RedisChatService redisChatService;

    /** 获取全部聊天会话列表，按创建时间倒序 */
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getAllSessions() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatSession> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatSession::getCreatedAt);
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);

        for (ChatSession session : sessions) {
            session.setLastMessage(redisChatService.getLastMessage(session.getId()));
        }

        return Result.success(convertList(sessions, ChatSessionVO::new));
    }

    /** 获取指定会话详情及最后一条消息 */
    @GetMapping("/sessions/{id}")
    public Result<ChatSessionVO> getSessionDetail(@PathVariable Long id) {
        ChatSession session = chatSessionMapper.selectSessionDetail(id);
        if (session != null) {
            session.setLastMessage(redisChatService.getLastMessage(id));
        }
        return session != null ? Result.success(convert(session, ChatSessionVO::new)) : Result.error("会话不存在");
    }

    /** 获取会话下的全部消息 */
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMessageVO>> getMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = chatService.getMessages(sessionId);
        return Result.success(convertList(messages, ChatMessageVO::new));
    }

    /** 管理员关闭指定会话 */
    @PutMapping("/session/{id}/close")
    public Result<Void> closeSession(@PathVariable Long id) {
        chatService.closeSession(id, "admin_close");
        return Result.success(null);
    }

    /** 获取客服系统中的活跃会话与在线坐席数 */
    @GetMapping("/active-count")
    public Result<Map<String, Object>> getActiveCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("activeSessions", 0);
        result.put("onlineAgents", 0);
        return Result.success(result);
    }
}
