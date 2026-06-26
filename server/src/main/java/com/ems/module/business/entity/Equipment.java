package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment")
public class Equipment extends BaseEntity {
    private String code;
    private String name;
    private String brand;
    private String model;
    private String serialNumber;
    private String category;
    private String specs;
    private LocalDate commissioningDate;
    private LocalDate warrantyExpiry;
    private String status;
    private Long projectId;
    private Long pointId;
}
