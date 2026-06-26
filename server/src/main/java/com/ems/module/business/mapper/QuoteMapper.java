package com.ems.module.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ems.module.business.entity.Quote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuoteMapper extends BaseMapper<Quote> {
}
