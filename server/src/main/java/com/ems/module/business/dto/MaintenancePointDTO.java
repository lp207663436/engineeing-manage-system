package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenancePointDTO {
    private Long id;
    @NotBlank(message = "点位编号不能为空")
    private String code;
    @NotBlank(message = "点位名称不能为空")
    private String name;
    private Long projectId;
    private String location;
    private String equipmentList;
    private Long managerId;
    private String status;
}
