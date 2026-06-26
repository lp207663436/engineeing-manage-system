package com.ems.module.business.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 双线结算看板聚合返回对象
 */
@Data
public class DashboardVO {
    private String projectName;
    /**
     * 维保费结算线(季度结算)
     */
    private Line maintenanceLine = new Line();
    /**
     * 报价费结算线(点位结算)
     */
    private Line pointLine = new Line();
    /**
     * 累计已收=维保费回款+报价费回款
     */
    private BigDecimal totalReceivedAmount = BigDecimal.ZERO;

    @Data
    public static class Line {
        /**
         * 总额
         */
        private BigDecimal totalAmount = BigDecimal.ZERO;
        /**
         * 已确认及之后状态金额和
         */
        private BigDecimal settledAmount = BigDecimal.ZERO;
        /**
         * 已开票金额
         */
        private BigDecimal invoicedAmount = BigDecimal.ZERO;
        /**
         * 已回款金额(received_amount 和)
         */
        private BigDecimal receivedAmount = BigDecimal.ZERO;
    }
}
