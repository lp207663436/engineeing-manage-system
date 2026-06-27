package com.ems.module.business.dto;

import lombok.Data;
import java.util.List;

@Data
public class MaintenanceStatVO {
    private List<FaultTypeStat> faultTypeStats;      // 故障类型分布
    private ResponseTimeStat responseTimeStat;       // 平均响应时长
    private List<EquipmentFaultRank> equipmentRanks; // 设备故障频次排行
    private List<WorkloadStat> workloadStats;        // 维保人员工作量
    private List<EquipmentHealth> equipmentHealth;   // 设备健康度评分

    @Data
    public static class FaultTypeStat {
        private String type;     // INSPECTION/REPAIR/MAINTENANCE
        private long count;
    }
    @Data
    public static class ResponseTimeStat {
        private double avgResponseHours;   // 平均响应时长(小时)
        private long totalCount;
    }
    @Data
    public static class EquipmentFaultRank {
        private Long equipmentId;
        private String equipmentName;
        private long faultCount;
    }
    @Data
    public static class WorkloadStat {
        private Long handlerId;
        private long taskCount;
        private long completedCount;
    }
    @Data
    public static class EquipmentHealth {
        private Long equipmentId;
        private String equipmentName;
        private double healthScore;  // 0-100,越高越健康
    }
}
