package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ProgressDTO;
import com.ems.module.business.entity.Progress;
import com.ems.module.business.mapper.ProgressMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressMapper progressMapper;

    public PageResult<Progress> page(long pageNum, long pageSize, String code, String nodeName, String status,
                                     String businessType, Long businessId, Long projectId) {
        LambdaQueryWrapper<Progress> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(Progress::getCode, code);
        if (StringUtils.hasText(nodeName)) wrapper.like(Progress::getNodeName, nodeName);
        if (StringUtils.hasText(status)) wrapper.eq(Progress::getStatus, status);
        if (StringUtils.hasText(businessType)) wrapper.eq(Progress::getBusinessType, businessType);
        if (businessId != null) wrapper.eq(Progress::getBusinessId, businessId);
        if (projectId != null) wrapper.eq(Progress::getProjectId, projectId);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Progress::getCreateTime);
        Page<Progress> page = progressMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Progress get(Long id) {
        Progress p = progressMapper.selectById(id);
        if (p == null) throw new BusinessException("进度不存在");
        return p;
    }

    public Progress create(ProgressDTO dto) {
        Progress p = new Progress();
        BeanUtils.copyProperties(dto, p);
        if (StringUtils.hasText(dto.getPlanStartDate())) p.setPlanStartDate(LocalDate.parse(dto.getPlanStartDate()));
        if (StringUtils.hasText(dto.getPlanEndDate())) p.setPlanEndDate(LocalDate.parse(dto.getPlanEndDate()));
        if (StringUtils.hasText(dto.getActualStartDate())) p.setActualStartDate(LocalDate.parse(dto.getActualStartDate()));
        if (StringUtils.hasText(dto.getActualEndDate())) p.setActualEndDate(LocalDate.parse(dto.getActualEndDate()));
        if (!StringUtils.hasText(p.getBusinessType())) p.setBusinessType("NEW_BUILD");
        if (p.getBusinessId() == null) p.setBusinessId(dto.getProjectId());
        if (!StringUtils.hasText(p.getStatus())) p.setStatus("PENDING");
        if (p.getProgressPercent() == null) p.setProgressPercent(0);
        p.setCreateBy(SecurityContext.getUserId());
        progressMapper.insert(p);
        return p;
    }

    public void update(ProgressDTO dto) {
        Progress existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getPlanStartDate())) existing.setPlanStartDate(LocalDate.parse(dto.getPlanStartDate()));
        if (StringUtils.hasText(dto.getPlanEndDate())) existing.setPlanEndDate(LocalDate.parse(dto.getPlanEndDate()));
        if (StringUtils.hasText(dto.getActualStartDate())) existing.setActualStartDate(LocalDate.parse(dto.getActualStartDate()));
        if (StringUtils.hasText(dto.getActualEndDate())) existing.setActualEndDate(LocalDate.parse(dto.getActualEndDate()));
        progressMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        progressMapper.deleteById(id);
    }
}
