package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("progress")
public class Progress extends BaseEntity {
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    private String nodeName;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Integer progressPercent;
    private Long managerId;
    private String status;
    private String remark;
}
