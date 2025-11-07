<template>
  <el-card>
    <div slot="header">设备管理</div>
    <el-button type="primary" @click="showAddDialog = true">添加设备</el-button>
    
    <el-table :data="devices" border style="margin-top: 20px;">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="设备名称"></el-table-column>
      <el-table-column prop="ip" label="IP地址"></el-table-column>
      <el-table-column prop="vendor" label="厂商"></el-table-column>
      <el-table-column prop="model" label="型号"></el-table-column>
      <el-table-column prop="protocol" label="协议"></el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'online' ? 'success' : 'danger'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="startInspect(scope.row.id)">巡检</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加设备弹窗 -->
    <el-dialog title="添加设备" v-model="showAddDialog">
      <el-form :model="newDevice" label-width="100px">
        <el-form-item label="设备名称">
          <el-input v-model="newDevice.name"></el-input>
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="newDevice.ip"></el-input>
        </el-form-item>
        <el-form-item label="厂商">
          <el-select v-model="newDevice.vendor">
            <el-option label="华为" value="华为"></el-option>
            <el-option label="思科" value="思科"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="newDevice.model"></el-input>
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="newDevice.protocol">
            <el-option label="SSH" value="ssh"></el-option>
            <el-option label="SNMP" value="snmp"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="newDevice.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="newDevice.password" type="password"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addDevice">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';

const devices = ref([]);
const showAddDialog = ref(false);
const newDevice = ref({
  name: '',
  ip: '',
  vendor: '华为',
  model: '',
  protocol: 'ssh',
  username: '',
  password: ''
});

// 获取设备列表
const getDevices = async () => {
  try {
    const { data } = await axios.get('/api/device');
    devices.value = data.data || [];
  } catch (e) {
    ElMessage.error('获取设备列表失败');
  }
};

// 添加设备
const addDevice = async () => {
  try {
    await axios.post('/api/device', {
      ...newDevice.value,
      password: newDevice.value.password  // 后端会加密存储
    });
    ElMessage.success('设备添加成功');
    showAddDialog.value = false;
    getDevices(); // 刷新列表
  } catch (e) {
    ElMessage.error('添加失败：' + (e.response?.data?.msg || e.message));
  }
};

// 触发巡检
const startInspect = async (deviceId) => {
  try {
    await axios.post('/api/inspect/start', { device_id: deviceId });
    ElMessage.success('巡检已启动');
  } catch (e) {
    ElMessage.error('启动巡检失败');
  }
};

onMounted(() => {
  getDevices();
});
</script>
