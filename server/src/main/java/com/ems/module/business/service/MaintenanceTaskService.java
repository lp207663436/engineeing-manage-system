package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenanceTaskDTO;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.entity.MaintenanceRecord;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.MaintenancePointMapper;
import com.ems.module.business.mapper.MaintenanceRecordMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {

    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final MaintenancePointMapper maintenancePointMapper;
    private final MaintenanceRecordMapper maintenanceRecordMapper;

    public PageResult<MaintenanceTask> page(long pageNum, long pageSize, String code, String type,
                                             String status, Long projectId, Long equipmentId) {
        LambdaQueryWrapper<MaintenanceTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenanceTask::getCode, code);
        if (StringUtils.hasText(type)) wrapper.eq(MaintenanceTask::getType, type);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenanceTask::getStatus, status);
        if (projectId != null) wrapper.eq(MaintenanceTask::getProjectId, projectId);
        if (equipmentId != null) wrapper.eq(MaintenanceTask::getEquipmentId, equipmentId);
        DataScopeHelper.applyTo(wrapper);
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
        if (!"PENDING".equals(existing.getStatus())) throw new BusinessException("仅待处理状态可派单");
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .eq(MaintenanceTask::getStatus, "PENDING")
                        .set(MaintenanceTask::getHandlerId, handlerId)
                        .set(MaintenanceTask::getStatus, "ASSIGNED"));
        if (rows == 0) throw new BusinessException("派单失败,工单状态可能已变更");
    }

    /**
     * 处理,status→PROCESSING
     */
    public void process(Long id, String handleMethod, String partsUsed) {
        MaintenanceTask existing = get(id);
        if (!"ASSIGNED".equals(existing.getStatus())) throw new BusinessException("仅已派单状态可开始处理");
        if (existing.getHandlerId() == null) throw new BusinessException("工单尚未派单,无法处理");
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .eq(MaintenanceTask::getStatus, "ASSIGNED")
                        .set(MaintenanceTask::getHandleMethod, handleMethod)
                        .set(MaintenanceTask::getPartsUsed, partsUsed)
                        .set(MaintenanceTask::getStatus, "PROCESSING"));
        if (rows == 0) throw new BusinessException("处理失败,工单状态可能已变更");
    }

    /**
     * 完工,status→WAITING_ACCEPTANCE
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, String completeDate) {
        MaintenanceTask existing = get(id);
        if (!"PROCESSING".equals(existing.getStatus())) throw new BusinessException("仅处理中状态可完工");
        if (StringUtils.hasText(completeDate)) existing.setCompleteDate(LocalDate.parse(completeDate));
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .eq(MaintenanceTask::getStatus, "PROCESSING")
                        .set(MaintenanceTask::getCompleteDate, existing.getCompleteDate())
                        .set(MaintenanceTask::getStatus, "WAITING_ACCEPTANCE"));
        if (rows == 0) throw new BusinessException("完工失败,工单状态可能已变更");

        // 完工后自动生成维保记录
        MaintenanceRecord record = new MaintenanceRecord();
        record.setCode("MR-" + System.currentTimeMillis());
        record.setProjectId(existing.getProjectId());
        record.setPointId(existing.getPointId());
        record.setTaskId(existing.getId());
        record.setEquipmentId(existing.getEquipmentId());
        record.setRecordType(existing.getType());
        record.setRecordDate(LocalDate.now());
        record.setRecorderId(existing.getHandlerId());
        record.setContent(existing.getTitle() + " - " + (existing.getDescription() != null ? existing.getDescription() : ""));
        record.setRemark(existing.getHandleMethod());
        record.setCreateBy(SecurityContext.getUserId());
        maintenanceRecordMapper.insert(record);
    }

    /**
     * 验收通过,status→COMPLETED
     */
    public void accept(Long id) {
        MaintenanceTask existing = get(id);
        if (!"WAITING_ACCEPTANCE".equals(existing.getStatus())) throw new BusinessException("仅待验收状态可验收通过");
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .eq(MaintenanceTask::getStatus, "WAITING_ACCEPTANCE")
                        .set(MaintenanceTask::getStatus, "COMPLETED"));
        if (rows == 0) throw new BusinessException("验收失败,工单状态可能已变更");
    }

    /**
     * 验收打回,status→PROCESSING
     */
    public void reject(Long id, String reason) {
        MaintenanceTask existing = get(id);
        if (!"WAITING_ACCEPTANCE".equals(existing.getStatus())) throw new BusinessException("仅待验收状态可打回");
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .eq(MaintenanceTask::getStatus, "WAITING_ACCEPTANCE")
                        .set(MaintenanceTask::getStatus, "PROCESSING")
                        .set(MaintenanceTask::getRemark, reason));
        if (rows == 0) throw new BusinessException("打回失败,工单状态可能已变更");
    }

    /**
     * 关闭,status→CLOSED
     */
    public void close(Long id) {
        MaintenanceTask existing = get(id);
        if (!"WAITING_ACCEPTANCE".equals(existing.getStatus()) && !"COMPLETED".equals(existing.getStatus()))
            throw new BusinessException("仅待验收或已完成状态可关闭");
        int rows = maintenanceTaskMapper.update(null,
                new LambdaUpdateWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getId, id)
                        .in(MaintenanceTask::getStatus, "WAITING_ACCEPTANCE", "COMPLETED")
                        .set(MaintenanceTask::getStatus, "CLOSED"));
        if (rows == 0) throw new BusinessException("关闭失败,工单状态可能已变更");
    }

    public void delete(Long id) {
        MaintenanceTask existing = get(id);
        if ("PROCESSING".equals(existing.getStatus()) || "CLOSED".equals(existing.getStatus()))
            throw new BusinessException("处理中或已关闭的工单不可删除");
        maintenanceTaskMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
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
            task.setCreateBy(contract.getCreateBy() != null ? contract.getCreateBy() : 0L);
            maintenanceTaskMapper.insert(task);
            generated++;
        }
        return generated;
    }
}
