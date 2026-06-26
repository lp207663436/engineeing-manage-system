package com.ems.module.system.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.system.dto.SysUserDTO;
import com.ems.module.system.dto.UserRoleDTO;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.service.SysUserService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping("/page")
    @RequirePermission("system:user:list")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Long deptId) {
        return Result.success(userService.page(pageNum, pageSize, name, deptId));
    }

    @PostMapping
    @RequirePermission("system:user:add")
    public Result<Void> create(@Valid @RequestBody SysUserDTO dto) {
        userService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("system:user:edit")
    public Result<Void> update(@Valid @RequestBody SysUserDTO dto) {
        userService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:user:delete")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PostMapping("/assignRoles")
    @RequirePermission("system:user:edit")
    public Result<Void> assignRoles(@RequestBody UserRoleDTO dto) {
        userService.assignRoles(dto);
        return Result.success();
    }

    @GetMapping("/roleIds/{userId}")
    @RequirePermission("system:user:list")
    public Result<List<Long>> getRoleIds(@PathVariable Long userId) {
        return Result.success(userService.getRoleIds(userId));
    }
}
