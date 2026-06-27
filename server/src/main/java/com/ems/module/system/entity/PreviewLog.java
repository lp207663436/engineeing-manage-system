package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件预览日志(独立结构,不继承 BaseEntity)
 */
@Data
@TableName("preview_log")
public class PreviewLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long attachmentId;

    private String attachmentName;

    private String businessType;

    private Long businessId;

    private String ip;

    private LocalDateTime previewTime;
}
