package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_payment")
public class ContractPayment extends BaseEntity {
    /**
     * 单号
     */
    private String code;
    /**
     * 合同ID
     */
    private Long contractId;
    /**
     * RECEIVABLE应收 / PAYABLE应付
     */
    private String type;
    /**
     * 计划日期
     */
    private LocalDate planDate;
    /**
     * 计划金额
     */
    private BigDecimal planAmount;
    /**
     * 实际金额
     */
    private BigDecimal actualAmount;
    /**
     * 实际日期
     */
    private LocalDate actualDate;
    /**
     * 发票号
     */
    private String invoiceNo;
    /**
     * PENDING待收付/RECEIVED已收付/OVERDUE逾期
     */
    private String status;
    private String remark;
}
