package com.ems.module.system.controller;

import com.ems.common.Result;
import com.ems.module.system.entity.SysMenu;
import com.ems.module.system.service.SysMenuService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @GetMapping("/list")
    @RequirePermission("system:menu:list")
    public Result<List<SysMenu>> list() {
        return Result.success(menuService.list());
    }

    @GetMapping("/tree")
    @RequirePermission("system:menu:list")
    public Result<List<SysMenu>> tree() {
        return Result.success(menuService.tree());
    }

    @GetMapping("/userMenus")
    public Result<List<SysMenu>> userMenus() {
        return Result.success(menuService.getUserMenus(SecurityContext.getUserId()));
    }

    @PostMapping
    @RequirePermission("system:menu:add")
    public Result<Void> create(@RequestBody SysMenu menu) {
        menuService.create(menu);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("system:menu:edit")
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}
