package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.QuoteDTO;
import com.ems.module.business.entity.Quote;
import com.ems.module.business.service.QuoteService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping("/page")
    @RequirePermission("business:quote:list")
    public Result<PageResult<Quote>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String code,
                                          @RequestParam(required = false) String customerName,
                                          @RequestParam(required = false) String status) {
        return Result.success(quoteService.page(pageNum, pageSize, code, customerName, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:quote:list")
    public Result<Quote> get(@PathVariable Long id) {
        return Result.success(quoteService.get(id));
    }

    @PostMapping
    @RequirePermission("business:quote:create")
    public Result<Quote> create(@Valid @RequestBody QuoteDTO dto) {
        return Result.success(quoteService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:quote:update")
    public Result<Void> update(@Valid @RequestBody QuoteDTO dto) {
        quoteService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:quote:delete")
    public Result<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return Result.success();
    }
}
