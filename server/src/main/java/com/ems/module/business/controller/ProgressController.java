package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.ProgressDTO;
import com.ems.module.business.entity.Progress;
import com.ems.module.business.service.ProgressService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/page")
    @RequirePermission("business:progress:list")
    @DataScope
    public Result<PageResult<Progress>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String code,
                                             @RequestParam(required = false) String nodeName,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String businessType,
                                             @RequestParam(required = false) Long businessId,
                                             @RequestParam(required = false) Long projectId) {
        return Result.success(progressService.page(pageNum, pageSize, code, nodeName, status, businessType, businessId, projectId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:progress:list")
    public Result<Progress> get(@PathVariable Long id) {
        return Result.success(progressService.get(id));
    }

    @PostMapping
    @RequirePermission("business:progress:create")
    public Result<Progress> create(@Valid @RequestBody ProgressDTO dto) {
        return Result.success(progressService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:progress:update")
    public Result<Void> update(@Valid @RequestBody ProgressDTO dto) {
        progressService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:progress:delete")
    public Result<Void> delete(@PathVariable Long id) {
        progressService.delete(id);
        return Result.success();
    }
}
