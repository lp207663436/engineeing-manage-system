package com.ems.module.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.datascope.DataScope;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.excel.ExcelExportUtil;
import com.ems.module.business.dto.ContractPaymentReportVO;
import com.ems.module.business.dto.MaintenanceWorkloadReportVO;
import com.ems.module.business.dto.ProgressReportVO;
import com.ems.module.business.dto.ProjectReportVO;
import com.ems.module.business.dto.SettlementReportVO;
import com.ems.module.business.entity.ContractPayment;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.entity.Progress;
import com.ems.module.business.entity.Project;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.mapper.ContractPaymentMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import com.ems.module.business.mapper.ProgressMapper;
import com.ems.module.business.mapper.ProjectMapper;
import com.ems.module.business.mapper.QuarterlySettlementMapper;
import com.ems.security.annotation.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 报表导出
 */
@RestController
@RequestMapping("/business/report")
@RequiredArgsConstructor
public class ReportController {

    private final ProjectMapper projectMapper;
    private final ContractPaymentMapper contractPaymentMapper;
    private final ProgressMapper progressMapper;
    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final QuarterlySettlementMapper quarterlySettlementMapper;

    @GetMapping("/export/project")
    @RequirePermission("business:report:export")
    @DataScope
    public void exportProject(HttpServletResponse response) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        List<Project> list = projectMapper.selectList(wrapper);
        List<ProjectReportVO> data = new ArrayList<>(list.size());
        for (Project p : list) {
            ProjectReportVO vo = new ProjectReportVO();
            vo.setCode(p.getCode());
            vo.setName(p.getName());
            vo.setCustomerName(p.getCustomerName());
            vo.setType(p.getType());
            vo.setStatus(p.getStatus());
            vo.setStartDate(p.getStartDate());
            vo.setEndDate(p.getEndDate());
            data.add(vo);
        }
        ExcelExportUtil.write(response, "项目台账", ProjectReportVO.class, data);
    }

    @GetMapping("/export/contract-payment")
    @RequirePermission("business:report:export")
    @DataScope
    public void exportContractPayment(HttpServletResponse response) {
        LambdaQueryWrapper<ContractPayment> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        List<ContractPayment> list = contractPaymentMapper.selectList(wrapper);
        List<ContractPaymentReportVO> data = new ArrayList<>(list.size());
        for (ContractPayment c : list) {
            ContractPaymentReportVO vo = new ContractPaymentReportVO();
            vo.setCode(c.getCode());
            vo.setContractId(c.getContractId());
            vo.setType(c.getType());
            vo.setPlanDate(c.getPlanDate());
            vo.setPlanAmount(c.getPlanAmount());
            vo.setActualAmount(c.getActualAmount());
            vo.setActualDate(c.getActualDate());
            vo.setStatus(c.getStatus());
            data.add(vo);
        }
        ExcelExportUtil.write(response, "合同收付款", ContractPaymentReportVO.class, data);
    }

    @GetMapping("/export/progress")
    @RequirePermission("business:report:export")
    @DataScope
    public void exportProgress(HttpServletResponse response) {
        LambdaQueryWrapper<Progress> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        List<Progress> list = progressMapper.selectList(wrapper);
        List<ProgressReportVO> data = new ArrayList<>(list.size());
        for (Progress p : list) {
            ProgressReportVO vo = new ProgressReportVO();
            vo.setCode(p.getCode());
            vo.setProjectId(p.getProjectId());
            vo.setNodeName(p.getNodeName());
            vo.setPlanStartDate(p.getPlanStartDate());
            vo.setPlanEndDate(p.getPlanEndDate());
            vo.setActualStartDate(p.getActualStartDate());
            vo.setActualEndDate(p.getActualEndDate());
            vo.setProgressPercent(p.getProgressPercent());
            vo.setStatus(p.getStatus());
            data.add(vo);
        }
        ExcelExportUtil.write(response, "进度执行", ProgressReportVO.class, data);
    }

    @GetMapping("/export/maintenance-workload")
    @RequirePermission("business:report:export")
    @DataScope
    public void exportMaintenanceWorkload(HttpServletResponse response) {
        LambdaQueryWrapper<MaintenanceTask> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        List<MaintenanceTask> list = maintenanceTaskMapper.selectList(wrapper);
        List<MaintenanceWorkloadReportVO> data = new ArrayList<>(list.size());
        for (MaintenanceTask t : list) {
            MaintenanceWorkloadReportVO vo = new MaintenanceWorkloadReportVO();
            vo.setCode(t.getCode());
            vo.setType(t.getType());
            vo.setTitle(t.getTitle());
            vo.setHandlerId(t.getHandlerId());
            vo.setPlanDate(t.getPlanDate());
            vo.setCompleteDate(t.getCompleteDate());
            vo.setStatus(t.getStatus());
            data.add(vo);
        }
        ExcelExportUtil.write(response, "维保工作量", MaintenanceWorkloadReportVO.class, data);
    }

    @GetMapping("/export/settlement")
    @RequirePermission("business:report:export")
    @DataScope
    public void exportSettlement(HttpServletResponse response) {
        LambdaQueryWrapper<QuarterlySettlement> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        List<QuarterlySettlement> list = quarterlySettlementMapper.selectList(wrapper);
        List<SettlementReportVO> data = new ArrayList<>(list.size());
        for (QuarterlySettlement s : list) {
            SettlementReportVO vo = new SettlementReportVO();
            vo.setCode(s.getCode());
            vo.setContractId(s.getContractId());
            vo.setPeriodNo(s.getPeriodNo());
            vo.setPeriodStartDate(s.getPeriodStartDate());
            vo.setPeriodEndDate(s.getPeriodEndDate());
            vo.setAmount(s.getAmount());
            vo.setStatus(s.getStatus());
            data.add(vo);
        }
        ExcelExportUtil.write(response, "双线结算", SettlementReportVO.class, data);
    }
}
