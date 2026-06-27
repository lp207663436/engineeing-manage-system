package com.ems.module.business.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报价版本对比 VO
 */
@Data
public class QuoteCompareVO {
    private QuoteDTO v1;
    private QuoteDTO v2;
    /**
     * 金额差(v2.amount - v1.amount)
     */
    private BigDecimal amountDiff;
    /**
     * 有效期差(天,v2.validUntil - v1.validUntil)
     */
    private Long validUntilDiffDays;
    /**
     * 明细差异描述
     */
    private String detailDiff;

    @Data
    public static class QuoteDTO {
        private Long id;
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
}
