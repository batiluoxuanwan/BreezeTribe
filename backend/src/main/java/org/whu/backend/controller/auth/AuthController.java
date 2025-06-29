package org.whu.backend.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.auth.*;
import org.whu.backend.dto.mediafile.FileUploadRequestDto;
import org.whu.backend.service.auth.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 重置密码
    @PutMapping("/resetPassword")
    public Result<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    // 修改头像
    @PutMapping("/updateAvatar")
    @Operation(
            summary = "上传文件 (通过DTO定义表单)",
            description = "接收包含文件的表单数据并处理上传。",
            // 2. 关键改动：明确描述请求体
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody( // 使用 io.swagger.v3.oas.annotations.parameters.RequestBody
                    description = "包含文件和其他可能的表单字段",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            // schema 指向你的DTO，Swagger会根据DTO中的@Schema注解来渲染表单字段
                            schema = @Schema(implementation = FileUploadRequestDto.class)
                    )
            )
    )
    public Result<?> updateAvatar(FileUploadRequestDto dto) {
        return authService.updateAvatar(dto.getFile());
    }

    // 修改用户名
    @PutMapping("/updateUsername")
    public Result<?> updateUsername(@RequestBody UpdateUsernameRequest request) {
        return authService.updateUsername(request);
    }

    @GetMapping("/me")
    public Result<Medto> me() {
        return Result.success("返回成功",authService.me());
    }

    // 换绑
    @PutMapping("/rebind")
    public Result<?> rebind(@RequestBody RebindRequest request) {
        return authService.rebind(request);
    }
}
