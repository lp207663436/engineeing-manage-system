package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.AcceptanceDTO;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.service.AcceptanceService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/business/acceptance")
@RequiredArgsConstructor
public class AcceptanceController {

    private final AcceptanceService acceptanceService;

    @GetMapping("/page")
    @RequirePermission("business:acceptance:list")
    public Result<PageResult<Acceptance>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String code,
                                               @RequestParam(required = false) String result,
                                               @RequestParam(required = false) String businessType,
                                               @RequestParam(required = false) Long businessId) {
        return Result.success(acceptanceService.page(pageNum, pageSize, code, result, businessType, businessId));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:acceptance:list")
    public Result<Acceptance> get(@PathVariable Long id) {
        return Result.success(acceptanceService.get(id));
    }

    @PostMapping
    @RequirePermission("business:acceptance:create")
    public Result<Void> create(@Valid @RequestBody AcceptanceDTO dto) {
        acceptanceService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("business:acceptance:update")
    public Result<Void> update(@Valid @RequestBody AcceptanceDTO dto) {
        acceptanceService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:acceptance:delete")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/result")
    @RequirePermission("business:acceptance:update")
    public Result<Void> submitResult(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String result = body.get("result");
        String remark = body.get("remark");
        acceptanceService.submitResult(id, result, remark);
        return Result.success();
    }
}
