package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ContractChangeDTO;
import com.ems.module.business.entity.ContractChange;
import com.ems.module.business.mapper.ContractChangeMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContractChangeService {

    private final ContractChangeMapper contractChangeMapper;

    public PageResult<ContractChange> page(long pageNum, long pageSize, Long contractId, String changeType, String status) {
        LambdaQueryWrapper<ContractChange> wrapper = new LambdaQueryWrapper<>();
        if (contractId != null) wrapper.eq(ContractChange::getContractId, contractId);
        if (StringUtils.hasText(changeType)) wrapper.eq(ContractChange::getChangeType, changeType);
        if (StringUtils.hasText(status)) wrapper.eq(ContractChange::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(ContractChange::getCreateTime);
        Page<ContractChange> page = contractChangeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public ContractChange get(Long id) {
        ContractChange c = contractChangeMapper.selectById(id);
        if (c == null) throw new BusinessException("合同变更记录不存在");
        return c;
    }

    public ContractChange create(ContractChangeDTO dto) {
        ContractChange c = new ContractChange();
        BeanUtils.copyProperties(dto, c);
        if (!StringUtils.hasText(c.getStatus())) c.setStatus("PENDING");
        c.setCreateBy(SecurityContext.getUserId());
        contractChangeMapper.insert(c);
        return c;
    }

    public void update(ContractChangeDTO dto) {
        ContractChange existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        contractChangeMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        contractChangeMapper.deleteById(id);
    }

    /**
     * 审核
     * @param id 变更记录ID
     * @param status APPROVED/REJECTED
     * @param remark 审核备注
     */
    public void audit(Long id, String status, String remark) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException("审核状态只能为 APPROVED 或 REJECTED");
        }
        ContractChange existing = get(id);
        if (!"PENDING".equals(existing.getStatus())) {
            throw new BusinessException("当前记录非待审核状态,无法审核");
        }
        existing.setStatus(status);
        existing.setApproverId(SecurityContext.getUserId());
        existing.setApproveTime(LocalDateTime.now());
        if (StringUtils.hasText(remark)) existing.setRemark(remark);
        contractChangeMapper.updateById(existing);
    }
}
