package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.ContractPaymentDTO;
import com.ems.module.business.entity.ContractPayment;
import com.ems.module.business.service.ContractPaymentService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/business/contract-payment")
@RequiredArgsConstructor
public class ContractPaymentController {

    private final ContractPaymentService contractPaymentService;

    @GetMapping("/page")
    @RequirePermission("business:contractPayment:list")
    @DataScope
    public Result<PageResult<ContractPayment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String code,
                                                    @RequestParam(required = false) Long contractId,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String status) {
        return Result.success(contractPaymentService.page(pageNum, pageSize, code, contractId, type, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:contractPayment:list")
    public Result<ContractPayment> get(@PathVariable Long id) {
        return Result.success(contractPaymentService.get(id));
    }

    @PostMapping
    @RequirePermission("business:contractPayment:create")
    public Result<ContractPayment> create(@Valid @RequestBody ContractPaymentDTO dto) {
        return Result.success(contractPaymentService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:contractPayment:update")
    public Result<Void> update(@Valid @RequestBody ContractPaymentDTO dto) {
        contractPaymentService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:contractPayment:delete")
    public Result<Void> delete(@PathVariable Long id) {
        contractPaymentService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/actual")
    @RequirePermission("business:contractPayment:update")
    public Result<Void> recordActual(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal actualAmount = null;
        Object amountObj = body.get("actualAmount");
        if (amountObj != null) {
            actualAmount = new BigDecimal(amountObj.toString());
        }
        LocalDate actualDate = null;
        Object dateObj = body.get("actualDate");
        if (dateObj != null && StringUtils.hasText(dateObj.toString())) {
            actualDate = LocalDate.parse(dateObj.toString());
        }
        String invoiceNo = body.get("invoiceNo") == null ? null : body.get("invoiceNo").toString();
        contractPaymentService.recordActual(id, actualAmount, actualDate, invoiceNo);
        return Result.success();
    }

    @GetMapping("/dashboard")
    @RequirePermission("business:contractPayment:list")
    public Result<Map<String, Object>> dashboard(@RequestParam Long contractId) {
        return Result.success(contractPaymentService.dashboard(contractId));
    }
}
