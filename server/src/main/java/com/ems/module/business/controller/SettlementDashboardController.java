package com.ems.module.business.controller;

import com.ems.common.Result;
import com.ems.module.business.dto.DashboardVO;
import com.ems.module.business.service.SettlementDashboardService;
import com.ems.security.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/business/dashboard/settlement")
@RequiredArgsConstructor
public class SettlementDashboardController {

    private final SettlementDashboardService settlementDashboardService;

    @GetMapping("/project/{projectId}")
    @RequirePermission("business:dashboard:list")
    public Result<DashboardVO> getProjectDashboard(@PathVariable Long projectId) {
        return Result.success(settlementDashboardService.getProjectDashboard(projectId));
    }

    @GetMapping("/project/{projectId}/detail")
    @RequirePermission("business:dashboard:list")
    public Result<Map<String, Object>> listProjectSettlements(@PathVariable Long projectId) {
        return Result.success(settlementDashboardService.listProjectSettlements(projectId));
    }
}
