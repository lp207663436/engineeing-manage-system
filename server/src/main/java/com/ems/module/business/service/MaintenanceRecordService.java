package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenanceRecordDTO;
import com.ems.module.business.entity.MaintenanceRecord;
import com.ems.module.business.mapper.MaintenanceRecordMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {

    private final MaintenanceRecordMapper maintenanceRecordMapper;

    public PageResult<MaintenanceRecord> page(long pageNum, long pageSize, String code, String recordType,
                                               Long projectId, Long equipmentId) {
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenanceRecord::getCode, code);
        if (StringUtils.hasText(recordType)) wrapper.eq(MaintenanceRecord::getRecordType, recordType);
        if (projectId != null) wrapper.eq(MaintenanceRecord::getProjectId, projectId);
        if (equipmentId != null) wrapper.eq(MaintenanceRecord::getEquipmentId, equipmentId);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(MaintenanceRecord::getRecordDate);
        Page<MaintenanceRecord> page = maintenanceRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public MaintenanceRecord get(Long id) {
        MaintenanceRecord r = maintenanceRecordMapper.selectById(id);
        if (r == null) throw new BusinessException("维保记录不存在");
        return r;
    }

    public MaintenanceRecord create(MaintenanceRecordDTO dto) {
        MaintenanceRecord r = new MaintenanceRecord();
        BeanUtils.copyProperties(dto, r);
        if (StringUtils.hasText(dto.getRecordDate())) r.setRecordDate(LocalDate.parse(dto.getRecordDate()));
        r.setCreateBy(SecurityContext.getUserId());
        maintenanceRecordMapper.insert(r);
        return r;
    }

    public void update(MaintenanceRecordDTO dto) {
        MaintenanceRecord existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getRecordDate())) existing.setRecordDate(LocalDate.parse(dto.getRecordDate()));
        maintenanceRecordMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        maintenanceRecordMapper.deleteById(id);
    }

    /**
     * 查设备维保历史
     */
    public List<MaintenanceRecord> listByEquipment(Long equipmentId) {
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenanceRecord::getEquipmentId, equipmentId)
                .orderByDesc(MaintenanceRecord::getRecordDate);
        return maintenanceRecordMapper.selectList(wrapper);
    }
}
