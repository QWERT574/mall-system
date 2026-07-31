package com.example.minimall;

import com.example.minimall.config.TraceIdFilter;
import com.example.minimall.controller.OrderController;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * order-service 上下文加载冒烟测试 — 不依赖 MySQL/Redis/Nacos/RocketMQ/Seata 真实环境。
 *
 * <p>目的：微服务主线此前没有任何机械验证层，本测试保证
 * 「配置文件合法 + 全部 Bean 可装配」这一最低回归边界在 CI 中可被自动拦截。
 *
 * <p>环境替身：
 * <ul>
 *   <li>数据库 → H2 内存库（MySQL 兼容模式），仅验证装配，不执行业务 SQL</li>
 *   <li>RocketMQTemplate → MockBean（避免 producer 启动期连接 NameServer）</li>
 *   <li>Nacos 注册/配置、Seata、Sentinel → 属性开关关闭</li>
 * </ul>
 */
@SpringBootTest(properties = {
        // H2 内存库替代 MySQL（仅上下文装配，不建表）
        "spring.datasource.url=jdbc:h2:mem:minimall-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // 不连 Nacos（注册中心 + 配置中心）
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        // 不连 Seata TC / Sentinel Dashboard
        "seata.enabled=false",
        "spring.cloud.sentinel.enabled=false",
        // application.yml 中 jwt.secret 无默认值，测试提供占位密钥
        "jwt.secret=unit-test-secret-0123456789-0123456789"
})
class OrderServiceContextSmokeTest {

    /** 挡掉 RocketMQ producer 的启动期网络连接 */
    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ApplicationContext context;

    /** 上下文可加载，核心 Web 入口 Bean 装配成功 */
    @Test
    void contextLoadsWithCoreBeans() {
        assertThat(context.getBean(OrderController.class)).isNotNull();
    }

    /** 链路追踪过滤器在位，且头名契约与网关侧 TraceIdGlobalFilter 一致 */
    @Test
    void traceIdFilterPresentWithStableHeaderContract() {
        assertThat(context.getBean(TraceIdFilter.class)).isNotNull();
        assertThat(TraceIdFilter.HEADER_TRACE_ID).isEqualTo("X-Trace-Id");
    }
}
