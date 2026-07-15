package com.example.minimall.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Result 包装器单元测试。
 * 项目内 80+ 处 Controller 均依赖此类的语义, 锁定关键行为以防回归。
 */
class ResultTest {

    @Test
    void success_shouldReturnCodeZeroWithData() {
        Result<String> r = Result.success("payload");

        assertEquals(0, r.getCode());
        assertEquals("success", r.getMessage());
        assertEquals("payload", r.getData());
    }

    @Test
    void error_withMessage_shouldReturnCodeOne() {
        Result<Object> r = Result.error("boom");

        assertEquals(1, r.getCode());
        assertEquals("boom", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void error_withCodeAndMessage_shouldPreserveCode() {
        Result<Object> r = Result.error(404, "not found");

        assertEquals(404, r.getCode());
        assertEquals("not found", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void noArgsConstructor_shouldProduceSentinel() {
        Result<Object> r = new Result<>();

        // 三个字段均为 null, 序列化/反序列化场景需要
        assertNull(r.getCode());
        assertNull(r.getMessage());
        assertNull(r.getData());
    }
}
