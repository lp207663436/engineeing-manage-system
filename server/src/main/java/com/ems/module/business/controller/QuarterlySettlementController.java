package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.service.QuarterlySettlementService;
import com.ems.security.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/business/quarterly-settlement")
@RequiredArgsConstructor
public class QuarterlySettlementController {

    private final QuarterlySettlementService quarterlySettlementService;

    @GetMapping("/page")
    @RequirePermission("business:quarterlySettlement:list")
    public Result<PageResult<QuarterlySettlement>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                         @RequestParam(defaultValue = "10") long pageSize,
                                                         @RequestParam(required = false) String code,
                                                         @RequestParam(required = false) Long contractId,
                                                         @RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) String status) {
        return Result.success(quarterlySettlementService.page(pageNum, pageSize, code, contractId, projectId, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:quarterlySettlement:list")
    public Result<QuarterlySettlement> get(@PathVariable Long id) {
        return Result.success(quarterlySettlementService.get(id));
    }

    @GetMapping("/contract/{contractId}")
    @RequirePermission("business:quarterlySettlement:list")
    public Result<List<QuarterlySettlement>> listByContract(@PathVariable Long contractId) {
        return Result.success(quarterlySettlementService.listByContract(contractId));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("business:quarterlySettlement:update")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : body.get("status").toString();
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        quarterlySettlementService.updateStatus(id, status, remark);
        return Result.success();
    }

    @PutMapping("/{id}/adjust")
    @RequirePermission("business:quarterlySettlement:update")
    public Result<Void> adjust(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        quarterlySettlementService.adjust(id, amount, remark);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:quarterlySettlement:delete")
    public Result<Void> delete(@PathVariable Long id) {
        quarterlySettlementService.delete(id);
        return Result.success();
    }

    @PostMapping("/contract/{contractId}/generate")
    @RequirePermission("business:quarterlySettlement:create")
    public Result<Void> generate(@PathVariable Long contractId) {
        quarterlySettlementService.generateForContract(contractId);
        return Result.success();
    }
}
