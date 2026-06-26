package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_point")
public class MaintenancePoint extends BaseEntity {
    private String code;
    private Long projectId;
    private String name;
    private String location;
    private String equipmentList;
    private Long managerId;
    /**
     * 点位状态:WAITING_QUOTE待报价/QUOTED已报价/CONSTRUCTING施工中/WAITING_ACCEPTANCE待验收/ACCEPTED已验收/SETTLED已结算
     */
    private String status;
}
