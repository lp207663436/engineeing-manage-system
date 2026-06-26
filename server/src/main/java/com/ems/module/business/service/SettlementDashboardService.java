package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.DashboardVO;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.PointSettlement;
import com.ems.module.business.entity.Project;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.PointSettlementMapper;
import com.ems.module.business.mapper.ProjectMapper;
import com.ems.module.business.mapper.QuarterlySettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 双线结算看板服务
 */
@Service
@RequiredArgsConstructor
public class SettlementDashboardService {

    private final QuarterlySettlementMapper quarterlySettlementMapper;
    private final PointSettlementMapper pointSettlementMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final ProjectMapper projectMapper;

    /**
     * 已确认及之后 = CONFIRMED, INVOICED, RECEIVED, CLOSED
     */
    private static final List<String> SETTLED_STATUS = Arrays.asList("CONFIRMED", "INVOICED", "RECEIVED", "CLOSED");
    /**
     * 已开票及之后 = INVOICED, RECEIVED, CLOSED
     */
    private static final List<String> INVOICED_STATUS = Arrays.asList("INVOICED", "RECEIVED", "CLOSED");
    /**
     * 已回款 = RECEIVED, CLOSED
     */
    private static final List<String> RECEIVED_STATUS = Arrays.asList("RECEIVED", "CLOSED");

    /**
     * 项目结算看板
     */
    public DashboardVO getProjectDashboard(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");
        DashboardVO vo = new DashboardVO();
        vo.setProjectName(project.getName());

        // 维保费线:季度结算
        DashboardVO.Line maintenanceLine = aggregateMaintenanceLine(projectId);
        vo.setMaintenanceLine(maintenanceLine);

        // 报价费线:点位结算
        DashboardVO.Line pointLine = aggregatePointLine(projectId);
        vo.setPointLine(pointLine);

        // 累计已收=维保费回款+报价费回款
        vo.setTotalReceivedAmount(maintenanceLine.getReceivedAmount().add(pointLine.getReceivedAmount()));
        return vo;
    }

    /**
     * 维保费线:totalAmount 取项目下维保主合同 totalAmount 求和;其余按 QuarterlySettlement 状态聚合
     */
    private DashboardVO.Line aggregateMaintenanceLine(Long projectId) {
        DashboardVO.Line line = new DashboardVO.Line();
        // totalAmount=项目下维保主合同总额求和
        LambdaQueryWrapper<MaintenanceContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(MaintenanceContract::getProjectId, projectId);
        List<MaintenanceContract> contracts = maintenanceContractMapper.selectList(contractWrapper);
        BigDecimal contractTotal = contracts.stream()
                .map(MaintenanceContract::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        line.setTotalAmount(contractTotal);

        // 聚合季度结算单
        LambdaQueryWrapper<QuarterlySettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuarterlySettlement::getProjectId, projectId);
        List<QuarterlySettlement> list = quarterlySettlementMapper.selectList(wrapper);
        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal invoiced = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (QuarterlySettlement s : list) {
            if (s.getAmount() == null) continue;
            if (SETTLED_STATUS.contains(s.getStatus())) settled = settled.add(s.getAmount());
            if (INVOICED_STATUS.contains(s.getStatus())) invoiced = invoiced.add(s.getAmount());
            if (RECEIVED_STATUS.contains(s.getStatus())) {
                BigDecimal rec = s.getReceivedAmount() != null ? s.getReceivedAmount() : BigDecimal.ZERO;
                received = received.add(rec);
            }
        }
        line.setSettledAmount(settled);
        line.setInvoicedAmount(invoiced);
        line.setReceivedAmount(received);
        return line;
    }

    /**
     * 报价费线:totalAmount 取项目下所有 PointSettlement.amount 求和;其余按状态聚合
     */
    private DashboardVO.Line aggregatePointLine(Long projectId) {
        DashboardVO.Line line = new DashboardVO.Line();
        LambdaQueryWrapper<PointSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointSettlement::getProjectId, projectId);
        List<PointSettlement> list = pointSettlementMapper.selectList(wrapper);
        // totalAmount=所有点位报价金额和(即 amount 求和)
        BigDecimal total = list.stream()
                .map(PointSettlement::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        line.setTotalAmount(total);

        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal invoiced = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (PointSettlement s : list) {
            if (s.getAmount() == null) continue;
            if (SETTLED_STATUS.contains(s.getStatus())) settled = settled.add(s.getAmount());
            if (INVOICED_STATUS.contains(s.getStatus())) invoiced = invoiced.add(s.getAmount());
            if (RECEIVED_STATUS.contains(s.getStatus())) {
                BigDecimal rec = s.getReceivedAmount() != null ? s.getReceivedAmount() : BigDecimal.ZERO;
                received = received.add(rec);
            }
        }
        line.setSettledAmount(settled);
        line.setInvoicedAmount(invoiced);
        line.setReceivedAmount(received);
        return line;
    }

    /**
     * 返回两条线的明细列表
     * {maintenanceLine:[QuarterlySettlement...], pointLine:[PointSettlement...]}
     */
    public Map<String, Object> listProjectSettlements(Long projectId) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<QuarterlySettlement> qWrapper = new LambdaQueryWrapper<>();
        qWrapper.eq(QuarterlySettlement::getProjectId, projectId)
                .orderByAsc(QuarterlySettlement::getPeriodNo);
        result.put("maintenanceLine", quarterlySettlementMapper.selectList(qWrapper));

        LambdaQueryWrapper<PointSettlement> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(PointSettlement::getProjectId, projectId)
                .orderByDesc(PointSettlement::getCreateTime);
        result.put("pointLine", pointSettlementMapper.selectList(pWrapper));
        return result;
    }
}
