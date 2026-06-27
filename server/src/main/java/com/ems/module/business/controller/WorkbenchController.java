package com.ems.module.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.Result;
import com.ems.module.business.dto.WorkbenchVO;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import com.ems.module.business.mapper.QuarterlySettlementMapper;
import com.ems.module.business.service.ApprovalService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 工作台聚合接口
 */
@RestController
@RequestMapping("/business/workbench")
@RequiredArgsConstructor
public class WorkbenchController {

    private final ApprovalService approvalService;
    private final AcceptanceMapper acceptanceMapper;
    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final QuarterlySettlementMapper quarterlySettlementMapper;

    @GetMapping("/summary")
    @RequirePermission("business:workbench:list")
    public Result<WorkbenchVO> summary() {
        WorkbenchVO vo = new WorkbenchVO();
        Long userId = SecurityContext.getUserId();

        // 待审批数(当前用户)
        vo.setPendingApprovals(userId == null ? 0 : approvalService.getPending(userId).size());

        // 待验收数
        Long pendingAcceptances = acceptanceMapper.selectCount(
                new LambdaQueryWrapper<Acceptance>().eq(Acceptance::getResult, "PENDING"));
        vo.setPendingAcceptances(pendingAcceptances == null ? 0 : pendingAcceptances);

        // 超期验收数(PENDING 且 create_time < 7天前)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long overdueAcceptances = acceptanceMapper.selectCount(
                new LambdaQueryWrapper<Acceptance>()
                        .eq(Acceptance::getResult, "PENDING")
                        .lt(Acceptance::getCreateTime, sevenDaysAgo));
        vo.setOverdueAcceptances(overdueAcceptances == null ? 0 : overdueAcceptances);

        // 超期维保任务数(status not in COMPLETED/CLOSED and plan_date < today)
        List<String> doneStatus = Arrays.asList("COMPLETED", "CLOSED");
        Long overdueTasks = maintenanceTaskMapper.selectCount(
                new LambdaQueryWrapper<MaintenanceTask>()
                        .notIn(MaintenanceTask::getStatus, doneStatus)
                        .lt(MaintenanceTask::getPlanDate, LocalDate.now()));
        vo.setOverdueTasks(overdueTasks == null ? 0 : overdueTasks);

        // 30天内到期维保合同数
        LocalDate today = LocalDate.now();
        LocalDate todayPlus30 = today.plusDays(30);
        Long expiringContracts = maintenanceContractMapper.selectCount(
                new LambdaQueryWrapper<MaintenanceContract>()
                        .eq(MaintenanceContract::getStatus, "ACTIVE")
                        .le(MaintenanceContract::getEndDate, todayPlus30)
                        .ge(MaintenanceContract::getEndDate, today));
        vo.setExpiringContracts(expiringContracts == null ? 0 : expiringContracts);

        // 待回款结算单数(status in DRAFT/REVIEWED/CONFIRMED/INVOICED)
        List<String> pendingStatus = Arrays.asList("DRAFT", "REVIEWED", "CONFIRMED", "INVOICED");
        Long pendingSettlements = quarterlySettlementMapper.selectCount(
                new LambdaQueryWrapper<QuarterlySettlement>()
                        .in(QuarterlySettlement::getStatus, pendingStatus));
        vo.setPendingSettlements(pendingSettlements == null ? 0 : pendingSettlements);

        return Result.success(vo);
    }
}
