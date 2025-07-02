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

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "通过邮箱或手机号注册新账户")
    @PostMapping("/register")
    public Result<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "注册请求体，包含用户名、密码、邮箱/手机号等",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class))
            )
            @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名/邮箱/手机号和密码登录")
    @PostMapping("/login")
    public Result<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登录请求体，包含账号和密码",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "忘记密码时使用验证码重置密码")
    @PutMapping("/resetPassword")
    public Result<?> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "包含验证码、新密码等信息",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class))
            )
            @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * 修改头像（Multipart 上传）
     */
    @Operation(
            summary = "修改头像（上传文件）",
            description = "通过 multipart/form-data 上传头像图片",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "上传的头像文件字段应命名为 file",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = FileUploadRequestDto.class)
                    )
            )
    )
    @PutMapping("/updateAvatar")
    public Result<?> updateAvatar(FileUploadRequestDto dto) {
        return authService.updateAvatar(dto.getFile());
    }

    /**
     * 修改用户名
     */
    @Operation(summary = "修改用户名", description = "更改当前用户的用户名")
    @PutMapping("/updateUsername")
    public Result<?> updateUsername(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "包含新用户名的请求体",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUsernameRequest.class))
            )
            @RequestBody UpdateUsernameRequest request) {
        return authService.updateUsername(request);
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的基本信息")
    @GetMapping("/me")
    public Result<Medto> me() {
        return Result.success("返回成功", authService.me());
    }

    /**
     * 更换绑定手机号或邮箱
     */
    @Operation(summary = "更换绑定信息", description = "更换当前用户绑定的手机号或邮箱")
    @PutMapping("/rebind")
    public Result<?> rebind(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "包含旧绑定、新绑定、验证码等字段",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RebindRequest.class))
            )
            @RequestBody RebindRequest request) {
        return authService.rebind(request);
    }
}
