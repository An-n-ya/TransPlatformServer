# TransPlatform — 后续待办清单

> 已实现: 用户认证、帖文 CRUD、Feed 流、互动（点赞/评论/收藏）
> 以下为**未实现的非核心功能**，按优先级排列。

---

## 优先级 P1 — 消息（私信）模块

- [ ] **WebSocket 认证** — 连接时从 query string 或 header 解析 JWT
- [ ] **在线状态管理** — `ChatWebSocketHandler` 维护 `userId → WebSocketSession`
- [ ] **发送/接收私信** — 消息路由到在线用户，离线用户消息存数据库
- [ ] **私信会话管理**
  - `POST /api/v1/sessions` — 创建私信会话
  - `GET /api/v1/sessions` — 获取会话列表
  - `GET /api/v1/sessions/{sessionId}/messages` — 消息历史（分页）
  - `POST /api/v1/sessions/{sessionId}/messages` — HTTP 备用发送
  - `GET /api/v1/messages/unread/count` — 未读消息数
- [ ] **HTTP 轮询模式** — 作为 WebSocket 的降级方案
- [ ] **未读消息推送** — 用户上线后自动推送离线期间的消息

## 优先级 P2 — 通知模块

- [ ] **通知数据表对接** — 实现 notifications 表的 CRUD
- [ ] **通知事件生产** — 点赞、评论、关注、私信等事件写入通知
- [ ] **通知列表 API**
  - `GET /api/v1/notifications` — 通知列表（分页）
  - `PUT /api/v1/notifications/{id}/read` — 标记已读
  - `PUT /api/v1/notifications/read-all` — 全部标记已读
- [ ] **实时推送通知** — 通过 WebSocket 推送新通知

## 优先级 P3 — 后台管理模块

- [ ] **内容审核** — `com.app.admin` 模块，管理员可审核/下架帖文
- [ ] **用户管理** — 管理员可启用/禁用用户
- [ ] **数据统计** — 日活、发帖量、注册量等基础统计

## 优先级 P4 — 系统增强

- [ ] **Docker 支持** — 完善 `Dockerfile` 和 `docker-compose.yml`
  - 集成 MySQL 8.0、Redis 7.x、RabbitMQ
  - 服务健康检查（`/actuator/health`）
  - JVM 参数 `-Xmx2g -Xms1g`
- [ ] **单元测试** — Service 层核心方法单元测试
  - `UserServiceTest` — 注册/登录/关注
  - `PostServiceTest` — 帖文 CRUD
  - `FeedServiceTest` — Feed 推送与拉取
  - `LikeServiceTest` — 点赞/取消
- [ ] **生产配置完善**
  - `application-prod.yml` — 生产数据库/Redis/RabbitMQ 配置
  - 敏感信息通过环境变量注入（已完成配置占位）
  - OSS 真实对接（阿里云 OSS / 腾讯云 COS SDK）
  - 图片压缩与 WebP 格式转换
- [ ] **接口限流** — 接入 Spring Cloud Gateway 或 Sentinel
- [ ] **API 版本管理** — 明确 `/api/v1/` 前缀策略
- [ ] **完整 API 文档** — 补充所有接口的 SpringDoc 注解和示例

## 已知优化项

- [ ] **Feed 流取关优化** — 使用 `user_posts:{userId} → Set<postId>` Redis 结构加速取关时的帖文移除
- [ ] **帖文缓存预热** — 热点帖文启动时自动加载到 Redis
- [ ] **分页优化** — 游标分页替代 offset 分页，提升大页码性能
- [ ] **Feed 流分页** — 使用 ZSET 按时间戳排序替代 LIST，支持更精确的分页
- [ ] **事务超时设置** — 为长时间运行的异步任务单独配置事务超时
