package com.inspect.service;

import com.inspect.model.Device;
import com.inspect.repository.DeviceRepository;
import com.inspect.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 设备业务逻辑服务
 * 修复依赖注入和方法调用错误
 */
@Service // 标识为业务服务组件
public class DeviceService {

    // 注入数据访问层（修复依赖找不到错误）
    @Autowired
    private DeviceRepository deviceRepo;

    // 注入加密工具类
    @Autowired
    private EncryptionUtil encryptionUtil;

    /**
     * 添加设备（自动加密密码）
     */
    public void addDevice(Device device) {
        // 调用Device的getPasswordEnc()获取密码（由@Data生成）
        String encryptedPwd = encryptionUtil.encrypt(device.getPasswordEnc());
        // 调用setPasswordEnc()设置加密后密码（由@Data生成）
        device.setPasswordEnc(encryptedPwd);
        deviceRepo.save(device); // 保存到数据库
    }

    /**
     * 查询所有设备
     */
    public List<Device> findAll() {
        return deviceRepo.findAll(); // JpaRepository自带方法
    }

    /**
     * 根据ID查询设备
     */
    public Device findById(Integer id) {
        Optional<Device> deviceOpt = deviceRepo.findById(id);
        return deviceOpt.orElse(null); // 不存在返回null
    }

    /**
     * 更新设备状态（在线/离线）
     */
    public void updateStatus(Integer id, String status) {
        Device device = findById(id);
        if (device != null) {
            device.setStatus(status); // 调用setter（由@Data生成）
            deviceRepo.save(device);
        }
    }

    /**
     * 获取解密后的密码（供采集服务使用）
     */
    public String getDecryptedPassword(Integer deviceId) {
        Device device = findById(deviceId);
        if (device == null) {
            return null;
        }
        // 调用getPasswordEnc()获取加密密码（由@Data生成）
        return encryptionUtil.decrypt(device.getPasswordEnc());
    }

    /**
     * 删除设备
     */
    public void deleteDevice(Integer id) {
        Device device = findById(id);
        if (device != null) {
            deviceRepo.delete(device);
        }
    }
}
