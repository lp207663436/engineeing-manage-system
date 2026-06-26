package com.ems.module.system.service;

import com.ems.module.system.dto.SysUserDTO;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SysUserServiceTest {

    @Autowired private SysUserService userService;
    @Autowired private SysUserMapper userMapper;

    @Test
    void createAndQuery_shouldInsertUser() {
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("test_" + System.currentTimeMillis());
        dto.setName("测试用户");
        dto.setPhone("13800138000");
        userService.create(dto);

        SysUser inserted = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getName, "测试用户")).get(0);
        assertNotNull(inserted.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("123456", inserted.getPassword()));
    }

    @Test
    void create_duplicateUsername_shouldThrow() {
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("admin");
        dto.setName("dup");
        assertThrows(RuntimeException.class, () -> userService.create(dto));
    }
}
