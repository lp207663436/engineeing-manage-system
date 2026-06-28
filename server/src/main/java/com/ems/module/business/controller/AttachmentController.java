package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.Attachment;
import com.ems.module.business.service.AttachmentService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/business/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/page")
    @RequirePermission("business:attachment:list")
    @DataScope
    public Result<PageResult<Attachment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) String businessType,
                                                @RequestParam(required = false) Long businessId) {
        return Result.success(attachmentService.page(pageNum, pageSize, name, businessType, businessId));
    }

    @GetMapping("/list")
    @RequirePermission("business:attachment:list")
    public Result<?> list(@RequestParam String businessType,
                           @RequestParam Long businessId) {
        return Result.success(attachmentService.listByBusiness(businessType, businessId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:attachment:list")
    public Result<Attachment> get(@PathVariable Long id) {
        Attachment attachment = attachmentService.get(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        // 业务权限校验:超管或附件创建人本人可访问(最低限度校验,完整数据权限由列表接口 @DataScope 保证)
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId == null || (!SecurityContext.isAdmin() && !currentUserId.equals(attachment.getCreateBy()))) {
            throw new BusinessException(403, "无权访问该附件");
        }
        return Result.success(attachment);
    }

    @PostMapping("/upload")
    @RequirePermission("business:attachment:upload")
    public Result<Attachment> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam String businessType,
                                      @RequestParam Long businessId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 文件扩展名白名单校验
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BusinessException("文件名无效,必须包含扩展名");
        }
        String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".md");
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
        // 生成存储路径:目录 uploads/yyyy/MM/uuid+原扩展名
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString() + ext;
        // 物理目录用 System.getProperty("user.dir") + "/uploads/" + 年/月
        Path dirPath = Paths.get(System.getProperty("user.dir"), "uploads", yearMonth);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());
        // 相对路径如 /uploads/2026/06/uuid.pdf
        String relativePath = "/uploads/" + yearMonth + "/" + fileName;
        // 构造附件记录
        Attachment attachment = new Attachment();
        attachment.setName(originalName);
        attachment.setFilePath(relativePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setBusinessType(businessType);
        attachment.setBusinessId(businessId);
        attachment.setCreateBy(SecurityContext.getUserId());
        attachmentService.create(attachment);
        return Result.success(attachment);
    }

    @GetMapping("/download/{id}")
    @RequirePermission("business:attachment:list")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        Attachment attachment = attachmentService.get(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        // 业务权限校验:超管或附件创建人本人可访问(最低限度校验,完整数据权限由列表接口 @DataScope 保证)
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId == null || (!SecurityContext.isAdmin() && !currentUserId.equals(attachment.getCreateBy()))) {
            throw new BusinessException(403, "无权访问该附件");
        }
        String relative = attachment.getFilePath().startsWith("/")
                ? attachment.getFilePath().substring(1) : attachment.getFilePath();
        // 路径遍历修复:规范化后校验是否在 uploads 基目录下
        Path base = Paths.get(System.getProperty("user.dir"), "uploads").normalize().toAbsolutePath();
        Path filePath = Paths.get(System.getProperty("user.dir"), relative).normalize().toAbsolutePath();
        if (!filePath.startsWith(base)) {
            throw new BusinessException(403, "非法文件路径");
        }
        if (!Files.exists(filePath)) {
            throw new BusinessException("文件不存在");
        }
        Resource resource = new FileSystemResource(filePath);
        String encodedName = URLEncoder.encode(attachment.getName(), "UTF-8").replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:attachment:delete")
    public Result<Void> delete(@PathVariable Long id) throws IOException {
        Attachment attachment = attachmentService.get(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        // 归属校验
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId == null || (!SecurityContext.isAdmin() && !currentUserId.equals(attachment.getCreateBy()))) {
            throw new BusinessException(403, "无权删除他人附件");
        }
        // 删除物理文件
        String relative = attachment.getFilePath().startsWith("/")
                ? attachment.getFilePath().substring(1) : attachment.getFilePath();
        Path base = Paths.get(System.getProperty("user.dir"), "uploads").normalize().toAbsolutePath();
        Path filePath = Paths.get(System.getProperty("user.dir"), relative).normalize().toAbsolutePath();
        if (filePath.startsWith(base)) {
            Files.deleteIfExists(filePath);
        }
        attachmentService.delete(id);
        return Result.success();
    }
}
