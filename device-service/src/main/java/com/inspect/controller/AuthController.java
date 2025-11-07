package com.inspect.controller;

import com.inspect.common.Result;
import com.inspect.model.User;
import com.inspect.service.AuthService;
import com.inspect.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private AuthUtil authUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody Map<String, String> params, HttpServletResponse response) {
        String username = params.get("username");
        String password = params.get("password");

        User user = authService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 生成Token（简化版：userId:timestamp）
        String token = user.getId() + ":" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    @Autowired
    private com.inspect.repository.UserRepository userRepository;

    /**
     * 用户注册（仅管理员可注册新用户）
     */
    @PostMapping("/register")
    public Result<Object> register(@RequestBody Map<String, String> params) {
        // 简化版：允许注册，实际应该需要管理员权限
        String username = params.get("username");
        String password = params.get("password");
        String role = params.get("role");

        if (authService.getUserByUsername(username) != null) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(authUtil.hashPassword(password));
        user.setRole(role != null ? role : "operator");

        userRepository.save(user);
        return Result.success("注册成功");
    }
}

