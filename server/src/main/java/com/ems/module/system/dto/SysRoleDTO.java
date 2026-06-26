package com.ems.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SysRoleDTO {
    private Long id;
    @NotBlank(message = "角色名不能为空")
    private String name;
    @NotBlank(message = "角色编码不能为空")
    private String code;
    private Integer dataScope;
    private Integer sort;
    private Integer status;
    private List<Long> menuIds;
}
