package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.dto.SysUserDTO;
import com.ems.module.system.dto.UserRoleDTO;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    /** 注入单例 BCryptPasswordEncoder(由 SecurityConfig 提供) */
    private final BCryptPasswordEncoder passwordEncoder;

    /** 密码复杂度:至少8位,包含字母和数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    public PageResult<SysUser> page(long pageNum, long pageSize, String name, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(SysUser::getName, name);
        if (deptId != null) wrapper.eq(SysUser::getDeptId, deptId);
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Transactional
    public void create(SysUserDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new BusinessException("用户名已存在");
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        // 密码复杂度校验:至少8位,包含字母和数字
        if (!PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
            throw new BusinessException("密码至少8位,且必须包含字母和数字");
        }
        SysUser user = new SysUser();
        user.setDeptId(dto.getDeptId());
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userMapper.insert(user);
    }

    @Transactional
    public void update(SysUserDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) throw new BusinessException("用户不存在");
        // 水平越权校验:非自我编辑时,需有对应数据权限
        Long currentUserId = SecurityContext.getUserId();
        boolean isSelfEdit = currentUserId != null && currentUserId.equals(dto.getId()) && !SecurityContext.isAdmin();
        if (!isSelfEdit) {
            DataScopeHelper.checkOwnership(user.getCreateBy());
        }
        // 自我保护:当前登录用户修改自己的账号时,禁止修改 status 和角色分配
        if (isSelfEdit) {
            if (dto.getStatus() != null && !dto.getStatus().equals(user.getStatus())) {
                throw new BusinessException("不能修改自己的账号状态");
            }
        }
        user.setDeptId(dto.getDeptId());
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        // 自我保护:非超管修改自己时不允许改 status
        if (isSelfEdit) {
            // 保持原 status 不变
        } else {
            user.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            // 修改密码时同样校验复杂度
            if (!PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
                throw new BusinessException("密码至少8位,且必须包含字母和数字");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        if (SecurityContext.SUPER_ADMIN_ID.equals(id)) throw new BusinessException("不能删除超级管理员");
        // 自我保护:禁止删除当前登录用户
        Long currentUserId = SecurityContext.getUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        // 水平越权校验:需有对应数据权限才能删除该用户
        SysUser target = userMapper.selectById(id);
        if (target != null) {
            DataScopeHelper.checkOwnership(target.getCreateBy());
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Transactional
    public void assignRoles(UserRoleDTO dto) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, dto.getUserId()));
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                userRoleMapper.insert(new SysUserRole(dto.getUserId(), roleId));
            }
        }
    }

    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }
}
