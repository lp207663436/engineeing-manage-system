package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractChangeDTO {
    private Long id;
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    @NotBlank(message = "变更类型不能为空")
    private String changeType;
    private String changeDesc;
    private Long supplementFileId;
    private Long approverId;
    private String status;
    private BigDecimal originalAmount;
    private BigDecimal newAmount;
    private String changeField;
    private String remark;
}
