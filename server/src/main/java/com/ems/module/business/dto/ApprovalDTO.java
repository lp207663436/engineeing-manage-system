package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalDTO {
    @NotBlank
    private String businessType;
    @NotNull
    private Long businessId;
    @NotBlank(message = "审批结果不能为空")
    private String result;
    private String opinion;
}
