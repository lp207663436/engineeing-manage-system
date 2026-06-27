package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerDTO {
    private Long id;
    @NotBlank(message = "客户编号不能为空")
    private String code;
    @NotBlank(message = "客户名称不能为空")
    private String name;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String bankAccount;
    private String bankName;
    private String remark;
}
