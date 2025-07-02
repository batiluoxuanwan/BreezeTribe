package org.whu.backend.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.tag.TagCreateDto;
import org.whu.backend.dto.tag.TagDto;
import org.whu.backend.dto.tag.TagUpdateDto;
import org.whu.backend.entity.Tag;
import org.whu.backend.repository.TagRepository;
import org.whu.backend.service.DtoConverter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员标签管理服务
 */
@Service
@Slf4j
public class AdminTagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DtoConverter dtoConverter;

    /**
     * 创建一个新标签
     */
    @Transactional
    public List<TagDto> createTags(List<TagCreateDto> createDtos) {
        return createDtos.stream().map(createDto -> {
            // 检查标签名是否已存在
            tagRepository.findByName(createDto.getName()).ifPresent(tag -> {
                throw new BizException("已存在同名标签：" + createDto.getName());
            });

            Tag newTag = new Tag();
            newTag.setName(createDto.getName());
            newTag.setCategory(createDto.getCategory());

            Tag savedTag = tagRepository.save(newTag);
            log.info("服务层：成功创建新标签，ID: {}, 名称: {}", savedTag.getId(), savedTag.getName());
            return dtoConverter.convertTagToDto(savedTag);
        }).collect(Collectors.toList());
    }

    /**
     * 获取标签列表，可按分类筛选
     */
    @Transactional(readOnly = true)
    public List<TagDto> getAllTags(Tag.TagCategory category) {
        List<Tag> tags;
        if (category != null) {
            log.info("服务层：按分类 '{}' 查询标签", category);
            tags = tagRepository.findByCategory(category);
        } else {
            log.info("服务层：查询所有标签");
            tags = tagRepository.findAll();
        }
        return tags.stream()
                .map(dtoConverter::convertTagToDto)
                .collect(Collectors.toList());
    }

    /**
     * 更新一个标签
     */
    @Transactional
    public TagDto updateTag(String id, TagUpdateDto updateDto) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new BizException("找不到ID为 " + id + " 的标签"));

        if (updateDto.getName() != null && !updateDto.getName().isBlank()) {
            // 如果要更新名称，需要检查新名称是否与别的标签重复
            tagRepository.findByName(updateDto.getName()).ifPresent(existingTag -> {
                if (!existingTag.getId().equals(id)) {
                    throw new BizException("更新失败，已存在同名标签：" + updateDto.getName());
                }
            });
            tag.setName(updateDto.getName());
        }
        if (updateDto.getCategory() != null) {
            tag.setCategory(updateDto.getCategory());
        }

        Tag updatedTag = tagRepository.save(tag);
        log.info("服务层：成功更新标签ID '{}'", updatedTag.getId());
        return dtoConverter.convertTagToDto(updatedTag);
    }

    /**
     * 删除一个标签
     */
    @Transactional
    public void deleteTag(String id) {
        // 1. 检查标签是否存在
        if (!tagRepository.existsById(id)) {
            throw new BizException("找不到要删除的标签");
        }

//        // 2. 【核心业务规则】检查标签是否正在被使用
//        if (tagRepository.isTagInUse(id)) {
//            log.warn("服务层：尝试删除一个正在使用中的标签ID '{}'，操作被阻止。", id);
//            throw new BizException("无法删除，该标签已被一个或多个产品/游记使用");
//        }

        // 3. 执行删除
        tagRepository.deleteById(id);
        log.info("服务层：成功删除标签ID '{}'", id);
    }
}