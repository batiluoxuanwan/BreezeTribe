package org.whu.backend.dto.mediafile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponseDto {
    private String fileId;      // 文件在我们系统中的唯一ID
    private String url;         // 文件的可访问URL
}