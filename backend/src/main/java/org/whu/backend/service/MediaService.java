package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.repository.FileUploadResponseDto;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.util.AliyunOssUtil;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class MediaService {
    @Autowired
    private FileUploadResponseDto mediaFileRepository;

    @Autowired
    private AuthRepository accountRepository; // 用于查找上传者

    @Transactional
    public MediaFile uploadAndSaveFile(MultipartFile file, String uploaderId) throws IOException {
        log.info("用户ID '{}' 正在上传新文件: {}", uploaderId, file.getOriginalFilename());

        // 1. 查找上传者账户是否存在
        Account uploader = accountRepository.findById(uploaderId)
                .orElseThrow(() -> new BizException("上传失败：找不到用户 " + uploaderId));

        // 2. 调用OSS服务上传文件
        String objectKey = uploader.getId()+"/"+ UUID.randomUUID();
        AliyunOssUtil.upload(objectKey,file.getInputStream());

        // 3. 创建并保存MediaFile实体到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setObjectKey(objectKey);
        mediaFile.setFileType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setUploaderId(uploader); // 关联上传者

        MediaFile savedFile = mediaFileRepository.save(mediaFile);
        log.info("文件信息已成功存入数据库, File ID: {}", savedFile.getId());

        return savedFile;
    }
}
