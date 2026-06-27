package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.common.datascope.DataScope;
import com.ems.module.business.dto.CustomerDTO;
import com.ems.module.business.entity.Customer;
import com.ems.module.business.service.CustomerService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/page")
    @RequirePermission("business:customer:list")
    @DataScope
    public Result<PageResult<Customer>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) String code) {
        return Result.success(customerService.page(pageNum, pageSize, name, code));
    }

    @GetMapping("/list")
    @RequirePermission("business:customer:list")
    public Result<List<Customer>> list() {
        return Result.success(customerService.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("business:customer:list")
    public Result<Customer> get(@PathVariable Long id) {
        return Result.success(customerService.get(id));
    }

    @PostMapping
    @RequirePermission("business:customer:create")
    public Result<Customer> create(@Valid @RequestBody CustomerDTO dto) {
        return Result.success(customerService.create(dto));
    }

    @PutMapping
    @RequirePermission("business:customer:update")
    public Result<Void> update(@Valid @RequestBody CustomerDTO dto) {
        customerService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:customer:delete")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
