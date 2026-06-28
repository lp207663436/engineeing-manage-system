package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.entity.SysDept;
import com.ems.module.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    public List<SysDept> list() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    public List<SysDept> tree() {
        return buildTree(list(), 0L);
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        return all.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .peek(d -> d.setChildren(buildTree(all, d.getId())))
                .toList();
    }

    public void create(SysDept dept) {
        deptMapper.insert(dept);
    }

    public void update(SysDept dept) {
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BusinessException("上级部门不能是自己");
        }
        SysDept existing = deptMapper.selectById(dept.getId());
        if (existing == null) throw new BusinessException("部门不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) throw new BusinessException("存在子部门,不能删除");
        SysDept existing = deptMapper.selectById(id);
        if (existing == null) throw new BusinessException("部门不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        deptMapper.deleteById(id);
    }
}
