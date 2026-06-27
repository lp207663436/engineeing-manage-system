package com.ems.module.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ems.module.business.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
