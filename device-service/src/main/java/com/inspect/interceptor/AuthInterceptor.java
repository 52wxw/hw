package com.inspect.interceptor;

import com.inspect.annotation.RequireRole;
import com.inspect.model.User;
import com.inspect.service.AuthService;
import com.inspect.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证拦截器：检查用户登录和权限
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private AuthService authService;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        // 如果没有权限注解，允许访问
        if (requireRole == null) {
            return true;
        }

        // 获取Token（从Header或Cookie）
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = getCookieValue(request, "token");
        }

        if (token == null || token.isEmpty()) {
            sendError(response, "未登录");
            return false;
        }

        // 解析Token获取用户ID（简化版：Token就是用户ID）
        Integer userId = parseToken(token);
        if (userId == null) {
            sendError(response, "Token无效");
            return false;
        }

        // 获取用户信息
        User user = authService.getUserById(userId);
        if (user == null) {
            sendError(response, "用户不存在");
            return false;
        }

        // 检查权限
        String requiredRole = requireRole.value();
        if (!authUtil.hasPermission(user.getRole(), requiredRole)) {
            sendError(response, "权限不足");
            return false;
        }

        // 将用户信息存入Request，供Controller使用
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", user.getRole());

        return true;
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Integer parseToken(String token) {
        try {
            // 简化版：Token格式为 "userId:timestamp"
            // 实际应该使用JWT
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String[] parts = token.split(":");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\"}");
    }
}


