package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.ContractDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.service.ContractService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping("/page")
    @RequirePermission("business:contract:list")
    public Result<PageResult<Contract>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) String code,
                                             @RequestParam(required = false) String status) {
        return Result.success(contractService.page(pageNum, pageSize, name, code, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:contract:list")
    public Result<Contract> get(@PathVariable Long id) {
        return Result.success(contractService.get(id));
    }

    @PostMapping
    @RequirePermission("business:contract:create")
    public Result<Void> create(@Valid @RequestBody ContractDTO dto) {
        contractService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("business:contract:update")
    public Result<Void> update(@Valid @RequestBody ContractDTO dto) {
        contractService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:contract:delete")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return Result.success();
    }
}
