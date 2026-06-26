package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.MaintenanceRecordDTO;
import com.ems.module.business.entity.MaintenanceRecord;
import com.ems.module.business.service.MaintenanceRecordService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/maintenance-record")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    @GetMapping("/page")
    @RequirePermission("business:maintenanceRecord:list")
    public Result<PageResult<MaintenanceRecord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String code,
                                                       @RequestParam(required = false) String recordType,
                                                       @RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) Long equipmentId) {
        return Result.success(maintenanceRecordService.page(pageNum, pageSize, code, recordType, projectId, equipmentId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:maintenanceRecord:list")
    public Result<MaintenanceRecord> get(@PathVariable Long id) {
        return Result.success(maintenanceRecordService.get(id));
    }

    @PostMapping
    @RequirePermission("business:maintenanceRecord:create")
    public Result<Void> create(@Valid @RequestBody MaintenanceRecordDTO dto) {
        maintenanceRecordService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("business:maintenanceRecord:update")
    public Result<Void> update(@Valid @RequestBody MaintenanceRecordDTO dto) {
        maintenanceRecordService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:maintenanceRecord:delete")
    public Result<Void> delete(@PathVariable Long id) {
        maintenanceRecordService.delete(id);
        return Result.success();
    }

    @GetMapping("/equipment/{equipmentId}")
    @RequirePermission("business:maintenanceRecord:list")
    public Result<List<MaintenanceRecord>> listByEquipment(@PathVariable Long equipmentId) {
        return Result.success(maintenanceRecordService.listByEquipment(equipmentId));
    }
}
