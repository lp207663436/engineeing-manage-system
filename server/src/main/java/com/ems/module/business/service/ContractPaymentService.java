package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ContractPaymentDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.ContractPayment;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.module.business.mapper.ContractPaymentMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPaymentService {

    private final ContractPaymentMapper contractPaymentMapper;
    private final ContractMapper contractMapper;

    public PageResult<ContractPayment> page(long pageNum, long pageSize, String code, Long contractId,
                                            String type, String status) {
        LambdaQueryWrapper<ContractPayment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(ContractPayment::getCode, code);
        if (contractId != null) wrapper.eq(ContractPayment::getContractId, contractId);
        if (StringUtils.hasText(type)) wrapper.eq(ContractPayment::getType, type);
        if (StringUtils.hasText(status)) wrapper.eq(ContractPayment::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(ContractPayment::getCreateTime);
        Page<ContractPayment> page = contractPaymentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public ContractPayment get(Long id) {
        ContractPayment p = contractPaymentMapper.selectById(id);
        if (p == null) throw new BusinessException("收付款记录不存在");
        return p;
    }

    public ContractPayment create(ContractPaymentDTO dto) {
        validateContractExist(dto.getContractId());
        ContractPayment p = new ContractPayment();
        BeanUtils.copyProperties(dto, p);
        if (StringUtils.hasText(dto.getPlanDate())) {
            p.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        }
        if (StringUtils.hasText(dto.getActualDate())) {
            p.setActualDate(LocalDate.parse(dto.getActualDate()));
        }
        if (!StringUtils.hasText(p.getCode())) {
            p.setCode("CP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        if (!StringUtils.hasText(p.getStatus())) {
            p.setStatus("PENDING");
        }
        p.setCreateBy(SecurityContext.getUserId());
        contractPaymentMapper.insert(p);
        return p;
    }

    public void update(ContractPaymentDTO dto) {
        ContractPayment existing = get(dto.getId());
        if (dto.getContractId() != null) {
            validateContractExist(dto.getContractId());
        }
        BeanUtils.copyProperties(dto, existing);
        if (dto.getPlanDate() != null) {
            existing.setPlanDate(StringUtils.hasText(dto.getPlanDate()) ? LocalDate.parse(dto.getPlanDate()) : null);
        }
        if (dto.getActualDate() != null) {
            existing.setActualDate(StringUtils.hasText(dto.getActualDate()) ? LocalDate.parse(dto.getActualDate()) : null);
        }
        contractPaymentMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        contractPaymentMapper.deleteById(id);
    }

    /**
     * 记录实际收付款。若 actualAmount >= planAmount 则状态置为 RECEIVED。
     */
    public void recordActual(Long id, BigDecimal actualAmount, LocalDate actualDate, String invoiceNo) {
        ContractPayment existing = get(id);
        existing.setActualAmount(actualAmount);
        existing.setActualDate(actualDate);
        if (StringUtils.hasText(invoiceNo)) {
            existing.setInvoiceNo(invoiceNo);
        }
        if (actualAmount != null && existing.getPlanAmount() != null
                && actualAmount.compareTo(existing.getPlanAmount()) >= 0) {
            existing.setStatus("RECEIVED");
        }
        contractPaymentMapper.updateById(existing);
    }

    /**
     * 合同收付款看板:返回 receivable/payable 的 planned、received/paid、overdue 统计。
     */
    public Map<String, Object> dashboard(Long contractId) {
        if (contractId == null) {
            throw new BusinessException("合同ID不能为空");
        }
        List<ContractPayment> list = contractPaymentMapper.selectList(
                Wrappers.<ContractPayment>lambdaQuery().eq(ContractPayment::getContractId, contractId));

        BigDecimal receivablePlanned = BigDecimal.ZERO;
        BigDecimal receivableReceived = BigDecimal.ZERO;
        BigDecimal receivableOverdue = BigDecimal.ZERO;
        BigDecimal payablePlanned = BigDecimal.ZERO;
        BigDecimal payablePaid = BigDecimal.ZERO;
        BigDecimal payableOverdue = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (ContractPayment p : list) {
            BigDecimal plan = p.getPlanAmount() == null ? BigDecimal.ZERO : p.getPlanAmount();
            BigDecimal actual = p.getActualAmount() == null ? BigDecimal.ZERO : p.getActualAmount();
            boolean overdue = "PENDING".equals(p.getStatus())
                    && p.getPlanDate() != null && p.getPlanDate().isBefore(today);
            if ("RECEIVABLE".equals(p.getType())) {
                receivablePlanned = receivablePlanned.add(plan);
                receivableReceived = receivableReceived.add(actual);
                if (overdue) receivableOverdue = receivableOverdue.add(plan.subtract(actual));
            } else if ("PAYABLE".equals(p.getType())) {
                payablePlanned = payablePlanned.add(plan);
                payablePaid = payablePaid.add(actual);
                if (overdue) payableOverdue = payableOverdue.add(plan.subtract(actual));
            }
        }

        Map<String, Object> receivable = new HashMap<>();
        receivable.put("planned", receivablePlanned);
        receivable.put("received", receivableReceived);
        receivable.put("overdue", receivableOverdue);

        Map<String, Object> payable = new HashMap<>();
        payable.put("planned", payablePlanned);
        payable.put("paid", payablePaid);
        payable.put("overdue", payableOverdue);

        Map<String, Object> result = new HashMap<>();
        result.put("receivable", receivable);
        result.put("payable", payable);
        return result;
    }

    private void validateContractExist(Long contractId) {
        if (contractId == null) {
            throw new BusinessException("合同ID不能为空");
        }
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
    }
}
