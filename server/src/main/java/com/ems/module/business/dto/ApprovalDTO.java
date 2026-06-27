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
    private String result;
    private String opinion;
}
