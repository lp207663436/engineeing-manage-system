package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.Project;
import com.ems.module.business.entity.QuarterlySettlement;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.ProjectMapper;
import com.ems.module.business.mapper.QuarterlySettlementMapper;
import com.ems.module.system.service.SysNotificationService;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuarterlySettlementService {
    /**
     * 合法状态流转:DRAFT→REVIEWED→CONFIRMED→INVOICED→RECEIVED→CLOSED
     */
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "DRAFT", Set.of("REVIEWED"),
        "REVIEWED", Set.of("CONFIRMED", "DRAFT"),
        "CONFIRMED", Set.of("INVOICED", "REVIEWED"),
        "INVOICED", Set.of("RECEIVED", "CONFIRMED"),
        "RECEIVED", Set.of("CLOSED", "INVOICED"),
        "CLOSED", Set.of()
    );

    private final QuarterlySettlementMapper quarterlySettlementMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final ProjectMapper projectMapper;
    private final SysNotificationService notificationService;

    public PageResult<QuarterlySettlement> page(long pageNum, long pageSize, String code, Long contractId,
                                                 Long projectId, String status) {
        LambdaQueryWrapper<QuarterlySettlement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(QuarterlySettlement::getCode, code);
        if (contractId != null) wrapper.eq(QuarterlySettlement::getContractId, contractId);
        if (projectId != null) wrapper.eq(QuarterlySettlement::getProjectId, projectId);
        if (StringUtils.hasText(status)) wrapper.eq(QuarterlySettlement::getStatus, status);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByAsc(QuarterlySettlement::getPeriodNo);
        Page<QuarterlySettlement> page = quarterlySettlementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public QuarterlySettlement get(Long id) {
        QuarterlySettlement s = quarterlySettlementMapper.selectById(id);
        if (s == null) throw new BusinessException("季度结算单不存在");
        return s;
    }

    /**
     * 查合同下所有结算单,按 periodNo 升序
     */
    public java.util.List<QuarterlySettlement> listByContract(Long contractId) {
        LambdaQueryWrapper<QuarterlySettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuarterlySettlement::getContractId, contractId)
                .orderByAsc(QuarterlySettlement::getPeriodNo);
        return quarterlySettlementMapper.selectList(wrapper);
    }

    /**
     * 状态流转:草稿→审核→确认→开票→回款→关闭
     */
    public void updateStatus(Long id, String status, String remark) {
        QuarterlySettlement existing = get(id);
        Set<String> allowed = VALID_TRANSITIONS.get(existing.getStatus());
        if (allowed == null || !allowed.contains(status)) {
            throw new BusinessException("非法状态流转: " + existing.getStatus() + " → " + status);
        }
        existing.setStatus(status);
        if (StringUtils.hasText(remark)) existing.setRemark(remark);
        quarterlySettlementMapper.updateById(existing);
    }

    /**
     * 手动调整金额(提前终止场景),需填 remark
     */
    public void adjust(Long id, BigDecimal amount, String remark) {
        if (amount == null) throw new BusinessException("调整金额不能为空");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException("调整金额必须大于0");
        if (!StringUtils.hasText(remark)) throw new BusinessException("调整金额必须填写备注说明");
        QuarterlySettlement existing = get(id);
        existing.setAmount(amount);
        existing.setAmountVersion(existing.getAmountVersion() == null ? 1 : existing.getAmountVersion() + 1);
        existing.setRemark(remark);
        quarterlySettlementMapper.updateById(existing);
    }

    public void delete(Long id) {
        QuarterlySettlement s = get(id);
        if (!"DRAFT".equals(s.getStatus())) throw new BusinessException("仅草稿状态可删除");
        quarterlySettlementMapper.deleteById(id);
    }

    /**
     * 为合同生成所有期次的结算单草稿(幂等:已存在的期次跳过)。
     * 算法:
     * 1. 查 MaintenanceContract,取 effectiveDate、totalAmount、periodCount
     * 2. baseAmount = totalAmount / periodCount(保留2位)。前 N-1 期用 baseAmount,最后一期吸纳尾差
     * 3. 对每期计算起止日,查重后 insert 草稿单,code=合同code+"-Q"+i
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateForContract(Long contractId) {
        MaintenanceContract contract = maintenanceContractMapper.selectById(contractId);
        if (contract == null) throw new BusinessException("维保主合同不存在");
        LocalDate effectiveDate = contract.getEffectiveDate();
        BigDecimal totalAmount = contract.getTotalAmount();
        Integer periodCount = contract.getPeriodCount();
        if (effectiveDate == null || totalAmount == null || periodCount == null || periodCount <= 0) {
            throw new BusinessException("合同生效日/总额/期数不完整,无法生成结算单");
        }

        // 单期金额(保留2位小数)
        BigDecimal baseAmount = totalAmount.divide(BigDecimal.valueOf(periodCount), 2, RoundingMode.HALF_UP);
        // 前N-1期之和
        BigDecimal prevSum = baseAmount.multiply(BigDecimal.valueOf(periodCount - 1));
        // 最后一期吸纳尾差
        BigDecimal lastAmount = totalAmount.subtract(prevSum);

        Long currentUserId = SecurityContext.getUserId();

        for (int i = 1; i <= periodCount; i++) {
            // 幂等:同 contractId + periodNo 已存在则跳过
            LambdaQueryWrapper<QuarterlySettlement> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(QuarterlySettlement::getContractId, contractId)
                    .eq(QuarterlySettlement::getPeriodNo, i);
            Long existCount = quarterlySettlementMapper.selectCount(existWrapper);
            if (existCount != null && existCount > 0) {
                continue;
            }

            LocalDate periodStart = effectiveDate.plusMonths((long) (i - 1) * 3);
            LocalDate periodEnd = effectiveDate.plusMonths((long) i * 3).minusDays(1);
            BigDecimal amount = (i == periodCount) ? lastAmount : baseAmount;

            QuarterlySettlement s = new QuarterlySettlement();
            s.setCode(contract.getCode() + "-Q" + i);
            s.setContractId(contractId);
            s.setProjectId(contract.getProjectId());
            s.setPeriodNo(i);
            s.setPeriodStartDate(periodStart);
            s.setPeriodEndDate(periodEnd);
            s.setAmount(amount);
            s.setAmountVersion(1);
            s.setStatus("DRAFT");
            s.setCreateBy(currentUserId);
            quarterlySettlementMapper.insert(s);
        }

        // 通知项目经理(项目的 managerId)
        if (contract.getProjectId() != null) {
            Project project = projectMapper.selectById(contract.getProjectId());
            if (project != null && project.getManagerId() != null) {
                notificationService.send(project.getManagerId(),
                        "季度结算单已生成",
                        "合同 " + contract.getCode() + " 的季度结算单已生成,请查收",
                        "SETTLEMENT", "QUARTERLY_SETTLEMENT", null);
            }
        }
    }
}
