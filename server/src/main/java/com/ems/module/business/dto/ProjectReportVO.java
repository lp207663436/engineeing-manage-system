package com.ems.module.business.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 项目台账报表导出 VO
 */
@Data
public class ProjectReportVO {

    @ExcelProperty("项目编号")
    private String code;

    @ExcelProperty("项目名称")
    private String name;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("项目类型")
    private String type;

    @ExcelProperty("项目状态")
    private String status;

    @ExcelProperty("开工日期")
    private LocalDate startDate;

    @ExcelProperty("竣工日期")
    private LocalDate endDate;
}
