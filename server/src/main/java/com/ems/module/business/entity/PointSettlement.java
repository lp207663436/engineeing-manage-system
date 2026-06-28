package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("point_settlement")
public class PointSettlement extends BaseEntity {
    private String code;
    private Long projectId;
    private Long pointId;
    private Long contractId;
    private String periodNo;
    private Long quoteId;
    private Long acceptanceId;
    /**
     * 结算金额(=报价金额)
     */
    private BigDecimal amount;
    /**
     * PENDING(待结算)/CONFIRMED(已确认)/INVOICED(已开票)/PARTIAL(部分收款)/RECEIVED(已收款)/CLOSED(已关闭)
     */
    private String status;
    private String invoiceNo;
    private BigDecimal receivedAmount;
    private LocalDate receivedDate;
    private String remark;
}
