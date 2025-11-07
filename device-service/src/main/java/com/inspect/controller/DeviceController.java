package com.inspect.controller;

import com.inspect.annotation.RequireRole;
import com.inspect.common.Result;
import com.inspect.model.Device;
import com.inspect.model.OperationLog;
import com.inspect.service.DeviceService;
import com.inspect.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private OperationLogService operationLogService;

    // 添加设备：需要operator权限
    @PostMapping
    @RequireRole("operator")
    public Result<Object> addDevice(@RequestBody Device device, HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("userId");
            deviceService.addDevice(device);
            // 记录操作日志
            operationLogService.logOperation(
                userId,
                device.getId(),
                "ADD_DEVICE",
                String.format("添加设备：%s (%s)", device.getName(), device.getIp()),
                "success"
            );
            return Result.success("设备添加成功");
        } catch (Exception e) {
            Integer userId = (Integer) request.getAttribute("userId");
            operationLogService.logOperation(
                userId,
                null,
                "ADD_DEVICE",
                String.format("添加设备失败：%s", e.getMessage()),
                "failed"
            );
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    // 获取设备列表：只读权限即可
    @GetMapping
    @RequireRole("readonly")
    public Result<List<Device>> getDevices() {
        List<Device> devices = deviceService.findAll();
        return Result.success(devices);
    }

    // 获取单设备：只读权限即可
    @GetMapping("/{id}")
    @RequireRole("readonly")
    public Result<Device> getDevice(@PathVariable Integer id) {
        Device device = deviceService.findById(id);
        if (device == null) {
            return Result.error("设备不存在", null);
        }
        return Result.success(device);
    }

    // 更新状态：需要operator权限
    @PutMapping("/{id}/status")
    @RequireRole("operator")
    public Result<Object> updateStatus(@PathVariable Integer id, @RequestParam String status, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        deviceService.updateStatus(id, status);
        operationLogService.logOperation(
            userId,
            id,
            "UPDATE_DEVICE_STATUS",
            String.format("更新设备状态：%s", status),
            "success"
        );
        return Result.success("状态更新成功");
    }

    // 删除设备：需要admin权限
    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Object> deleteDevice(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("userId");
            Device device = deviceService.findById(id);
            if (device == null) {
                return Result.error("设备不存在");
            }
            deviceService.deleteDevice(id);
            operationLogService.logOperation(
                userId,
                id,
                "DELETE_DEVICE",
                String.format("删除设备：%s (%s)", device.getName(), device.getIp()),
                "success"
            );
            return Result.success("设备删除成功");
        } catch (Exception e) {
            Integer userId = (Integer) request.getAttribute("userId");
            operationLogService.logOperation(
                userId,
                id,
                "DELETE_DEVICE",
                String.format("删除设备失败：%s", e.getMessage()),
                "failed"
            );
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    // 获取操作日志：需要readonly权限
    @GetMapping("/{id}/logs")
    @RequireRole("readonly")
    public Result<Object> getDeviceLogs(@PathVariable Integer id) {
        List<OperationLog> logs = operationLogService.getDeviceLogs(id);
        return Result.success(logs);
    }
}

