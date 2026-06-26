package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ContractDTO {
    private Long id;
    @NotBlank(message = "合同编号不能为空")
    private String code;
    @NotBlank(message = "合同名称不能为空")
    private String name;
    private String partyA;
    private String partyB;
    private String signDate;
    private BigDecimal amount;
    private String category;
    private String paymentMethod;
    private Long projectId;
    private String status;
    private String startDate;
    private String endDate;
    private String remark;
}
