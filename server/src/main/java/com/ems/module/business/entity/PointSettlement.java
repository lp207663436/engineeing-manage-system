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
    private Long quoteId;
    private Long acceptanceId;
    /**
     * 结算金额(=报价金额)
     */
    private BigDecimal amount;
    /**
     * PENDING待结算/CONFIRMED已确认/INVOICED已开票/RECEIVED已回款/CLOSED已关闭
     */
    private String status;
    private String invoiceNo;
    private BigDecimal receivedAmount;
    private LocalDate receivedDate;
    private String remark;
}
