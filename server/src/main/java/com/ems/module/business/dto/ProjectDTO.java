package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectDTO {
    private Long id;
    @NotBlank(message = "项目编号不能为空")
    private String code;
    @NotBlank(message = "项目名称不能为空")
    private String name;
    private String customerName;
    private Long managerId;
    private String address;
    private String startDate;
    private String endDate;
    private String type;
    private String status;
    private String description;
}
