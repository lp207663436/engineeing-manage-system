package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.module.business.dto.MaintenanceStatVO;
import com.ems.module.business.entity.Equipment;
import com.ems.module.business.entity.MaintenanceRecord;
import com.ems.module.business.entity.MaintenanceTask;
import com.ems.module.business.mapper.EquipmentMapper;
import com.ems.module.business.mapper.MaintenanceRecordMapper;
import com.ems.module.business.mapper.MaintenanceTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 维保统计服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaintenanceStatService {

    private final MaintenanceRecordMapper maintenanceRecordMapper;
    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final EquipmentMapper equipmentMapper;

    /**
     * 维保统计
     */
    public MaintenanceStatVO stat() {
        MaintenanceStatVO vo = new MaintenanceStatVO();
        vo.setFaultTypeStats(statFaultType());
        vo.setResponseTimeStat(statResponseTime());
        vo.setEquipmentRanks(statEquipmentFaultRank());
        vo.setWorkloadStats(statWorkload());
        vo.setEquipmentHealth(statEquipmentHealth());
        return vo;
    }

    /**
     * 故障类型分布:按 record_type 分组 count(maintenance_record)
     */
    private List<MaintenanceStatVO.FaultTypeStat> statFaultType() {
        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>().last("LIMIT 10000"));
        Map<String, Long> grouped = records.stream()
                .filter(r -> r.getRecordType() != null)
                .collect(Collectors.groupingBy(MaintenanceRecord::getRecordType, Collectors.counting()));
        List<MaintenanceStatVO.FaultTypeStat> result = new ArrayList<>();
        grouped.forEach((type, count) -> {
            MaintenanceStatVO.FaultTypeStat s = new MaintenanceStatVO.FaultTypeStat();
            s.setType(type);
            s.setCount(count);
            result.add(s);
        });
        return result;
    }

    /**
     * 平均响应时长:avg(完工日期 - 计划日期 的小时差) of maintenance_task where status=COMPLETED
     */
    private MaintenanceStatVO.ResponseTimeStat statResponseTime() {
        List<MaintenanceTask> tasks = maintenanceTaskMapper.selectList(
                new LambdaQueryWrapper<MaintenanceTask>()
                        .eq(MaintenanceTask::getStatus, "COMPLETED")
                        .last("LIMIT 10000"));
        MaintenanceStatVO.ResponseTimeStat stat = new MaintenanceStatVO.ResponseTimeStat();
        stat.setAvgResponseHours(0.0);
        stat.setTotalCount(0);
        if (tasks.isEmpty()) {
            return stat;
        }
        double totalHours = 0;
        long validCount = 0;
        for (MaintenanceTask t : tasks) {
            if (t.getPlanDate() == null || t.getCompleteDate() == null) {
                continue;
            }
            LocalDate plan = t.getPlanDate();
            LocalDate complete = t.getCompleteDate();
            long hours = Duration.between(plan.atStartOfDay(), complete.atStartOfDay()).toHours();
            totalHours += hours;
            validCount++;
        }
        stat.setTotalCount(validCount);
        stat.setAvgResponseHours(validCount == 0 ? 0.0 : totalHours / validCount);
        return stat;
    }

    /**
     * 设备故障频次排行:按 equipment_id 分组 count(maintenance_record where record_type='REPAIR'),取前10
     */
    private List<MaintenanceStatVO.EquipmentFaultRank> statEquipmentFaultRank() {
        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>()
                        .eq(MaintenanceRecord::getRecordType, "REPAIR")
                        .last("LIMIT 10000"));
        Map<Long, Long> grouped = records.stream()
                .filter(r -> r.getEquipmentId() != null)
                .collect(Collectors.groupingBy(MaintenanceRecord::getEquipmentId, Collectors.counting()));
        if (grouped.isEmpty()) {
            return new ArrayList<>();
        }
        // 取设备名称
        List<Equipment> equipments = equipmentMapper.selectBatchIds(grouped.keySet());
        Map<Long, String> nameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName, (a, b) -> a));

        List<MaintenanceStatVO.EquipmentFaultRank> result = new ArrayList<>();
        grouped.forEach((eqId, count) -> {
            MaintenanceStatVO.EquipmentFaultRank rank = new MaintenanceStatVO.EquipmentFaultRank();
            rank.setEquipmentId(eqId);
            rank.setEquipmentName(nameMap.get(eqId));
            rank.setFaultCount(count);
            result.add(rank);
        });
        // 按故障次数倒序,取前10
        result.sort(Comparator.comparingLong(MaintenanceStatVO.EquipmentFaultRank::getFaultCount).reversed());
        if (result.size() > 10) {
            return new ArrayList<>(result.subList(0, 10));
        }
        return result;
    }

    /**
     * 维保人员工作量:按 handler_id 分组 count(maintenance_task),count(status=COMPLETED)
     */
    private List<MaintenanceStatVO.WorkloadStat> statWorkload() {
        List<MaintenanceTask> tasks = maintenanceTaskMapper.selectList(
                new LambdaQueryWrapper<MaintenanceTask>().last("LIMIT 10000"));
        Map<Long, List<MaintenanceTask>> grouped = tasks.stream()
                .filter(t -> t.getHandlerId() != null)
                .collect(Collectors.groupingBy(MaintenanceTask::getHandlerId));
        List<MaintenanceStatVO.WorkloadStat> result = new ArrayList<>();
        grouped.forEach((handlerId, taskList) -> {
            MaintenanceStatVO.WorkloadStat s = new MaintenanceStatVO.WorkloadStat();
            s.setHandlerId(handlerId);
            s.setTaskCount(taskList.size());
            long completed = taskList.stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()))
                    .count();
            s.setCompletedCount(completed);
            result.add(s);
        });
        return result;
    }

    /**
     * 设备健康度评分:100 - faultCount*5(最低0),faultCount 从 maintenance_record count
     */
    private List<MaintenanceStatVO.EquipmentHealth> statEquipmentHealth() {
        List<Equipment> equipments = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>().last("LIMIT 10000"));
        if (equipments.isEmpty()) {
            return new ArrayList<>();
        }
        // 统计每个设备的故障记录数
        List<MaintenanceRecord> records = maintenanceRecordMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecord>().last("LIMIT 10000"));
        Map<Long, Long> faultCountMap = records.stream()
                .filter(r -> r.getEquipmentId() != null)
                .collect(Collectors.groupingBy(MaintenanceRecord::getEquipmentId, Collectors.counting()));
        List<MaintenanceStatVO.EquipmentHealth> result = new ArrayList<>();
        for (Equipment e : equipments) {
            MaintenanceStatVO.EquipmentHealth h = new MaintenanceStatVO.EquipmentHealth();
            h.setEquipmentId(e.getId());
            h.setEquipmentName(e.getName());
            long faultCount = faultCountMap.getOrDefault(e.getId(), 0L);
            double score = 100 - faultCount * 5;
            if (score < 0) {
                score = 0;
            }
            h.setHealthScore(score);
            result.add(h);
        }
        return result;
    }
}
