package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * 故障类型分布:SQL GROUP BY record_type 聚合 count
     */
    private List<MaintenanceStatVO.FaultTypeStat> statFaultType() {
        QueryWrapper<MaintenanceRecord> wrapper = new QueryWrapper<>();
        wrapper.select("record_type", "count(*) as cnt")
               .isNotNull("record_type")
               .groupBy("record_type");
        List<Map<String, Object>> maps = maintenanceRecordMapper.selectMaps(wrapper);
        List<MaintenanceStatVO.FaultTypeStat> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            MaintenanceStatVO.FaultTypeStat s = new MaintenanceStatVO.FaultTypeStat();
            s.setType((String) map.get("record_type"));
            Number cnt = (Number) map.get("cnt");
            s.setCount(cnt == null ? 0L : cnt.longValue());
            result.add(s);
        }
        return result;
    }

    /**
     * 平均响应时长:SQL 聚合 avg(timestampdiff(HOUR, plan_date, complete_date)) of maintenance_task where status=COMPLETED
     */
    private MaintenanceStatVO.ResponseTimeStat statResponseTime() {
        QueryWrapper<MaintenanceTask> wrapper = new QueryWrapper<>();
        wrapper.select("count(*) as total_cnt",
                       "avg(timestampdiff(HOUR, plan_date, complete_date)) as avg_hours")
               .eq("status", "COMPLETED")
               .isNotNull("plan_date")
               .isNotNull("complete_date");
        List<Map<String, Object>> maps = maintenanceTaskMapper.selectMaps(wrapper);
        MaintenanceStatVO.ResponseTimeStat stat = new MaintenanceStatVO.ResponseTimeStat();
        if (maps.isEmpty() || maps.get(0).get("total_cnt") == null) {
            stat.setAvgResponseHours(0.0);
            stat.setTotalCount(0);
            return stat;
        }
        Map<String, Object> map = maps.get(0);
        Number total = (Number) map.get("total_cnt");
        stat.setTotalCount(total == null ? 0 : total.longValue());
        Number avgHours = (Number) map.get("avg_hours");
        stat.setAvgResponseHours(avgHours == null ? 0.0 : avgHours.doubleValue());
        return stat;
    }

    /**
     * 设备故障频次排行:SQL GROUP BY equipment_id 聚合 count where record_type='REPAIR',取前10
     */
    private List<MaintenanceStatVO.EquipmentFaultRank> statEquipmentFaultRank() {
        QueryWrapper<MaintenanceRecord> wrapper = new QueryWrapper<>();
        wrapper.select("equipment_id", "count(*) as cnt")
               .eq("record_type", "REPAIR")
               .isNotNull("equipment_id")
               .groupBy("equipment_id")
               .orderByDesc("cnt")
               .last("LIMIT 10");
        List<Map<String, Object>> maps = maintenanceRecordMapper.selectMaps(wrapper);
        if (maps.isEmpty()) {
            return new ArrayList<>();
        }
        // 取设备名称
        Set<Long> eqIds = new HashSet<>();
        for (Map<String, Object> map : maps) {
            Number eqId = (Number) map.get("equipment_id");
            if (eqId != null) {
                eqIds.add(eqId.longValue());
            }
        }
        Map<Long, String> nameMap = new HashMap<>();
        if (!eqIds.isEmpty()) {
            List<Equipment> equipments = equipmentMapper.selectBatchIds(eqIds);
            nameMap = equipments.stream()
                    .collect(Collectors.toMap(Equipment::getId, Equipment::getName, (a, b) -> a));
        }

        List<MaintenanceStatVO.EquipmentFaultRank> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            MaintenanceStatVO.EquipmentFaultRank rank = new MaintenanceStatVO.EquipmentFaultRank();
            Number eqId = (Number) map.get("equipment_id");
            Long equipmentId = eqId == null ? null : eqId.longValue();
            rank.setEquipmentId(equipmentId);
            rank.setEquipmentName(nameMap.get(equipmentId));
            Number cnt = (Number) map.get("cnt");
            rank.setFaultCount(cnt == null ? 0L : cnt.longValue());
            result.add(rank);
        }
        return result;
    }

    /**
     * 维保人员工作量:SQL GROUP BY handler_id 聚合 count(*) + sum(case when status='COMPLETED' then 1 else 0 end)
     */
    private List<MaintenanceStatVO.WorkloadStat> statWorkload() {
        QueryWrapper<MaintenanceTask> wrapper = new QueryWrapper<>();
        wrapper.select("handler_id",
                       "count(*) as total_cnt",
                       "sum(case when status='COMPLETED' then 1 else 0 end) as completed_cnt")
               .isNotNull("handler_id")
               .groupBy("handler_id");
        List<Map<String, Object>> maps = maintenanceTaskMapper.selectMaps(wrapper);
        List<MaintenanceStatVO.WorkloadStat> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            MaintenanceStatVO.WorkloadStat s = new MaintenanceStatVO.WorkloadStat();
            Number handlerId = (Number) map.get("handler_id");
            s.setHandlerId(handlerId == null ? null : handlerId.longValue());
            Number totalCnt = (Number) map.get("total_cnt");
            s.setTaskCount(totalCnt == null ? 0 : totalCnt.longValue());
            Number completedCnt = (Number) map.get("completed_cnt");
            s.setCompletedCount(completedCnt == null ? 0L : completedCnt.longValue());
            result.add(s);
        }
        return result;
    }

    /**
     * 设备健康度评分:100 - faultCount*5(最低0),faultCount 从 SQL GROUP BY 聚合
     */
    private List<MaintenanceStatVO.EquipmentHealth> statEquipmentHealth() {
        List<Equipment> equipments = equipmentMapper.selectList(new LambdaQueryWrapper<>());
        if (equipments.isEmpty()) {
            return new ArrayList<>();
        }
        // SQL GROUP BY 统计每个设备的故障记录数
        QueryWrapper<MaintenanceRecord> wrapper = new QueryWrapper<>();
        wrapper.select("equipment_id", "count(*) as cnt")
               .isNotNull("equipment_id")
               .groupBy("equipment_id");
        List<Map<String, Object>> maps = maintenanceRecordMapper.selectMaps(wrapper);
        Map<Long, Long> faultCountMap = new HashMap<>();
        for (Map<String, Object> map : maps) {
            Number eqId = (Number) map.get("equipment_id");
            Number cnt = (Number) map.get("cnt");
            if (eqId != null) {
                faultCountMap.put(eqId.longValue(), cnt == null ? 0L : cnt.longValue());
            }
        }

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
