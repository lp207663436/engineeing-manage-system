package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.EquipmentDTO;
import com.ems.module.business.entity.Equipment;
import com.ems.module.business.service.EquipmentService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/page")
    @RequirePermission("business:equipment:list")
    @DataScope
    public Result<PageResult<Equipment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String code,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long projectId) {
        return Result.success(equipmentService.page(pageNum, pageSize, code, name, category, status, projectId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:equipment:list")
    public Result<Equipment> get(@PathVariable Long id) {
        return Result.success(equipmentService.get(id));
    }

    @PostMapping
    @RequirePermission("business:equipment:create")
    public Result<Equipment> create(@Valid @RequestBody EquipmentDTO dto) {
        return Result.success(equipmentService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:equipment:update")
    public Result<Void> update(@Valid @RequestBody EquipmentDTO dto) {
        equipmentService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:equipment:delete")
    public Result<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return Result.success();
    }
}
