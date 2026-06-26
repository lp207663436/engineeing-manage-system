package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private Long deptId;
    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private Integer status;
}
