package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenanceTaskDTO {
    private Long id;
    @NotBlank(message = "工单编号不能为空")
    private String code;
    private Long projectId;
    private Long pointId;
    private Long equipmentId;
    @NotBlank(message = "任务类型不能为空")
    private String type;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    private Long reporterId;
    private Long handlerId;
    private String handleMethod;
    private String partsUsed;
    private String status;
    private String planDate;
    private String planInspectDate;
    private String completeDate;
    private String remark;
}
