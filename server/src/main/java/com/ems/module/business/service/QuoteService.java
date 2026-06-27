package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.dto.QuoteDTO;
import com.ems.module.business.entity.Quote;
import com.ems.module.business.mapper.QuoteMapper;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteMapper quoteMapper;

    public PageResult<Quote> page(long pageNum, long pageSize, String code, String customerName, String status) {
        LambdaQueryWrapper<Quote> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.like(Quote::getCode, code);
        if (StringUtils.hasText(customerName)) wrapper.like(Quote::getCustomerName, customerName);
        if (StringUtils.hasText(status)) wrapper.eq(Quote::getStatus, status);
        wrapper.orderByDesc(Quote::getCreateTime);
        Page<Quote> page = quoteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public Quote get(Long id) {
        Quote q = quoteMapper.selectById(id);
        if (q == null) throw new BusinessException("报价不存在");
        return q;
    }

    public Quote create(QuoteDTO dto) {
        Quote q = new Quote();
        BeanUtils.copyProperties(dto, q);
        if (StringUtils.hasText(dto.getQuoteDate())) q.setQuoteDate(LocalDate.parse(dto.getQuoteDate()));
        if (StringUtils.hasText(dto.getValidUntil())) q.setValidUntil(LocalDate.parse(dto.getValidUntil()));
        if (!StringUtils.hasText(q.getBusinessType())) q.setBusinessType("NEW_BUILD");
        if (q.getBusinessId() == null) q.setBusinessId(dto.getProjectId());
        if (!StringUtils.hasText(q.getStatus())) q.setStatus("DRAFT");
        if (q.getVersion() == null) q.setVersion(1);
        q.setCreateBy(SecurityContext.getUserId());
        quoteMapper.insert(q);
        return q;
    }

    public void update(QuoteDTO dto) {
        Quote existing = get(dto.getId());
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getQuoteDate())) existing.setQuoteDate(LocalDate.parse(dto.getQuoteDate()));
        if (StringUtils.hasText(dto.getValidUntil())) existing.setValidUntil(LocalDate.parse(dto.getValidUntil()));
        quoteMapper.updateById(existing);
    }

    public void delete(Long id) {
        get(id);
        quoteMapper.deleteById(id);
    }
}
