package com.ems.module.system.controller;

import com.ems.common.PageResult;
import com.ems.common.Result;
import com.ems.module.system.entity.SysNotification;
import com.ems.module.system.service.SysNotificationService;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/notification")
@RequiredArgsConstructor
public class SysNotificationController {

    private final SysNotificationService notificationService;

    @GetMapping("/page")
    @RequirePermission("system:notification:list")
    public Result<PageResult<SysNotification>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityContext.getUserId();
        return Result.success(notificationService.page(pageNum, pageSize, userId, isRead));
    }

    @GetMapping("/unread")
    @RequirePermission("system:notification:list")
    public Result<List<SysNotification>> unread() {
        Long userId = SecurityContext.getUserId();
        return Result.success(notificationService.listUnread(userId));
    }

    @GetMapping("/unread/count")
    @RequirePermission("system:notification:list")
    public Result<Integer> unreadCount() {
        Long userId = SecurityContext.getUserId();
        return Result.success(notificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    @RequirePermission("system:notification:list")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    @RequirePermission("system:notification:list")
    public Result<Void> markAllRead() {
        Long userId = SecurityContext.getUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:notification:list")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.success();
    }
}
