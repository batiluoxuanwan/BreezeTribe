package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.whu.backend.dto.tag.TagCreateDto;
import org.whu.backend.dto.tag.TagDto;
import org.whu.backend.dto.tag.TagUpdateDto;
import org.whu.backend.service.admin.AdminTagService;

import java.util.List;

/**
 * 管理员-标签库管理
 * <p>
 * 提供对平台全局标签的增、删、改、查功能。
 */
@Tag(name = "管理员-标签库管理", description = "提供对平台全局标签的增、删、改、查功能")
@RestController
@Slf4j
@RequestMapping("/api/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    @Autowired
    private AdminTagService adminTagService;

    @Operation(summary = "创建新标签（可以批量）")
    @PostMapping
    public Result<List<TagDto>> createTag(@Valid @RequestBody List<TagCreateDto> createDtos) {
        log.info("请求日志：管理员正在创建一个新标签，标签数量: {} ", createDtos.size());
        List<TagDto> createdTag = adminTagService.createTags(createDtos);
        return Result.success("标签创建成功", createdTag);
    }

//    @Operation(summary = "获取所有标签列表", description = "可以按分类进行筛选")
//    @GetMapping
//    public Result<List<TagDto>> getAllTags(
//            @Parameter(description = "按分类筛选（可选），如 THEME, TARGET_AUDIENCE 等")
//            @RequestParam(required = false) org.whu.backend.entity.Tag.TagCategory category) {
//        log.info("请求日志：管理员正在查询标签列表，筛选分类: {}", category);
//        List<TagDto> tags = adminTagService.getAllTags(category);
//        return Result.success("查询成功", tags);
//    }

    @Operation(summary = "获取所有标签列表（分页）", description = "可以按分类进行筛选")
    @GetMapping
    public Result<PageResponseDto<TagDto>> getAllTags(
            @Parameter(description = "按分类筛选（可选），如 THEME, TARGET_AUDIENCE 等")
            @RequestParam(required = false) org.whu.backend.entity.Tag.TagCategory category,
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        log.info("请求日志：管理员正在查询标签列表（分页），筛选分类: {}, 分页参数: {}", category, pageRequestDto);
        PageResponseDto<TagDto> resultPage = adminTagService.getAllTags(category, pageRequestDto);
        return Result.success("查询成功", resultPage);
    }

    @Operation(summary = "更新一个标签的信息")
    @PutMapping("/{id}")
    public Result<TagDto> updateTag(
            @Parameter(description = "要更新的标签ID") @PathVariable String id,
            @Valid @RequestBody TagUpdateDto updateDto) {
        log.info("请求日志：管理员正在更新标签ID '{}'", id);
        TagDto updatedTag = adminTagService.updateTag(id, updateDto);
        return Result.success("标签更新成功", updatedTag);
    }

    @Operation(summary = "删除一个标签", description = "如果标签已被任何产品或游记使用，则无法删除。")
    @DeleteMapping("/{id}")
    public Result<?> deleteTag(@Parameter(description = "要删除的标签ID") @PathVariable String id) {
        log.info("请求日志：管理员正在尝试删除标签ID '{}'", id);
        adminTagService.deleteTag(id);
        return Result.success("标签删除成功");
    }
}