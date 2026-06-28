package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenancePointDTO;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.mapper.MaintenancePointMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MaintenancePointService {

    /**
     * 合法状态流转(仅正向):WAITING_QUOTE→QUOTED→CONSTRUCTING→WAITING_ACCEPTANCE→ACCEPTED→SETTLED
     */
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "WAITING_QUOTE", Set.of("QUOTED"),
        "QUOTED", Set.of("CONSTRUCTING"),
        "CONSTRUCTING", Set.of("WAITING_ACCEPTANCE"),
        "WAITING_ACCEPTANCE", Set.of("ACCEPTED"),
        "ACCEPTED", Set.of("SETTLED"),
        "SETTLED", Set.of()
    );

    private final MaintenancePointMapper maintenancePointMapper;

    @Lazy
    @Autowired
    private AcceptanceService acceptanceService;

    public PageResult<MaintenancePoint> page(long pageNum, long pageSize, String code, String name,
                                             String status, Long projectId) {
        LambdaQueryWrapper<MaintenancePoint> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenancePoint::getCode, code);
        if (StringUtils.hasText(name)) wrapper.like(MaintenancePoint::getName, name);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenancePoint::getStatus, status);
        if (projectId != null) wrapper.eq(MaintenancePoint::getProjectId, projectId);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(MaintenancePoint::getCreateTime);
        Page<MaintenancePoint> page = maintenancePointMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public MaintenancePoint get(Long id) {
        MaintenancePoint p = maintenancePointMapper.selectById(id);
        if (p == null) throw new BusinessException("维护点位不存在");
        DataScopeHelper.checkOwnership(p.getCreateBy());
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
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        BeanUtils.copyProperties(dto, existing);
        maintenancePointMapper.updateById(existing);
    }

    public void delete(Long id) {
        MaintenancePoint existing = get(id);
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        maintenancePointMapper.deleteById(id);
    }

    /**
     * 更新点位状态,校验合法转换。
     * 合法流转:WAITING_QUOTE→QUOTED→CONSTRUCTING→WAITING_ACCEPTANCE→ACCEPTED→SETTLED
     * 当状态转为 WAITING_ACCEPTANCE 时,自动创建验收单草稿。
     */
    public void updateStatus(Long id, String status) {
        MaintenancePoint existing = maintenancePointMapper.selectById(id);
        if (existing == null) throw new BusinessException("维护点位不存在");
        String currentStatus = existing.getStatus();
        // 相同状态不重复流转
        if (status.equals(currentStatus)) return;
        Set<String> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(status)) {
            throw new BusinessException("非法状态流转: " + currentStatus + " → " + status);
        }
        int rows = maintenancePointMapper.update(null,
                new LambdaUpdateWrapper<MaintenancePoint>()
                        .eq(MaintenancePoint::getId, id)
                        .eq(MaintenancePoint::getStatus, currentStatus)
                        .set(MaintenancePoint::getStatus, status));
        if (rows == 0) throw new BusinessException("状态更新失败,点位状态可能已变更");

        // 当点位状态转为 WAITING_ACCEPTANCE 时,自动创建验收单草稿
        if ("WAITING_ACCEPTANCE".equals(status)) {
            acceptanceService.createDraftWhenCompleted(id);
        }
    }
}
