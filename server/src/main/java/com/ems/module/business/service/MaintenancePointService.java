package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenancePointDTO;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.mapper.MaintenancePointMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MaintenancePointService {

    private final MaintenancePointMapper maintenancePointMapper;

    public PageResult<MaintenancePoint> page(long pageNum, long pageSize, String code, String name,
                                             String status, Long projectId) {
        LambdaQueryWrapper<MaintenancePoint> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenancePoint::getCode, code);
        if (StringUtils.hasText(name)) wrapper.like(MaintenancePoint::getName, name);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenancePoint::getStatus, status);
        if (projectId != null) wrapper.eq(MaintenancePoint::getProjectId, projectId);
        wrapper.orderByDesc(MaintenancePoint::getCreateTime);
        Page<MaintenancePoint> page = maintenancePointMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public MaintenancePoint get(Long id) {
        MaintenancePoint p = maintenancePointMapper.selectById(id);
        if (p == null) throw new BusinessException("维护点位不存在");
        return p;
    }

    public MaintenancePoint create(MaintenancePointDTO dto) {
        MaintenancePoint p = new MaintenancePoint();
        BeanUtils.copyProperties(dto, p);
        if (!StringUtils.hasText(p.getStatus())) p.setStatus("WAITING_QUOTE");
        p.setCreateBy(SecurityContext.getUserId());
        maintenancePointMapper.insert(p);
        return p;
    }

    public void update(MaintenancePointDTO dto) {
        MaintenancePoint existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        maintenancePointMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        maintenancePointMapper.deleteById(id);
    }
}
