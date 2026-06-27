package com.ems.module.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.entity.MaintenancePoint;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.mapper.MaintenancePointMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 维保巡检工单自动生成定时任务。
 * 每天 02:00 扫描所有 ACTIVE 维保主合同,为关联合同下的点位生成当月巡检工单(幂等)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceInspectScheduler {

    private final MaintenanceContractMapper maintenanceContractMapper;
    private final MaintenancePointMapper maintenancePointMapper;
    private final MaintenanceTaskMapper maintenanceTaskMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        LambdaQueryWrapper<MaintenanceContract> cWrapper = new LambdaQueryWrapper<>();
        cWrapper.eq(MaintenanceContract::getStatus, "ACTIVE");
        List<MaintenanceContract> contracts = maintenanceContractMapper.selectList(cWrapper);
        log.info("[MaintenanceInspectScheduler] 扫描生效维保合同数:{}", contracts.size());

        LocalDate today = LocalDate.now();
        String planCode = "INS-" + today.getYear() + String.format("%02d", today.getMonthValue());
        int generated = 0;
        for (MaintenanceContract contract : contracts) {
            try {
                if (contract.getProjectId() == null) continue;
                // 查项目下所有点位
                LambdaQueryWrapper<MaintenancePoint> pWrapper = new LambdaQueryWrapper<>();
                pWrapper.eq(MaintenancePoint::getProjectId, contract.getProjectId());
                List<MaintenancePoint> points = maintenancePointMapper.selectList(pWrapper);
                for (MaintenancePoint point : points) {
                    try {
                        // 幂等:同月同点位已生成则跳过
                        LambdaQueryWrapper<MaintenanceTask> tWrapper = new LambdaQueryWrapper<>();
                        tWrapper.eq(MaintenanceTask::getProjectId, contract.getProjectId())
                                .eq(MaintenanceTask::getPointId, point.getId())
                                .eq(MaintenanceTask::getType, "INSPECTION")
                                .likeRight(MaintenanceTask::getCode, planCode);
                        Long exists = maintenanceTaskMapper.selectCount(tWrapper);
                        if (exists > 0) continue;

                        MaintenanceTask task = new MaintenanceTask();
                        task.setCode(planCode + "-" + point.getId());
                        task.setProjectId(contract.getProjectId());
                        task.setPointId(point.getId());
                        task.setType("INSPECTION");
                        task.setTitle(point.getName() + " 月度巡检");
                        task.setDescription("自动生成的月度巡检工单");
                        task.setStatus("PENDING");
                        task.setPlanDate(today);
                        task.setPlanInspectDate(today.plusDays(7));
                        task.setCreateBy(contract.getCreateBy() != null ? contract.getCreateBy() : 0L);
                        maintenanceTaskMapper.insert(task);
                        generated++;
                    } catch (Exception e) {
                        log.error("点位[{}]生成巡检工单失败", point.getId(), e);
                    }
                }
            } catch (Exception e) {
                log.error("合同[{}]巡检工单生成失败", contract.getId(), e);
            }
        }
        log.info("[MaintenanceInspectScheduler] 巡检工单生成完成,本次生成 {} 单", generated);
    }
}
