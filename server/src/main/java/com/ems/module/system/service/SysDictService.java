package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.dto.SysDictDTO;
import com.ems.module.system.dto.SysDictItemDTO;
import com.ems.module.system.entity.SysDict;
import com.ems.module.system.entity.SysDictItem;
import com.ems.module.system.mapper.SysDictItemMapper;
import com.ems.module.system.mapper.SysDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictMapper sysDictMapper;
    private final SysDictItemMapper sysDictItemMapper;

    /* ==================== 字典 CRUD ==================== */

    public PageResult<SysDict> page(long pageNum, long pageSize, String name, String code) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(SysDict::getName, name);
        if (StringUtils.hasText(code)) wrapper.like(SysDict::getCode, code);
        wrapper.orderByDesc(SysDict::getId);
        Page<SysDict> page = sysDictMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public List<SysDict> list() {
        return sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>().orderByDesc(SysDict::getId));
    }

    public SysDict get(Long id) {
        SysDict d = sysDictMapper.selectById(id);
        if (d == null) throw new BusinessException("字典不存在");
        // 水平越权校验
        DataScopeHelper.checkOwnership(d.getCreateBy());
        return d;
    }

    public SysDict create(SysDictDTO dto) {
        SysDict d = new SysDict();
        BeanUtils.copyProperties(dto, d);
        sysDictMapper.insert(d);
        return d;
    }

    public void update(SysDictDTO dto) {
        SysDict existing = get(dto.getId());
        // 水平越权校验(get 内已校验,此处再次确认 existing 的归属)
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        BeanUtils.copyProperties(dto, existing);
        sysDictMapper.updateById(existing);
    }

    public void delete(Long id) {
        SysDict d = get(id);
        // 水平越权校验
        DataScopeHelper.checkOwnership(d.getCreateBy());
        sysDictMapper.deleteById(id);
        // 同步删除字典项
        sysDictItemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
    }

    /* ==================== 字典项 CRUD ==================== */

    public List<SysDictItem> itemsByCode(String code) {
        SysDict dict = sysDictMapper.selectOne(new LambdaQueryWrapper<SysDict>().eq(SysDict::getCode, code));
        if (dict == null) throw new BusinessException("字典不存在:" + code);
        return sysDictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dict.getId())
                        .orderByAsc(SysDictItem::getSort));
    }

    public List<SysDictItem> itemsByDictId(Long dictId) {
        return sysDictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dictId)
                        .orderByAsc(SysDictItem::getSort));
    }

    public SysDictItem itemCreate(SysDictItemDTO dto) {
        SysDictItem item = new SysDictItem();
        BeanUtils.copyProperties(dto, item);
        if (item.getSort() == null) item.setSort(0);
        sysDictItemMapper.insert(item);
        return item;
    }

    public void itemUpdate(SysDictItemDTO dto) {
        SysDictItem existing = sysDictItemMapper.selectById(dto.getId());
        if (existing == null) throw new BusinessException("字典项不存在");
        BeanUtils.copyProperties(dto, existing);
        sysDictItemMapper.updateById(existing);
    }

    public void itemDelete(Long id) {
        SysDictItem existing = sysDictItemMapper.selectById(id);
        if (existing == null) throw new BusinessException("字典项不存在");
        sysDictItemMapper.deleteById(id);
    }
}
