package com.example.minimall.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 应用上下文静态访问器。
 *
 * <p>在非 Spring 托管的位置（如工具类、静态方法）通过 {@link #getBean(Class)}、
 * {@link #getBean(String)} 获取 Bean，避免反复注入。
 */
@Component
public class AppContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /** 按类型获取 Spring 容器中的 Bean。 */
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /** 按名称获取 Spring 容器中的 Bean。 */
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }
}
