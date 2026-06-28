package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ContractChangeDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.ContractChange;
import com.ems.module.business.mapper.ContractChangeMapper;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContractChangeService {

    private final ContractChangeMapper contractChangeMapper;
    private final ContractMapper contractMapper;

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
        Contract contract = contractMapper.selectById(dto.getContractId());
        if (contract == null) throw new BusinessException("关联合同不存在");
        ContractChange c = new ContractChange();
        BeanUtils.copyProperties(dto, c);
        if (!StringUtils.hasText(c.getStatus())) c.setStatus("PENDING");
        c.setCreateBy(SecurityContext.getUserId());
        contractChangeMapper.insert(c);
        return c;
    }

    public void update(ContractChangeDTO dto) {
        ContractChange existing = get(dto.getId());
        if ("APPROVED".equals(existing.getStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        // REJECTED 状态允许修改并重新提交(状态回到 PENDING)
        BeanUtils.copyProperties(dto, existing);
        if ("REJECTED".equals(existing.getStatus())) {
            existing.setStatus("PENDING");
        }
        int rows = contractChangeMapper.updateById(existing);
        if (rows == 0) throw new BusinessException("更新失败,记录可能已被修改");
    }

    public void delete(Long id) {
        ContractChange existing = get(id);
        if ("APPROVED".equals(existing.getStatus())) throw new BusinessException("已审核通过的变更不可删除");
        contractChangeMapper.deleteById(id);
    }

    /**
     * 审核
     * @param id 变更记录ID
     * @param status APPROVED/REJECTED
     * @param remark 审核备注
     */
    @Transactional(rollbackFor = Exception.class)
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

        // 审核通过且金额变更时,回写合同主表金额
        if ("APPROVED".equals(status) && "AMOUNT".equals(existing.getChangeField()) && existing.getNewAmount() != null) {
            if (existing.getNewAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessException("变更金额必须大于0");
            }
            Contract contract = contractMapper.selectById(existing.getContractId());
            if (contract != null) {
                contract.setAmount(existing.getNewAmount());
                contractMapper.updateById(contract);
            }
        }
    }
}
