package com.example.minimall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spring MVC 扩展配置。
 * <p>
 * 自定义 JSON 消息转换器以支持 Java 8 时间类型（{@link LocalDate}、{@link LocalDateTime}、{@link LocalTime}），
 * 同时配置静态资源与上传文件的访问映射。
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.location:uploads}")
    private String uploadLocation;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 配置消息转换器：注册 UTF-8 字符串转换器与支持 Java 8 时间格式的 Jackson 转换器。
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 配置 StringHttpMessageConverter 使用 UTF-8 编码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(0, stringConverter);

        // 配置 MappingJackson2HttpMessageConverter 使用 UTF-8 编码并支持 Java 8 日期时间
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));

        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        jacksonConverter.setObjectMapper(objectMapper);
        converters.add(0, jacksonConverter);
    }

    /**
     * 配置静态资源映射：/images/** 走 classpath 静态目录，/uploads/** 走外部上传目录。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态图片资源映射
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // 配置上传文件映射：使用绝对路径,避免 JVM 工作目录不一致导致 404
        String location = resolveUploadLocation();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * 将 upload.location 解析为 file:... 形式的绝对路径
     * 支持相对/绝对路径,自动 fallback 到常见候选目录,
     * 兼容从项目根目录或 backend/ 目录启动两种场景
     */
    private String resolveUploadLocation() {
        String userDir = System.getProperty("user.dir");
        File userDirFile = new File(userDir);
        File first = new File(uploadLocation);
        if (!first.isAbsolute()) {
            first = new File(userDirFile, uploadLocation);
        }
        String[] candidates = {
            first.getAbsolutePath(),                                              // 配置项解析后的绝对路径
            new File(userDirFile, "backend/" + uploadLocation).getAbsolutePath(),  // user.dir/backend/<upload.location>
            new File(new File(userDirFile, ".."), uploadLocation).getAbsolutePath(),  // 上级目录/<upload.location>
            new File(new File(userDirFile, ".."), "backend/" + uploadLocation).getAbsolutePath() // 上级目录/backend/<upload.location>
        };
        for (String c : candidates) {
            if (c == null) continue;
            File dir = new File(c);
            if (dir.exists() && dir.isDirectory()) {
                String abs = dir.getAbsolutePath();
                if (!abs.endsWith(File.separator)) abs += File.separator;
                return "file:" + abs;
            }
        }
        // 都没找到,使用第一个候选(配置项),保持原行为
        String abs = candidates[0];
        if (!abs.endsWith(File.separator)) abs += File.separator;
        return "file:" + abs;
    }
}