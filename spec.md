### 一、项目概述

请帮我搭建一个**类小红书的 UGC（用户生成内容）平台**后端服务，核心功能包括用户发布图文帖文、关注/取关其他用户、Feed 流浏览、私信聊天。目标用户规模初期约 **1000 人**。

---

### 二、技术栈要求

|层级|技术选型|说明|
|---|---|---|
|**运行环境**|JDK 17+|使用 LTS 版本|
|**后端框架**|Spring Boot 3.x|采用**模块化单体架构**，不要拆分为微服务|
|**构建工具**|Maven 或 Gradle|推荐 Maven|
|**主数据库**|MySQL 8.0+|存储用户、帖子、关注关系等结构化数据|
|**缓存数据库**|Redis 7.x|用于缓存 Feed 流、热点数据、计数等|
|**对象存储**|阿里云 OSS / 腾讯云 COS|存储用户上传的图片（通过预签名 URL 上传）|
|**消息队列**|RabbitMQ 或 RocketMQ|用于异步处理发帖、点赞等事件|
|**实时通讯**|WebSocket (Spring 原生支持)|实现私信实时收发|
|**API 规范**|RESTful API|统一返回格式 `{code, message, data}`|

---

### 三、模块划分与职责

请按以下模块组织代码，每个模块独立成包（package），**各模块之间通过 Service 层调用，严禁跨模块直接操作数据库表**：

|模块|包名|核心职责|
|---|---|---|
|**用户模块**|`com.app.user`|注册/登录（JWT）、用户资料管理、关注/取关、粉丝列表、关注列表|
|**内容模块**|`com.app.content`|发布帖文（图文）、帖文详情、删除帖文、帖文列表|
|**互动模块**|`com.app.interaction`|点赞/取消点赞、评论/回复评论、收藏/取消收藏|
|**Feed 流模块**|`com.app.feed`|首页 Feed 流（基于关注关系的时间线）、推荐 Feed（可简化）|
|**消息模块**|`com.app.message`|私信会话管理、发送/接收私信、未读消息统计、WebSocket 连接管理|
|**通知模块**|`com.app.notification`|系统通知（点赞、评论、关注、私信等事件的通知）|
|**后台管理模块（可选）**|`com.app.admin`|内容审核、用户管理等基础管理功能|

---

### 四、核心数据表设计

请提供完整的 Flyway/Liquibase 迁移脚本或 SQL DDL，至少包含以下表：

users              # 用户表（id, username, nickname, password_hash, avatar, bio, created_at）
follows            # 关注关系表（id, follower_id, followee_id, status, created_at）
posts              # 帖文表（id, user_id, content, images(JSON数组), location, likes_count, comments_count, status, created_at）
post_images        # 帖文图片表（可选，独立存储）
likes              # 点赞表（id, user_id, target_type(post/comment), target_id, created_at）
comments           # 评论表（id, post_id, user_id, parent_id(支持嵌套回复), content, likes_count, created_at）
collections        # 收藏表（id, user_id, post_id, created_at）
sessions           # 私信会话表（id, type(single/group), created_at）
session_participants # 会话参与者表（id, session_id, user_id, last_read_at）
messages           # 私信消息表（id, session_id, sender_id, type(text/image), content, status, created_at）
notifications      # 通知表（id, user_id, type, content, is_read, target_id, created_at）

---

### 五、关键功能实现要求

#### 5.1 用户认证与安全

- 使用 **Spring Security + JWT** 实现无状态认证
    
- 提供注册、登录、刷新 Token 接口
    
- 密码使用 BCrypt 加密
    
- 所有业务接口（除注册/登录外）均需携带 JWT 进行身份验证
    

#### 5.2 Feed 流实现

- 采用 **写扩散（Push）模式**：
    
    - 用户发布帖文后，将帖文 ID 推送到所有粉丝的 Redis Feed 列表中
        
    - Feed 流数据格式：`feed:{userId} = List<PostId>`，存储最近 200-500 条
        
    - 用户刷新首页时，根据 Feed 列表批量查询帖文详情（带缓存）
        
- 关注/取关时，需要维护用户的 Feed 列表（关注时拉取历史帖文，取关时移除）
    

#### 5.3 图片上传

- 客户端通过 API 获取 **OSS 预签名上传 URL**，直接上传图片到 OSS
    
- 帖文发布时，携带已上传的图片 URL 列表
    
- 图片建议进行压缩和格式转换（如 WebP）
    

#### 5.4 私信系统

- 使用 **WebSocket** 实现实时消息推送
    
- 建立连接时通过 JWT 进行身份认证
    
- 消息格式：`{sessionId, senderId, type, content, timestamp}`
    
- 离线用户的消息存储到数据库，上线后推送未读消息
    

#### 5.5 异步处理

使用 **RabbitMQ** 处理以下场景（确保核心接口快速响应）：

- 发布帖文 → 异步：写入 ES（如果引入）、推送到粉丝 Feed 流、触发内容审核、发送通知
    
- 点赞/评论 → 异步：更新帖文计数、发送通知
    
- 关注/取关 → 异步：维护 Feed 列表
    

#### 5.6 缓存策略

- **帖文详情**：缓存 5 分钟，点赞/评论数变更时更新缓存（Cache-Aside 模式）
    
- **用户信息**：缓存 30 分钟
    
- **热门帖文列表**：缓存 1 分钟
    
- 使用 Spring Cache 注解（`@Cacheable`、`@CacheEvict`）简化缓存操作
    

---

### 六、API 接口规范

请为每个模块提供完整的 RESTful API，遵循以下规范：

# 用户模块
POST   /api/v1/auth/register        # 注册
POST   /api/v1/auth/login           # 登录
POST   /api/v1/auth/refresh         # 刷新Token
GET    /api/v1/users/me             # 获取当前用户信息
PUT    /api/v1/users/me             # 更新当前用户信息
GET    /api/v1/users/{userId}       # 获取指定用户信息
POST   /api/v1/users/{userId}/follow    # 关注
DELETE /api/v1/users/{userId}/follow    # 取关
GET    /api/v1/users/{userId}/followers # 粉丝列表（分页）
GET    /api/v1/users/{userId}/followees # 关注列表（分页）
# 内容模块
POST   /api/v1/posts                # 发布帖文
GET    /api/v1/posts/{postId}       # 获取帖文详情
DELETE /api/v1/posts/{postId}       # 删除帖文
GET    /api/v1/users/{userId}/posts # 获取用户的帖文列表（分页）
# Feed流模块
GET    /api/v1/feed                 # 获取首页Feed流（分页，返回帖文列表）
# 互动模块
POST   /api/v1/posts/{postId}/like      # 点赞
DELETE /api/v1/posts/{postId}/like      # 取消点赞
POST   /api/v1/posts/{postId}/comments  # 发表评论
DELETE /api/v1/comments/{commentId}     # 删除评论
GET    /api/v1/posts/{postId}/comments  # 获取评论列表（分页，支持嵌套回复）
# 消息模块
GET    /api/v1/sessions             # 获取私信会话列表
POST   /api/v1/sessions             # 创建私信会话
GET    /api/v1/sessions/{sessionId}/messages # 获取会话消息历史（分页）
POST   /api/v1/sessions/{sessionId}/messages # 发送消息（HTTP，备用）
GET    /api/v1/messages/unread/count # 获取未读消息总数
# 通知模块
GET    /api/v1/notifications        # 获取通知列表（分页）
PUT    /api/v1/notifications/{id}/read # 标记通知为已读
PUT    /api/v1/notifications/read-all # 全部标记已读
# 文件上传模块
GET    /api/v1/upload/credentials   # 获取OSS临时凭证/预签名URL

### 七、项目结构（推荐）

src/main/java/com/app/
├── config/           # 配置类（JWT、WebSocket、Redis、RabbitMQ、OSS、CORS等）
├── common/           # 公共组件（统一返回结果、异常处理、分页对象、工具类）
├── user/             # 用户模块
│   ├── controller/
│   ├── service/      # 接口 + 实现
│   ├── model/        # 实体类、DTO、VO
│   └── repository/   # JPA/MyBatis-Plus Repository
├── content/          # 内容模块（同上）
├── interaction/      # 互动模块（同上）
├── feed/             # Feed流模块（同上）
├── message/          # 消息模块（同上）
├── notification/     # 通知模块（同上）
└── admin/            # 后台管理模块（可选）

### 八、非功能性要求

1. **日志与监控**：使用 SLF4J + Logback 记录请求日志、关键操作日志，日志需包含 traceId 便于链路追踪
    
2. **参数校验**：使用 `@Valid` + `@NotNull` 等注解进行参数校验，统一异常处理
    
3. **接口文档**：集成 SpringDoc OpenAPI (Swagger 3)，生成在线 API 文档
    
4. **配置管理**：使用 `application.yml` 分层配置（dev、test、prod），敏感信息通过环境变量注入
    
5. **数据库迁移**：使用 Flyway 管理数据库 Schema 变更
    
6. **单元测试**：为 Service 层核心方法编写单元测试
    
7. **Docker 支持**：提供 Dockerfile 和 docker-compose.yml，支持一键启动（含 MySQL、Redis、RabbitMQ）
    

---

### 九、技术权衡与取舍说明

1. **本次实现不引入 Elasticsearch**：1000 人规模下，MySQL 的 LIKE 查询配合简单分页足以应对搜索需求，未来可平滑升级
    
2. **不实现复杂的推荐算法**：Feed 流暂时仅基于关注关系的时间线排序，暂不考虑个性化推荐
    
3. **WebSocket 保持轻量**：使用 Spring 原生 WebSocket + 内存存储 Session，不引入额外的 Session 共享方案（规模增长后可扩展）
    
4. **私信不实现“已读/未读”复杂状态**：仅记录未读数量，简化实现
    

---

### 十、代码质量标准

- 遵循阿里巴巴 Java 开发手册
    
- Controller 层仅负责参数接收和返回，不包含业务逻辑
    
- Service 接口需有完整的 JavaDoc 注释
    
- 所有对外返回的 VO 对象必须进行字段过滤（不返回密码、敏感信息等）
    
- 数据库查询使用分页，防止全表扫描
    
- 事务管理：在 Service 层使用 `@Transactional` 确保数据一致性
    

---

### 附录：部署配置参考

本服务将部署在 **4核4GB内存、3M带宽** 的云服务器上，请在设计时充分考虑资源限制：

- JVM 堆内存建议设置为 `-Xmx2g -Xms1g`
    
- 数据库连接池大小建议 20-50
    
- Redis 最大内存限制 512MB
    
- 所有静态资源（图片）必须通过 OSS + CDN 访问，不走服务器带宽
    
- 建议配置健康检查端点 `/actuator/health`
    

---

