package com.example.minimall.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceIdFilter 单元测试。
 * 验证: 1) 优先使用上游头 2) 无头时生成新 ID 3) 写回响应头 4) finally 清理 MDC
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        // 防止污染后续测试
        MDC.clear();
    }

    @Test
    void should_use_header_trace_id_when_provided() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(TraceIdFilter.HEADER_TRACE_ID, "abc-123-from-upstream");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicMdcCapturingChain chain = new AtomicMdcCapturingChain();
        filter.doFilter(req, resp, chain);

        // 响应头回写
        assertEquals("abc-123-from-upstream", resp.getHeader(TraceIdFilter.HEADER_TRACE_ID));
        // 链路中能看到 traceId
        assertEquals("abc-123-from-upstream", chain.capturedTraceId);
        // 结束后 MDC 已被清理
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID));
    }

    @Test
    void should_generate_trace_id_when_header_missing() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicMdcCapturingChain chain = new AtomicMdcCapturingChain();
        filter.doFilter(req, resp, chain);

        String generated = resp.getHeader(TraceIdFilter.HEADER_TRACE_ID);
        assertNotNull(generated);
        // 32 字符十六进制 UUID
        assertEquals(32, generated.length());
        assertTrue(generated.matches("[0-9a-f]{32}"));
        assertEquals(generated, chain.capturedTraceId);
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID));
    }

    @Test
    void should_clear_mdc_even_when_chain_throws() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        try {
            filter.doFilter(req, resp, (request, response) -> {
                // 模拟下游抛异常, 验证 finally 仍清理 MDC
                throw new RuntimeException("boom");
            });
        } catch (Exception ignored) {
            // 期望异常被抛出
        }

        // 关键: 即便链路异常, MDC 也必须清理, 否则 ThreadLocal 泄漏
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID));
    }

    /**
     * 用于捕获链路执行时 MDC 中的 traceId。
     */
    private static class AtomicMdcCapturingChain implements FilterChain {
        volatile String capturedTraceId;

        @Override
        public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
                throws IOException, ServletException {
            this.capturedTraceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
        }
    }
}
