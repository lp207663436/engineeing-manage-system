package com.ems.module.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.service.MaintenanceTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 维保巡检工单自动生成定时任务。
 * 每天 02:00 扫描所有 ACTIVE 维保主合同,通过 MaintenanceTaskService.generateInspection 生成当月巡检工单(幂等)。
 * 使用 Redis 分布式锁防止并发执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceInspectScheduler {

    private final MaintenanceContractMapper maintenanceContractMapper;
    private final MaintenanceTaskService maintenanceTaskService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_KEY = "lock:maintenance:inspect:scheduler";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        // Redis 分布式锁,防止多实例并发执行
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (locked == null || !locked) {
            log.info("[MaintenanceInspectScheduler] 上次任务仍在执行,跳过本次调度");
            return;
        }
        try {
            LambdaQueryWrapper<MaintenanceContract> cWrapper = new LambdaQueryWrapper<>();
            cWrapper.eq(MaintenanceContract::getStatus, "ACTIVE");
            List<MaintenanceContract> contracts = maintenanceContractMapper.selectList(cWrapper);
            log.info("[MaintenanceInspectScheduler] 扫描生效维保合同数:{}", contracts.size());

            int generated = 0;
            for (MaintenanceContract contract : contracts) {
                try {
                    // 调用 MaintenanceTaskService.generateInspection 生成巡检工单
                    Integer count = maintenanceTaskService.generateInspection(contract.getId());
                    if (count != null) {
                        generated += count;
                    }
                } catch (Exception e) {
                    log.error("合同[{}]巡检工单生成失败", contract.getId(), e);
                }
            }
            log.info("[MaintenanceInspectScheduler] 巡检工单生成完成,本次生成 {} 单", generated);
        } finally {
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }
}
