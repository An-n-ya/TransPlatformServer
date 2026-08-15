#!/bin/sh
# =====================================================================
# TransPlatform 容器入口脚本
#
# 解决宿主 bind mount 目录（/app/uploads）所有权不匹配问题：
#   1. 以 root 身份启动
#   2. 修复上传目录权限（宿主目录可能是 root 所有）
#   3. 降权为 appuser 运行应用
# =====================================================================
set -e

echo "[entrypoint] 初始化数据目录权限..."
mkdir -p /app/uploads /app/data
chown -R appuser:appuser /app/uploads /app/data

# 指定 Java 完整路径（su 切换用户后 PATH 可能不含 java）
JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"

echo "[entrypoint] 以 appuser 启动应用..."
exec su -s /bin/sh appuser -c "exec $JAVA_BIN $JAVA_OPTS -jar /app/app.jar"
