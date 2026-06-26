package com.ems.module.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuoteDTO {
    private Long id;
    @NotBlank(message = "报价编号不能为空")
    private String code;
    private Long projectId;
    private String businessType;
    private Long businessId;
    private BigDecimal amount;
    private String quoteDate;
    private String validUntil;
    private String quotePerson;
    private String customerName;
    private Integer version;
    private String status;
    private String summary;
}
