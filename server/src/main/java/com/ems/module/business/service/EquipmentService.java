package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.EquipmentDTO;
import com.ems.module.business.entity.Equipment;
import com.ems.module.business.mapper.EquipmentMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;

    public PageResult<Equipment> page(long pageNum, long pageSize, String code, String name,
                                      String category, String status, Long projectId) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(Equipment::getCode, code);
        if (StringUtils.hasText(name)) wrapper.like(Equipment::getName, name);
        if (StringUtils.hasText(category)) wrapper.eq(Equipment::getCategory, category);
        if (StringUtils.hasText(status)) wrapper.eq(Equipment::getStatus, status);
        if (projectId != null) wrapper.eq(Equipment::getProjectId, projectId);
        wrapper.orderByDesc(Equipment::getCreateTime);
        Page<Equipment> page = equipmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Equipment get(Long id) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) throw new BusinessException("设备不存在");
        return e;
    }

    public Equipment create(EquipmentDTO dto) {
        Equipment e = new Equipment();
        BeanUtils.copyProperties(dto, e);
        if (StringUtils.hasText(dto.getCommissioningDate())) e.setCommissioningDate(LocalDate.parse(dto.getCommissioningDate()));
        if (StringUtils.hasText(dto.getWarrantyExpiry())) e.setWarrantyExpiry(LocalDate.parse(dto.getWarrantyExpiry()));
        if (!StringUtils.hasText(e.getStatus())) e.setStatus("NORMAL");
        e.setCreateBy(SecurityContext.getUserId());
        equipmentMapper.insert(e);
        return e;
    }

    public void update(EquipmentDTO dto) {
        Equipment existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getCommissioningDate())) existing.setCommissioningDate(LocalDate.parse(dto.getCommissioningDate()));
        if (StringUtils.hasText(dto.getWarrantyExpiry())) existing.setWarrantyExpiry(LocalDate.parse(dto.getWarrantyExpiry()));
        equipmentMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        equipmentMapper.deleteById(id);
    }
}
