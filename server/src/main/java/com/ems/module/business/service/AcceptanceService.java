package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.AcceptanceDTO;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AcceptanceService {

    private final AcceptanceMapper acceptanceMapper;

    public PageResult<Acceptance> page(long pageNum, long pageSize, String code, String result,
                                       String businessType, Long businessId) {
        LambdaQueryWrapper<Acceptance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(Acceptance::getCode, code);
        if (StringUtils.hasText(result)) wrapper.eq(Acceptance::getResult, result);
        if (StringUtils.hasText(businessType)) wrapper.eq(Acceptance::getBusinessType, businessType);
        if (businessId != null) wrapper.eq(Acceptance::getBusinessId, businessId);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Acceptance::getCreateTime);
        Page<Acceptance> page = acceptanceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Acceptance get(Long id) {
        Acceptance a = acceptanceMapper.selectById(id);
        if (a == null) throw new BusinessException("验收单不存在");
        return a;
    }

    public Acceptance create(AcceptanceDTO dto) {
        Acceptance a = new Acceptance();
        BeanUtils.copyProperties(dto, a);
        if (StringUtils.hasText(dto.getAcceptDate())) a.setAcceptDate(LocalDate.parse(dto.getAcceptDate()));
        if (!StringUtils.hasText(a.getBusinessType())) a.setBusinessType("NEW_BUILD");
        if (a.getBusinessId() == null) a.setBusinessId(dto.getProjectId());
        if (!StringUtils.hasText(a.getResult())) a.setResult("PENDING");
        if (a.getRectifyCount() == null) a.setRectifyCount(0);
        a.setCreateBy(SecurityContext.getUserId());
        acceptanceMapper.insert(a);
        return a;
    }

    public void update(AcceptanceDTO dto) {
        Acceptance existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getAcceptDate())) existing.setAcceptDate(LocalDate.parse(dto.getAcceptDate()));
        acceptanceMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        acceptanceMapper.deleteById(id);
    }

    /**
     * 提交验收结论。若 result=FAIL 则 rectify_count+1;若 rectify_count>3 则强制 result=ARBITRATION。
     */
    public void submitResult(Long id, String result, String remark) {
        Acceptance existing = get(id);
        if ("FAIL".equals(result)) {
            int count = (existing.getRectifyCount() == null ? 0 : existing.getRectifyCount()) + 1;
            existing.setRectifyCount(count);
            if (count > 3) {
                result = "ARBITRATION";
            }
        }
        existing.setResult(result);
        if (StringUtils.hasText(remark)) existing.setRemark(remark);
        acceptanceMapper.updateById(existing);
    }
}
