package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.SupplierDTO;
import com.ems.module.business.entity.Supplier;
import com.ems.module.business.mapper.SupplierMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public PageResult<Supplier> page(long pageNum, long pageSize, String name, String code) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(Supplier::getName, name);
        if (StringUtils.hasText(code)) wrapper.like(Supplier::getCode, code);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Supplier::getCreateTime);
        Page<Supplier> page = supplierMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public List<Supplier> list() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Supplier::getCreateTime);
        return supplierMapper.selectList(wrapper);
    }

    public Supplier get(Long id) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException("供应商不存在");
        return s;
    }

    public Supplier create(SupplierDTO dto) {
        Supplier s = new Supplier();
        BeanUtils.copyProperties(dto, s);
        s.setCreateBy(SecurityContext.getUserId());
        supplierMapper.insert(s);
        return s;
    }

    public void update(SupplierDTO dto) {
        Supplier existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        supplierMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        supplierMapper.deleteById(id);
    }
}
