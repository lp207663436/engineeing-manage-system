package com.ems.module.business.controller;

import com.ems.common.Result;
import com.ems.module.business.dto.MaintenanceStatVO;
import com.ems.module.business.service.MaintenanceStatService;
import com.ems.security.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 维保统计
 */
@RestController
@RequestMapping("/business/maintenance-stat")
@RequiredArgsConstructor
public class MaintenanceStatController {

    private final MaintenanceStatService maintenanceStatService;

    @GetMapping("/summary")
    @RequirePermission("business:maintenanceStat:list")
    public Result<MaintenanceStatVO> summary() {
        return Result.success(maintenanceStatService.stat());
    }
}
