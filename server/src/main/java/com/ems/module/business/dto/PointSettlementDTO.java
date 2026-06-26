package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PointSettlementDTO {
    private Long id;
    @NotBlank(message = "结算单编号不能为空")
    private String code;
    private Long projectId;
    @NotNull(message = "关联点位不能为空")
    private Long pointId;
    @NotNull(message = "关联报价不能为空")
    private Long quoteId;
    private Long acceptanceId;
    private BigDecimal amount;
    private String status;
    private String invoiceNo;
    private BigDecimal receivedAmount;
    private String receivedDate;
    private String remark;
}
