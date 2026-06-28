package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ContractChangeDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.ContractChange;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.mapper.ContractChangeMapper;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.module.business.mapper.QuarterlySettlementMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractChangeService {

    private final ContractChangeMapper contractChangeMapper;
    private final ContractMapper contractMapper;
    private final QuarterlySettlementMapper quarterlySettlementMapper;

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
        DataScopeHelper.checkOwnership(c.getCreateBy());
        return c;
    }

    public ContractChange create(ContractChangeDTO dto) {
        Contract contract = contractMapper.selectById(dto.getContractId());
        if (contract == null) throw new BusinessException("关联合同不存在");
        ContractChange c = new ContractChange();
        BeanUtils.copyProperties(dto, c);
        // 金额变更时校验 newAmount > 0
        if ("AMOUNT".equals(c.getChangeField()) && c.getNewAmount() != null
                && c.getNewAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("变更金额必须大于0");
        }
        if (!StringUtils.hasText(c.getStatus())) c.setStatus("PENDING");
        c.setCreateBy(SecurityContext.getUserId());
        contractChangeMapper.insert(c);
        return c;
    }

    public void update(ContractChangeDTO dto) {
        ContractChange existing = get(dto.getId());
        DataScopeHelper.checkOwnership(existing.getCreateBy());
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
        DataScopeHelper.checkOwnership(existing.getCreateBy());
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

        // 校验前置:审核通过且金额变更时,在 updateById 之前校验 newAmount > 0
        if ("APPROVED".equals(status) && "AMOUNT".equals(existing.getChangeField())
                && existing.getNewAmount() != null
                && existing.getNewAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("变更金额必须大于0");
        }

        existing.setStatus(status);
        existing.setApproverId(SecurityContext.getUserId());
        existing.setApproveTime(LocalDateTime.now());
        if (StringUtils.hasText(remark)) existing.setRemark(remark);
        contractChangeMapper.updateById(existing);

        // 审核通过后,回写合同主表对应字段
        if ("APPROVED".equals(status)) {
            Contract contract = contractMapper.selectById(existing.getContractId());
            if (contract != null) {
                if ("AMOUNT".equals(existing.getChangeField()) && existing.getNewAmount() != null) {
                    contract.setAmount(existing.getNewAmount());
                    // 维保合同金额变更:从下一期起按新金额重算 QuarterlySettlement.amount,递增 amountVersion,当期保持旧金额
                    recalcQuarterlySettlements(existing.getContractId(), existing.getNewAmount());
                } else if ("START_DATE".equals(existing.getChangeField()) && existing.getNewDate() != null) {
                    contract.setStartDate(existing.getNewDate());
                } else if ("END_DATE".equals(existing.getChangeField()) && existing.getNewDate() != null) {
                    contract.setEndDate(existing.getNewDate());
                }
                contractMapper.updateById(contract);
            }
        }
    }

    /**
     * 维保合同金额变更后,从下一期起按新金额重算 QuarterlySettlement.amount,
     * 递增 amountVersion,当期保持旧金额。
     */
    private void recalcQuarterlySettlements(Long contractId, BigDecimal newAmount) {
        List<QuarterlySettlement> settlements = quarterlySettlementMapper.selectList(
                new LambdaQueryWrapper<QuarterlySettlement>()
                        .eq(QuarterlySettlement::getContractId, contractId)
                        .orderByAsc(QuarterlySettlement::getPeriodNo));
        if (settlements.isEmpty()) return;

        LocalDate today = LocalDate.now();
        int totalPeriods = settlements.size();

        // 找到当前期(今天在 periodStartDate 和 periodEndDate 之间)
        int currentPeriodIndex = -1;
        for (int i = 0; i < settlements.size(); i++) {
            QuarterlySettlement s = settlements.get(i);
            if ((s.getPeriodStartDate() == null || !today.isBefore(s.getPeriodStartDate()))
                    && (s.getPeriodEndDate() == null || !today.isAfter(s.getPeriodEndDate()))) {
                currentPeriodIndex = i;
                break;
            }
        }
        // 找不到当前期(所有期次都已结束或都未开始),不重算
        if (currentPeriodIndex == -1) return;
        // 没有后续期次,不重算
        if (currentPeriodIndex + 1 >= settlements.size()) return;

        // 新的单期金额 = 新总额 / 总期数(保留2位)
        BigDecimal newBaseAmount = newAmount.divide(BigDecimal.valueOf(totalPeriods), 2, RoundingMode.HALF_UP);

        // 计算不变期(当期及之前)的金额之和
        BigDecimal unchangedSum = BigDecimal.ZERO;
        for (int i = 0; i <= currentPeriodIndex; i++) {
            BigDecimal amt = settlements.get(i).getAmount();
            unchangedSum = unchangedSum.add(amt == null ? BigDecimal.ZERO : amt);
        }

        // 重算后续期次
        int futureCount = settlements.size() - currentPeriodIndex - 1;
        for (int i = currentPeriodIndex + 1; i < settlements.size(); i++) {
            QuarterlySettlement s = settlements.get(i);
            Integer oldVersion = s.getAmountVersion() == null ? 0 : s.getAmountVersion();
            BigDecimal amount;
            if (i == settlements.size() - 1) {
                // 最后一期吸纳尾差:总额 - 不变期之和 - 前面重算期之和
                BigDecimal recalcedPrevSum = newBaseAmount.multiply(BigDecimal.valueOf(futureCount - 1));
                amount = newAmount.subtract(unchangedSum).subtract(recalcedPrevSum);
            } else {
                amount = newBaseAmount;
            }
            quarterlySettlementMapper.update(null,
                    new LambdaUpdateWrapper<QuarterlySettlement>()
                            .eq(QuarterlySettlement::getId, s.getId())
                            .eq(QuarterlySettlement::getAmountVersion, oldVersion)
                            .set(QuarterlySettlement::getAmount, amount)
                            .set(QuarterlySettlement::getAmountVersion, oldVersion + 1));
        }
    }
}
