package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierDTO {
    private Long id;
    @NotBlank(message = "供应商编号不能为空")
    private String code;
    @NotBlank(message = "供应商名称不能为空")
    private String name;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String bankAccount;
    private String bankName;
    private String remark;
}
