package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow")
public class ApprovalFlow extends BaseEntity {
    private String code;
    private String name;
    private String businessType;
    private Integer enabled;
    private String remark;
}
