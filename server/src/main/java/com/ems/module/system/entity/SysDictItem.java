package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 数据字典项(系统表,简化,不继承 BaseEntity)
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long dictId;

    private String label;

    private String value;

    private Integer sort;
}
