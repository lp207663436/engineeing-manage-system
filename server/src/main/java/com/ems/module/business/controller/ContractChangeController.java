package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.ContractChangeDTO;
import com.ems.module.business.entity.ContractChange;
import com.ems.module.business.service.ContractChangeService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/contract-change")
@RequiredArgsConstructor
public class ContractChangeController {

    private final ContractChangeService contractChangeService;

    @GetMapping("/page")
    @RequirePermission("business:contractChange:list")
    @DataScope
    public Result<PageResult<ContractChange>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) Long contractId,
                                                    @RequestParam(required = false) String changeType,
                                                    @RequestParam(required = false) String status) {
        return Result.success(contractChangeService.page(pageNum, pageSize, contractId, changeType, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:contractChange:list")
    public Result<ContractChange> get(@PathVariable Long id) {
        return Result.success(contractChangeService.get(id));
    }

    @PostMapping
    @RequirePermission("business:contractChange:create")
    public Result<ContractChange> create(@Valid @RequestBody ContractChangeDTO dto) {
        return Result.success(contractChangeService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:contractChange:update")
    public Result<Void> update(@Valid @RequestBody ContractChangeDTO dto) {
        contractChangeService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:contractChange:delete")
    public Result<Void> delete(@PathVariable Long id) {
        contractChangeService.delete(id);
        return Result.success();
    }

    @PostMapping("/audit")
    @RequirePermission("business:contractChange:audit")
    public Result<Void> audit(@RequestParam Long id,
                              @RequestParam String status,
                              @RequestParam(required = false) String remark) {
        contractChangeService.audit(id, status, remark);
        return Result.success();
    }
}
