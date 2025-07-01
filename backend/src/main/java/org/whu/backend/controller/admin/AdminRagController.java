package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.rag.RagDataService;
import org.whu.backend.util.AliyunOssUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@Tag(name = "管理员-RAG智能问答管理", description = "用于同步和导出RAG知识库")
@RestController
@RequestMapping("/api/admin/rag")
@PreAuthorize("hasRole('ADMIN')") // 确保只有管理员能访问
@Slf4j
public class AdminRagController {

    @Autowired
    private RagDataService ragDataService;

    @Operation(summary = "手动触发RAG知识库同步", description = "从旅行团数据同步到RAG知识库中间表")
    @PostMapping("/sync")
    public Result<?> triggerSync() {
        log.info("接收到手动同步RAG知识库的请求...");
        // 核心业务逻辑
        ragDataService.syncKnowledgeBase();
        log.info("RAG知识库同步任务已成功完成！");
        // 按照你们的风格，返回统一的Result格式
        return Result.success("知识库同步任务已成功完成！");
    }

    @Operation(summary = "导出知识库为XLSX文件并获取下载链接", description = "将知识库内容生成XLSX文件上传到OSS，并返回一个有时效性的下载URL")
    @GetMapping("/export-xlsx-url")
    public Result<String> exportKnowledgeToXlsxUrl() throws IOException {
        log.info("接收到导出RAG知识库为XLSX的请求...");

        // 1. 从服务层获取XLSX文件的字节数组
        byte[] xlsxBytes = ragDataService.exportKnowledgeToXlsxBytes();
        if (xlsxBytes.length == 0) {
            return Result.failure("知识库为空或生成文件失败，无需导出。");
        }

        // 2. 定义OSS对象名
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String objectName = String.format("rag-exports/breezetribe_knowledge_base_%s.xlsx", timestamp); //后缀改为.xlsx

        // 3. 将字节数组转换为输入流并上传
        InputStream inputStream = new ByteArrayInputStream(xlsxBytes);
        AliyunOssUtil.upload(objectName, inputStream);
        log.info("XLSX文件成功上传至OSS，对象名为: {}", objectName);

        // 4. 生成并返回下载链接
        String downloadUrl = AliyunOssUtil.generatePresignedGetUrl(objectName, DtoConverter.EXPIRE_TIME);
        log.info("成功生成OSS下载链接: {}", downloadUrl);

        return Result.success("XLSX文件已生成并上传，请通过返回的URL下载。", downloadUrl);
    }
}