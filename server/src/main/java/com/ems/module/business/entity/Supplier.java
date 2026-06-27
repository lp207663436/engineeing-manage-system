package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier")
public class Supplier extends BaseEntity {
    private String code;
    private String name;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String bankAccount;
    private String bankName;
    private String remark;
}
