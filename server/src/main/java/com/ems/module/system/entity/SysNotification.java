package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知(独立结构,不继承 BaseEntity)
 */
@Data
@TableName("sys_notification")
public class SysNotification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收人
     */
    private Long userId;

    private String title;

    private String content;

    /**
     * SETTLEMENT/APPROVAL/OVERDUE/WARRANTY/ACCEPTANCE
     */
    private String type;

    /**
     * 业务类型(可选)
     */
    private String businessType;

    /**
     * 业务ID(可选)
     */
    private Long businessId;

    /**
     * 0未读 1已读
     */
    private Integer isRead;

    private LocalDateTime createTime;
}
