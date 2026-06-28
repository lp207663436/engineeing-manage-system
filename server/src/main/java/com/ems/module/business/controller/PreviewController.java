package com.ems.module.business.controller;

import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.Attachment;
import com.ems.module.business.service.AttachmentService;
import com.ems.module.system.service.PreviewLogService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@RestController
@RequestMapping("/business/preview")
@RequiredArgsConstructor
@Slf4j
public class PreviewController {

    private final AttachmentService attachmentService;
    private final PreviewLogService previewLogService;

    @GetMapping("/{attachmentId}")
    @RequirePermission("business:attachment:list")
    public void preview(@PathVariable Long attachmentId,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        Attachment attachment = attachmentService.get(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 业务权限校验:超管或附件创建人本人可访问(最低限度校验,完整数据权限由列表接口 @DataScope 保证)
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId == null || (!SecurityContext.isAdmin() && !currentUserId.equals(attachment.getCreateBy()))) {
            throw new BusinessException(403, "无权访问该附件");
        }

        // 解析文件物理路径(与 AttachmentController 上传逻辑保持一致)
        String relative = attachment.getFilePath().startsWith("/")
                ? attachment.getFilePath().substring(1) : attachment.getFilePath();
        // 路径遍历修复:规范化后校验是否在 uploads 基目录下
        Path base = Paths.get(System.getProperty("user.dir"), "uploads").normalize().toAbsolutePath();
        Path filePath = Paths.get(System.getProperty("user.dir"), relative).normalize().toAbsolutePath();
        if (!filePath.startsWith(base)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!Files.exists(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 记录预览日志
        previewLogService.record(
                SecurityContext.getUserId(),
                attachmentId,
                attachment.getName(),
                attachment.getBusinessType(),
                attachment.getBusinessId(),
                resolveClientIp(request)
        );

        // Content-Type 白名单:非白名单类型强制下载,防止 XSS
        Set<String> safeTypes = Set.of("image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf", "text/plain", "video/mp4");
        String contentType = attachment.getFileType();
        String encodedName = URLEncoder.encode(attachment.getName(), "UTF-8").replace("+", "%20");
        if (!safeTypes.contains(contentType)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
        } else {
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"" + encodedName + "\"");
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(Files.size(filePath));

        // 写文件流
        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    /**
     * 解析客户端真实 IP(兼容反向代理)
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP,取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
