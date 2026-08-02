# ============================================
# 单体后端构建（含 common 模块依赖）
# 通过 git 获取 common 模块，不依赖构建上下文
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 安装 git（用于获取 common 模块）
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

# 从仓库获取 common 模块（只拉 common 目录，节省时间）
RUN git clone --depth 1 --filter=blob:none --sparse https://github.com/QWERT574/mall-system.git /tmp/repo \
 && cd /tmp/repo && git sparse-checkout set common \
 && cp -r /tmp/repo/common /build/common \
 && rm -rf /tmp/repo

# 先构建 common 安装到本地 Maven 仓库
COPY pom.xml ./backend/pom.xml
RUN mvn -B -q -f backend/pom.xml dependency:resolve || true
RUN mvn -B -q -f /build/common/pom.xml -DskipTests install

# 再构建 backend
COPY src ./backend/src
RUN mvn -B -q -f backend/pom.xml -DskipTests package

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd -r app && useradd -r -g app app \
 && mkdir -p /app/data/uploads /app/logs \
 && chown -R app:app /app

COPY --from=builder /build/backend/target/minimall-0.0.1-SNAPSHOT.jar app.jar

USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends wget ca-certificates tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

USER app

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
