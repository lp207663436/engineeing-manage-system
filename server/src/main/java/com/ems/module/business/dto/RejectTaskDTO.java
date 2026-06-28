package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectTaskDTO {
    @NotBlank(message = "打回原因不能为空")
    private String reason;
}
