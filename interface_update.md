## 用户接口更新(删除用逻辑删除)
`DELETE /api/v1/comments/{commentId}` 该接口只能删除自己的评论
`DELETE /api/v1/posts/{postId}` 该接口只能删除自己的贴文
- 用户不拥有删除话题的接口


## 新增位置解析接口
`GET /api/v1/location/reverse-geocode?latitude=39.951335108535&longitude=116.51484487905`
- 逆地理编码，默认调用 BigDataCloud（`https://api-bdc.net/data/reverse-geocode`），失败时回退百度地图 reverse_geocoding/v3
- API Key：BigDataCloud 从环境变量 `BIG_DATA_CLOUD_KEY_API` 读取，百度从环境变量 `BAIDU_API_KEY` 读取
- 只关注返回结果中的 `city` 字段，作为 `location` 信息返回给前端（city 为空时回退到 province/principalSubdivision/locality）
- 境外坐标：BigDataCloud（默认路径）直接返回境外真实城市；仅当回退到百度且返回 status=240 时，`location` 才记为 `"境外"`
- 入参：`latitude` 纬度(-90~90)、`longitude` 经度(-180~180)，坐标类型默认为 WGS-84 GPS 坐标
- 返回：`{"code":200,"message":"success","data":{"location":"北京市"}}`

## 新增管理员接口(拥有完整接口，能够CRUD任意内容，注意，删除使用逻辑删除)
`DELETE /admin/v1/comments/{commentId}`
`GET /admin/v1/posts/{postId}/comments`
`GET /admin/v1/comments/{commentId}/replies`
`GET /admin/v1/search`
`POST /admin/v1/upload/image`
`DELETE /admin/v1/posts/{postId}`
`GET /admin/v1/posts`
`DELETE /admin/v1/topics/{topicId}`
`GET /admin/v1/topics/{topicId}`
`GET /admin/v1/topics`
`GET /admin/v1/topics/host`
`POST /admin/v1/topics`
`PUT /admin/v1/topics/{topicId}`
`GET /admin/v1/users/me`
`GET /admin/v1/users/{userId}`
`GET /admin/v1/users/{userId}/followers`
`GET /admin/v1/users/{userId}/followees`
`POST /admin/v1/auth/login`
`POST /admin/v1/auth/refresh`
`POST /admin/v1/invitations`




