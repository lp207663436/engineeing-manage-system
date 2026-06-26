package com.ems.module.business.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 季度结算单DTO(结算单由系统生成,非用户直接填,故无 @NotBlank/@NotNull 校验)
 */
@Data
public class QuarterlySettlementDTO {
    private Long id;
    private String code;
    private Long contractId;
    private Long projectId;
    private Integer periodNo;
    private String periodStartDate;
    private String periodEndDate;
    private BigDecimal amount;
    private Integer amountVersion;
    private String status;
    private String invoiceNo;
    private BigDecimal receivedAmount;
    private String receivedDate;
    private String remark;
}
