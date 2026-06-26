package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenanceRecordDTO {
    private Long id;
    @NotBlank(message = "记录编号不能为空")
    private String code;
    private Long taskId;
    private Long projectId;
    private Long pointId;
    private Long equipmentId;
    @NotBlank(message = "记录类型不能为空")
    private String recordType;
    @NotBlank(message = "维保内容不能为空")
    private String content;
    private String partsUsed;
    private Long recorderId;
    @NotBlank(message = "记录日期不能为空")
    private String recordDate;
    private String remark;
}
