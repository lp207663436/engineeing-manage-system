package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.dto.SysRoleDTO;
import com.ems.module.system.entity.SysRole;
import com.ems.module.system.entity.SysRoleMenu;
import com.ems.module.system.mapper.SysRoleMapper;
import com.ems.module.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 超管角色编码,受保护不可修改 code 和 dataScope */
    private static final String ADMIN_ROLE_CODE = "admin";

    public PageResult<SysRole> page(long pageNum, long pageSize, String name) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(SysRole::getName, name);
        wrapper.orderByAsc(SysRole::getSort);
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
    }

    @Transactional
    public void create(SysRoleDTO dto) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
        if (count > 0) throw new BusinessException("角色编码已存在");
        SysRole role = new SysRole();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDataScope(dto.getDataScope() == null ? 4 : dto.getDataScope());
        role.setSort(dto.getSort() == null ? 0 : dto.getSort());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), dto.getMenuIds());
    }

    @Transactional
    public void update(SysRoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) throw new BusinessException("角色不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(role.getCreateBy());
        // 保护超管角色:admin 角色禁止修改 code 和 dataScope 字段
        if (ADMIN_ROLE_CODE.equals(role.getCode())) {
            if (dto.getCode() != null && !ADMIN_ROLE_CODE.equals(dto.getCode())) {
                throw new BusinessException("不能修改超级管理员角色编码");
            }
            if (dto.getDataScope() != null && !dto.getDataScope().equals(role.getDataScope())) {
                throw new BusinessException("不能修改超级管理员角色数据权限");
            }
            // 超管角色只允许更新 name/sort/status,保留原 code 和 dataScope
            role.setName(dto.getName());
            role.setSort(dto.getSort());
            role.setStatus(dto.getStatus());
        } else {
            role.setName(dto.getName());
            role.setDataScope(dto.getDataScope());
            role.setSort(dto.getSort());
            role.setStatus(dto.getStatus());
        }
        roleMapper.updateById(role);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, dto.getId()));
        saveRoleMenus(dto.getId(), dto.getMenuIds());
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        if (ADMIN_ROLE_CODE.equals(role.getCode())) throw new BusinessException("不能删除超级管理员角色");
        // 水平越权校验
        DataScopeHelper.checkOwnership(role.getCreateBy());
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    /**
     * 仅更新角色-菜单关联,不触碰角色基本信息。
     * 用于"分配菜单"场景,避免前端误传角色名等字段导致清空。
     */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException("角色不存在");
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        saveRoleMenus(roleId, menuIds);
    }

    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) return;
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
        }
    }
}
