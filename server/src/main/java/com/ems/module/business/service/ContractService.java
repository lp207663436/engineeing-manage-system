package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.ContractDTO;
import com.ems.module.business.entity.Contract;
import com.ems.module.business.mapper.ContractMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;

    public PageResult<Contract> page(long pageNum, long pageSize, String name, String code, String status) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(Contract::getName, name);
        if (StringUtils.hasText(code)) wrapper.like(Contract::getCode, code);
        if (StringUtils.hasText(status)) wrapper.eq(Contract::getStatus, status);
        wrapper.orderByDesc(Contract::getCreateTime);
        Page<Contract> page = contractMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Contract get(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BusinessException("合同不存在");
        return c;
    }

    public void create(ContractDTO dto) {
        Contract c = new Contract();
        BeanUtils.copyProperties(dto, c);
        if (StringUtils.hasText(dto.getSignDate())) c.setSignDate(LocalDate.parse(dto.getSignDate()));
        if (StringUtils.hasText(dto.getStartDate())) c.setStartDate(LocalDate.parse(dto.getStartDate()));
        if (StringUtils.hasText(dto.getEndDate())) c.setEndDate(LocalDate.parse(dto.getEndDate()));
        if (!StringUtils.hasText(c.getStatus())) c.setStatus("DRAFT");
        c.setCreateBy(SecurityContext.getUserId());
        contractMapper.insert(c);
    }

    public void update(ContractDTO dto) {
        Contract existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getSignDate())) existing.setSignDate(LocalDate.parse(dto.getSignDate()));
        if (StringUtils.hasText(dto.getStartDate())) existing.setStartDate(LocalDate.parse(dto.getStartDate()));
        if (StringUtils.hasText(dto.getEndDate())) existing.setEndDate(LocalDate.parse(dto.getEndDate()));
        contractMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        contractMapper.deleteById(id);
    }
}
