# =====================================================================
# TransPlatform — 多阶段构建 Dockerfile
#
# 用法（配合 docker-compose.yml 一键启动）:
#   cd scripts && docker compose up -d
#
# 单独构建镜像:
#   cd scripts && docker build -t trans-platform:1.0.0 .
# =====================================================================

# ---------- Stage 1: 构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom.xml 并预下载依赖，利用 Docker 层缓存加速重复构建
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 拷贝源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 2: 运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 从构建阶段复制产物
COPY --from=build /build/target/trans-platform-*.jar app.jar

# 入口脚本：修复上传目录权限后降权运行
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# 运行用户（非 root，更安全）
RUN useradd -r -u 1001 appuser && chown -R appuser:appuser /app

EXPOSE 8081

# JVM 参数可通过环境变量覆盖（如 -XX:-UseContainerSupport）
ENV JAVA_OPTS="-XX:-UseContainerSupport -Dserver.port=8081"

# 入口以 root 启动，修复权限后降权为 appuser 运行
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
