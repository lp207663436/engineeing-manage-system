package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.AcceptanceDTO;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.PointSettlement;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.module.business.mapper.PointSettlementMapper;
import com.ems.module.system.service.SysNotificationService;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AcceptanceService {

    private final AcceptanceMapper acceptanceMapper;
    private final SysNotificationService notificationService;
    private final PointSettlementMapper pointSettlementMapper;
    private final ContractMapper contractMapper;

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
        // 通知验收人
        if (dto.getAcceptorId() != null) {
            notificationService.send(dto.getAcceptorId(),
                    "待验收通知",
                    "您有新的验收任务待处理,验收单号: " + a.getCode(),
                    "ACCEPTANCE",
                    "ACCEPTANCE",
                    a.getId());
        }
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

        // 验收通过时自动创建点位结算单草稿
        if ("PASS".equals(result)) {
            createPointSettlementDraft(existing);
        }

        // 通知验收单创建人
        if (existing.getCreateBy() != null) {
            String resultText = "PASS".equals(result) ? "验收通过"
                    : "FAIL".equals(result) ? "验收不通过" : "待仲裁";
            notificationService.send(existing.getCreateBy(),
                    "验收结论通知",
                    "验收单 " + existing.getCode() + " 结论: " + resultText
                            + (remark != null ? ", 备注: " + remark : ""),
                    "ACCEPTANCE",
                    "ACCEPTANCE",
                    existing.getId());
        }
    }

    /**
     * 验收通过后自动创建点位结算单草稿(PENDING 状态)
     */
    private void createPointSettlementDraft(Acceptance acceptance) {
        PointSettlement ps = new PointSettlement();
        ps.setProjectId(acceptance.getProjectId());
        ps.setAcceptanceId(acceptance.getId());
        ps.setQuoteId(acceptance.getQuoteId());

        // 从验收关联的项目获取 contractId
        if (acceptance.getProjectId() != null) {
            Contract contract = contractMapper.selectOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getProjectId, acceptance.getProjectId())
                            .last("LIMIT 1"));
            if (contract != null) {
                ps.setContractId(contract.getId());
            }
        }

        // pointId: 维护型点位验收时 businessId 即为 pointId
        if ("MAINTENANCE_POINT".equals(acceptance.getBusinessType())) {
            ps.setPointId(acceptance.getBusinessId());
        }

        // periodNo: 当前季度(如 2026-Q2)
        LocalDate now = LocalDate.now();
        int quarter = (now.getMonthValue() - 1) / 3 + 1;
        ps.setPeriodNo(now.getYear() + "-Q" + quarter);

        // amount: 暂设为0,后续由报价或合同金额回填
        ps.setAmount(BigDecimal.ZERO);
        ps.setStatus("PENDING");
        ps.setCreateBy(SecurityContext.getUserId());
        pointSettlementMapper.insert(ps);
    }
}
