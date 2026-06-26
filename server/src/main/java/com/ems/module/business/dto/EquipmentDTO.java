package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EquipmentDTO {
    private Long id;
    @NotBlank(message = "设备编号不能为空")
    private String code;
    @NotBlank(message = "设备名称不能为空")
    private String name;
    private String brand;
    private String model;
    private String serialNumber;
    private String category;
    private String specs;
    private String commissioningDate;
    private String warrantyExpiry;
    private String status;
    private Long projectId;
    private Long pointId;
}
