package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.module.system.entity.SysNotification;
import com.ems.module.system.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SysNotificationService {

    private final SysNotificationMapper sysNotificationMapper;

    /**
     * 发送通知(完整版)
     */
    public void send(Long userId, String title, String content, String type, String businessType, Long businessId) {
        if (userId == null) {
            return;
        }
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setBusinessType(businessType);
        n.setBusinessId(businessId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(n);
    }

    /**
     * 发送通知(简化版)
     */
    public void sendToUser(Long userId, String title, String content, String type) {
        send(userId, title, content, type, null, null);
    }

    /**
     * 分页查询通知,按 userId 过滤,按 createTime 倒序
     */
    public PageResult<SysNotification> page(long pageNum, long pageSize, Long userId, Integer isRead) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysNotification::getUserId, userId);
        }
        if (isRead != null) {
            wrapper.eq(SysNotification::getIsRead, isRead);
        }
        wrapper.orderByDesc(SysNotification::getCreateTime);
        Page<SysNotification> page = sysNotificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    /**
     * 查未读列表
     */
    public List<SysNotification> listUnread(Long userId) {
        return sysNotificationMapper.selectList(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0)
                        .orderByDesc(SysNotification::getCreateTime));
    }

    /**
     * 未读数
     */
    public int countUnread(Long userId) {
        Long count = sysNotificationMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 标记已读
     */
    public void markRead(Long id) {
        SysNotification n = sysNotificationMapper.selectById(id);
        if (n == null) {
            return;
        }
        n.setIsRead(1);
        sysNotificationMapper.updateById(n);
    }

    /**
     * 全部已读
     */
    public void markAllRead(Long userId) {
        SysNotification update = new SysNotification();
        update.setIsRead(1);
        sysNotificationMapper.update(update,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
    }

    /**
     * 删除
     */
    public void delete(Long id) {
        sysNotificationMapper.deleteById(id);
    }
}
