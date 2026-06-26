package com.ems.module.system.controller;

import com.ems.common.Result;
import com.ems.module.system.entity.SysDept;
import com.ems.module.system.service.SysDeptService;
import com.ems.security.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/list")
    @RequirePermission("system:dept:list")
    public Result<List<SysDept>> list() {
        return Result.success(deptService.list());
    }

    @GetMapping("/tree")
    @RequirePermission("system:dept:list")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.tree());
    }

    @PostMapping
    @RequirePermission("system:dept:add")
    public Result<Void> create(@RequestBody SysDept dept) {
        deptService.create(dept);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("system:dept:edit")
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:dept:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }
}
