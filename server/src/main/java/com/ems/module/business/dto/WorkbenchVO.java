package com.ems.module.business.dto;

import lombok.Data;

@Data
public class WorkbenchVO {

    /**
     * 待审批数(当前用户)
     */
    private long pendingApprovals;

    /**
     * 待验收数
     */
    private long pendingAcceptances;

    /**
     * 超期验收数
     */
    private long overdueAcceptances;

    /**
     * 超期维保任务数(planDate<今天且未完成)
     */
    private long overdueTasks;

    /**
     * 30天内到期维保合同数
     */
    private long expiringContracts;

    /**
     * 待回款结算单数(quarterly_settlement status in DRAFT/REVIEWED/CONFIRMED/INVOICED)
     */
    private long pendingSettlements;
}
