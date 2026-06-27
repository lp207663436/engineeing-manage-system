package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_log")
public class ApprovalLog extends BaseEntity {
    private Long flowId;
    private String businessType;
    private Long businessId;
    private Integer nodeOrder;
    private Long approverId;
    private String result;
    private String opinion;
    private LocalDateTime approveTime;
}
