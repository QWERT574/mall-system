package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger / OpenAPI 文档配置。
 * <p>
 * 基于 Springfox 注册 Docket，扫描 controller 包生成 OpenAPI 3.0 接口文档，
 * 供开发与联调阶段查看与测试后端 API。
 * </p>
 */
@Configuration
public class SwaggerConfig {

    /**
     * 装配 Docket：扫描 controller 包下所有接口生成 OpenAPI 文档。
     *
     * @return Docket 实例
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.minimall.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 构建 API 文档的元信息：标题、描述、联系人与版本号。
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("乡村振兴农产品销售平台 API文档")
                .description("提供农产品管理、订单管理、售后服务、AI助手等功能的API接口")
                .contact(new Contact("MiniMall Team", "", "support@minimall.com"))
                .version("1.0.0")
                .build();
    }
}
