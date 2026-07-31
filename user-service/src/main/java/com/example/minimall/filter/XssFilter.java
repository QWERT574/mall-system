package com.example.minimall.filter;

import com.example.minimall.security.XssRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XSS 防护过滤器。
 * <p>
 * 仅对 /api/** 路径生效：使用 {@link XssRequestWrapper} 包装 HttpServletRequest，
 * 在请求参数被读取时过滤其中的 XSS 攻击载荷，避免恶意脚本入库或被回显到前端。
 * </p>
 */
@Component
@Order(1)
public class XssFilter implements Filter {

    /**
     * 过滤器初始化钩子，当前无自定义资源加载逻辑。
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * 仅对 /api/** 走 XSS 包装，其他路径原样放行。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.contains("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        XssRequestWrapper xssRequest = new XssRequestWrapper(httpRequest);
        chain.doFilter(xssRequest, response);
    }

    /**
     * 过滤器销毁钩子，当前无需释放资源。
     */
    @Override
    public void destroy() {
    }
}
