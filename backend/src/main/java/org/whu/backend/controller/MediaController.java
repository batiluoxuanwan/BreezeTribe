package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.mediafile.FileUploadRequestDto;
import org.whu.backend.dto.mediafile.FileUploadResponseDto;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.service.MediaService;
import org.whu.backend.util.AliyunOssUtil;
import org.whu.backend.util.SecurityUtil;

import java.io.IOException;

@Tag(name = "用户/经销商-个人媒体库", description = "用户/经销商管理自己上传的图片和视频")
@RestController
@Slf4j
@RequestMapping("/api/user/media")
// @PreAuthorize("hasRole('USER') or hasRole('DEALER')") // TODO: 用户和商家都可以有自己的媒体库
public class MediaController {
    @Autowired
    MediaService userMediaService;

    public static final long EXPIRE_TIME = 60 * 60 * 4;
    public static final String IMAGE_PROCESS = "image/resize,l_1600/quality,q_50";

    @Operation(
            summary = "上传文件 (通过DTO定义表单)",
            description = "接收包含文件的表单数据并处理上传。",
            // 2. 关键改动：明确描述请求体
            requestBody = @RequestBody( // 使用 io.swagger.v3.oas.annotations.parameters.RequestBody
                    description = "包含文件和其他可能的表单字段",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            // schema 指向你的DTO，Swagger会根据DTO中的@Schema注解来渲染表单字段
                            schema = @Schema(implementation = FileUploadRequestDto.class)
                    )
            )
    )
    @PostMapping("/upload")
    public Result<FileUploadResponseDto> uploadFile(@Valid FileUploadRequestDto dto) throws IOException {
        String currentUserId = SecurityUtil.getCurrentUserId();
        log.info("用户ID '{}' 访问文件上传接口", currentUserId);

        // 1. 调用Service完成上传和数据库保存
        MediaFile savedFile = userMediaService.uploadAndSaveFile(dto.getFile(), currentUserId);

        // 2. 构建返回给前端的DTO TODO: 目前只对图片进行了处理，后续还应该处理视频，可以测试升级成秒传
        FileUploadResponseDto responseDto = FileUploadResponseDto.builder()
                .fileId(savedFile.getId())
                .url(AliyunOssUtil.generatePresignedGetUrl(savedFile.getObjectKey(),EXPIRE_TIME,IMAGE_PROCESS))
                .build();

        return Result.success("文件上传成功", responseDto);
    }

    @Operation(summary = "获取我的媒体库文件列表（分页）")
    @GetMapping
    public Result<PageResponseDto<MediaFile>> getMyMediaFiles(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 1. 获取当前登录用户ID
        //       2. 分页查询与该用户ID关联的 MediaFile
        return Result.success();
    }

    @Operation(summary = "从我的媒体库删除一个文件")
    @DeleteMapping("/{fileId}")
    public Result<?> deleteMediaFile(@PathVariable String fileId) {
        // TODO: 1. 获取当前登录用户ID
        //       2. 验证该文件是否属于此用户
        //       3. (可选)检查该文件是否还在被其他地方（如已发布的旅行团）使用，如果在使用，可以禁止删除或给出提示
        //       4. 从数据库和OSS上删除文件
        return Result.success("文件已删除");
    }
}