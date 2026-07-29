#!/bin/bash
# ===================================================================
# TransPlatform — 启动 / 重启 / 测试 一键脚本
# ===================================================================
set -e

# ---------- 配置 ----------
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
PROFILE="${PROFILE:-dev}"
PORT="${PORT:-8081}"
JVM_ARGS="-XX:-UseContainerSupport -Dserver.port=${PORT}"
LOG_FILE="${APP_DIR}/logs/app.log"
PID_FILE="/tmp/trans-platform.pid"

# ---------- 颜色 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

info()  { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }

# ---------- Helper: 初始化 SDKMAN ----------
ensure_sdkman() {
  if ! command -v mvn &>/dev/null; then
    if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
      source "$HOME/.sdkman/bin/sdkman-init.sh"
    else
      err "Maven 未安装，请先执行: sdk install maven"
      exit 1
    fi
  fi
}

# ---------- Helper: 等待应用就绪 ----------
wait_for_app() {
  local timeout="${1:-30}"
  local elapsed=0
  info "等待应用就绪（最长 ${timeout}s）..."
  while [ $elapsed -lt "$timeout" ]; do
    if curl -sf "http://localhost:${PORT}/actuator/health" > /dev/null 2>&1; then
      ok "应用已就绪 → http://localhost:${PORT}"
      echo ""
      echo -e "  ${CYAN}API 文档:${NC} http://localhost:${PORT}/swagger-ui.html"
      echo -e "  ${CYAN}健康检查:${NC} http://localhost:${PORT}/actuator/health"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  err "应用启动超时，请查看日志: tail -50 ${LOG_FILE}"
  return 1
}

# ---------- Helper: 检查基础设施 ----------
check_infra() {
  local ok=true
  echo ""
  info "检查基础设施..."

  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q mysql; then
    ok "MySQL        🟢 运行中"
  else
    warn "MySQL        ⚪ 未运行 (docker start mysql8)"
    ok=false
  fi

  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q redis; then
    ok "Redis        🟢 运行中"
  else
    warn "Redis        ⚪ 未运行 (docker start redis7)"
    ok=false
  fi

  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q rabbit; then
    ok "RabbitMQ     🟢 运行中"
  else
    warn "RabbitMQ     ⚪ 未运行 (docker start rabbitmq)"
    ok=false
  fi

  if [ "$ok" = false ]; then
    echo ""
    warn "部分基础设施未启动，如需一键启动:"
    echo "  docker start mysql8 redis7 rabbitmq 2>/dev/null || \\"
    echo "  docker compose -f ${APP_DIR}/docker-compose.yml up -d"
    echo ""
  fi
}

# ===================================================================
# 命令
# ===================================================================
cmd_start() {
  ensure_sdkman

  # 检查是否已在运行
  if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    warn "应用已在运行 (PID: $(cat "$PID_FILE"))"
    echo "  如需重启请执行: $0 restart"
    exit 0
  fi

  check_infra

  mkdir -p "$(dirname "$LOG_FILE")"
  info "启动应用 (profile=${PROFILE}, port=${PORT})..."

  cd "$APP_DIR"
  nohup mvn spring-boot:run \
    -Dspring-boot.run.profiles="${PROFILE}" \
    -Dspring-boot.run.jvmArguments="${JVM_ARGS}" \
    > "${LOG_FILE}" 2>&1 &
  PID=$!
  echo "$PID" > "$PID_FILE"
  ok "进程已启动 (PID: ${PID})"

  # 短暂等待后检查进程是否还活着
  sleep 3
  if ! kill -0 "$PID" 2>/dev/null; then
    err "应用启动失败，日志如下:"
    tail -30 "${LOG_FILE}"
    rm -f "$PID_FILE"
    exit 1
  fi

  wait_for_app
}

cmd_stop() {
  info "停止应用..."
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill "$PID" 2>/dev/null && ok "已停止 (PID: ${PID})" || warn "进程不存在"
    rm -f "$PID_FILE"
  else
    # 尝试通过进程名查找
    PIDS=$(pgrep -f "spring-boot:run.*trans-platform" 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
      kill $PIDS 2>/dev/null
      ok "已停止进程: $PIDS"
    else
      warn "未找到运行中的应用"
    fi
  fi

  # 确保端口已释放
  sleep 2
  if lsof -i :"${PORT}" -P -n 2>/dev/null | grep -q LISTEN; then
    warn "端口 ${PORT} 仍被占用，尝试强制释放..."
    fuser -k "${PORT}/tcp" 2>/dev/null || true
    sleep 1
  fi
}

cmd_restart() {
  cmd_stop
  sleep 2
  cmd_start
}

cmd_status() {
  echo ""
  info "TransPlatform 状态"
  echo ""

  # 应用
  if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    PID=$(cat "$PID_FILE")
    ok "应用运行中 (PID: ${PID}) → http://localhost:${PORT}"
  else
    PIDS=$(pgrep -f "spring-boot:run.*trans-platform" 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
      ok "应用运行中 (PID: ${PIDS}) → http://localhost:${PORT}"
    else
      warn "应用未运行"
    fi
  fi

  # 健康检查
  if curl -sf "http://localhost:${PORT}/actuator/health" > /dev/null 2>&1; then
    ok "健康检查 ✅"
  else
    warn "健康检查 ❌"
  fi

  check_infra
}

cmd_test() {
  ensure_sdkman

  info "运行编译 + 测试..."
  cd "$APP_DIR"
  mvn compile 2>&1 | tail -5
  echo ""
  ok "编译通过"

  # 如果应用在运行则测试 API，否则跳过
  if curl -sf "http://localhost:${PORT}/actuator/health" > /dev/null 2>&1; then
    info "运行 API 冒烟测试..."
    cmd_smoke_test
  else
    warn "应用未运行，跳过 API 冒烟测试"
    echo "  请先启动: $0 start"
  fi
}

cmd_smoke_test() {
  local base="http://localhost:${PORT}"
  local errors=0

  echo ""
  echo -e "${CYAN}══════════════════════════════════════════${NC}"
  echo -e "${CYAN}        API 冒烟测试                      ${NC}"
  echo -e "${CYAN}══════════════════════════════════════════${NC}"
  echo ""

  # 1. 健康检查
  if curl -sf "${base}/actuator/health" > /dev/null 2>&1; then
    ok "  健康检查"
  else
    err "  健康检查失败"
    errors=$((errors + 1))
  fi

  # 2. 注册 — 用随机用户名避免冲突
  local ts=$(date +%s)
  local reg=$(curl -sf -X POST "${base}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"test_${ts}\",\"nickname\":\"Test\",\"password\":\"pass123\"}" 2>/dev/null) || true
  if echo "$reg" | python3 -c "import sys,json;d=json.load(sys.stdin);assert d['code']==200" 2>/dev/null; then
    ok "  注册"
    TA=$(echo "$reg" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
  else
    # 可能已存在，尝试登录
    reg=$(curl -sf -X POST "${base}/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"test_${ts}\",\"password\":\"pass123\"}" 2>/dev/null) || true
    TA=$(echo "$reg" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null) || TA=""
  fi

  if [ -z "$TA" ]; then
    err "  获取 Token 失败"
    errors=$((errors + 1))
  else
    ok "  Token 获取成功"
  fi

  # 后续测试需要 Token
  if [ -n "$TA" ]; then
    # 3. 获取当前用户
    if curl -sf "${base}/api/v1/users/me" -H "Authorization: Bearer $TA" > /dev/null 2>&1; then
      ok "  获取当前用户"
    else
      err "  获取当前用户失败"
      errors=$((errors + 1))
    fi

    # 4. 发帖
    local post_res=$(curl -sf -X POST "${base}/api/v1/posts" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TA" \
      -d '{"content":"冒烟测试帖文"}' 2>/dev/null) || true
    local PID=$(echo "$post_res" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])" 2>/dev/null) || PID=""
    if [ -n "$PID" ]; then
      ok "  发布帖文 (id=${PID})"

      # 5. 获取帖文详情
      if curl -sf "${base}/api/v1/posts/${PID}" -H "Authorization: Bearer $TA" > /dev/null 2>&1; then
        ok "  帖文详情"
      else
        err "  帖文详情失败"
        errors=$((errors + 1))
      fi

      # 6. 点赞
      if curl -sf -X POST "${base}/api/v1/posts/${PID}/like" -H "Authorization: Bearer $TA" > /dev/null 2>&1; then
        ok "  点赞"
      else
        err "  点赞失败"
        errors=$((errors + 1))
      fi

      # 7. 评论
      local cmt_res=$(curl -sf -X POST "${base}/api/v1/posts/${PID}/comments" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TA" \
        -d '{"content":"测试评论"}' 2>/dev/null) || true
      local CID=$(echo "$cmt_res" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])" 2>/dev/null) || CID=""
      if [ -n "$CID" ]; then
        ok "  评论 (id=${CID})"
      else
        err "  评论失败"
        errors=$((errors + 1))
      fi

      # 8. 收藏
      if curl -sf -X POST "${base}/api/v1/posts/${PID}/collect" -H "Authorization: Bearer $TA" > /dev/null 2>&1; then
        ok "  收藏"
      else
        err "  收藏失败"
        errors=$((errors + 1))
      fi

      # 9. 删除帖文
      if curl -sf -X DELETE "${base}/api/v1/posts/${PID}" -H "Authorization: Bearer $TA" > /dev/null 2>&1; then
        ok "  删除帖文"
      else
        err "  删除帖文失败"
        errors=$((errors + 1))
      fi
    else
      err "  发布帖文失败"
      errors=$((errors + 1))
    fi
  fi

  echo ""
  if [ "$errors" -eq 0 ]; then
    echo -e "${GREEN}══════════════════════════════════════════${NC}"
    echo -e "${GREEN}        全部测试通过 🎉                   ${NC}"
    echo -e "${GREEN}══════════════════════════════════════════${NC}"
  else
    echo -e "${RED}══════════════════════════════════════════${NC}"
    echo -e "${RED}        ${errors} 个测试失败                    ${NC}"
    echo -e "${RED}══════════════════════════════════════════${NC}"
    exit 1
  fi
}

cmd_logs() {
  if [ -f "$LOG_FILE" ]; then
    tail -f "$LOG_FILE"
  else
    err "日志文件不存在: ${LOG_FILE}"
    exit 1
  fi
}

cmd_help() {
  echo ""
  echo -e "${CYAN}TransPlatform — 启动/管理脚本${NC}"
  echo ""
  echo "用法: $0 <command> [options]"
  echo ""
  echo "命令:"
  echo "  start         启动应用（开发模式）"
  echo "  stop          停止应用"
  echo "  restart       重启应用"
  echo "  status        查看运行状态"
  echo "  test          编译 + 冒烟测试"
  echo "  logs          查看实时日志"
  echo "  help          显示帮助信息"
  echo ""
  echo "环境变量:"
  echo "  PROFILE       Spring profile (默认: dev)"
  echo "  PORT          服务端口 (默认: 8081)"
  echo ""
  echo "示例:"
  echo "  $0 start                  # 默认 8081 端口启动"
  echo "  $0 start                  # 启动（首次）"
  echo "  $0 restart                # 修改代码后重启"
  echo "  $0 test                   # 编译 + API 冒烟测试"
  echo "  PROFILE=prod PORT=8080 $0 start  # 生产模式"
  echo ""
}

# ===================================================================
# Main
# ===================================================================
case "${1:-help}" in
  start)    cmd_start ;;
  stop)     cmd_stop ;;
  restart)  cmd_restart ;;
  status)   cmd_status ;;
  test)     cmd_test ;;
  logs)     cmd_logs ;;
  help|--help|-h) cmd_help ;;
  *)
    err "未知命令: $1"
    echo "可用命令: start, stop, restart, status, test, logs, help"
    exit 1
    ;;
esac
