package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.entity.SysMenu;
import com.ems.module.system.entity.SysRoleMenu;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysMenuMapper;
import com.ems.module.system.mapper.SysRoleMenuMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 任务9:加载用户权限码 */
    public Set<String> getPermissionsByUserId(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return Collections.emptySet();
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) return Collections.emptySet();
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toSet());
    }

    /** 任务12:全部菜单列表 */
    public List<SysMenu> list() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    /** 任务12:菜单树 */
    public List<SysMenu> tree() {
        return buildTree(list(), 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        return all.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .peek(m -> m.setChildren(buildTree(all, m.getId())))
                .toList();
    }

    /** 任务12:用户菜单树(超管返回全部) */
    public List<SysMenu> getUserMenus(Long userId) {
        if (SecurityContext.SUPER_ADMIN_ID.equals(userId)) return tree();
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return List.of();
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) return List.of();
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        return buildTree(menus, 0L);
    }

    public void create(SysMenu menu) {
        menuMapper.insert(menu);
    }

    public void update(SysMenu menu) {
        SysMenu existing = menuMapper.selectById(menu.getId());
        if (existing == null) throw new BusinessException("菜单不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        // 删除前检查是否有子菜单,有则禁止删除
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("请先删除子菜单");
        }
        SysMenu existing = menuMapper.selectById(id);
        if (existing == null) throw new BusinessException("菜单不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        menuMapper.deleteById(id);
    }
}
