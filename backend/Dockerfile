# ============================================
# Stage 1: Build with Maven (含完整 JDK + Maven)
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先只复制 pom.xml → 利用 Docker 层缓存
# 没改 pom 时这一步直接命中缓存，几秒完成
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码 → 触发增量编译
COPY src ./src
RUN mvn -B -q -DskipTests package

# ============================================
# Stage 2: Runtime - 极简 JRE 镜像
# ============================================
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="minimall" \
      version="0.0.1" \
      description="Spring Boot backend for minmall e-commerce"

WORKDIR /app

# 非 root 用户（安全最佳实践）
RUN groupadd -r app && useradd -r -g app app \
 && mkdir -p /app/data/uploads /app/logs \
 && chown -R app:app /app

# 复制构建产物（精确文件名，匹配 pom.xml 的 artifactId + version）
COPY --from=builder /build/target/minimall-0.0.1-SNAPSHOT.jar app.jar

# 安装 wget（健康检查用，eclipse-temurin 基础镜像默认不带）
USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends wget ca-certificates tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 切回非 root
USER app

# JVM 参数：
#   UseG1GC           - G1 垃圾回收器（低延迟）
#   MaxRAMPercentage   - 容器内存自适应（不用 -Xmx 写死）
#   file.encoding      - 强制 UTF-8（避免容器内中文乱码）
#   security.egd       - 用 urandom 避免启动卡顿
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

# 健康检查（依赖 /actuator/health，需在 application.yml 暴露）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

# sh -c 让 $JAVA_OPTS 环境变量能展开
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
