package com.ems.common.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.exception.BusinessException;
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
 * - dataScope=3(本部门及以下):create_by IN (本部门+所有子孙部门用户ID,递归查询)
 * - dataScope=4(本人):create_by = currentUserId
 */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    public static <T> void applyTo(LambdaQueryWrapper<T> wrapper, String createByField) {
        CurrentUser user = SecurityContext.get();
        if (user == null) return;
        if (user.getUserId() == null) return;
        if (SecurityContext.SUPER_ADMIN_ID.equals(user.getUserId())) return; // 超管看全部

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
                    // 递归查询本部门及所有子孙部门(MySQL 8.0+ WITH RECURSIVE)
                    wrapper.apply("(" + createByField + " IN (SELECT id FROM sys_user WHERE deleted = 0 AND dept_id IN (" +
                            "WITH RECURSIVE dept_tree AS (" +
                            "SELECT id FROM sys_dept WHERE id = {0} AND deleted = 0 " +
                            "UNION ALL " +
                            "SELECT d.id FROM sys_dept d INNER JOIN dept_tree t ON d.parent_id = t.id WHERE d.deleted = 0" +
                            ") SELECT id FROM dept_tree" +
                            ")))", deptId);
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

    /**
     * 水平越权校验:检查当前登录用户是否有权操作某条记录(由 createBy 标识归属)。
     * 规则:
     * - 无登录上下文:抛 401
     * - 超管(userId=1):放行
     * - createBy 为 null:仅超管可操作
     * - createBy 等于当前用户:放行(本人数据)
     * - dataScope<=3(本部门及以下):放行(部门内数据)
     * - 其他:抛 403
     *
     * @param createBy 被操作记录的创建人 ID
     */
    public static void checkOwnership(Long createBy) {
        CurrentUser user = SecurityContext.get();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(401, "未登录");
        }
        // 超管放行
        if (SecurityContext.SUPER_ADMIN_ID.equals(user.getUserId())) {
            return;
        }
        // createBy 为 null 的记录仅超管可操作
        if (createBy == null) {
            throw new BusinessException(403, "无权操作该数据");
        }
        // 本人数据放行
        if (createBy.equals(user.getUserId())) {
            return;
        }
        // 部门数据权限(dataScope<=3 即本部门及以下)放行
        Integer scope = user.getDataScope();
        if (scope != null && scope <= 3) {
            return;
        }
        throw new BusinessException(403, "无权操作该数据");
    }
}
