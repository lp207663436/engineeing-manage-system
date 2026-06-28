package com.ems.module.system.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.entity.ApprovalLog;
import com.ems.module.business.entity.ApprovalNode;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.entity.ContractPayment;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.module.business.mapper.ApprovalLogMapper;
import com.ems.module.business.mapper.ApprovalNodeMapper;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.module.business.mapper.ContractPaymentMapper;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.system.entity.SysNotification;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysNotificationMapper;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.module.system.service.SysNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知定时扫描任务。
 * 每天 09:00 扫描:
 * 1. 验收催办:PENDING 且 create_time < now()-7天 → 通知 acceptor_id
 * 2. 维保合同到期:ACTIVE 且 end_date 在今天~今天+30天 → 通知 createBy
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final SysNotificationService notificationService;
    private final SysNotificationMapper notificationMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final MaintenanceContractMapper maintenanceContractMapper;
    private final SysUserMapper sysUserMapper;
    private final ContractPaymentMapper contractPaymentMapper;
    private final ContractMapper contractMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        scanOverdueAcceptance();
        scanExpiringContracts();
        scanOverduePayments();
        scanPendingApprovals();
    }

    /**
     * 验收催办:超期未处理(PENDING 且创建超过 7 天)
     * 去重:3 天内已发过相同通知则跳过
     */
    private void scanOverdueAcceptance() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Acceptance> list = acceptanceMapper.selectList(
                new LambdaQueryWrapper<Acceptance>()
                        .eq(Acceptance::getResult, "PENDING")
                        .lt(Acceptance::getCreateTime, sevenDaysAgo));
        log.info("[NotificationScheduler] 超期验收单数: {}", list.size());
        for (Acceptance a : list) {
            try {
                if (a.getAcceptorId() == null) {
                    continue;
                }
                // 催办去重:3 天内已发过相同通知则跳过
                Long existCount = notificationMapper.selectCount(
                        new LambdaQueryWrapper<SysNotification>()
                                .eq(SysNotification::getUserId, a.getAcceptorId())
                                .eq(SysNotification::getBusinessType, "ACCEPTANCE")
                                .eq(SysNotification::getBusinessId, a.getId())
                                .eq(SysNotification::getType, "ACCEPTANCE")
                                .ge(SysNotification::getCreateTime, LocalDateTime.now().minusDays(3)));
                if (existCount != null && existCount > 0) {
                    continue;
                }
                notificationService.send(a.getAcceptorId(),
                        "验收催办",
                        "验收单 " + a.getCode() + " 超期未处理,请尽快处理",
                        "ACCEPTANCE", "ACCEPTANCE", a.getId());
            } catch (Exception e) {
                log.error("[NotificationScheduler] 验收催办通知发送失败, acceptanceId={}", a.getId(), e);
            }
        }
    }

    /**
     * 维保合同到期提醒:30天内即将到期
     * 去重:1 天内已发过相同通知则跳过
     */
    private void scanExpiringContracts() {
        LocalDate today = LocalDate.now();
        LocalDate todayPlus30 = today.plusDays(30);
        List<MaintenanceContract> list = maintenanceContractMapper.selectList(
                new LambdaQueryWrapper<MaintenanceContract>()
                        .eq(MaintenanceContract::getStatus, "ACTIVE")
                        .le(MaintenanceContract::getEndDate, todayPlus30)
                        .ge(MaintenanceContract::getEndDate, today));
        log.info("[NotificationScheduler] 即将到期维保合同数: {}", list.size());
        for (MaintenanceContract c : list) {
            try {
                if (c.getCreateBy() == null) {
                    continue;
                }
                // 去重:1 天内已发过相同通知则跳过
                Long existCount = notificationMapper.selectCount(
                        new LambdaQueryWrapper<SysNotification>()
                                .eq(SysNotification::getUserId, c.getCreateBy())
                                .eq(SysNotification::getBusinessType, "MAINTENANCE_CONTRACT")
                                .eq(SysNotification::getBusinessId, c.getId())
                                .eq(SysNotification::getType, "WARRANTY")
                                .ge(SysNotification::getCreateTime, LocalDateTime.now().minusDays(1)));
                if (existCount != null && existCount > 0) {
                    continue;
                }
                notificationService.send(c.getCreateBy(),
                        "维保合同即将到期",
                        "维保合同 " + c.getCode() + " 即将到期,请及时处理",
                        "WARRANTY", "MAINTENANCE_CONTRACT", c.getId());
            } catch (Exception e) {
                log.error("[NotificationScheduler] 合同到期通知发送失败, contractId={}", c.getId(), e);
            }
        }
    }

    /**
     * 扫描逾期收付款:contract_payment 中 status=PENDING 且 plan_date < today,发送 OVERDUE 通知给合同创建人
     */
    private void scanOverduePayments() {
        LocalDate today = LocalDate.now();
        List<ContractPayment> list = contractPaymentMapper.selectList(
                new LambdaQueryWrapper<ContractPayment>()
                        .eq(ContractPayment::getStatus, "PENDING")
                        .lt(ContractPayment::getPlanDate, today));
        log.info("[NotificationScheduler] 逾期收付款数: {}", list.size());
        for (ContractPayment p : list) {
            try {
                if (p.getContractId() == null) {
                    continue;
                }
                Contract contract = contractMapper.selectById(p.getContractId());
                if (contract == null || contract.getCreateBy() == null) {
                    continue;
                }
                // 去重:同收付款记录 3 天内不重复发送
                Long existCount = notificationMapper.selectCount(
                        new LambdaQueryWrapper<SysNotification>()
                                .eq(SysNotification::getUserId, contract.getCreateBy())
                                .eq(SysNotification::getBusinessType, "CONTRACT_PAYMENT")
                                .eq(SysNotification::getBusinessId, p.getId())
                                .eq(SysNotification::getType, "OVERDUE")
                                .ge(SysNotification::getCreateTime, LocalDateTime.now().minusDays(3)));
                if (existCount != null && existCount > 0) {
                    continue;
                }
                String direction = "RECEIVABLE".equals(p.getType()) ? "应收" : "应付";
                notificationService.send(contract.getCreateBy(),
                        direction + "逾期提醒",
                        direction + "单 " + p.getCode() + " 已逾期,计划日期 " + p.getPlanDate() + ",请尽快处理",
                        "OVERDUE", "CONTRACT_PAYMENT", p.getId());
            } catch (Exception e) {
                log.error("[NotificationScheduler] 逾期收付款通知发送失败, paymentId={}", p.getId(), e);
            }
        }
    }

    /**
     * 扫描待审批超时:approval_log 中 result IS NULL 且 create_time 超过 24 小时,发送 APPROVAL 通知给对应角色的审批人
     */
    private void scanPendingApprovals() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<ApprovalLog> list = approvalLogMapper.selectList(
                new LambdaQueryWrapper<ApprovalLog>()
                        .isNull(ApprovalLog::getResult)
                        .lt(ApprovalLog::getCreateTime, twentyFourHoursAgo));
        log.info("[NotificationScheduler] 超时待审批数: {}", list.size());
        for (ApprovalLog logEntry : list) {
            try {
                // 查找当前审批节点,获取审批角色
                ApprovalNode node = approvalNodeMapper.selectOne(
                        new LambdaQueryWrapper<ApprovalNode>()
                                .eq(ApprovalNode::getFlowId, logEntry.getFlowId())
                                .eq(ApprovalNode::getNodeOrder, logEntry.getNodeOrder())
                                .last("LIMIT 1"));
                if (node == null || node.getApproverRoleId() == null) {
                    continue;
                }
                // 查找该角色下的所有用户
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getRoleId, node.getApproverRoleId()));
                for (SysUserRole ur : userRoles) {
                    // 去重:同审批日志同用户 24 小时内不重复发送
                    Long existCount = notificationMapper.selectCount(
                            new LambdaQueryWrapper<SysNotification>()
                                    .eq(SysNotification::getUserId, ur.getUserId())
                                    .eq(SysNotification::getBusinessType, "APPROVAL")
                                    .eq(SysNotification::getBusinessId, logEntry.getId())
                                    .eq(SysNotification::getType, "APPROVAL")
                                    .ge(SysNotification::getCreateTime, LocalDateTime.now().minusHours(24)));
                    if (existCount != null && existCount > 0) {
                        continue;
                    }
                    notificationService.send(ur.getUserId(),
                            "审批超时提醒",
                            "您有一条待审批记录已超过 24 小时未处理,请尽快审批",
                            "APPROVAL", "APPROVAL", logEntry.getId());
                }
            } catch (Exception e) {
                log.error("[NotificationScheduler] 审批超时通知发送失败, logId={}", logEntry.getId(), e);
            }
        }
    }
}
