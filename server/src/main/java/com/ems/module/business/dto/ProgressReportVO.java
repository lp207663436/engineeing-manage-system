package com.ems.module.business.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 进度执行报表导出 VO
 */
@Data
public class ProgressReportVO {

    @ExcelProperty("编号")
    private String code;

    @ExcelProperty("项目ID")
    private Long projectId;

    @ExcelProperty("节点名称")
    private String nodeName;

    @ExcelProperty("计划开始日期")
    private LocalDate planStartDate;

    @ExcelProperty("计划结束日期")
    private LocalDate planEndDate;

    @ExcelProperty("实际开始日期")
    private LocalDate actualStartDate;

    @ExcelProperty("实际结束日期")
    private LocalDate actualEndDate;

    @ExcelProperty("进度百分比")
    private Integer progressPercent;

    @ExcelProperty("状态")
    private String status;
}
