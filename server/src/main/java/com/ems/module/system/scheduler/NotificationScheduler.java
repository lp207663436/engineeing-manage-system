package com.ems.module.system.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.Acceptance;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.AcceptanceMapper;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.system.entity.SysNotification;
import com.ems.module.system.mapper.SysNotificationMapper;
import com.ems.module.system.mapper.SysUserMapper;
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

    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        scanOverdueAcceptance();
        scanExpiringContracts();
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
        }
    }
}
