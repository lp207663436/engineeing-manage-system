package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
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
        role.setName(dto.getName());
        role.setDataScope(dto.getDataScope());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        roleMapper.updateById(role);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, dto.getId()));
        saveRoleMenus(dto.getId(), dto.getMenuIds());
    }

    @Transactional
    public void delete(Long id) {
        if (id == 1L) throw new BusinessException("不能删除超级管理员角色");
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
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
