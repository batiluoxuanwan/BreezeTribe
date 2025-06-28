package org.whu.backend.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.post.PostCreateRequestDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.post.PostUpdateRequestDto;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.user.UserPostService;
import org.whu.backend.util.AccountUtil;

@Tag(name = "用户-游记管理", description = "用户发布和管理自己的游记")
@RestController
@RequestMapping("/api/user/posts")
@PreAuthorize("hasRole('USER')")
@Slf4j
public class UserPostController {

    @Autowired
    private UserPostService userPostService;
    @Autowired
    private DtoConverter dtoConverter;

    @Operation(summary = "发布一篇新游记")
    @PostMapping
    public Result<PostDetailDto> createPost(@Valid @RequestBody PostCreateRequestDto createRequestDto) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问发布游记接口, 标题: '{}'", currentUserId, createRequestDto.getTitle());

        TravelPost createdPost = userPostService.createPost(createRequestDto, currentUserId);

        PostDetailDto dto = dtoConverter.convertPostToDetailDto(createdPost);

        return Result.success(dto);
    }


    // 获取我发布的游记列表（分页）
    @Operation(summary = "获取我发布的游记列表（分页）")
    @GetMapping
    public Result<PageResponseDto<PostSummaryDto>> getMyPosts(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问获取自己的游记列表接口", currentUserId);

        PageResponseDto<PostSummaryDto> resultPage = userPostService.getMyPosts(currentUserId, pageRequestDto);
        return Result.success(resultPage);
    }

    // 获取我发布的某一篇游记的详细信息
    @Operation(summary = "获取我发布的某一篇游记的详细信息")
    @GetMapping("/{id}")
    public Result<PostDetailDto> getMyPostDetails(@PathVariable String id) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问获取自己的游记详情接口, Post ID: {}", currentUserId, id);

        PostDetailDto postDetails = userPostService.getMyPostDetails(id, currentUserId);
        return Result.success(postDetails);
    }


    // 更新我发布的某一篇游记
    @Operation(summary = "更新我发布的某一篇游记")
    @PutMapping("/{id}")
    public Result<PostDetailDto> updatePost(@PathVariable String id, @Valid @RequestBody PostUpdateRequestDto updateRequestDto) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问更新游记接口, Post ID: {}", currentUserId, id);

        TravelPost updatedPost = userPostService.updatePost(id, updateRequestDto, currentUserId);
        PostDetailDto dto = dtoConverter.convertPostToDetailDto(updatedPost);

        return Result.success(dto);
    }

    // 删除我发布的某一篇游记
    @Operation(summary = "删除我发布的某一篇游记")
    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable String id) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问删除游记接口, Post ID: {}", currentUserId, id);

        userPostService.deletePost(id, currentUserId);

        return Result.success("游记删除成功");
    }
}