package com.ems.module.business.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同收付款报表导出 VO
 */
@Data
public class ContractPaymentReportVO {

    @ExcelProperty("单号")
    private String code;

    @ExcelProperty("合同ID")
    private Long contractId;

    @ExcelProperty("类型")
    private String type;

    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @ExcelProperty("计划金额")
    private BigDecimal planAmount;

    @ExcelProperty("实际金额")
    private BigDecimal actualAmount;

    @ExcelProperty("实际日期")
    private LocalDate actualDate;

    @ExcelProperty("状态")
    private String status;
}
