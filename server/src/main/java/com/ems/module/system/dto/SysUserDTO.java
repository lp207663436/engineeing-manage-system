package com.ems.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysUserDTO {
    private Long id;
    private Long deptId;
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String password;
    @NotBlank(message = "姓名不能为空")
    private String name;
    private String phone;
    private String email;
    private Integer status;
}
