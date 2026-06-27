package com.ems.module.business.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 双线结算报表导出 VO
 */
@Data
public class SettlementReportVO {

    @ExcelProperty("单号")
    private String code;

    @ExcelProperty("合同ID")
    private Long contractId;

    @ExcelProperty("期数")
    private Integer periodNo;

    @ExcelProperty("开始日期")
    private LocalDate periodStartDate;

    @ExcelProperty("结束日期")
    private LocalDate periodEndDate;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("状态")
    private String status;
}
