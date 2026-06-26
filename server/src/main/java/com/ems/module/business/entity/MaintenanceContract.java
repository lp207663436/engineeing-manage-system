package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_contract")
public class MaintenanceContract extends BaseEntity {
    private String code;
    private String name;
    private Long projectId;
    private String partyA;
    private String partyB;
    private LocalDate signDate;
    /**
     * 合同生效日(季度起算点)
     */
    private LocalDate effectiveDate;
    /**
     * 合同总额
     */
    private BigDecimal totalAmount;
    /**
     * 合同期月数(必须%3==0且≥6)
     */
    private Integer periodMonths;
    /**
     * 合同期数(=periodMonths/3)
     */
    private Integer periodCount;
    private String responseSla;
    private String scope;
    /**
     * ACTIVE生效/TERMINATED终止/EXPIRED到期
     */
    private String status;
    /**
     * 合同到期日(=effectiveDate+periodMonths-1天)
     */
    private LocalDate endDate;
    private String remark;
}
