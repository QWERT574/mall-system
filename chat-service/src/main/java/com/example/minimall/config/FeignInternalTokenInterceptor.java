package com.example.minimall.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feign 全局拦截器 — 为所有服务间调用自动附加 X-Internal-Token。
 *
 * <p>与提供方的 InternalTokenFilter 配对使用：/api/internal/** 接口只信任
 * 携带正确内部令牌的请求，防止外部直连微服务端口伪造内部调用。
 * 令牌通过环境变量 INTERNAL_TOKEN 注入，所有微服务共享同一值。
 */
@Component
public class FeignInternalTokenInterceptor implements RequestInterceptor {

    @Value("${internal.token:minimall-internal-token-dev}")
    private String internalToken;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Token", internalToken);
    }
}
