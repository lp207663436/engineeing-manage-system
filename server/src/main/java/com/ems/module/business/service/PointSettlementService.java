package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Autowired
    private MaintenancePointService maintenancePointService;

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
        DataScopeHelper.checkOwnership(s.getCreateBy());
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
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        BeanUtils.copyProperties(dto, existing, "status", "receivedAmount", "receivedDate", "invoiceNo", "id", "createTime", "createBy");
        if (StringUtils.hasText(dto.getReceivedDate())) existing.setReceivedDate(LocalDate.parse(dto.getReceivedDate()));
        pointSettlementMapper.updateById(existing);
    }

    public void delete(Long id) {
        PointSettlement existing = get(id);
        if ("RECEIVED".equals(existing.getStatus())) throw new BusinessException("已收款的结算单不可删除");
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        pointSettlementMapper.deleteById(id);
    }

    /**
     * 登记实收:回款登记,应用层计算新值和状态,条件更新实现并发控制,状态→RECEIVED 或 PARTIAL
     */
    public void receive(Long id, BigDecimal receivedAmount, String receivedDate, String invoiceNo) {
        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("实收金额必须大于0");
        PointSettlement existing = get(id);
        if ("RECEIVED".equals(existing.getStatus())) throw new BusinessException("已收款,不可重复登记");

        // 应用层计算:先查询当前 received_amount,计算新值和状态,再用条件更新
        BigDecimal currentReceived = existing.getReceivedAmount() == null
                ? BigDecimal.ZERO : existing.getReceivedAmount();
        BigDecimal newReceived = currentReceived.add(receivedAmount);
        if (existing.getAmount() != null && newReceived.compareTo(existing.getAmount()) > 0)
            throw new BusinessException("累计实收金额不能超过应收金额");

        String newStatus = (existing.getAmount() != null
                && newReceived.compareTo(existing.getAmount()) >= 0)
                ? "RECEIVED" : "PARTIAL";

        int rows = pointSettlementMapper.update(null,
                new LambdaUpdateWrapper<PointSettlement>()
                        .eq(PointSettlement::getId, id)
                        .ne(PointSettlement::getStatus, "RECEIVED")
                        .set(PointSettlement::getReceivedAmount, newReceived)
                        .set(PointSettlement::getStatus, newStatus)
                        .set(PointSettlement::getReceivedDate,
                                receivedDate != null ? LocalDate.parse(receivedDate) : null)
                        .set(PointSettlement::getInvoiceNo, invoiceNo));
        if (rows == 0) throw new BusinessException("登记失败,记录可能已被其他操作更新,请刷新后重试");

        // 结算完成(已收款)时更新点位状态为 SETTLED
        if ("RECEIVED".equals(newStatus) && existing.getPointId() != null) {
            maintenancePointService.updateStatus(existing.getPointId(), "SETTLED");
        }
    }

    /**
     * 确认结算:状态 PENDING → CONFIRMED
     */
    public void confirm(Long id) {
        PointSettlement existing = get(id);
        if (!"PENDING".equals(existing.getStatus())) {
            throw new BusinessException("仅待结算状态可确认");
        }
        int rows = pointSettlementMapper.update(null,
                new LambdaUpdateWrapper<PointSettlement>()
                        .eq(PointSettlement::getId, id)
                        .eq(PointSettlement::getStatus, "PENDING")
                        .set(PointSettlement::getStatus, "CONFIRMED"));
        if (rows == 0) throw new BusinessException("确认失败,状态可能已变更");
    }

    /**
     * 开票:状态 CONFIRMED → INVOICED
     */
    public void invoice(Long id) {
        PointSettlement existing = get(id);
        if (!"CONFIRMED".equals(existing.getStatus())) {
            throw new BusinessException("仅已确认状态可开票");
        }
        int rows = pointSettlementMapper.update(null,
                new LambdaUpdateWrapper<PointSettlement>()
                        .eq(PointSettlement::getId, id)
                        .eq(PointSettlement::getStatus, "CONFIRMED")
                        .set(PointSettlement::getStatus, "INVOICED"));
        if (rows == 0) throw new BusinessException("开票失败,状态可能已变更");
    }
}
