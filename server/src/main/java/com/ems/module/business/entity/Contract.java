package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    private String code;
    private String name;
    private String partyA;
    private String partyB;
    private LocalDate signDate;
    private BigDecimal amount;
    private String category;
    private String paymentMethod;
    private Long projectId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
