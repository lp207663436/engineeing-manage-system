package com.ems.module.system.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.system.dto.SysRoleDTO;
import com.ems.module.system.entity.SysRole;
import com.ems.module.system.service.SysRoleService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/page")
    @RequirePermission("system:role:list")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name) {
        return Result.success(roleService.page(pageNum, pageSize, name));
    }

    @GetMapping("/list")
    @RequirePermission("system:role:list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @PostMapping
    @RequirePermission("system:role:add")
    public Result<Void> create(@Valid @RequestBody SysRoleDTO dto) {
        roleService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("system:role:edit")
    public Result<Void> update(@Valid @RequestBody SysRoleDTO dto) {
        roleService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @GetMapping("/menuIds/{roleId}")
    @RequirePermission("system:role:list")
    public Result<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return Result.success(roleService.getMenuIds(roleId));
    }

    /**
     * 分配菜单:仅接收 menuIds,不触碰角色基本信息,避免清空角色名。
     */
    @PostMapping("/{id}/menus")
    @RequirePermission("system:role:edit")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody AssignMenusRequest request) {
        roleService.assignMenus(id, request.getMenuIds());
        return Result.success();
    }

    /**
     * 分配菜单请求体
     */
    @lombok.Data
    public static class AssignMenusRequest {
        private List<Long> menuIds;
    }
}
