package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.entity.MediaFile;

@Tag(name = "用户/经销商-个人媒体库", description = "用户/经销商管理自己上传的图片和视频")
@RestController
@RequestMapping("/api/user/media")
// @PreAuthorize("hasRole('USER') or hasRole('DEALER')") // TODO: 用户和商家都可以有自己的媒体库
public class MediaController {

    @Operation(summary = "上传一个新文件到我的媒体库")
    @PostMapping("/upload")
    public Result<MediaFile> uploadFile(MultipartFile file) {
        // TODO: 1. 获取当前登录用户ID
        //       2. 调用OSS服务上传文件
        //       3. 将文件信息存入 MediaFile 表，并关联上当前用户ID
        //       4. 返回创建成功的 MediaFile 对象
        return Result.success();
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