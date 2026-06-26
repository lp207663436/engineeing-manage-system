package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_record")
public class MaintenanceRecord extends BaseEntity {
    private String code;
    private Long taskId;
    private Long projectId;
    private Long pointId;
    private Long equipmentId;
    /**
     * 记录类型 INSPECTION巡检/REPAIR维修/MAINTENANCE保养
     */
    private String recordType;
    private String content;
    private String partsUsed;
    private Long recorderId;
    private LocalDate recordDate;
    private String remark;
}
