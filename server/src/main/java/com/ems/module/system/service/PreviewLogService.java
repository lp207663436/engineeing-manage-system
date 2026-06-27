package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.module.system.entity.PreviewLog;
import com.ems.module.system.mapper.PreviewLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PreviewLogService {

    private final PreviewLogMapper previewLogMapper;

    /**
     * 创建预览记录
     */
    public void record(Long userId, Long attachmentId, String attachmentName,
                       String businessType, Long businessId, String ip) {
        try {
            PreviewLog previewLog = new PreviewLog();
            previewLog.setUserId(userId);
            previewLog.setAttachmentId(attachmentId);
            previewLog.setAttachmentName(attachmentName);
            previewLog.setBusinessType(businessType);
            previewLog.setBusinessId(businessId);
            previewLog.setIp(ip);
            previewLog.setPreviewTime(LocalDateTime.now());
            previewLogMapper.insert(previewLog);
        } catch (Exception e) {
            // 日志记录失败不应影响预览主流程
            log.error("记录预览日志失败: attachmentId={}, userId={}", attachmentId, userId, e);
        }
    }

    /**
     * 分页查询预览日志(管理员审计用)
     */
    public PageResult<PreviewLog> page(long pageNum, long pageSize) {
        LambdaQueryWrapper<PreviewLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PreviewLog::getPreviewTime);
        Page<PreviewLog> page = previewLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }
}
