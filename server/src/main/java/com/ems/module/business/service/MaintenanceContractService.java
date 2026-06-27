package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.MaintenanceContractDTO;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MaintenanceContractService {

    private final MaintenanceContractMapper maintenanceContractMapper;

    public PageResult<MaintenanceContract> page(long pageNum, long pageSize, String code, String name,
                                                String status, Long projectId) {
        LambdaQueryWrapper<MaintenanceContract> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(MaintenanceContract::getCode, code);
        if (StringUtils.hasText(name)) wrapper.like(MaintenanceContract::getName, name);
        if (StringUtils.hasText(status)) wrapper.eq(MaintenanceContract::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        if (projectId != null) wrapper.eq(MaintenanceContract::getProjectId, projectId);
        wrapper.orderByDesc(MaintenanceContract::getCreateTime);
        Page<MaintenanceContract> page = maintenanceContractMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public MaintenanceContract get(Long id) {
        MaintenanceContract c = maintenanceContractMapper.selectById(id);
        if (c == null) throw new BusinessException("维保主合同不存在");
        return c;
    }

    public MaintenanceContract create(MaintenanceContractDTO dto) {
        validatePeriod(dto.getPeriodMonths());
        MaintenanceContract c = new MaintenanceContract();
        BeanUtils.copyProperties(dto, c);
        if (StringUtils.hasText(dto.getSignDate())) c.setSignDate(LocalDate.parse(dto.getSignDate()));
        if (StringUtils.hasText(dto.getEffectiveDate())) {
            LocalDate effective = LocalDate.parse(dto.getEffectiveDate());
            c.setEffectiveDate(effective);
            c.setEndDate(getEndDate(effective, dto.getPeriodMonths()));
        }
        c.setPeriodCount(dto.getPeriodMonths() / 3);
        if (!StringUtils.hasText(c.getStatus())) c.setStatus("ACTIVE");
        c.setCreateBy(SecurityContext.getUserId());
        maintenanceContractMapper.insert(c);
        return c;
    }

    public void update(MaintenanceContractDTO dto) {
        validatePeriod(dto.getPeriodMonths());
        MaintenanceContract existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getSignDate())) existing.setSignDate(LocalDate.parse(dto.getSignDate()));
        if (StringUtils.hasText(dto.getEffectiveDate())) {
            LocalDate effective = LocalDate.parse(dto.getEffectiveDate());
            existing.setEffectiveDate(effective);
            existing.setEndDate(getEndDate(effective, dto.getPeriodMonths()));
        }
        existing.setPeriodCount(dto.getPeriodMonths() / 3);
        maintenanceContractMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        maintenanceContractMapper.deleteById(id);
    }

    /**
     * 校验合同期:必须为3个月整数倍且不少于6个月
     */
    private void validatePeriod(Integer periodMonths) {
        if (periodMonths == null || periodMonths % 3 != 0 || periodMonths < 6) {
            throw new BusinessException("合同期必须为3个月整数倍且不少于6个月");
        }
    }

    /**
     * 计算合同到期日=生效日+合同期月数-1天
     */
    public LocalDate getEndDate(LocalDate effectiveDate, Integer periodMonths) {
        if (effectiveDate == null || periodMonths == null) return null;
        return effectiveDate.plusMonths(periodMonths).minusDays(1);
    }
}
