package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenanceTaskDTO;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {

    private final MaintenanceTaskMapper maintenanceTaskMapper;

    public PageResult<MaintenanceTask> page(long pageNum, long pageSize, String code, String type,
                                             String status, Long projectId, Long equipmentId) {
        LambdaQueryWrapper<MaintenanceTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenanceTask::getCode, code);
        if (StringUtils.hasText(type)) wrapper.eq(MaintenanceTask::getType, type);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenanceTask::getStatus, status);
        if (projectId != null) wrapper.eq(MaintenanceTask::getProjectId, projectId);
        if (equipmentId != null) wrapper.eq(MaintenanceTask::getEquipmentId, equipmentId);
        wrapper.orderByDesc(MaintenanceTask::getCreateTime);
        Page<MaintenanceTask> page = maintenanceTaskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public MaintenanceTask get(Long id) {
        MaintenanceTask t = maintenanceTaskMapper.selectById(id);
        if (t == null) throw new BusinessException("维保工单不存在");
        return t;
    }

    public void create(MaintenanceTaskDTO dto) {
        MaintenanceTask t = new MaintenanceTask();
        BeanUtils.copyProperties(dto, t);
        if (StringUtils.hasText(dto.getPlanDate())) t.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        if (StringUtils.hasText(dto.getCompleteDate())) t.setCompleteDate(LocalDate.parse(dto.getCompleteDate()));
        if (!StringUtils.hasText(t.getStatus())) t.setStatus("PENDING");
        t.setCreateBy(SecurityContext.getUserId());
        maintenanceTaskMapper.insert(t);
    }

    public void update(MaintenanceTaskDTO dto) {
        MaintenanceTask existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getPlanDate())) existing.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        if (StringUtils.hasText(dto.getCompleteDate())) existing.setCompleteDate(LocalDate.parse(dto.getCompleteDate()));
        maintenanceTaskMapper.updateById(existing);
    }

    /**
     * 派单,status→ASSIGNED
     */
    public void assign(Long id, Long handlerId) {
        if (handlerId == null) throw new BusinessException("处理人不能为空");
        MaintenanceTask existing = get(id);
        existing.setHandlerId(handlerId);
        existing.setStatus("ASSIGNED");
        maintenanceTaskMapper.updateById(existing);
    }

    /**
     * 处理,status→PROCESSING
     */
    public void process(Long id, String handleMethod, String partsUsed) {
        MaintenanceTask existing = get(id);
        existing.setHandleMethod(handleMethod);
        existing.setPartsUsed(partsUsed);
        existing.setStatus("PROCESSING");
        maintenanceTaskMapper.updateById(existing);
    }

    /**
     * 完工,status→WAITING_ACCEPTANCE
     */
    public void complete(Long id, String completeDate) {
        MaintenanceTask existing = get(id);
        if (StringUtils.hasText(completeDate)) existing.setCompleteDate(LocalDate.parse(completeDate));
        existing.setStatus("WAITING_ACCEPTANCE");
        maintenanceTaskMapper.updateById(existing);
    }

    /**
     * 关闭,status→CLOSED
     */
    public void close(Long id) {
        MaintenanceTask existing = get(id);
        existing.setStatus("CLOSED");
        maintenanceTaskMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        maintenanceTaskMapper.deleteById(id);
    }
}
