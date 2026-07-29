package com.app.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发布帖文请求")
public class PostCreateRequest {

    @Size(max = 2000, message = "文字内容最长 2000 个字符")
    @Schema(description = "文字内容")
    private String content;

    @Schema(description = "图片 URL 列表（通过预签名上传后获取的 URL）")
    private List<String> images;

    @Size(max = 200, message = "位置信息最长 200 个字符")
    @Schema(description = "发布位置")
    private String location;
}
