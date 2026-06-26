package com.ems.module.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.entity.MaintenanceContract;
import com.ems.module.business.mapper.MaintenanceContractMapper;
import com.ems.module.business.service.QuarterlySettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 季度结算单自动生成定时任务。
 * 每日00:00执行,扫描所有生效中的维保主合同,幂等生成草稿结算单。
 * 定时任务无登录用户,createBy 由 SecurityContext 返回 null(实体 Long 可为空)。
 */
@Component
@RequiredArgsConstructor
public class QuarterlySettlementJob {

    private final QuarterlySettlementService quarterlySettlementService;
    private final MaintenanceContractMapper maintenanceContractMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    public void execute() {
        LambdaQueryWrapper<MaintenanceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenanceContract::getStatus, "ACTIVE");
        List<MaintenanceContract> contracts = maintenanceContractMapper.selectList(wrapper);
        System.out.println("[QuarterlySettlementJob] 扫描生效合同数:" + contracts.size());
        for (MaintenanceContract contract : contracts) {
            try {
                quarterlySettlementService.generateForContract(contract.getId());
            } catch (Exception e) {
                // 单个合同失败只日志不中断后续合同处理
                System.out.println("[QuarterlySettlementJob] 合同[" + contract.getId() + "]生成结算单失败:" + e.getMessage());
            }
        }
        System.out.println("[QuarterlySettlementJob] 季度结算单生成任务执行完成");
    }
}
