package com.grad.camp.controller;

import com.grad.camp.common.R;
import com.grad.camp.dto.AuthDtos;
import com.grad.camp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证与个人中心接口。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<AuthDtos.LoginResp> login(@Valid @RequestBody AuthDtos.LoginReq req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
        authService.register(req);
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @PutMapping("/profile")
    public R<Map<String, Object>> profile(@RequestBody AuthDtos.ProfileReq req) {
        return R.ok(authService.updateProfile(req));
    }

    @PutMapping("/password")
    public R<Void> password(@RequestBody AuthDtos.PasswordReq req) {
        authService.changePassword(req);
        return R.ok();
    }
}
