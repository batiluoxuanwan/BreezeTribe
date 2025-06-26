package org.whu.backend.dto.mediafile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaFileDto {
    @Schema(description = "文件ID")
    private String id;

    @Schema(description = "文件的公开访问URL (带签名)")
    private String url;

    @Schema(description = "文件类型 (MIME type)")
    private String fileType;

    @Schema(description = "文件大小 (字节)")
    private Long fileSize;

    @Schema(description = "上传时间")
    private LocalDateTime createdTime;
}