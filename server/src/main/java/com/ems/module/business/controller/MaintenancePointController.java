package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.MaintenancePointDTO;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.service.MaintenancePointService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/maintenance-point")
@RequiredArgsConstructor
public class MaintenancePointController {

    private final MaintenancePointService maintenancePointService;

    @GetMapping("/page")
    @RequirePermission("business:maintenancePoint:list")
    public Result<PageResult<MaintenancePoint>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) String code,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Long projectId) {
        return Result.success(maintenancePointService.page(pageNum, pageSize, code, name, status, projectId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:maintenancePoint:list")
    public Result<MaintenancePoint> get(@PathVariable Long id) {
        return Result.success(maintenancePointService.get(id));
    }

    @PostMapping
    @RequirePermission("business:maintenancePoint:create")
    public Result<MaintenancePoint> create(@Valid @RequestBody MaintenancePointDTO dto) {
        return Result.success(maintenancePointService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:maintenancePoint:update")
    public Result<Void> update(@Valid @RequestBody MaintenancePointDTO dto) {
        maintenancePointService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:maintenancePoint:delete")
    public Result<Void> delete(@PathVariable Long id) {
        maintenancePointService.delete(id);
        return Result.success();
    }
}
