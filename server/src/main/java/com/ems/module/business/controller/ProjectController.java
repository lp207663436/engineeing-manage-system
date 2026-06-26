package com.ems.module.business.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.business.dto.ProjectDTO;
import com.ems.module.business.entity.Project;
import com.ems.module.business.service.ProjectService;
import com.ems.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/page")
    @RequirePermission("business:project:list")
    public Result<PageResult<Project>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String status) {
        return Result.success(projectService.page(pageNum, pageSize, name, type, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("business:project:list")
    public Result<Project> get(@PathVariable Long id) {
        return Result.success(projectService.get(id));
    }

    @PostMapping
    @RequirePermission("business:project:create")
    public Result<Void> create(@Valid @RequestBody ProjectDTO dto) {
        projectService.create(dto);
        return Result.success();
    }

    @PutMapping
    @RequirePermission("business:project:update")
    public Result<Void> update(@Valid @RequestBody ProjectDTO dto) {
        projectService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("business:project:delete")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.success();
    }
}
