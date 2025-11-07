package com.inspect.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 密码加密解密工具（AES算法）
 */
@Component // 注入Spring容器
public class EncryptionUtil {

    // 从配置文件读取加密密钥（与application.yml中的encrypt.key对应）
    @Value("${encrypt.key}")
    private String secretKey;

    private static final String ALGORITHM = "AES"; // 加密算法

    /**
     * 加密数据
     */
    public String encrypt(String data) {
        try {
            // 生成密钥（AES要求密钥长度16/24/32字节）
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            // 加密后Base64编码（方便存储）
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败：" + e.getMessage());
        }
    }

    /**
     * 解密数据
     */
    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            // 先Base64解码，再解密
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("解密失败：" + e.getMessage());
        }
    }
}
