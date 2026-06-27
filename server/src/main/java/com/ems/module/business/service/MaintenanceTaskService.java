package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenanceTaskDTO;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.MaintenancePointMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {

    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final MaintenancePointMapper maintenancePointMapper;

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

    public MaintenanceTask create(MaintenanceTaskDTO dto) {
        MaintenanceTask t = new MaintenanceTask();
        BeanUtils.copyProperties(dto, t);
        if (StringUtils.hasText(dto.getPlanDate())) t.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        if (StringUtils.hasText(dto.getPlanInspectDate())) t.setPlanInspectDate(LocalDate.parse(dto.getPlanInspectDate()));
        if (StringUtils.hasText(dto.getCompleteDate())) t.setCompleteDate(LocalDate.parse(dto.getCompleteDate()));
        if (!StringUtils.hasText(t.getStatus())) t.setStatus("PENDING");
        t.setCreateBy(SecurityContext.getUserId());
        maintenanceTaskMapper.insert(t);
        return t;
    }

    public void update(MaintenanceTaskDTO dto) {
        MaintenanceTask existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getPlanDate())) existing.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        if (StringUtils.hasText(dto.getPlanInspectDate())) existing.setPlanInspectDate(LocalDate.parse(dto.getPlanInspectDate()));
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

    public Integer generateInspection(Long contractId) {
        MaintenanceContract contract = maintenanceContractMapper.selectById(contractId);
        if (contract == null) throw new BusinessException("维保合同不存在");
        if (!"ACTIVE".equals(contract.getStatus())) throw new BusinessException("合同非生效状态");
        if (contract.getProjectId() == null) return 0;

        LambdaQueryWrapper<MaintenancePoint> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(MaintenancePoint::getProjectId, contract.getProjectId());
        List<MaintenancePoint> points = maintenancePointMapper.selectList(pWrapper);

        LocalDate today = LocalDate.now();
        String planCode = "INS-" + today.getYear() + String.format("%02d", today.getMonthValue());
        int generated = 0;
        for (MaintenancePoint point : points) {
            LambdaQueryWrapper<MaintenanceTask> tWrapper = new LambdaQueryWrapper<>();
            tWrapper.eq(MaintenanceTask::getProjectId, contract.getProjectId())
                    .eq(MaintenanceTask::getPointId, point.getId())
                    .eq(MaintenanceTask::getType, "INSPECTION")
                    .likeRight(MaintenanceTask::getCode, planCode);
            Long exists = maintenanceTaskMapper.selectCount(tWrapper);
            if (exists > 0) continue;

            MaintenanceTask task = new MaintenanceTask();
            task.setCode(planCode + "-" + point.getId());
            task.setProjectId(contract.getProjectId());
            task.setPointId(point.getId());
            task.setType("INSPECTION");
            task.setTitle(point.getName() + " 月度巡检");
            task.setDescription("手动触发的月度巡检工单");
            task.setStatus("PENDING");
            task.setPlanDate(today);
            task.setPlanInspectDate(today.plusDays(7));
            task.setCreateBy(SecurityContext.getUserId());
            maintenanceTaskMapper.insert(task);
            generated++;
        }
        return generated;
    }
}
