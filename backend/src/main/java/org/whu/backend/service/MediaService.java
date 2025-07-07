package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.mediafile.MediaFileDto;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.association.PackageImage;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.travelpost.PostImage;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.MediaFileRepository;
import org.whu.backend.repository.PackageImageRepository;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.repository.post.PostImageRepository;
import org.whu.backend.util.AliyunOssUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MediaService {
    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Autowired
    private AuthRepository accountRepository; // 用于查找上传者
    @Autowired
    private PackageImageRepository packageImageRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private PostImageRepository postImageRepository;

    @Transactional
    public MediaFile uploadAndSaveFile(MultipartFile file, String uploaderId) throws IOException {
        log.info("用户ID '{}' 正在上传新文件: {}", uploaderId, file.getOriginalFilename());

        // 1. 查找上传者账户是否存在
        Account uploader = accountRepository.findById(uploaderId)
                .orElseThrow(() -> new BizException("上传失败：找不到用户 " + uploaderId));

        // 2. 调用OSS服务上传文件
        String objectKey = uploader.getId() + "/" + UUID.randomUUID();
        AliyunOssUtil.upload(objectKey, file.getInputStream());

        // 3. 创建并保存MediaFile实体到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setObjectKey(objectKey);
        mediaFile.setFileType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setUploader(uploader); // 关联上传者

        MediaFile savedFile = mediaFileRepository.save(mediaFile);
        log.info("文件信息已成功存入数据库, File ID: {}", savedFile.getId());

        return savedFile;
    }

    // 获取当前用户的媒体库文件列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<MediaFileDto> getMyMediaFiles(String currentUserId, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在获取自己的媒体库列表, 分页参数: {}", currentUserId, pageRequestDto);

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        Page<MediaFile> mediaFilePage = mediaFileRepository.findByUploaderId(currentUserId, pageable);

        List<MediaFileDto> dtos = mediaFilePage.getContent().stream()
                .map(dtoConverter::convertMediaFileToDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(mediaFilePage, dtos);
    }


    /**
     * 从我的媒体库删除一个文件
     * 增加了更智能的错误提示，会告知用户文件具体被哪个项目占用了。
     */
    @Transactional
    public void deleteMediaFile(String fileId, String currentUserId) {
        log.info("用户ID '{}' 正在尝试删除媒体文件ID '{}'...", currentUserId, fileId);

        // 1. 查找文件并验证所有权 (逻辑不变)
        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException("找不到ID为 " + fileId + " 的文件"));

        if (!mediaFile.getUploader().getId().equals(currentUserId)) {
            log.error("权限拒绝：用户ID '{}' 试图删除不属于自己的文件ID '{}'。", currentUserId, fileId);
            throw new BizException(HttpStatus.UNAUTHORIZED.value(), "无权删除此文件");
        }

        // 2. 检查文件是否被旅行团使用
        // 我们不再用exists，而是尝试查找第一个使用它的PackageImage记录
        Optional<PackageImage> inUseByPackage = packageImageRepository.findTopByMediaFileId(fileId);
        if (inUseByPackage.isPresent()) {
            TravelPackage blockingPackage = inUseByPackage.get().getTravelPackage();
            String errorMessage = String.format(
                    "无法删除：该文件正在被旅行团 [%s] 使用。请先从其图集中移除。",
                    blockingPackage.getTitle()
            );
            log.warn("删除失败：文件ID '{}' 正在被旅行团ID '{}' 使用。", fileId, blockingPackage.getId());
            throw new BizException(errorMessage);
        }

        // 3. 检查文件是否被游记使用
        Optional<PostImage> inUseByPost = postImageRepository.findTopByMediaFileId(fileId);
        if (inUseByPost.isPresent()) {
            TravelPost blockingPost = inUseByPost.get().getTravelPost();
            String errorMessage = String.format(
                    "无法删除：该文件正在被游记 [%s] 使用。请先从其图集中移除。",
                    blockingPost.getTitle()
            );
            log.warn("删除失败：文件ID '{}' 正在被游记ID '{}' 使用。", fileId, blockingPost.getId());
            throw new BizException(errorMessage);
        }

        // 4. 如果所有检查都通过，则执行删除 (逻辑不变)
        AliyunOssUtil.delete(mediaFile.getObjectKey());
        mediaFileRepository.delete(mediaFile);

        log.info("文件ID '{}' 已被用户ID '{}' 成功从媒体库和OSS中删除。", fileId, currentUserId);
    }
}
