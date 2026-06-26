package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quarterly_settlement")
public class QuarterlySettlement extends BaseEntity {
    private String code;
    /**
     * 关联维保主合同(maintenance_contract.id)
     */
    private Long contractId;
    /**
     * 关联项目
     */
    private Long projectId;
    /**
     * 第几期(1-based)
     */
    private Integer periodNo;
    /**
     * 本期开始日
     */
    private LocalDate periodStartDate;
    /**
     * 本期结束日
     */
    private LocalDate periodEndDate;
    /**
     * 本期维保费金额
     */
    private BigDecimal amount;
    /**
     * 适用金额版本
     */
    private Integer amountVersion;
    /**
     * DRAFT草稿/REVIEWED已审核/CONFIRMED已确认/INVOICED已开票/RECEIVED已回款/CLOSED已关闭
     */
    private String status;
    private String invoiceNo;
    /**
     * 已回款金额
     */
    private BigDecimal receivedAmount;
    /**
     * 回款日期
     */
    private LocalDate receivedDate;
    private String remark;
}
