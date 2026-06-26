package com.ems.module.system.controller;

import com.ems.common.Result;
import com.ems.module.system.dto.LoginDTO;
import com.ems.module.system.dto.LoginVO;
import com.ems.module.system.service.AuthService;
import com.ems.security.context.SecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = SecurityContext.getUserId();
        if (userId != null) authService.logout(userId);
        return Result.success();
    }
}
