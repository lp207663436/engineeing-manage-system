package com.ems.module.business.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDTO {
    @NotNull(message = "处理人不能为空")
    private Long handlerId;
}
