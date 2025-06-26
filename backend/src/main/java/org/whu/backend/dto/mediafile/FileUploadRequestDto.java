package org.whu.backend.dto.mediafile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequestDto {
    @NotNull
    @Schema(description = "要上传的文件", type = "string", format = "binary") // 告诉Swagger这是个文件
    MultipartFile file;
}
