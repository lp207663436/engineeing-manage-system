package com.ems.module.system.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.system.dto.SysDictDTO;
import com.ems.module.system.dto.SysDictItemDTO;
import com.ems.module.system.entity.SysDict;
import com.ems.module.system.entity.SysDictItem;
import com.ems.module.system.service.SysDictService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping("/page")
    @RequirePermission("system:dict:list")
    public Result<PageResult<SysDict>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String code) {
        return Result.success(sysDictService.page(pageNum, pageSize, name, code));
    }

    @GetMapping("/list")
    @RequirePermission("system:dict:list")
    public Result<List<SysDict>> list() {
        return Result.success(sysDictService.list());
    }

    @GetMapping("/itemsByCode")
    @RequirePermission("system:dict:list")
    public Result<List<SysDictItem>> itemsByCode(@RequestParam String code) {
        return Result.success(sysDictService.itemsByCode(code));
    }

    @GetMapping("/itemsByDictId")
    @RequirePermission("system:dict:list")
    public Result<List<SysDictItem>> itemsByDictId(@RequestParam Long dictId) {
        return Result.success(sysDictService.itemsByDictId(dictId));
    }

    @GetMapping("/{id}")
    @RequirePermission("system:dict:list")
    public Result<SysDict> get(@PathVariable Long id) {
        return Result.success(sysDictService.get(id));
    }

    @PostMapping
    @RequirePermission("system:dict:create")
    public Result<SysDict> create(@Valid @RequestBody SysDictDTO dto) {
        return Result.success(sysDictService.create(dto));
    }

    @PutMapping
    @RequirePermission("system:dict:update")
    public Result<Void> update(@Valid @RequestBody SysDictDTO dto) {
        sysDictService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:dict:delete")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictService.delete(id);
        return Result.success();
    }

    @PostMapping("/item")
    @RequirePermission("system:dict:create")
    public Result<SysDictItem> itemCreate(@Valid @RequestBody SysDictItemDTO dto) {
        return Result.success(sysDictService.itemCreate(dto));
    }

    @PutMapping("/item")
    @RequirePermission("system:dict:update")
    public Result<Void> itemUpdate(@Valid @RequestBody SysDictItemDTO dto) {
        sysDictService.itemUpdate(dto);
        return Result.success();
    }

    @DeleteMapping("/item/{id}")
    @RequirePermission("system:dict:delete")
    public Result<Void> itemDelete(@PathVariable Long id) {
        sysDictService.itemDelete(id);
        return Result.success();
    }
}
