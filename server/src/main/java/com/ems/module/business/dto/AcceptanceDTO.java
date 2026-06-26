package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptanceDTO {
    private Long id;
    @NotBlank(message = "验收单编号不能为空")
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    private Long quoteId;
    private Long acceptorId;
    private String acceptDate;
    private String actualQuantity;
    private String result;
    private Integer rectifyCount;
    private String remark;
}
