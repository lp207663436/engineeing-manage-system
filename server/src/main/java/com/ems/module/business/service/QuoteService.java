package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.QuoteCompareVO;
import com.ems.module.business.dto.QuoteDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.Quote;
import com.ems.module.business.mapper.ContractMapper;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteMapper quoteMapper;
    private final ContractMapper contractMapper;

    @Lazy
    @Autowired
    private MaintenancePointService maintenancePointService;

    public PageResult<Quote> page(long pageNum, long pageSize, String code, String customerName, String status) {
        LambdaQueryWrapper<Quote> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(Quote::getCode, code);
        if (StringUtils.hasText(customerName)) wrapper.like(Quote::getCustomerName, customerName);
        if (StringUtils.hasText(status)) wrapper.eq(Quote::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Quote::getCreateTime);
        Page<Quote> page = quoteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Quote get(Long id) {
        Quote q = quoteMapper.selectById(id);
        if (q == null) throw new BusinessException("报价不存在");
        DataScopeHelper.checkOwnership(q.getCreateBy());
        return q;
    }

    public Quote create(QuoteDTO dto) {
        Quote q = new Quote();
        BeanUtils.copyProperties(dto, q);
        if (StringUtils.hasText(dto.getQuoteDate())) q.setQuoteDate(LocalDate.parse(dto.getQuoteDate()));
        if (StringUtils.hasText(dto.getValidUntil())) q.setValidUntil(LocalDate.parse(dto.getValidUntil()));
        if (!StringUtils.hasText(q.getBusinessType())) q.setBusinessType("NEW_BUILD");
        if (q.getBusinessId() == null) q.setBusinessId(dto.getProjectId());
        if (!StringUtils.hasText(q.getStatus())) q.setStatus("DRAFT");
        if (q.getVersion() == null) q.setVersion(1);
        q.setCreateBy(SecurityContext.getUserId());
        quoteMapper.insert(q);
        return q;
    }

    public void update(QuoteDTO dto) {
        Quote existing = get(dto.getId());
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getQuoteDate())) existing.setQuoteDate(LocalDate.parse(dto.getQuoteDate()));
        if (StringUtils.hasText(dto.getValidUntil())) existing.setValidUntil(LocalDate.parse(dto.getValidUntil()));
        quoteMapper.updateById(existing);
    }

    public void delete(Long id) {
        Quote existing = get(id);
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        quoteMapper.deleteById(id);
    }

    /**
     * 确认报价(status 改为 CONFIRMED)。
     * 同一时间只允许一个生效报价:将同 businessId 的其他 CONFIRMED 报价置为 VOID。
     * 维保点位报价确认时,更新点位状态为 QUOTED。
     */
    public void confirm(Long id) {
        Quote existing = get(id);
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        // 将同 businessId 的其他 CONFIRMED 报价置为 VOID
        if (existing.getBusinessId() != null) {
            quoteMapper.update(null,
                    new LambdaUpdateWrapper<Quote>()
                            .eq(Quote::getBusinessId, existing.getBusinessId())
                            .eq(Quote::getStatus, "CONFIRMED")
                            .ne(Quote::getId, id)
                            .set(Quote::getStatus, "VOID"));
        }
        existing.setStatus("CONFIRMED");
        quoteMapper.updateById(existing);
        // 维保点位报价确认时,更新点位状态为 QUOTED
        if ("MAINTENANCE_POINT".equals(existing.getBusinessType()) && existing.getBusinessId() != null) {
            maintenancePointService.updateStatus(existing.getBusinessId(), "QUOTED");
        }
    }

    /**
     * 报价转合同:根据报价信息创建合同草稿
     */
    public Contract convertToContract(Long quoteId) {
        Quote quote = get(quoteId);
        DataScopeHelper.checkOwnership(quote.getCreateBy());
        Contract contract = new Contract();
        contract.setCode("CT-" + quote.getCode());
        contract.setName(quote.getCustomerName() != null ? quote.getCustomerName() + " 合同" : "合同草稿");
        contract.setProjectId(quote.getProjectId());
        contract.setAmount(quote.getAmount());
        contract.setStatus("DRAFT");
        contract.setApprovalStatus("NONE");
        contract.setRemark("由报价 " + quote.getCode() + " 转换");
        contract.setCreateBy(SecurityContext.getUserId());
        contractMapper.insert(contract);
        return contract;
    }

    /**
     * 列出某项目下所有报价版本(按 version 倒序)
     */
    public List<Quote> listVersions(Long projectId) {
        if (projectId == null) throw new BusinessException("项目ID不能为空");
        return quoteMapper.selectList(
                new LambdaQueryWrapper<Quote>()
                        .eq(Quote::getProjectId, projectId)
                        .orderByDesc(Quote::getVersion));
    }

    /**
     * 对比两个报价版本
     */
    public QuoteCompareVO compareVersions(Long projectId, Long v1Id, Long v2Id) {
        if (projectId == null) throw new BusinessException("项目ID不能为空");
        if (v1Id == null || v2Id == null) throw new BusinessException("对比版本ID不能为空");
        Quote q1 = get(v1Id);
        Quote q2 = get(v2Id);
        if (!projectId.equals(q1.getProjectId()) || !projectId.equals(q2.getProjectId())) {
            throw new BusinessException("报价版本不属于同一项目");
        }
        QuoteCompareVO vo = new QuoteCompareVO();
        vo.setV1(toVO(q1));
        vo.setV2(toVO(q2));

        // 金额差 v2 - v1
        BigDecimal a1 = q1.getAmount() == null ? BigDecimal.ZERO : q1.getAmount();
        BigDecimal a2 = q2.getAmount() == null ? BigDecimal.ZERO : q2.getAmount();
        vo.setAmountDiff(a2.subtract(a1));

        // 有效期差(天) v2 - v1
        if (q1.getValidUntil() != null && q2.getValidUntil() != null) {
            vo.setValidUntilDiffDays(ChronoUnit.DAYS.between(q1.getValidUntil(), q2.getValidUntil()));
        }

        // 明细差异描述
        StringBuilder diff = new StringBuilder();
        if (!a1.equals(a2)) {
            diff.append("金额由 ").append(a1).append(" 变更为 ").append(a2).append(";");
        }
        if (q1.getValidUntil() != null && q2.getValidUntil() != null && !q1.getValidUntil().equals(q2.getValidUntil())) {
            diff.append("有效期由 ").append(q1.getValidUntil()).append(" 变更为 ").append(q2.getValidUntil()).append(";");
        }
        String s1 = q1.getSummary() == null ? "" : q1.getSummary();
        String s2 = q2.getSummary() == null ? "" : q2.getSummary();
        if (!s1.equals(s2)) {
            diff.append("报价摘要发生变化;");
        }
        if (q1.getVersion() != null && q2.getVersion() != null && !q1.getVersion().equals(q2.getVersion())) {
            diff.append("版本由 v").append(q1.getVersion()).append(" 变更为 v").append(q2.getVersion()).append(";");
        }
        vo.setDetailDiff(diff.length() == 0 ? "无差异" : diff.toString());
        return vo;
    }

    private QuoteCompareVO.QuoteDTO toVO(Quote q) {
        QuoteCompareVO.QuoteDTO d = new QuoteCompareVO.QuoteDTO();
        BeanUtils.copyProperties(q, d);
        return d;
    }
}
