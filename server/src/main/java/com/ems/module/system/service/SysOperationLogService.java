package com.ems.module.system.service;

import com.ems.module.system.entity.SysOperationLog;
import com.ems.module.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务(仅 insert)。
 * 通过 AOP 切面 {@link com.ems.common.annotation.OperationLogAspect} 异步写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    /**
     * 异步插入操作日志,避免影响主流程性能。
     */
    @Async
    public void insert(SysOperationLog log) {
        try {
            sysOperationLogMapper.insert(log);
        } catch (Exception e) {
            SysOperationLogService.log.warn("操作日志写入失败: {}", e.getMessage());
        }
    }
}
