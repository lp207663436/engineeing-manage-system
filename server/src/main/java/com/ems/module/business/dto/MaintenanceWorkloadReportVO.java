package com.ems.module.business.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 维保工作量报表导出 VO
 */
@Data
public class MaintenanceWorkloadReportVO {

    @ExcelProperty("工单编号")
    private String code;

    @ExcelProperty("任务类型")
    private String type;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("处理人ID")
    private Long handlerId;

    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @ExcelProperty("完工日期")
    private LocalDate completeDate;

    @ExcelProperty("状态")
    private String status;
}
