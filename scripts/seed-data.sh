#!/usr/bin/env bash
# =====================================================================
# TransPlatform — 测试数据一键注入脚本（可重复运行，不报错）
#
# 功能:
#  1. 确保 alice / bob / charlie 三个测试账号存在（不存在才创建）
#  2. 三人互相关注（6 条关注关系）
#  3. 通过 REST API 注入尽可能多的测试数据:
#     话题 / 帖文 / 点赞 / 评论 / 回复 / 收藏 / 置顶帖 /
#     头像与背景图上传 / Feed / 搜索 / 通知 / Token 刷新 / 邀请码 ...
#
# 说明:
#  - 注册接口强制密码 ≥ 6 位（400: 密码长度 6-100 个字符），因此 "pass"
#    无法通过 /auth/register 创建用户。本脚本仅"创建用户"这一步通过
#    SQLite 直插（bcrypt 加密）实现，其余全部调用后端 REST API。
#  - 邮箱接口（绑定邮箱/找回密码）依赖 RESEND_API_KEY 且发送有 60s
#    冷却，无法保证可重复运行，故默认跳过。
#
# 用法:
#   ./scripts/seed-data.sh                        # 默认 http://localhost:8081
#   BASE_URL=http://localhost:8082 ./scripts/seed-data.sh
#   PASSWORD=pass123456 ./scripts/seed-data.sh    # 自定义测试密码
#
# 依赖: curl, jq, sqlite3, python3(含 bcrypt 模块)
#
# 约定: 日志（info/ok/warn/err）全部输出到 stderr；
#        stdout 只用于输出"数据"（响应体等），便于命令替换捕获。
# =====================================================================

# ---------- 路径解析（兼容 scripts/ 子目录或项目根目录调用） ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ "$(basename "$SCRIPT_DIR")" = "scripts" ]; then
    APP_DIR="$(dirname "$SCRIPT_DIR")"
else
    APP_DIR="$SCRIPT_DIR"
fi

# ---------- 配置 ----------
BASE_URL="${BASE_URL:-http://localhost:8081}"
PASSWORD="${PASSWORD:-pass}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
DB_FILE="${APP_DIR}/data/trans_platform.db"
TMP_IMG="${APP_DIR}/logs/seed_test.png"

# ---------- 颜色与日志（全部 → stderr） ----------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info() { echo -e "${CYAN}[INFO]${NC}  $1" >&2; }
ok()   { echo -e "${GREEN}[OK]${NC}    $1" >&2; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $1" >&2; }
err()  { echo -e "${RED}[ERROR]${NC} $1" >&2; }
hr()   { echo -e "${CYAN}──────────────────────────────────────────────${NC}" >&2; }

# ---------- HTTP 辅助 ----------
API_CODE=0   # HTTP 状态码（api/api_form 调用后可用）
API_BODY=""  # 响应体（api/api_form 调用后可用）

# api METHOD PATH [JSON_BODY] [TOKEN]
api() {
    local method="$1" path="$2" body="${3:-}" token="${4:-}"
    local tmp code_file
    tmp=$(mktemp); code_file=$(mktemp)
    local args=(-sS -X "$method" "${BASE_URL}${path}" -o "$tmp" -w '%{http_code}')
    [ -n "$token" ] && args+=(-H "Authorization: Bearer ${token}")
    if [ -n "$body" ]; then
        args+=(-H "Content-Type: application/json" -d "$body")
    fi
    curl "${args[@]}" >"$code_file" 2>/dev/null || true
    API_CODE=$(cat "$code_file")
    API_BODY=$(cat "$tmp")
    rm -f "$tmp" "$code_file"
}

# api_form METHOD PATH TOKEN "field=value" "field=@file;type=image/png" ...
api_form() {
    local method="$1" path="$2" token="$3"; shift 3
    local tmp code_file
    tmp=$(mktemp); code_file=$(mktemp)
    local args=(-sS -X "$method" "${BASE_URL}${path}" -o "$tmp" -w '%{http_code}')
    [ -n "$token" ] && args+=(-H "Authorization: Bearer ${token}")
    local f
    for f in "$@"; do args+=(-F "$f"); done
    curl "${args[@]}" >"$code_file" 2>/dev/null || true
    API_CODE=$(cat "$code_file")
    API_BODY=$(cat "$tmp")
    rm -f "$tmp" "$code_file"
}

jq_get() { jq -r "$1 // empty" 2>/dev/null; }
msg() { jq -r '.message // empty' 2>/dev/null; }

# 校验上一步 API 调用，非 200 输出警告（不中断）
check() { # label
    if [ "$API_CODE" = "200" ]; then
        ok "  $1"
    else
        warn "  $1 (HTTP ${API_CODE})"
    fi
}

# ---------- 依赖与前置检查 ----------
need() { command -v "$1" >/dev/null 2>&1 || { err "缺少依赖: $1，请先安装"; exit 1; }; }
need curl; need jq; need sqlite3; need python3
python3 -c "import bcrypt" 2>/dev/null || { err "缺少 python3 bcrypt 模块，请执行: pip install bcrypt"; exit 1; }

info "目标: ${BASE_URL}"
if ! curl -sf "${BASE_URL}/actuator/health" >/dev/null 2>&1; then
    err "应用未就绪，请先启动: ./scripts/run.sh start"
    exit 1
fi
ok "应用健康检查通过"

# =====================================================================
# 1. 确保测试账号存在（SQLite 直插 + bcrypt，仅创建缺失账号）
# =====================================================================
hr
info "步骤 1/9: 确保测试账号 alice / bob / charlie 存在（密码: ${PASSWORD}）"

seed_user() { # username nickname role
    local username="$1" nickname="$2" role="$3"
    local hash inserted
    hash=$(PASSWORD_HASH="$PASSWORD" python3 -c 'import os,bcrypt;print(bcrypt.hashpw(os.environ["PASSWORD_HASH"].encode(), bcrypt.gensalt(rounds=10)).decode())')
    inserted=$(sqlite3 "$DB_FILE" \
        "INSERT OR IGNORE INTO users (username, nickname, password, role, status) \
         SELECT '${username}','${nickname}','${hash}','${role}',1; \
         SELECT changes();" 2>/dev/null) || { err "写入 SQLite 失败: ${DB_FILE}"; exit 1; }
    if [ "$inserted" = "1" ]; then
        ok "  已创建 ${username}（密码: ${PASSWORD}）"
    else
        ok "  ${username} 已存在，跳过创建"
    fi
}

seed_user "$ADMIN_USERNAME" "管理员" "admin"
seed_user "alice"   "Alice"   "user"
seed_user "bob"     "Bob"     "user"
seed_user "charlie" "Charlie" "user"

# =====================================================================
# 2. 登录三个账号
# =====================================================================
hr
info "步骤 2/9: 登录 alice / bob / charlie"

login_user() { # username → 输出响应体（stdout）；失败则退出
    local username="$1"
    api POST "/api/v1/auth/login" "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}"
    if [ "$API_CODE" != "200" ]; then
        err "登录 ${username} 失败 (HTTP ${API_CODE}): $(echo "$API_BODY" | msg)"
        exit 1
    fi
    echo "$API_BODY"
}

ALICE=$(login_user alice)
ALICE_TOKEN=$(echo "$ALICE" | jq_get '.data.accessToken')
ALICE_REFRESH=$(echo "$ALICE" | jq_get '.data.refreshToken')
ALICE_ID=$(echo "$ALICE" | jq_get '.data.user.id')
ok "  alice  登录成功 (id=${ALICE_ID})"

BOB=$(login_user bob)
BOB_TOKEN=$(echo "$BOB" | jq_get '.data.accessToken')
BOB_ID=$(echo "$BOB" | jq_get '.data.user.id')
ok "  bob    登录成功 (id=${BOB_ID})"

CHARLIE=$(login_user charlie)
CHARLIE_TOKEN=$(echo "$CHARLIE" | jq_get '.data.accessToken')
CHARLIE_ID=$(echo "$CHARLIE" | jq_get '.data.user.id')
ok "  charlie 登录成功 (id=${CHARLIE_ID})"

# =====================================================================
# 3. 管理员生成邀请码（尽力而为，失败不中断）
# =====================================================================
hr
info "步骤 3/9: 管理员生成邀请码（验证 admin 接口）"

api POST "/api/v1/auth/login" "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}"
if [ "$API_CODE" = "200" ]; then
    ADMIN_TOKEN=$(echo "$API_BODY" | jq_get '.data.accessToken')
    api POST "/api/v1/invitations" '{"count":1,"days":7,"scene":"seed"}' "$ADMIN_TOKEN"
    if [ "$API_CODE" = "200" ]; then
        INV_CODE=$(echo "$API_BODY" | jq_get '.data[0].code')
        ok "  已生成邀请码: ${INV_CODE}（scene=seed）"
    else
        warn "  生成邀请码失败 (HTTP ${API_CODE})，跳过"
    fi
else
    warn "  ${ADMIN_USERNAME} 登录失败（HTTP ${API_CODE}），跳过邀请码接口"
fi

# =====================================================================
# 4. 话题（不存在才创建）
# =====================================================================
hr
info "步骤 4/9: 创建话题 旅行 / 美食 / 编程"

ensure_topic() { # name desc → stdout 输出话题 id
    local name="$1" desc="$2" tid
    api GET "/api/v1/topics?size=100"
    tid=$(echo "$API_BODY" | jq -r --arg n "$name" '.data.content[] | select(.name == $n) | .id' 2>/dev/null | head -1)
    if [ -n "$tid" ]; then
        echo "  ⏭  话题「${name}」已存在 (id=${tid})" >&2
        echo "$tid"
        return 0
    fi
    # 话题写入接口需要登录（anyRequest().authenticated()）
    api POST "/api/v1/topics" "$(jq -nc --arg n "$name" --arg d "$desc" '{name:$n, description:$d}')" "$ALICE_TOKEN"
    tid=$(echo "$API_BODY" | jq_get '.data.id')
    if [ -n "$tid" ]; then
        echo "  ✓ 已创建话题「${name}」 (id=${tid})" >&2
        echo "$tid"
    else
        echo "  ⚠ 创建话题「${name}」失败 (HTTP ${API_CODE})" >&2
        echo ""
    fi
}

TOPIC_TRAVEL=$(ensure_topic "旅行" "旅行攻略、游记、路线分享")
TOPIC_FOOD=$(ensure_topic   "美食" "美食探店、菜谱分享")
TOPIC_CODE=$(ensure_topic   "编程" "技术交流、开源项目")

# =====================================================================
# 5. 资料更新 + 图片上传（JSON 与 multipart 两种方式）
# =====================================================================
hr
info "步骤 5/9: 更新资料 / 上传头像与主页背景图"

# 5.1 生成一张 4x4 的测试 PNG
python3 - <<'PYEOF' > "$TMP_IMG"
import struct, zlib
def chunk(t, d):
    c = t + d
    return struct.pack('>I', len(d)) + c + struct.pack('>I', zlib.crc32(c))
w = h = 4
ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
raw = b''.join(b'\x00' + b'\xff\x00\x00' * w for _ in range(h))
open('/dev/stdout', 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))
PYEOF
[ -s "$TMP_IMG" ] && ok "  已生成测试图片 ${TMP_IMG}" || { err "生成测试图片失败"; exit 1; }

# 5.2 POST /upload/image — 单独上传接口
api_form POST "/api/v1/upload/image" "$ALICE_TOKEN" "file=@${TMP_IMG};type=image/png"
UPLOAD_URL=$(echo "$API_BODY" | jq_get '.data.url')
check "POST /upload/image 上传图片"
[ -n "$UPLOAD_URL" ] && ok "    图片 URL: ${UPLOAD_URL}"

# 5.3 PUT /me (JSON) — 更新资料
api PUT "/api/v1/users/me" \
    "$(jq -nc --arg b '热爱旅行与美食的测试账号' --arg a "${UPLOAD_URL:-http://localhost:8081/uploads/seed.png}" '{bio:$b, avatar:$a}')" \
    "$ALICE_TOKEN"
check "PUT /users/me (JSON) — alice 更新 bio/avatar"

api PUT "/api/v1/users/me" "$(jq -nc --arg b '后端开发，喜欢开源' '{bio:$b}')" "$BOB_TOKEN"
check "PUT /users/me (JSON) — bob 更新 bio"

api PUT "/api/v1/users/me" "$(jq -nc --arg b '前端爱好者，摄影萌新' '{bio:$b}')" "$CHARLIE_TOKEN"
check "PUT /users/me (JSON) — charlie 更新 bio"

# 5.4 PUT /me (multipart) — 上传头像 + 主页背景图（bioHeaderImg 文件上传）
api_form PUT "/api/v1/users/me" "$ALICE_TOKEN" \
    "nickname=Alice" \
    "avatar=@${TMP_IMG};type=image/png" \
    "bioHeaderImg=@${TMP_IMG};type=image/png"
check "PUT /users/me (multipart) — alice 上传头像 + 主页背景图"

# =====================================================================
# 6. 互相关注（先查已关注列表，只补缺失的关注关系）
# =====================================================================
hr
info "步骤 6/9: 三人互相关注"

get_followee_ids() { # token userId → stdout 输出已关注 id 列表
    api GET "/api/v1/users/$2/followees?size=100" "" "$1"
    echo "$API_BODY" | jq -r '.data.content[].id' 2>/dev/null
}

ALICE_FOLLOWEES=$(get_followee_ids "$ALICE_TOKEN" "$ALICE_ID")
BOB_FOLLOWEES=$(get_followee_ids "$BOB_TOKEN" "$BOB_ID")
CHARLIE_FOLLOWEES=$(get_followee_ids "$CHARLIE_TOKEN" "$CHARLIE_ID")

ensure_follow() { # token followeeId existing_list label
    local token="$1" followee_id="$2" existing="$3" label="$4"
    if echo "$existing" | grep -qx "$followee_id"; then
        echo "  ⏭  ${label} 已关注，跳过" >&2
        return 0
    fi
    api POST "/api/v1/users/${followee_id}/follow" "" "$token"
    if [ "$API_CODE" = "200" ]; then
        ok "  ${label}"
    else
        warn "  ${label} 失败 (HTTP ${API_CODE})"
    fi
}

ensure_follow "$ALICE_TOKEN"   "$BOB_ID"     "$ALICE_FOLLOWEES"   "alice → bob"
ensure_follow "$ALICE_TOKEN"   "$CHARLIE_ID" "$ALICE_FOLLOWEES"   "alice → charlie"
ensure_follow "$BOB_TOKEN"     "$ALICE_ID"   "$BOB_FOLLOWEES"     "bob → alice"
ensure_follow "$BOB_TOKEN"     "$CHARLIE_ID" "$BOB_FOLLOWEES"     "bob → charlie"
ensure_follow "$CHARLIE_TOKEN" "$ALICE_ID"   "$CHARLIE_FOLLOWEES" "charlie → alice"
ensure_follow "$CHARLIE_TOKEN" "$BOB_ID"     "$CHARLIE_FOLLOWEES" "charlie → bob"

# 6.2 取关再关注（验证 DELETE /users/{id}/follow 与重新关注路径）
api DELETE "/api/v1/users/${ALICE_ID}/follow" "" "$BOB_TOKEN"
if [ "$API_CODE" = "200" ]; then
    ok "  bob 取关 alice（DELETE /users/{id}/follow）"
    api POST "/api/v1/users/${ALICE_ID}/follow" "" "$BOB_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  bob 重新关注 alice（re-follow 路径）" || warn "  bob 重新关注 alice 失败 (HTTP ${API_CODE})"
else
    warn "  bob 取关 alice 失败 (HTTP ${API_CODE})，跳过重新关注"
fi

# 等待 RabbitMQ 异步消费关注事件，Feed 列表生效
sleep 3

# =====================================================================
# 7. 发布帖文（JSON + multipart 两种方式）
# =====================================================================
hr
info "步骤 7/9: 发布帖文"

create_post() { # token content topicIdsJson [location] → stdout 输出帖文 id
    local token="$1" content="$2" topic_ids="$3" location="${4:-}"
    api POST "/api/v1/posts" \
        "$(jq -nc --arg c "$content" --argjson t "$topic_ids" --arg l "$location" '{content:$c, topicIds:$t, location:$l}')" \
        "$token"
    echo "$API_BODY" | jq_get '.data.id'
}

POST_A1=$(create_post "$ALICE_TOKEN"   "大家好，我是 Alice！今天天气真好，出去走走～" "[${TOPIC_TRAVEL}]" "北京")
POST_A2=$(create_post "$ALICE_TOKEN"   "周末去爬山，有一起的吗？#旅行 #户外" "[${TOPIC_TRAVEL}]")
POST_B1=$(create_post "$BOB_TOKEN"     "今天尝试了新菜谱，番茄牛腩炖得超级烂～" "[${TOPIC_FOOD}]")
POST_B2=$(create_post "$BOB_TOKEN"     "最近在学 Spring Boot 3 虚拟线程，分享几个踩坑记录" "[${TOPIC_CODE}]")
POST_C1=$(create_post "$CHARLIE_TOKEN" "想找人一起维护一个开源小项目，有兴趣的评论～" "[${TOPIC_CODE}]")
POST_C2=$(create_post "$CHARLIE_TOKEN" "城市夜骑路线推荐：江边绿道，全程 20km" "[${TOPIC_TRAVEL}]")
ok "  6 篇 JSON 帖文已发布 (a1=${POST_A1} b1=${POST_B1} c1=${POST_C1} ...)"

# multipart 帖文（带图片）
api_form POST "/api/v1/posts" "$ALICE_TOKEN" \
    "content=带图测试帖（multipart 图片上传）" \
    "location=杭州" \
    "topicIds=${TOPIC_TRAVEL}" \
    "images=@${TMP_IMG};type=image/png"
POST_MULTI=$(echo "$API_BODY" | jq_get '.data.id')
[ -n "$POST_MULTI" ] && ok "  multipart 帖文已发布 (id=${POST_MULTI})" || warn "  multipart 帖文失败 (HTTP ${API_CODE})"

# 临时帖文 — 用于验证 DELETE /posts/{id}
api POST "/api/v1/posts" "$(jq -nc --arg c '临时帖文，马上删除' '{content:$c}')" "$ALICE_TOKEN"
POST_THROW=$(echo "$API_BODY" | jq_get '.data.id')
[ -n "$POST_THROW" ] && ok "  临时帖文已发布 (id=${POST_THROW})" || warn "  临时帖文发布失败 (HTTP ${API_CODE})"

# =====================================================================
# 8. 互动：点赞 / 评论 / 回复 / 收藏（+ 各类删除接口验证）
# =====================================================================
hr
info "步骤 8/9: 注入互动数据（点赞 / 评论 / 回复 / 收藏）"

like_post() { # token postId label
    api POST "/api/v1/posts/$2/like" "" "$1"
    [ "$API_CODE" = "200" ] && ok "  $3" || warn "  $3 失败 (HTTP ${API_CODE})"
}
unlike_post() { # token postId label
    api DELETE "/api/v1/posts/$2/like" "" "$1"
    [ "$API_CODE" = "200" ] && ok "  $3" || warn "  $3 失败 (HTTP ${API_CODE})"
}
collect_post() { # token postId label
    api POST "/api/v1/posts/$2/collect" "" "$1"
    [ "$API_CODE" = "200" ] && ok "  $3" || warn "  $3 失败 (HTTP ${API_CODE})"
}
uncollect_post() { # token postId label
    api DELETE "/api/v1/posts/$2/collect" "" "$1"
    [ "$API_CODE" = "200" ] && ok "  $3" || warn "  $3 失败 (HTTP ${API_CODE})"
}
comment_on() { # token postId content → stdout 输出响应体
    api POST "/api/v1/posts/$2/comments" "$(jq -nc --arg c "$3" '{content:$c}')" "$1"
    echo "$API_BODY"
}
reply_to() { # token commentId content → stdout 输出响应体
    api POST "/api/v1/comments/$2/replies" "$(jq -nc --arg c "$3" '{content:$c}')" "$1"
    echo "$API_BODY"
}

echo "  --- 点赞 ---" >&2
like_post "$ALICE_TOKEN" "$POST_B1" "alice 点赞 bob 的帖文 #${POST_B1}"
like_post "$ALICE_TOKEN" "$POST_B2" "alice 点赞 bob 的帖文 #${POST_B2}"
like_post "$ALICE_TOKEN" "$POST_C1" "alice 点赞 charlie 的帖文 #${POST_C1}"
like_post "$BOB_TOKEN"   "$POST_A1" "bob 点赞 alice 的帖文 #${POST_A1}"
like_post "$BOB_TOKEN"   "$POST_C2" "bob 点赞 charlie 的帖文 #${POST_C2}"
like_post "$CHARLIE_TOKEN" "$POST_A2" "charlie 点赞 alice 的帖文 #${POST_A2}"
like_post "$CHARLIE_TOKEN" "$POST_B2" "charlie 点赞 bob 的帖文 #${POST_B2}（随后取消）"
unlike_post "$CHARLIE_TOKEN" "$POST_B2" "charlie 取消点赞 #${POST_B2}（DELETE /posts/{id}/like）"

echo "  --- 收藏 ---" >&2
collect_post "$ALICE_TOKEN" "$POST_B1" "alice 收藏 bob 的帖文 #${POST_B1}"
collect_post "$BOB_TOKEN"   "$POST_C1" "bob 收藏 charlie 的帖文 #${POST_C1}"
collect_post "$CHARLIE_TOKEN" "$POST_A1" "charlie 收藏 alice 的帖文 #${POST_A1}"
collect_post "$ALICE_TOKEN" "$POST_B2" "alice 收藏 bob 的帖文 #${POST_B2}（随后取消）"
uncollect_post "$ALICE_TOKEN" "$POST_B2" "alice 取消收藏 #${POST_B2}（DELETE /posts/{id}/collect）"

echo "  --- 评论 / 回复 ---" >&2
C_BOB_A1=$(comment_on "$BOB_TOKEN" "$POST_A1" "欢迎欢迎！")
C_BOB_A1_ID=$(echo "$C_BOB_A1" | jq_get '.data.id')
[ -n "$C_BOB_A1_ID" ] && ok "  bob 评论 alice 帖文 (评论id=${C_BOB_A1_ID})" || warn "  bob 评论失败 (HTTP ${API_CODE})"

C_CHAR_A1=$(comment_on "$CHARLIE_TOKEN" "$POST_A1" "风景真不错！")
C_CHAR_A1_ID=$(echo "$C_CHAR_A1" | jq_get '.data.id')
[ -n "$C_CHAR_A1_ID" ] && ok "  charlie 评论 alice 帖文 (评论id=${C_CHAR_A1_ID})" || warn "  charlie 评论失败 (HTTP ${API_CODE})"

C_ALICE_B1=$(comment_on "$ALICE_TOKEN" "$POST_B1" "看着就好吃！")
C_ALICE_B1_ID=$(echo "$C_ALICE_B1" | jq_get '.data.id')
[ -n "$C_ALICE_B1_ID" ] && ok "  alice 评论 bob 帖文 (评论id=${C_ALICE_B1_ID})" || warn "  alice 评论失败 (HTTP ${API_CODE})"

if [ -n "$C_BOB_A1_ID" ]; then
    R_ALICE=$(reply_to "$ALICE_TOKEN" "$C_BOB_A1_ID" "谢谢 Bob ～")
    R_ALICE_ID=$(echo "$R_ALICE" | jq_get '.data.id')
    [ -n "$R_ALICE_ID" ] && ok "  alice 回复 bob 的评论 (回复id=${R_ALICE_ID})" || warn "  alice 回复失败 (HTTP ${API_CODE})"
fi
if [ -n "$C_CHAR_A1_ID" ]; then
    R_BOB=$(reply_to "$BOB_TOKEN" "$C_CHAR_A1_ID" "同感同感！")
    R_BOB_ID=$(echo "$R_BOB" | jq_get '.data.id')
    [ -n "$R_BOB_ID" ] && ok "  bob 回复 charlie 的评论 (回复id=${R_BOB_ID})" || warn "  bob 回复失败 (HTTP ${API_CODE})"
fi

echo "  --- 评论点赞（含取消）---" >&2
if [ -n "$C_CHAR_A1_ID" ]; then
    api POST "/api/v1/comments/${C_CHAR_A1_ID}/like" "" "$BOB_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  bob 点赞 charlie 的评论 #${C_CHAR_A1_ID}" || warn "  评论点赞失败 (HTTP ${API_CODE})"
fi
if [ -n "$R_ALICE_ID" ]; then
    api POST "/api/v1/comments/${R_ALICE_ID}/like" "" "$CHARLIE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  charlie 点赞 alice 的回复 #${R_ALICE_ID}（随后取消）" || warn "  回复点赞失败 (HTTP ${API_CODE})"
    api DELETE "/api/v1/comments/${R_ALICE_ID}/like" "" "$CHARLIE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  charlie 取消点赞回复（DELETE /comments/{id}/like）" || warn "  取消评论点赞失败 (HTTP ${API_CODE})"
fi

echo "  --- 删除接口验证 ---" >&2
# 临时评论 → 删除
TMP_COMMENT=$(comment_on "$CHARLIE_TOKEN" "$POST_A2" "这条评论马上删掉")
TMP_COMMENT_ID=$(echo "$TMP_COMMENT" | jq_get '.data.id')
if [ -n "$TMP_COMMENT_ID" ]; then
    api DELETE "/api/v1/comments/${TMP_COMMENT_ID}" "" "$CHARLIE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  删除临时评论 #${TMP_COMMENT_ID}（DELETE /comments/{id}）" || warn "  删除评论失败 (HTTP ${API_CODE})"
fi
# 临时帖文 → 删除
if [ -n "$POST_THROW" ]; then
    api DELETE "/api/v1/posts/${POST_THROW}" "" "$ALICE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  删除临时帖文 #${POST_THROW}（DELETE /posts/{id}）" || warn "  删除帖文失败 (HTTP ${API_CODE})"
fi

echo "  --- 置顶帖 ---" >&2
api DELETE "/api/v1/users/me/pinned-post" "" "$ALICE_TOKEN"
[ "$API_CODE" = "200" ] && ok "  清除 alice 置顶帖（DELETE /users/me/pinned-post）" || warn "  清除置顶帖失败 (HTTP ${API_CODE})"
if [ -n "$POST_A1" ]; then
    api PUT "/api/v1/users/me/pinned-post" "$(jq -nc --argjson p "$POST_A1" '{postId:$p}')" "$ALICE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  alice 置顶自己的帖文 #${POST_A1}" || warn "  设置置顶帖失败 (HTTP ${API_CODE})"
fi

# =====================================================================
# 9. 查询验证：Feed / 搜索 / 通知 / 各类列表
# =====================================================================
hr
info "步骤 9/9: 查询验证（Feed / 搜索 / 通知 / 列表）"

echo "  --- Feed 流 ---" >&2
api GET "/api/v1/feed?page=0&size=10" "" "$ALICE_TOKEN"
FEED_TOTAL=$(echo "$API_BODY" | jq_get '.data.totalElements')
ok "  GET /feed (alice) → ${FEED_TOTAL:-0} 条动态"

echo "  --- 搜索 ---" >&2
api GET "/api/v1/search?category=user&keyword=ali"
SEARCH_COUNT=$(echo "$API_BODY" | jq_get '.data.data.content | length')
ok "  GET /search?category=user&keyword=ali → ${SEARCH_COUNT:-0} 条结果"

echo "  --- 通知 ---" >&2
api GET "/api/v1/notifications?size=10" "" "$ALICE_TOKEN"
NOTI_FIRST_ID=$(echo "$API_BODY" | jq_get '.data.content[0].id')
api GET "/api/v1/notifications/unread/count" "" "$ALICE_TOKEN"
UNREAD=$(echo "$API_BODY" | jq_get '.data')
ok "  GET /notifications (alice) 未读数=${UNREAD:-0}"
if [ -n "$NOTI_FIRST_ID" ]; then
    api PUT "/api/v1/notifications/${NOTI_FIRST_ID}/read" "" "$ALICE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  PUT /notifications/${NOTI_FIRST_ID}/read" || warn "  标记通知已读失败 (HTTP ${API_CODE})"
fi
api PUT "/api/v1/notifications/read-all" "" "$ALICE_TOKEN"
[ "$API_CODE" = "200" ] && ok "  PUT /notifications/read-all" || warn "  全部已读失败 (HTTP ${API_CODE})"

echo "  --- 帖文查询 ---" >&2
api GET "/api/v1/posts" "$(jq -nc --argjson t "${TOPIC_TRAVEL:-0}" '{topicId:$t}')" "$ALICE_TOKEN"
check "  GET /posts?body={topicId:旅行}"
api GET "/api/v1/posts" "$(jq -nc --argjson u "$BOB_ID" '{userId:$u}')" "$ALICE_TOKEN"
check "  GET /posts?body={userId:bob}"
if [ -n "$POST_A1" ]; then
    api GET "/api/v1/posts/${POST_A1}" "" "$ALICE_TOKEN"
    check "  GET /posts/${POST_A1} 帖文详情"
    api GET "/api/v1/posts/${POST_A1}/comments?size=20" "" "$ALICE_TOKEN"
    check "  GET /posts/${POST_A1}/comments 评论列表"
fi
if [ -n "$C_BOB_A1_ID" ]; then
    api GET "/api/v1/comments/${C_BOB_A1_ID}/replies?size=20" "" "$ALICE_TOKEN"
    check "  GET /comments/${C_BOB_A1_ID}/replies 回复列表"
fi

echo "  --- 用户与关系查询 ---" >&2
api GET "/api/v1/users/me" "" "$ALICE_TOKEN"
check "  GET /users/me (alice)"
api GET "/api/v1/users/${BOB_ID}" "" "$ALICE_TOKEN"
check "  GET /users/${BOB_ID} (bob 详情)"
api GET "/api/v1/users/check-username?username=alice"
check "  GET /users/check-username?username=alice"
api GET "/api/v1/users/${ALICE_ID}/followers?size=100" "" "$ALICE_TOKEN"
check "  GET /users/${ALICE_ID}/followers"
api GET "/api/v1/users/${ALICE_ID}/followees?size=100" "" "$ALICE_TOKEN"
check "  GET /users/${ALICE_ID}/followees"
api GET "/api/v1/users/me/liked-posts?size=20" "" "$ALICE_TOKEN"
check "  GET /users/me/liked-posts (alice)"
api GET "/api/v1/users/me/collected-posts?size=20" "" "$BOB_TOKEN"
check "  GET /users/me/collected-posts (bob)"

echo "  --- 话题查询 ---" >&2
api GET "/api/v1/topics/hot?limit=5"
check "  GET /topics/hot"
if [ -n "$TOPIC_FOOD" ]; then
    api GET "/api/v1/topics/${TOPIC_FOOD}"
    check "  GET /topics/${TOPIC_FOOD} 话题详情"
    api PUT "/api/v1/topics/${TOPIC_FOOD}" \
        "$(jq -nc --arg n '美食' --arg d '美食探店、菜谱分享、吃货聚集地' '{name:$n, description:$d}')" "$ALICE_TOKEN"
    check "  PUT /topics/${TOPIC_FOOD} 更新话题"
fi
# 临时话题 → 删除（验证 DELETE /topics/{id}）
api POST "/api/v1/topics" \
    "$(jq -nc --arg n "临时话题-$(date +%s)" --arg d '临时话题，用于验证删除接口' '{name:$n, description:$d}')" "$ALICE_TOKEN"
TMP_TOPIC_ID=$(echo "$API_BODY" | jq_get '.data.id')
if [ -n "$TMP_TOPIC_ID" ]; then
    ok "  临时话题已创建 (id=${TMP_TOPIC_ID})"
    api DELETE "/api/v1/topics/${TMP_TOPIC_ID}" "" "$ALICE_TOKEN"
    [ "$API_CODE" = "200" ] && ok "  删除临时话题（DELETE /topics/{id}）" || warn "  删除话题失败 (HTTP ${API_CODE})"
fi

echo "  --- Token 刷新 ---" >&2
api POST "/api/v1/auth/refresh" "$(jq -nc --arg r "$ALICE_REFRESH" '{refreshToken:$r}')"
NEW_TOKEN=$(echo "$API_BODY" | jq_get '.data.accessToken')
[ -n "$NEW_TOKEN" ] && ok "  POST /auth/refresh — 刷新成功" || warn "  刷新 Token 失败 (HTTP ${API_CODE})"

# 清理临时图片
rm -f "$TMP_IMG"

# =====================================================================
# 汇总
# =====================================================================
hr
echo ""
echo -e "  ${GREEN}══════════════════════════════════════════════${NC}" >&2
echo -e "  ${GREEN}         测试数据注入完成 🎉                   ${NC}" >&2
echo -e "  ${GREEN}══════════════════════════════════════════════${NC}" >&2
echo ""
echo -e "  ${CYAN}测试账号${NC}" >&2
echo -e "    alice / bob / charlie    密码: ${PASSWORD}" >&2
echo -e "    ${ADMIN_USERNAME}（管理员）          密码: ${ADMIN_PASSWORD}" >&2
echo ""
echo -e "  ${CYAN}本次注入${NC}" >&2
echo -e "    帖文: a1=${POST_A1:-?} a2=${POST_A2:-?} b1=${POST_B1:-?} b2=${POST_B2:-?} c1=${POST_C1:-?} c2=${POST_C2:-?} multipart=${POST_MULTI:-?}" >&2
echo -e "    话题: 旅行=${TOPIC_TRAVEL:-?} 美食=${TOPIC_FOOD:-?} 编程=${TOPIC_CODE:-?}" >&2
echo -e "    关注关系: 6 条（三人互相关注）" >&2
echo ""
echo -e "  ${CYAN}入口${NC}" >&2
echo -e "    API 文档: ${BASE_URL}/swagger-ui.html" >&2
echo -e "    健康检查: ${BASE_URL}/actuator/health" >&2
echo ""
echo -e "  ${YELLOW}提示${NC}: 邮箱相关接口（绑定邮箱/找回密码）因依赖 RESEND_API_KEY" >&2
echo -e "  ${YELLOW}     且存在发送冷却，未纳入本脚本。${NC}" >&2
echo ""
