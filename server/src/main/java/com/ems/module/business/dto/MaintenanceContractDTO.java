package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MaintenanceContractDTO {
    private Long id;
    @NotBlank(message = "合同编号不能为空")
    private String code;
    @NotBlank(message = "合同名称不能为空")
    private String name;
    @NotNull(message = "关联项目不能为空")
    private Long projectId;
    private String partyA;
    private String partyB;
    private String signDate;
    @NotBlank(message = "合同生效日不能为空")
    private String effectiveDate;
    @NotNull(message = "合同总额不能为空")
    private BigDecimal totalAmount;
    @NotNull(message = "合同期月数不能为空")
    private Integer periodMonths;
    private Integer periodCount;
    private String responseSla;
    private String scope;
    private String status;
    private String endDate;
    private String remark;
}
