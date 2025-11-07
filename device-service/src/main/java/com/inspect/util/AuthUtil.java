package com.inspect.util;

import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 认证工具类（密码哈希、Token生成等）
 */
@Component
public class AuthUtil {

    /**
     * 生成密码哈希（SHA-256）
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    /**
     * 检查权限
     * @param userRole 用户角色
     * @param requiredRole 所需角色
     * @return 是否有权限
     */
    public boolean hasPermission(String userRole, String requiredRole) {
        if ("admin".equals(userRole)) {
            return true; // 管理员拥有所有权限
        }
        if ("operator".equals(userRole)) {
            return !"admin".equals(requiredRole); // 运维员不能执行管理员操作
        }
        return "readonly".equals(requiredRole); // 只读用户只能查看
    }
}

