package com.example.minimall;

import com.example.minimall.utils.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityUtil 单元测试
 * <p>
 * 覆盖场景：
 * <ol>
 *   <li>未登录（无 Authentication）</li>
 *   <li>匿名用户（anonymousUser）</li>
 *   <li>普通用户（Long userId）</li>
 *   <li>数字字符串</li>
 *   <li>非数字字符串</li>
 * </ol>
 * </p>
 */
@DisplayName("SecurityUtil.getCurrentUserId() 单元测试")
class SecurityUtilTest {

    @AfterEach
    void tearDown() {
        // 每个测试后清理上下文，避免污染
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("未登录时返回 null")
    void shouldReturnNullWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertNull(SecurityUtil.getCurrentUserId());
        assertFalse(SecurityUtil.isAuthenticated());
    }

    @Test
    @DisplayName("匿名用户返回 null")
    void shouldReturnNullForAnonymousUser() {
        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertNull(SecurityUtil.getCurrentUserId());
        assertFalse(SecurityUtil.isAuthenticated());
    }

    @Test
    @DisplayName("JwtAuthenticationFilter 写入的 Long userId 正确读取")
    void shouldReturnLongUserId() {
        // 模拟 JwtAuthenticationFilter 的写入：principal = userId (Long)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                1001L, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(1001L, SecurityUtil.getCurrentUserId());
        assertTrue(SecurityUtil.isAuthenticated());
    }

    @Test
    @DisplayName("数字字符串 principal 也能解析为 Long")
    void shouldParseNumericString() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "8888", null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(8888L, SecurityUtil.getCurrentUserId());
    }

    @Test
    @DisplayName("非数字字符串 principal 返回 null")
    void shouldReturnNullForNonNumericString() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "openid_abc", null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertNull(SecurityUtil.getCurrentUserId());
    }

    @Test
    @DisplayName("isAuthenticated 与 getCurrentUserId 行为一致")
    void isAuthenticatedShouldConsistent() {
        // 未登录
        SecurityContextHolder.clearContext();
        assertFalse(SecurityUtil.isAuthenticated());

        // 已登录
        Authentication auth = new UsernamePasswordAuthenticationToken(
                1L, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(SecurityUtil.isAuthenticated());
    }
}
