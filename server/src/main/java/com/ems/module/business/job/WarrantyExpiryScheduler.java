package com.ems.module.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.system.service.SysNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 维保合同到期扫描定时任务。
 * 每天 09:00 扫描 maintenance_contract 表中 status='ACTIVE' 且 end_date 在 30 天内(含已过期)的合同,
 * 对每个到期合同创建 SysNotification 通知(接收人为合同创建人,即商务/管理员)。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WarrantyExpiryScheduler {

    private final MaintenanceContractMapper maintenanceContractMapper;
    private final SysNotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        LocalDate today = LocalDate.now();
        LocalDate todayPlus30 = today.plusDays(30);
        // endDate <= today+30 (含已过期:endDate < today)
        List<MaintenanceContract> list = maintenanceContractMapper.selectList(
                new LambdaQueryWrapper<MaintenanceContract>()
                        .eq(MaintenanceContract::getStatus, "ACTIVE")
                        .le(MaintenanceContract::getEndDate, todayPlus30));
        log.info("[WarrantyExpiryScheduler] 到期/即将到期维保合同数: {}", list.size());
        for (MaintenanceContract c : list) {
            if (c.getCreateBy() == null) {
                continue;
            }
            String title = "维保合同即将到期: " + (c.getName() == null ? c.getCode() : c.getName());
            String content = "维保合同 " + c.getCode() + " ";
            if (c.getEndDate().isBefore(today)) {
                content += "已于 " + c.getEndDate() + " 过期,请及时处理";
            } else {
                content += "将于 " + c.getEndDate() + " 到期,请及时处理";
            }
            notificationService.send(c.getCreateBy(),
                    title,
                    content,
                    "WARRANTY", "MAINTENANCE_CONTRACT", c.getId());
        }
        log.info("[WarrantyExpiryScheduler] 维保到期通知发送完成");
    }
}
