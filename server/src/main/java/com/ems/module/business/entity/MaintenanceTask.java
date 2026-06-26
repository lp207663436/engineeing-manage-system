package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_task")
public class MaintenanceTask extends BaseEntity {
    private String code;
    private Long projectId;
    private Long pointId;
    private Long equipmentId;
    /**
     * 任务类型 INSPECTION巡检/REPAIR故障报修
     */
    private String type;
    private String title;
    private String description;
    private Long reporterId;
    private Long handlerId;
    private String handleMethod;
    private String partsUsed;
    /**
     * PENDING待派单/ASSIGNED已派单/PROCESSING处理中/WAITING_ACCEPTANCE待验收/COMPLETED已完成/CLOSED已关闭
     */
    private String status;
    private LocalDate planDate;
    private LocalDate completeDate;
    private String remark;
}
