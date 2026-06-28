package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项(系统表)
 * 继承 BaseEntity 以支持软删除(deleted)和审计字段(create_by/create_time/update_time)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private Long dictId;

    private String label;

    private String value;

    private Integer sort;
}
