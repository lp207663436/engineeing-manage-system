package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.MaintenanceContractDTO;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.service.MaintenanceContractService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/maintenance-contract")
@RequiredArgsConstructor
public class MaintenanceContractController {

    private final MaintenanceContractService maintenanceContractService;

    @GetMapping("/page")
    @RequirePermission("business:maintenanceContract:list")
    @DataScope
    public Result<PageResult<MaintenanceContract>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String code,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Long projectId) {
        return Result.success(maintenanceContractService.page(pageNum, pageSize, code, name, status, projectId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:maintenanceContract:list")
    public Result<MaintenanceContract> get(@PathVariable Long id) {
        return Result.success(maintenanceContractService.get(id));
    }

    @PostMapping
    @RequirePermission("business:maintenanceContract:create")
    public Result<MaintenanceContract> create(@Valid @RequestBody MaintenanceContractDTO dto) {
        return Result.success(maintenanceContractService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:maintenanceContract:update")
    public Result<Void> update(@Valid @RequestBody MaintenanceContractDTO dto) {
        maintenanceContractService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:maintenanceContract:delete")
    public Result<Void> delete(@PathVariable Long id) {
        maintenanceContractService.delete(id);
        return Result.success();
    }
}
