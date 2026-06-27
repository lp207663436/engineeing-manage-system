package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ProjectDTO;
import com.ems.module.business.entity.Project;
import com.ems.module.business.mapper.ProjectMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public PageResult<Project> page(long pageNum, long pageSize, String name, String type, String status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(Project::getName, name);
        if (StringUtils.hasText(type)) wrapper.eq(Project::getType, type);
        if (StringUtils.hasText(status)) wrapper.eq(Project::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Project::getCreateTime);
        Page<Project> page = projectMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Project get(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) throw new BusinessException("项目不存在");
        return p;
    }

    public Project create(ProjectDTO dto) {
        Project p = new Project();
        BeanUtils.copyProperties(dto, p);
        if (StringUtils.hasText(dto.getStartDate())) p.setStartDate(LocalDate.parse(dto.getStartDate()));
        if (StringUtils.hasText(dto.getEndDate())) p.setEndDate(LocalDate.parse(dto.getEndDate()));
        if (!StringUtils.hasText(p.getType())) p.setType("NEW_BUILD");
        if (!StringUtils.hasText(p.getStatus())) p.setStatus("DRAFT");
        p.setCreateBy(SecurityContext.getUserId());
        projectMapper.insert(p);
        return p;
    }

    public void update(ProjectDTO dto) {
        Project existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getStartDate())) existing.setStartDate(LocalDate.parse(dto.getStartDate()));
        if (StringUtils.hasText(dto.getEndDate())) existing.setEndDate(LocalDate.parse(dto.getEndDate()));
        projectMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        projectMapper.deleteById(id);
    }
}
