package com.example.minimall;

import com.example.minimall.mapper.ActivityMapper;
import com.example.minimall.mapper.ActivityParticipantMapper;
import com.example.minimall.model.Activity;
import com.example.minimall.service.ActivityService;
import com.example.minimall.utils.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ActivityService.save() 单元测试
 * <p>
 * 覆盖 TODO 1：从 SecurityUtil 取当前 userId 作为 created_by
 * </p>
 */
@DisplayName("ActivityService.save() createdBy 单元测试")
class ActivityServiceTest {

    private ActivityMapper activityMapper;
    private ActivityParticipantMapper activityParticipantMapper;
    private ActivityService activityService;

    @BeforeEach
    void setUp() throws Exception {
        activityMapper = mock(ActivityMapper.class);
        activityParticipantMapper = mock(ActivityParticipantMapper.class);
        activityService = new ActivityService(activityMapper, activityParticipantMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("已登录用户：createdBy 取 SecurityContext 中的 userId")
    void shouldUseCurrentUserIdWhenLoggedIn() {
        // 模拟登录用户 userId = 42
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        42L, null, AuthorityUtils.createAuthorityList("ROLE_USER")));

        // 模拟 mapper.insert 写入成功
        when(activityMapper.insert(any(Activity.class))).thenReturn(1);

        Activity activity = new Activity();
        activity.setTitle("测试活动");
        activityService.save(activity);

        // 捕获传入的 Activity，验证 createdBy
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity saved = captor.getValue();

        assertEquals(42L, saved.getCreatedBy(),
                "已登录用户应将 createdBy 设为 42（从 SecurityContext 读取）");
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("未登录用户：createdBy 兜底为 1L")
    void shouldFallbackTo1WhenNotLoggedIn() {
        SecurityContextHolder.clearContext();

        when(activityMapper.insert(any(Activity.class))).thenReturn(1);

        Activity activity = new Activity();
        activityService.save(activity);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity saved = captor.getValue();

        assertEquals(1L, saved.getCreatedBy(),
                "未登录时应兜底为 1L");
    }

    @Test
    @DisplayName("更新活动：不修改 createdBy，只刷新 updatedAt")
    void shouldNotChangeCreatedByOnUpdate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        99L, null, AuthorityUtils.createAuthorityList("ROLE_USER")));

        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);

        Activity activity = new Activity();
        activity.setId(123L); // 已有 ID → 走更新分支
        activity.setCreatedBy(7L); // 旧值
        activityService.save(activity);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(captor.capture());
        Activity updated = captor.getValue();

        assertEquals(7L, updated.getCreatedBy(),
                "更新时不应修改 createdBy");
        assertNotNull(updated.getUpdatedAt(), "更新时应刷新 updatedAt");
    }
}
