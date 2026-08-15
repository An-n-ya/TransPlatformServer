# TransPlatform — 类小红书 UGC 平台后端

基于 Spring Boot 3.x + JWT + Redis + RabbitMQ 的 UGC 内容平台后端服务。

## 已完成的核心功能

| 模块 | 功能 | 状态 |
|------|------|------|
| **用户认证** | 注册/登录/JWT 刷新/BCrypt 加密 | ✅ |
| **用户管理** | 资料查看/修改、关注/取关、粉丝/关注列表 | ✅ |
| **帖文 CRUD** | 发布/详情/删除（逻辑删除）/用户帖文列表 | ✅ |
| **互动** | 点赞/取消、评论/回复/删除、收藏/取消收藏 | ✅ |
| **Feed 流** | 写扩散(Push)模式 + Redis List 分页 | ✅ |
| **异步处理** | RabbitMQ 事件驱动（发帖推送、关注维护） | ✅ |
| **缓存策略** | Spring Cache + Redis（帖文 5min / 用户 30min） | ✅ |
| **文件上传** | 模拟预签名 URL（可对接 OSS） | ✅ |
| **API 文档** | SpringDoc OpenAPI (Swagger 3) | ✅ |
| **TraceId** | 请求链路追踪 | ✅ |

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.5 |
| 认证 | Spring Security + JWT (jjwt 0.12.5) |
| ORM | Spring Data JPA + Hibernate |
| 数据库 | SQLite (Flyway 迁移，单文件持久化) |
| 缓存 | Redis 7.x (Spring Cache) |
| 消息队列 | RabbitMQ |
| 文档 | SpringDoc OpenAPI 2.5.0 |
| 构建 | Maven |

## 快速启动

### 方式一：Docker 一键启动（推荐）

> 一键拉起 应用 + Redis + RabbitMQ，SQLite 数据库单文件持久化到宿主机
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

常用环境变量（`docker compose` 前设置）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DATA_DIR` | `/var/opt/trans` | 持久化卷根目录（data/redis/rabbitmq/uploads） |

| `APP_PORT` | `8081` | 应用对外端口 |
| `JWT_SECRET` | 占位值 | 生产环境务必设置强随机密钥 |
| `STORAGE_PROVIDER` | `mock` | 存储：`mock`/`cos`/`s3` |

### 方式二：本地开发（host 运行）

### 前置条件

- JDK 17+
- Docker & Docker Compose（或本地安装 Redis 7.x + RabbitMQ）
- Maven 3.8+

### 1. 启动基础设施

```bash
# 使用 Docker Compose 启动 Redis + RabbitMQ
# SQLite 数据库无需额外服务，首次启动自动创建 data/trans_platform.db

# Redis
docker run -d --name redis7 -p 6379:6379 redis:7

# RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

### 2. 数据库说明

SQLite 单文件数据库，首次启动由 Flyway 自动建表（V1-V2 迁移）。
本地开发路径：`data/trans_platform.db`；Docker 部署路径：`/app/data/trans_platform.db`（挂载到宿主机 `DATA_DIR/data/`）。

### 3. 启动应用

```bash
# 开发模式
export MYSQL_PASSWORD=root
./scripts/run.sh start          # 编译 + 启动（默认 8081 端口）
./scripts/run.sh restart        # 重启
./scripts/run.sh test           # 编译 + API 冒烟测试
./scripts/run.sh stop           # 停止
```

### 4. 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# API 文档
open http://localhost:8080/swagger-ui.html
```

## 测试 API

### 注册用户

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","nickname":"Alice","password":"pass123"}'
```

### 登录获取 Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'
```

### 发布帖文（需 Token）

```bash
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"content":"Hello trans-platform!","images":["https://example.com/img1.jpg"],"location":"Beijing"}'
```

### 获取 Feed 流

```bash
curl http://localhost:8080/api/v1/feed?page=0&size=20 \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## 项目结构

```
src/main/java/com/app/
├── AppApplication.java          # 启动类
├── config/                      # 配置类
│   ├── SecurityConfig.java      # Spring Security + JWT
│   ├── JwtAuthentication.java   # 自定义认证令牌
│   ├── RedisConfig.java         # Redis 序列化与缓存
│   ├── RabbitConfig.java        # RabbitMQ 交换机/队列
│   ├── CorsConfig.java          # 跨域
│   ├── OpenApiConfig.java       # Swagger 文档
│   ├── WebSocketConfig.java     # WebSocket 配置（桩）
│   └── ChatWebSocketHandler.java
├── common/                      # 公共组件
│   ├── ApiResponse.java         # 统一返回 {code, message, data}
│   ├── PageResult.java          # 分页封装
│   ├── BaseEntity.java          # JPA 基类
│   ├── JwtUtil.java             # JWT 工具类
│   ├── GlobalExceptionHandler.java # 全局异常处理
│   ├── TraceFilter.java         # TraceId 过滤器
│   └── PostCreatedEvent.java    # 帖文创建事件
├── user/                        # 用户模块
│   ├── controller/...
│   ├── service/...
│   ├── model/
│   └── repository/
├── content/                     # 内容模块
│   ├── PostController.java
│   ├── PostServiceImpl.java
│   ├── Post.java / PostImage.java
│   └── PostRepository.java
├── interaction/                 # 互动模块
│   ├── InteractionController.java
│   ├── LikeService / CommentService / CollectionService
│   └── Like.java / Comment.java / Collection.java
├── feed/                        # Feed 流模块
│   ├── FeedController.java
│   ├── FeedServiceImpl.java
│   ├── FeedEventConsumer.java
│   └── FollowEventConsumer.java
└── upload/                      # 文件上传
    └── UploadController.java
```

## 部署参考

**服务器规格**: 4核 4GB 内存 / 3M 带宽

```bash
# JVM 参数
java -jar -Xmx2g -Xms1g trans-platform-1.0.0-SNAPSHOT.jar

# 生产模式
java -jar -Dspring.profiles.active=prod \
  -DJWT_SECRET=<your-secret> \
  -DMYSQL_HOST=<host> -DMYSQL_USERNAME=<user> -DMYSQL_PASSWORD=<pass> \
  -DREDIS_HOST=<host> -DREDIS_PASSWORD=<pass> \
  -DRABBITMQ_HOST=<host> -DRABBITMQ_USERNAME=<user> -DRABBITMQ_PASSWORD=<pass> \
  trans-platform-1.0.0-SNAPSHOT.jar
```


### 使用 Docker 部署
```bash
# 启动前务必设置强 JWT 密钥
JWT_SECRET=$(openssl rand -hex 32) \
MYSQL_PASSWORD=你的密码 \
docker compose up -d
```

## 未实现功能

详见 [TODO.md](./TODO.md)，包括：
- 私信系统（WebSocket 实时通讯、会话管理）
- 通知模块（系统通知、推送）
- 后台管理（审核、用户管理）
- Docker 完整部署配置
- 单元测试
- 生产环境 OSS 对接

## 设计决策

- **不引入 ES**: 1000 人规模下 SQLite/MySQL 查询足够
- **不实现推荐算法**: Feed 流基于关注时间线排序
- **轻量 WebSocket**: Spring 原生支持，内存存储 Session
- **简化私信**: 只记录未读数量，不实现已读/未读复杂状态
