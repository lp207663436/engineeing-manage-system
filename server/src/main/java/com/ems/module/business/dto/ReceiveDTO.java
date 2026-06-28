package com.ems.module.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReceiveDTO {
    @NotNull(message = "实收金额不能为空")
    @DecimalMin(value = "0.01", message = "实收金额必须大于0")
    private BigDecimal receivedAmount;
    private String receivedDate;
    private String invoiceNo;
}
