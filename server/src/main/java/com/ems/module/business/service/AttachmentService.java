package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.Attachment;
import com.ems.module.business.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;

    public PageResult<Attachment> page(long pageNum, long pageSize, String name, String businessType, Long businessId) {
        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) wrapper.like(Attachment::getName, name);
        if (StringUtils.hasText(businessType)) wrapper.eq(Attachment::getBusinessType, businessType);
        if (businessId != null) wrapper.eq(Attachment::getBusinessId, businessId);
        wrapper.orderByDesc(Attachment::getCreateTime);
        Page<Attachment> page = attachmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public List<Attachment> listByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(businessType)) wrapper.eq(Attachment::getBusinessType, businessType);
        if (businessId != null) wrapper.eq(Attachment::getBusinessId, businessId);
        wrapper.orderByDesc(Attachment::getCreateTime);
        return attachmentMapper.selectList(wrapper);
    }

    public Attachment get(Long id) {
        Attachment a = attachmentMapper.selectById(id);
        if (a == null) throw new BusinessException("附件不存在");
        return a;
    }

    public Attachment create(Attachment entity) {
        attachmentMapper.insert(entity);
        return entity;
    }

    public void delete(Long id) {
        get(id);
        attachmentMapper.deleteById(id);
    }

    public void deleteByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attachment::getBusinessType, businessType);
        wrapper.eq(Attachment::getBusinessId, businessId);
        attachmentMapper.delete(wrapper);
    }
}
