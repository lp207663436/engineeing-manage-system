package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.SupplierDTO;
import com.ems.module.business.entity.Supplier;
import com.ems.module.business.service.SupplierService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/page")
    @RequirePermission("business:supplier:list")
    @DataScope
    public Result<PageResult<Supplier>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) String code) {
        return Result.success(supplierService.page(pageNum, pageSize, name, code));
    }

    @GetMapping("/list")
    @RequirePermission("business:supplier:list")
    public Result<List<Supplier>> list() {
        return Result.success(supplierService.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("business:supplier:list")
    public Result<Supplier> get(@PathVariable Long id) {
        return Result.success(supplierService.get(id));
    }

    @PostMapping
    @RequirePermission("business:supplier:create")
    public Result<Supplier> create(@Valid @RequestBody SupplierDTO dto) {
        return Result.success(supplierService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:supplier:update")
    public Result<Void> update(@Valid @RequestBody SupplierDTO dto) {
        supplierService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:supplier:delete")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.success();
    }
}
