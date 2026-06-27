package com.ems.common.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;

/**
 * 数据权限工具类。
 * 业务 Service 的 page 方法在 wrapper 构造末尾调用 {@link #applyTo} 即可按当前用户 dataScope 过滤。
 *
 * 过滤规则(与 sys_role.data_scope 注释一致):
 * - 无登录上下文(定时任务等):不过滤
 * - 超管(userId=1):不过滤
 * - dataScope=1(全部):不过滤
 * - dataScope=2(本部门):create_by IN (本部门用户ID)
 * - dataScope=3(本部门及以下):create_by IN (本部门+直接子部门用户ID)
 * - dataScope=4(本人):create_by = currentUserId
 */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    public static <T> void applyTo(LambdaQueryWrapper<T> wrapper, String createByField) {
        CurrentUser user = SecurityContext.get();
        if (user == null) return;
        if (user.getUserId() == null) return;
        if (user.getUserId() == 1L) return; // 超管看全部

        Integer scope = user.getDataScope();
        if (scope == null || scope == 1) return; // 全部数据

        Long userId = user.getUserId();
        Long deptId = user.getDeptId();

        switch (scope) {
            case 4: // 本人
                wrapper.apply(createByField + " = {0}", userId);
                break;
            case 2: // 本部门
                if (deptId == null) {
                    wrapper.apply(createByField + " = {0}", userId);
                } else {
                    wrapper.apply(createByField + " IN (SELECT id FROM sys_user WHERE deleted = 0 AND dept_id = {0})", deptId);
                }
                break;
            case 3: // 本部门及以下
                if (deptId == null) {
                    wrapper.apply(createByField + " = {0}", userId);
                } else {
                    // 包含本部门 + 直接子部门(两级,简化处理)
                    wrapper.apply("(" + createByField + " IN (SELECT id FROM sys_user WHERE deleted = 0 AND dept_id = {0})" +
                            " OR " + createByField + " IN (SELECT id FROM sys_user WHERE deleted = 0 AND dept_id IN " +
                            "(SELECT id FROM sys_dept WHERE deleted = 0 AND parent_id = {0})))", deptId);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 使用 DataScopeContext 中存储的字段名(由 @DataScope 注解配置)。
     */
    public static <T> void applyTo(LambdaQueryWrapper<T> wrapper) {
        String field = DataScopeContext.get();
        applyTo(wrapper, field == null ? "create_by" : field);
    }
}
