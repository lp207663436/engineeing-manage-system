package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quote")
public class Quote extends BaseEntity {
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    private BigDecimal amount;
    private LocalDate quoteDate;
    private LocalDate validUntil;
    private String quotePerson;
    private String customerName;
    private Integer version;
    private String status;
    private String approvalStatus;
    private String summary;
}
