# YX · 银杏叶社区 后端服务

**YX（银杏叶社区）** 的配套后端 API —— 一个面向跨性别（Trans）社区的 UGC 社交平台服务端。
基于 **Spring Boot 3 + JWT + Redis + RabbitMQ** 构建，为
[Flutter 客户端（TransPlatformFlutter）](https://github.com/An-n-ya/TransPlatformFlutter) 与
[后台管理（TransPlatformManagement）](https://github.com/An-n-ya/TransPlatformManagement) 提供统一 API。

> ⚠️ **项目状态：早期开发阶段**
> 生产环境：`https://yx.annya.work`（App API 与后台管理共用，经 Caddy 反向代理）。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [快速启动](#快速启动)
- [环境变量](#环境变量)
- [项目结构](#项目结构)
- [API 概览](#api-概览)
- [后台管理](#后台管理)
- [测试](#测试)
- [CI / CD 与部署](#ci--cd-与部署)
- [设计决策](#设计决策)
- [开源许可](#开源许可)

---

## 功能特性

### 用户与认证

- 注册 / 登录 / JWT 刷新（access 24h + refresh 30d，`jjwt`）
- **邀请码注册**：管理员生成邀请码（数量 / 有效期 / 场景）、注册时校验；邮件发送邀请码能力（Resend）服务层已就绪
- 用户名可用性实时校验（`/users/check-username`）
- 邮箱绑定与验证码校验、**找回密码**（发送重置码 + 重置）
- BCrypt 密码加密、管理员账号首次启动自动创建

### 内容与互动

- **帖文**：发布（JSON / multipart 多图）、详情、逻辑删除、统一条件查询、作者帖文列表、置顶
- **互动**：点赞（帖文 / 评论）、评论与**多层回复**、收藏 / 取消收藏、我点赞 / 我收藏列表
- **Feed 流**：广场 / 关注 / 附近 三类时间线，**写扩散（Push）+ Redis**，游标分页

### 社区能力

- **话题**：CRUD、热门话题、帖文关联话题
- **搜索**：用户 / 话题分类搜索
- **通知**：列表（分页）、未读数、单条已读、全部已读

### 平台能力

- **位置**：逆地理编码（`BigDataCloud` 默认，失败回退百度地图）
- **文件上传**：可插拔存储（`mock` / 腾讯云 **COS** / S3 兼容对象存储）
- **后台管理 API**：`/admin/v1/*`（认证、用户、内容、话题、邀请码、上传、统计、搜索）
- **统计**：发帖量、新注册人数、活跃用户数、发帖/注册每日趋势
- **运维**：SpringDoc OpenAPI（Swagger）、TraceId 链路追踪、Actuator 健康检查、结构化日志（Logstash）

## 技术栈

| 层级 | 选型 |
|---|---|
| 语言 / 框架 | Java 17 · Spring Boot 3.2.5 |
| 认证 | Spring Security + JWT（jjwt 0.12.5） |
| ORM | Spring Data JPA + Hibernate |
| 数据库 | SQLite（Flyway 迁移 V1–V7，单文件持久化） |
| 缓存 | Redis 7.x（Spring Cache + Feed 写扩散） |
| 消息队列 | RabbitMQ（发帖推送 / 关注维护等异步事件） |
| 对象存储 | mock（本地磁盘） / 腾讯云 COS / S3 兼容（AWS SDK） |
| 邮件 | Resend（验证码、邀请码、找回密码） |
| 文档 | SpringDoc OpenAPI 2.5.0 |
| 日志 | Logback + Logstash JSON encoder（含 TraceId） |
| 构建 / 部署 | Maven · Docker Compose · GitHub Actions |

## 快速启动

### 方式一：Docker 一键启动（推荐）

> 一键拉起 应用 + Redis + RabbitMQ，SQLite 数据库与上传文件持久化到宿主机
> 默认目录 `/var/opt/trans`，可通过 `DATA_DIR` 自定义。

```bash
cd scripts

docker compose up -d              # 默认持久化目录 /var/opt/trans
DATA_DIR=/mnt/data docker compose up -d   # 自定义持久化目录

# 查看状态
curl http://localhost:8081/actuator/health   # → {"status":"UP"}

# 停止（保留数据）
docker compose down

# 停止并删除所有数据
docker compose down -v
```

首次启动会自动执行 Flyway 迁移建表，并创建管理员账号
（`ADMIN_USERNAME` / `ADMIN_PASSWORD`，默认 `admin` / `admin123`，**生产务必修改**）。

### 方式二：本地开发（host 运行）

**前置条件**：JDK 17+、Docker（或本机 Redis 7.x + RabbitMQ）、Maven 3.8+。

```bash
# 1. 启动基础设施（Redis + RabbitMQ，可用项目自带 compose）
cd scripts && docker compose up -d redis rabbitmq

# 2. 编译 + 启动应用（默认 8081 端口，自动切到 SDKMAN 管理的 JDK 17）
./scripts/run.sh start

# 其他命令
./scripts/run.sh restart        # 重启
./scripts/run.sh status         # 状态 + 基础设施检查
./scripts/run.sh test           # 编译 + API 冒烟测试
./scripts/run.sh logs           # 实时日志
./scripts/run.sh stop           # 停止
```

> SQLite 无需额外数据库服务，首次启动自动在 `data/trans_platform.db` 建库建表。

### 3. 验证

```bash
# 健康检查
curl http://localhost:8081/actuator/health

# API 文档（Swagger UI）
open http://localhost:8081/swagger-ui.html

# 注入测试数据（可选，alice/bob/charlie 三个账号 + 大量帖文/互动）
./scripts/seed-data.sh
```

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `APP_PORT` | `8081` | 应用对外端口 |
| `JWT_SECRET` | 占位值 | **生产务必设置**强随机密钥（≥32 字符） |
| `SQLITE_PATH` | `/app/data/trans_platform.db` | SQLite 数据库文件路径 |
| `STORAGE_PROVIDER` | `mock` | 存储：`mock` / `cos` / `s3` |
| `COS_SECRET_ID` / `COS_SECRET_KEY` / `COS_REGION` / `COS_BUCKET` | — | 腾讯云 COS（`provider=cos` 时必填） |
| `STORAGE_ENDPOINT` / `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` / `STORAGE_BUCKET` | — | S3 兼容对象存储（`provider=s3` 时必填） |
| `RESEND_API_KEY` | — | Resend 邮件服务密钥（验证码 / 邀请码 / 找回密码） |
| `BIG_DATA_CLOUD_KEY_API` | — | 逆地理编码默认服务 API Key |
| `BAIDU_API_KEY` | — | 逆地理编码回退服务 API Key |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` | 首次启动创建的管理员账号 |
| `DATA_DIR` | `/var/opt/trans` | Docker 持久化卷根目录（data / uploads / redis / rabbitmq） |

## 项目结构

```
src/main/java/com/app/
├── AppApplication.java          # 启动类
├── config/                      # 配置：Security+JWT、Redis、Rabbit、CORS、OpenAPI、
│                                #       WebSocket（桩）、管理员引导
├── common/                      # 统一返回 / 分页 / 游标分页 / 异常 / JwtUtil / TraceId
├── user/                        # 用户与认证模块（注册/登录/邮箱/找回密码/关注/置顶/资料）
├── content/                     # 内容模块（帖文 CRUD、统一查询、多图）
├── interaction/                 # 互动模块（点赞、评论/回复、收藏）
├── feed/                        # Feed 流模块（广场/关注/附近、写扩散 + Redis 游标分页）
├── topic/                       # 话题模块（CRUD、热门、帖文关联）
├── search/                      # 搜索模块（用户 / 话题分类搜索）
├── notification/                # 通知模块（列表、未读数、已读）
├── invitation/                  # 邀请码模块（生成、邮件发送、注册校验）
├── email/                       # 邮件服务（Resend + 验证码）
├── admin/                       # 后台管理 API（认证/用户/内容/话题/邀请码/上传/统计/搜索）
├── location/                    # 位置模块（逆地理编码，BigDataCloud + 百度回退）
├── upload/                      # 文件上传（mock / COS / S3 可插拔存储）
└── message/                     # 私信模块（预留：schema 已建，功能待实现）

src/main/resources/
├── application.yml              # 应用配置（profile=dev）
├── storage.yml                  # 存储配置（外部可覆盖）
├── logback-spring.xml           # 日志（JSON encoder + TraceId）
└── db/migration/                # Flyway 迁移（V1–V7）

scripts/
├── docker-compose.yml           # 一键编排（app + redis + rabbitmq）
├── Dockerfile                   # 多阶段构建镜像
├── run.sh                       # 本地启停 / 冒烟测试脚本
├── deploy.sh                    # 服务器端部署脚本（CI 远程调用）
└── seed-data.sh                 # 测试数据一键注入

.github/workflows/deploy.yml     # 推送到 main 自动 SSH 部署
```

## API 概览

| 前缀 | 说明 |
|---|---|
| `POST /api/v1/auth/register` · `login` · `refresh` | 注册（邀请码）/ 登录 / Token 刷新 |
| `POST /api/v1/auth/password/send-reset-code` · `reset` | 找回密码 |
| `GET /api/v1/users/check-username` | 用户名可用性校验 |
| `GET/PUT /api/v1/users/me` | 当前用户资料（含头像 / bio / 背景图 multipart） |
| `PUT/DELETE /api/v1/users/me/pinned-post` | 置顶帖 |
| `POST/DELETE /api/v1/users/me/email/send-code` · `verify` | 邮箱绑定 |
| `GET /api/v1/users/{userId}` · `{userId}/followers` · `followees` | 用户资料 / 粉丝 / 关注列表 |
| `POST/DELETE /api/v1/users/{userId}/follow` | 关注 / 取关 |
| `POST /api/v1/posts` · `GET/DELETE /api/v1/posts/{postId}` · `GET /api/v1/posts` | 帖文 CRUD + 统一查询 |
| `POST/DELETE /api/v1/posts/{postId}/like` | 点赞 / 取消 |
| `GET/POST /api/v1/posts/{postId}/comments` · `DELETE /api/v1/comments/{commentId}` | 评论 |
| `GET/POST /api/v1/comments/{commentId}/replies` | 回复（多层） |
| `POST/DELETE /api/v1/posts/{postId}/collect` | 收藏 / 取消收藏 |
| `GET /api/v1/users/me/liked-posts` · `collected-posts` | 我点赞 / 我收藏 |
| `GET /api/v1/feed` | 三类 Feed 流（广场 / 关注 / 附近，游标分页） |
| `POST/GET/PUT /api/v1/topics` · `GET /api/v1/topics/hot` | 话题管理 / 热门话题 |
| `GET /api/v1/search` | 用户 / 话题搜索 |
| `GET /api/v1/notifications` · `/unread/count` · `PUT /{id}/read` · `/read-all` | 通知 |
| `POST /api/v1/invitations` | 生成邀请码 |
| `GET /api/v1/location/reverse-geocode` | 经纬度 → 城市 |
| `POST /api/v1/upload/image` | 上传图片 |
| `/admin/v1/*` | 后台管理 API（详见下节） |

> 完整接口文档见运行时 Swagger UI（`/swagger-ui.html`）。

## 后台管理

内置 `/admin/v1/*` 管理接口，配合前端
[TransPlatformManagement](https://github.com/An-n-ya/TransPlatformManagement)（部署于 `/management/`）使用：

- **认证**：`/admin/v1/auth/login` · `refresh`（独立管理员 Token）
- **内容管理**：帖文列表 / 逻辑删除、评论与回复查看 / 删除
- **用户管理**：用户资料、粉丝 / 关注列表
- **话题管理**：CRUD、置顶（host）
- **邀请码**：生成邀请码
- **上传 / 搜索 / 统计**：图片上传、全局搜索、数据统计（注册量 / 发帖量 / 互动量 / 每日趋势）

## 测试

```bash
./scripts/run.sh test    # 编译 + API 冒烟测试（注册→登录→发帖→点赞→评论→收藏→删除）
```

单元 / 集成测试（`src/test`）：Feed 写扩散逻辑、Feed SQL 查询集成测试、
逆地理编码服务（BigDataCloud / 百度）、应用上下文加载。

```bash
mvn test
```

## CI / CD 与部署

- **自动部署**：`.github/workflows/deploy.yml` —— 推送到 `main` 后，通过 SSH 调用
  服务器端 `scripts/deploy.sh`：拉取代码 → Docker 构建 → 滚动启动 → 健康检查。
- **生产拓扑**：Vultr VPS（Docker Compose 运行 app + Redis + RabbitMQ）+ Caddy 反向代理
  （同一源 `yx.annya.work` 分流 App API 与 `/management/` 后台）+ Cloudflare（DNS / CDN）。

部署前请务必设置：`JWT_SECRET`、`RESEND_API_KEY`、存储密钥与逆地理编码 API Key。

## 设计决策

- **不引入 ES**：社区规模下 SQLite 的 `LIKE` 搜索足够；仅搜索用户与话题，负载可控。
- **Feed 流写扩散**：关注关系变更通过 RabbitMQ 异步维护，保证关注流实时性。
- **不实现推荐算法**：Feed 流基于关注时间线排序，简单可预期。
- **SQLite 而非 MySQL**：单文件、零运维，配合 Flyway 迁移，非常适合当前规模与部署。

## 开源许可

本项目基于 **Apache License 2.0** 开源，详见 [LICENSE](LICENSE)。
