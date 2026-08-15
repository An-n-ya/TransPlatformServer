#!/bin/bash
# =====================================================================
# TransPlatform 服务器端部署脚本（由 GitHub Action 远程调用）
#
# 流程：拉取代码 → 构建镜像 → 启动应用 → 健康检查
# =====================================================================
set -e

APP_DIR="${APP_DIR:-/root/app/TransPlatformServer}"
HEALTH_URL="${HEALTH_URL:-http://localhost:8081/actuator/health}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-180}"   # 秒

echo "============================================="
echo "  TransPlatform 部署开始 $(date '+%F %T')"
echo "============================================="

# ---------- 1. 拉取最新代码 ----------
echo "[1/4] 拉取最新代码..."
cd "${APP_DIR}"
git fetch origin main
git reset --hard origin/main
echo "      当前版本: $(git log --oneline -1)"

# ---------- 2. 构建并启动 ----------
echo "[2/4] Docker 构建并启动..."
cd "${APP_DIR}/scripts"
docker compose up -d --build --remove-orphans app

# ---------- 3. 等待应用就绪 ----------
echo "[3/4] 等待应用就绪（最长 ${WAIT_TIMEOUT}s）..."
elapsed=0
while [ "${elapsed}" -lt "${WAIT_TIMEOUT}" ]; do
    if curl -sf "${HEALTH_URL}" > /dev/null 2>&1; then
        echo "      应用已就绪 ✓"
        break
    fi
    sleep 5
    elapsed=$((elapsed + 5))
done

# ---------- 4. 健康检查 ----------
echo "[4/4] 最终健康检查..."
if curl -sf "${HEALTH_URL}" > /dev/null 2>&1; then
    STATUS=$(curl -s "${HEALTH_URL}")
    echo "      健康状态: ${STATUS}"
    echo ""
    echo "============================================="
    echo "  ✅ 部署成功！"
    echo "============================================="
    exit 0
else
    echo "      ❌ 应用在 ${WAIT_TIMEOUT}s 内未就绪，请查看日志："
    echo "      docker logs trans-app --tail 50"
    echo ""
    echo "============================================="
    echo "  ❌ 部署失败！"
    echo "============================================="
    exit 1
fi
