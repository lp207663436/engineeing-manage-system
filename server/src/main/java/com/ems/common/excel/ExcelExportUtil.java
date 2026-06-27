package com.ems.common.excel;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 通用 EasyExcel 导出工具类
 */
@Slf4j
public class ExcelExportUtil {

    private ExcelExportUtil() {
    }

    /**
     * 写出 Excel 到 HttpServletResponse
     *
     * @param response HttpServletResponse
     * @param fileName 文件名(不含扩展名)
     * @param head     表头类
     * @param data     数据列表
     */
    public static void write(HttpServletResponse response, String fileName, Class<?> head, List<?> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            EasyExcel.write(response.getOutputStream(), head)
                    .sheet("Sheet1")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("Excel 导出失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel 导出失败", e);
        }
    }
}
