package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    private Long parentId;
    private String name;
    private Integer type;
    private String permission;
    private String path;
    private String icon;
    private Integer sort;
    private Integer status;

    @TableField(exist = false)
    private List<SysMenu> children;
}
