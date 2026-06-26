package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.PointSettlementDTO;
import com.ems.module.business.entity.PointSettlement;
import com.ems.module.business.service.PointSettlementService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/point-settlement")
@RequiredArgsConstructor
public class PointSettlementController {

    private final PointSettlementService pointSettlementService;

    @GetMapping("/page")
    @RequirePermission("business:pointSettlement:list")
    public Result<PageResult<PointSettlement>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String code,
                                                    @RequestParam(required = false) Long pointId,
                                                    @RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) String status) {
        return Result.success(pointSettlementService.page(pageNum, pageSize, code, pointId, projectId, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:pointSettlement:list")
    public Result<PointSettlement> get(@PathVariable Long id) {
        return Result.success(pointSettlementService.get(id));
    }

    @PostMapping
    @RequirePermission("business:pointSettlement:create")
    public Result<Void> create(@Valid @RequestBody PointSettlementDTO dto) {
        pointSettlementService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("business:pointSettlement:update")
    public Result<Void> update(@Valid @RequestBody PointSettlementDTO dto) {
        pointSettlementService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:pointSettlement:delete")
    public Result<Void> delete(@PathVariable Long id) {
        pointSettlementService.delete(id);
        return Result.success();
    }
}
