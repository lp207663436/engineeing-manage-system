package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance")
public class Acceptance extends BaseEntity {
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    private Long quoteId;
    private Long acceptorId;
    private LocalDate acceptDate;
    private String actualQuantity;
    private String result;
    private Integer rectifyCount;
    private String remark;
}
