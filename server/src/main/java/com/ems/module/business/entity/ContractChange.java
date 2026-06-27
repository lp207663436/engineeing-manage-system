package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_change")
public class ContractChange extends BaseEntity {
    private Long contractId;
    /**
     * 变更类型:AMOUNT_CHANGE金额变更/SCOPE_CHANGE范围变更/TERM_CHANGE期限变更/OTHER其他
     */
    private String changeType;
    private String changeDesc;
    private Long supplementFileId;
    private Long approverId;
    private LocalDateTime approveTime;
    /**
     * 审核状态:NONE无/PENDING待审核/APPROVED已通过/REJECTED已驳回
     */
    private String status;
    /**
     * 原金额
     */
    private BigDecimal originalAmount;
    /**
     * 变更后金额
     */
    private BigDecimal newAmount;
    /**
     * 变更字段(如 AMOUNT/START_DATE/END_DATE)
     */
    private String changeField;
    private String remark;
}
