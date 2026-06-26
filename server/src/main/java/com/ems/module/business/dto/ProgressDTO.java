package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProgressDTO {
    private Long id;
    @NotBlank(message = "进度编号不能为空")
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;
    private String planStartDate;
    private String planEndDate;
    private String actualStartDate;
    private String actualEndDate;
    private Integer progressPercent;
    private Long managerId;
    private String status;
    private String remark;
}
