package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.ApprovalDTO;
import com.ems.module.business.entity.ApprovalFlow;
import com.ems.module.business.entity.ApprovalLog;
import com.ems.module.business.entity.ApprovalNode;
import com.ems.module.business.service.ApprovalService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/start")
    @RequirePermission("business:approval:start")
    public Result<Void> start(@Valid @RequestBody ApprovalDTO dto) {
        approvalService.startApproval(dto.getBusinessType(), dto.getBusinessId());
        return Result.success();
    }

    @PostMapping("/{logId}/approve")
    @RequirePermission("business:approval:approve")
    public Result<Void> approve(@PathVariable Long logId, @Valid @RequestBody ApprovalDTO dto) {
        approvalService.approve(logId, dto.getResult(), dto.getOpinion());
        return Result.success();
    }

    @GetMapping("/pending")
    @RequirePermission("business:approval:list")
    public Result<List<ApprovalLog>> pending() {
        CurrentUser user = SecurityContext.get();
        if (user == null) throw new RuntimeException("未登录");
        return Result.success(approvalService.getPending(user.getUserId()));
    }

    @GetMapping("/history")
    @RequirePermission("business:approval:list")
    public Result<List<ApprovalLog>> history() {
        CurrentUser user = SecurityContext.get();
        if (user == null) throw new RuntimeException("未登录");
        return Result.success(approvalService.getHistory(user.getUserId()));
    }

    @GetMapping("/page")
    @RequirePermission("business:approval:list")
    public Result<PageResult<ApprovalLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String businessType,
                                                 @RequestParam(required = false) Long businessId,
                                                 @RequestParam(required = false) String result) {
        return Result.success(approvalService.page(pageNum, pageSize, businessType, businessId, result));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:approval:list")
    public Result<ApprovalLog> get(@PathVariable Long id) {
        return Result.success(approvalService.get(id));
    }

    @GetMapping("/progress")
    @RequirePermission("business:approval:list")
    public Result<List<ApprovalLog>> progress(@RequestParam String businessType,
                                              @RequestParam Long businessId) {
        return Result.success(approvalService.listByBusiness(businessType, businessId));
    }

    // ==================== 审批流配置 ====================

    @GetMapping("/flow/list")
    @RequirePermission("business:approval:config")
    public Result<List<ApprovalFlow>> listFlows() {
        return Result.success(approvalService.listFlows());
    }

    @PostMapping("/flow")
    @RequirePermission("business:approval:config")
    public Result<ApprovalFlow> saveFlow(@RequestBody ApprovalFlow flow) {
        if (flow.getId() != null) {
            approvalService.updateFlow(flow);
        } else {
            approvalService.createFlow(flow);
        }
        return Result.success(flow);
    }

    @DeleteMapping("/flow/{id}")
    @RequirePermission("business:approval:config")
    public Result<Void> deleteFlow(@PathVariable Long id) {
        approvalService.deleteFlow(id);
        return Result.success();
    }

    @GetMapping("/flow/{flowId}/nodes")
    @RequirePermission("business:approval:config")
    public Result<List<ApprovalNode>> listNodes(@PathVariable Long flowId) {
        return Result.success(approvalService.listNodes(flowId));
    }

    @PostMapping("/flow/{flowId}/nodes")
    @RequirePermission("business:approval:config")
    public Result<Void> saveNodes(@PathVariable Long flowId, @RequestBody List<ApprovalNode> nodes) {
        approvalService.saveNodes(flowId, nodes);
        return Result.success();
    }
}
