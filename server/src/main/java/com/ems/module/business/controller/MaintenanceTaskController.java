package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.AssignDTO;
import com.ems.module.business.dto.CompleteDTO;
import com.ems.module.business.dto.MaintenanceTaskDTO;
import com.ems.module.business.dto.ProcessDTO;
import com.ems.module.business.dto.RejectTaskDTO;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.service.MaintenanceTaskService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/maintenance-task")
@RequiredArgsConstructor
public class MaintenanceTaskController {

    private final MaintenanceTaskService maintenanceTaskService;

    @GetMapping("/page")
    @DataScope
    @RequirePermission("business:maintenanceTask:list")
    public Result<PageResult<MaintenanceTask>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) String code,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Long projectId,
                                                     @RequestParam(required = false) Long equipmentId) {
        return Result.success(maintenanceTaskService.page(pageNum, pageSize, code, type, status, projectId, equipmentId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:maintenanceTask:list")
    public Result<MaintenanceTask> get(@PathVariable Long id) {
        return Result.success(maintenanceTaskService.get(id));
    }

    @PostMapping
    @RequirePermission("business:maintenanceTask:create")
    public Result<MaintenanceTask> create(@Valid @RequestBody MaintenanceTaskDTO dto) {
        return Result.success(maintenanceTaskService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> update(@Valid @RequestBody MaintenanceTaskDTO dto) {
        maintenanceTaskService.update(dto);
        return Result.success();
    }

    @PutMapping("/{id}/assign")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignDTO dto) {
        maintenanceTaskService.assign(id, dto.getHandlerId());
        return Result.success();
    }

    @PutMapping("/{id}/process")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> process(@PathVariable Long id, @Valid @RequestBody ProcessDTO dto) {
        maintenanceTaskService.process(id, dto.getHandleMethod(), dto.getPartsUsed());
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> complete(@PathVariable Long id, @Valid @RequestBody CompleteDTO dto) {
        maintenanceTaskService.complete(id, dto.getCompleteDate());
        return Result.success();
    }

    @PutMapping("/{id}/accept")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> accept(@PathVariable Long id) {
        maintenanceTaskService.accept(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectTaskDTO dto) {
        maintenanceTaskService.reject(id, dto.getReason());
        return Result.success();
    }

    @PutMapping("/{id}/close")
    @RequirePermission("business:maintenanceTask:update")
    public Result<Void> close(@PathVariable Long id) {
        maintenanceTaskService.close(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:maintenanceTask:delete")
    public Result<Void> delete(@PathVariable Long id) {
        maintenanceTaskService.delete(id);
        return Result.success();
    }

    @PostMapping("/generate-inspection")
    @RequirePermission("business:maintenanceTask:create")
    public Result<Integer> generateInspection(@RequestParam Long contractId) {
        return Result.success(maintenanceTaskService.generateInspection(contractId));
    }
}
