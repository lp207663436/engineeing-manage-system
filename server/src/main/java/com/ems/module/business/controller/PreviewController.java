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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@RestController
@RequestMapping("/business/preview")
@RequiredArgsConstructor
@Slf4j
public class PreviewController {

    private final AttachmentService attachmentService;
    private final PreviewLogService previewLogService;

    /** 需要加水印的图片类型 */
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    /** 安全预览白名单类型(可 inline 展示) */
    private static final Set<String> SAFE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain", "video/mp4");

    @GetMapping("/{attachmentId}")
    @RequirePermission("business:attachment:list")
    public void preview(@PathVariable Long attachmentId,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        Attachment attachment = attachmentService.get(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 业务权限校验:超管或附件创建人本人或当前用户 dataScope<=3(本部门及以下)可访问
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(403, "无权访问该附件");
        }
        boolean allowed = SecurityContext.isAdmin()
                || currentUserId.equals(attachment.getCreateBy());
        if (!allowed) {
            Integer dataScope = SecurityContext.get() != null ? SecurityContext.get().getDataScope() : null;
            if (dataScope == null || dataScope > 3) {
                throw new BusinessException(403, "无权访问该附件");
            }
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

        String contentType = attachment.getFileType();
        String encodedName = URLEncoder.encode(attachment.getName(), "UTF-8").replace("+", "%20");

        // 非白名单类型(Office 等):强制下载并提示
        if (!SAFE_TYPES.contains(contentType)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
            response.setHeader("X-Preview-Message", "请下载查看");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(Files.size(filePath));
            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            return;
        }

        // 白名单类型 inline 展示
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + encodedName + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 图片预览:叠加水印(用户名+时间戳),半透明灰色文字,斜向重复
        if (IMAGE_TYPES.contains(contentType)) {
            try {
                BufferedImage image = ImageIO.read(filePath.toFile());
                if (image != null) {
                    BufferedImage watermarked = addWatermark(image);
                    String formatName = contentType.substring(contentType.indexOf("/") + 1);
                    if ("jpeg".equals(formatName)) formatName = "jpg";
                    response.setContentLengthLong(-1);
                    OutputStream out = response.getOutputStream();
                    ImageIO.write(watermarked, formatName, out);
                    out.flush();
                    return;
                }
            } catch (Exception e) {
                log.warn("图片水印处理失败,回退原始文件流: {}", e.getMessage());
            }
        }

        // PDF 预览:TODO PDF 水印需要 iText/PDFBox 依赖,暂标注待实现,当前直接输出原始流
        if ("application/pdf".equals(contentType)) {
            // TODO: PDF 水印需要引入 iText/PDFBox 依赖,解析 PDF 页面后叠加水印文字,暂标注待实现
            log.debug("PDF 预览暂未实现水印,直接输出原始文件: {}", attachment.getName());
        }

        // 默认:直接写文件流(含 text/plain、video/mp4、PDF 及水印失败回退)
        response.setContentLengthLong(Files.size(filePath));
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
     * 为图片叠加水印:用户名+时间戳,半透明灰色文字,斜向(-30度)重复平铺。
     */
    private BufferedImage addWatermark(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        // 创建带 alpha 通道的目标图(保证水印透明度可正确合成)
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            // 绘制原图背景
            g.drawImage(original, 0, 0, null);
            // 水印文字:用户名 + 时间戳
            String username = SecurityContext.get() != null && SecurityContext.get().getUsername() != null
                    ? SecurityContext.get().getUsername() : "unknown";
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String watermarkText = username + " " + time;

            // 半透明灰色文字
            g.setColor(new Color(128, 128, 128, 80));
            int fontSize = Math.max(12, Math.min(w, h) / 20);
            g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            // 斜向 -30 度
            g.setTransform(AffineTransform.getRotateInstance(Math.toRadians(-30), w / 2.0, h / 2.0));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

            // 斜向重复平铺
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(watermarkText);
            int textHeight = fm.getHeight();
            int stepX = textWidth + 80;
            int stepY = textHeight + 60;
            // 覆盖比图片更大的区域以填满旋转后可视区
            for (int y = -h; y < h * 2; y += stepY) {
                for (int x = -w; x < w * 2; x += stepX) {
                    g.drawString(watermarkText, x, y);
                }
            }
        } finally {
            g.dispose();
        }
        return result;
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
