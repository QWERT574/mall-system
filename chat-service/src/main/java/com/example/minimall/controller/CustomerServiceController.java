package com.example.minimall.controller;

import com.example.minimall.common.Result;
import com.example.minimall.feign.UserFeignClient;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatSession;
import com.example.minimall.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客服坐席与会话分配/转接相关接口。
 *
 * <p><b>重构说明</b>：原直接注入 {@code UserMapper} 访问 user 表的代码
 * 已替换为 {@link UserFeignClient} Feign 远程调用 user-service 的内部 API
 * （/api/internal/user/**）。chat-service 不再持有 user 表的本地 Mapper。</p>
 */
@RestController
@RequestMapping("/api/cs")
public class CustomerServiceController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceController.class);

    /** 用户服务 Feign 客户端（跨域查客服账号，替代原 UserMapper） */
    private final UserFeignClient userFeignClient;

    /** 聊天会话 Mapper */
    private final ChatSessionMapper chatSessionMapper;

    public CustomerServiceController(UserFeignClient userFeignClient, ChatSessionMapper chatSessionMapper) {
        this.userFeignClient = userFeignClient;
        this.chatSessionMapper = chatSessionMapper;
    }

    /** 获取所有客服坐席列表 */
    @GetMapping("/agents")
    public Result<List<Map<String, Object>>> getAgents() {
        try {
            // user_type=2 为管理员/客服
            List<User> adminUsers = userFeignClient.listByUserType(2, null);
            return Result.success(toAgentList(adminUsers));
        } catch (Exception e) {
            logger.error("获取客服代理列表失败", e);
            return Result.error("获取客服代理列表失败：" + e.getMessage());
        }
    }

    /** 获取状态为可用的客服坐席列表 */
    @GetMapping("/agents/available")
    public Result<List<Map<String, Object>>> getAvailableAgents() {
        try {
            // user_type=2 且 status=1（启用）
            List<User> adminUsers = userFeignClient.listByUserType(2, 1);
            return Result.success(toAgentList(adminUsers));
        } catch (Exception e) {
            logger.error("获取可用客服代理列表失败", e);
            return Result.error("获取可用客服代理列表失败：" + e.getMessage());
        }
    }

    /** 更新客服坐席状态（启用/禁用） */
    @PutMapping("/agent/{agentId}/status")
    public Result<Void> updateAgentStatus(@PathVariable Long agentId, @RequestBody Map<String, Object> request) {
        try {
            Integer status = (Integer) request.get("status");
            boolean ok = userFeignClient.updateUserStatus(agentId, status);
            if (!ok) {
                return Result.error("代理不存在");
            }
            return Result.success(null);
        } catch (Exception e) {
            logger.error("更新代理状态失败", e);
            return Result.error("更新代理状态失败：" + e.getMessage());
        }
    }

    /** 给指定会话分配客服 */
    @PostMapping("/session/{sessionId}/assign")
    public Result<Void> assignAgent(@PathVariable Long sessionId, @RequestBody Map<String, Object> request) {
        try {
            Long agentId = request.get("agentId") != null ?
                    Long.valueOf(request.get("agentId").toString()) : null;
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            session.setAgentId(agentId);
            chatSessionMapper.updateById(session);
            return Result.success(null);
        } catch (Exception e) {
            logger.error("分配代理失败", e);
            return Result.error("分配代理失败：" + e.getMessage());
        }
    }

    /** 将会话转接给其他客服 */
    @PostMapping("/transfer")
    public Result<Void> transferSession(@RequestBody Map<String, Object> request) {
        try {
            Long sessionId = request.get("sessionId") != null ?
                    Long.valueOf(request.get("sessionId").toString()) : null;
            Long toAgentId = request.get("toAgentId") != null ?
                    Long.valueOf(request.get("toAgentId").toString()) : null;
            if (sessionId == null || toAgentId == null) {
                return Result.error("参数不完整");
            }
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            session.setAgentId(toAgentId);
            chatSessionMapper.updateById(session);
            return Result.success(null);
        } catch (Exception e) {
            logger.error("转接会话失败", e);
            return Result.error("转接会话失败：" + e.getMessage());
        }
    }

    /** 获取会话的转接记录（当前为占位实现） */
    @GetMapping("/transfer/{sessionId}/logs")
    public Result<List<Object>> getTransferLogs(@PathVariable Long sessionId) {
        return Result.success(new ArrayList<>());
    }

    /** 将 User 列表转换为客服坐席展示对象列表 */
    private List<Map<String, Object>> toAgentList(List<User> users) {
        List<Map<String, Object>> agents = new ArrayList<>();
        if (users == null) {
            return agents;
        }
        for (User user : users) {
            Map<String, Object> agent = new HashMap<>();
            agent.put("id", user.getId());
            agent.put("agentName", user.getNickname());
            agent.put("agentType", 1);
            agent.put("status", user.getStatus());
            agent.put("createdAt", user.getCreatedAt());
            agents.add(agent);
        }
        return agents;
    }
}
