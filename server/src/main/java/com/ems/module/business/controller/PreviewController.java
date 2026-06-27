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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        // 解析文件物理路径(与 AttachmentController 上传逻辑保持一致)
        String relative = attachment.getFilePath().startsWith("/")
                ? attachment.getFilePath().substring(1) : attachment.getFilePath();
        Path filePath = Paths.get(System.getProperty("user.dir"), relative);
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

        // 设置响应头:在线预览(inline),非下载
        String contentType = attachment.getFileType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline");
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
