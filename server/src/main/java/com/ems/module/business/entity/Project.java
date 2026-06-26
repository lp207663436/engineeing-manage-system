package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {
    private String code;
    private String name;
    private String customerName;
    private Long managerId;
    private String address;
    private LocalDate startDate;
    private LocalDate endDate;
    private String type;
    private String status;
    private String description;
}
