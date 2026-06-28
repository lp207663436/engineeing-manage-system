package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.datascope.DataScopeHelper;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.CustomerDTO;
import com.ems.module.business.entity.Customer;
import com.ems.module.business.mapper.CustomerMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;

    public PageResult<Customer> page(long pageNum, long pageSize, String name, String code) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(Customer::getName, name);
        if (StringUtils.hasText(code)) wrapper.like(Customer::getCode, code);
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = customerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public List<Customer> list() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        DataScopeHelper.applyTo(wrapper);
        wrapper.orderByDesc(Customer::getCreateTime);
        return customerMapper.selectList(wrapper);
    }

    public Customer get(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException("客户不存在");
        DataScopeHelper.checkOwnership(c.getCreateBy());
        return c;
    }

    public Customer create(CustomerDTO dto) {
        Customer c = new Customer();
        BeanUtils.copyProperties(dto, c);
        c.setCreateBy(SecurityContext.getUserId());
        customerMapper.insert(c);
        return c;
    }

    public void update(CustomerDTO dto) {
        Customer existing = get(dto.getId());
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        BeanUtils.copyProperties(dto, existing);
        customerMapper.updateById(existing);
    }

    public void delete(Long id) {
        Customer existing = get(id);
        DataScopeHelper.checkOwnership(existing.getCreateBy());
        customerMapper.deleteById(id);
    }
}
