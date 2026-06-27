package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.PointSettlementDTO;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.entity.PointSettlement;
import com.ems.module.business.entity.Quote;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.module.business.mapper.PointSettlementMapper;
import com.ems.module.business.mapper.QuoteMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PointSettlementService {

    private final PointSettlementMapper pointSettlementMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final QuoteMapper quoteMapper;

    public PageResult<PointSettlement> page(long pageNum, long pageSize, String code, Long pointId,
                                            Long projectId, String status) {
        LambdaQueryWrapper<PointSettlement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(PointSettlement::getCode, code);
        if (pointId != null) wrapper.eq(PointSettlement::getPointId, pointId);
        if (projectId != null) wrapper.eq(PointSettlement::getProjectId, projectId);
        if (StringUtils.hasText(status)) wrapper.eq(PointSettlement::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(PointSettlement::getCreateTime);
        Page<PointSettlement> page = pointSettlementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public PointSettlement get(Long id) {
        PointSettlement s = pointSettlementMapper.selectById(id);
        if (s == null) throw new BusinessException("点位结算单不存在");
        return s;
    }

    public PointSettlement create(PointSettlementDTO dto) {
        // 强校验:关联验收单必须存在且 result=PASS(验收通过)
        if (dto.getAcceptanceId() == null) {
            throw new BusinessException("点位验收未通过,不可结算");
        }
        Acceptance acceptance = acceptanceMapper.selectById(dto.getAcceptanceId());
        if (acceptance == null || !"PASS".equals(acceptance.getResult())) {
            throw new BusinessException("点位验收未通过,不可结算");
        }
        PointSettlement s = new PointSettlement();
        BeanUtils.copyProperties(dto, s);
        // amount 从 quote 取(若 dto.amount 为空则查 Quote)
        if (s.getAmount() == null && dto.getQuoteId() != null) {
            Quote quote = quoteMapper.selectById(dto.getQuoteId());
            if (quote != null) s.setAmount(quote.getAmount());
        }
        if (StringUtils.hasText(dto.getReceivedDate())) s.setReceivedDate(LocalDate.parse(dto.getReceivedDate()));
        if (!StringUtils.hasText(s.getStatus())) s.setStatus("PENDING");
        s.setCreateBy(SecurityContext.getUserId());
        pointSettlementMapper.insert(s);
        return s;
    }

    public void update(PointSettlementDTO dto) {
        PointSettlement existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing, "status", "receivedAmount", "receivedDate", "invoiceNo", "id", "createTime", "createBy");
        if (StringUtils.hasText(dto.getReceivedDate())) existing.setReceivedDate(LocalDate.parse(dto.getReceivedDate()));
        pointSettlementMapper.updateById(existing);
    }

    public void delete(Long id) {
        PointSettlement existing = get(id);
        if ("RECEIVED".equals(existing.getStatus())) throw new BusinessException("已收款的结算单不可删除");
        pointSettlementMapper.deleteById(id);
    }

    /**
     * 登记实收:回款登记,状态→RECEIVED
     */
    public void receive(Long id, BigDecimal receivedAmount, String receivedDate, String invoiceNo) {
        PointSettlement existing = get(id);
        if ("RECEIVED".equals(existing.getStatus())) throw new BusinessException("已收款,不可重复登记");
        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("实收金额必须大于0");
        if (existing.getAmount() != null && receivedAmount.compareTo(existing.getAmount()) > 0)
            throw new BusinessException("实收金额不能超过应收金额");
        existing.setReceivedAmount(receivedAmount);
        existing.setReceivedDate(receivedDate != null ? LocalDate.parse(receivedDate) : null);
        existing.setInvoiceNo(invoiceNo);
        existing.setStatus("RECEIVED");
        pointSettlementMapper.updateById(existing);
    }
}
