package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractPaymentDTO {
    private Long id;
    private String code;
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    @NotBlank(message = "类型不能为空")
    private String type;
    /**
     * 计划日期(yyyy-MM-dd)
     */
    private String planDate;
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    /**
     * 实际日期(yyyy-MM-dd)
     */
    private String actualDate;
    private String invoiceNo;
    private String status;
    private String remark;
}
