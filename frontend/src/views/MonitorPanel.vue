<template>
  <div class="monitor-panel">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>监控面板</span>
          <el-button type="primary" size="small" @click="refreshData">刷新</el-button>
        </div>
      </template>

      <!-- 设备概览 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalDevices }}</div>
              <div class="stat-label">总设备数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value" style="color: #67C23A;">{{ stats.onlineDevices }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value" style="color: #E6A23C;">{{ stats.warningDevices }}</div>
              <div class="stat-label">告警设备</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value" style="color: #F56C6C;">{{ stats.faultDevices }}</div>
              <div class="stat-label">故障设备</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 设备列表 -->
      <el-table :data="devices" border style="width: 100%">
        <el-table-column prop="name" label="设备名称" width="150"></el-table-column>
        <el-table-column prop="ip" label="IP地址" width="120"></el-table-column>
        <el-table-column prop="vendor" label="厂商" width="100"></el-table-column>
        <el-table-column prop="model" label="型号" width="120"></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'online' ? 'success' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="CPU使用率" width="120">
          <template #default="scope">
            <el-progress 
              :percentage="scope.row.cpuUsage || 0" 
              :color="getProgressColor(scope.row.cpuUsage)"
              :stroke-width="8"
            ></el-progress>
          </template>
        </el-table-column>
        <el-table-column label="内存使用率" width="120">
          <template #default="scope">
            <el-progress 
              :percentage="scope.row.memoryUsage || 0" 
              :color="getProgressColor(scope.row.memoryUsage)"
              :stroke-width="8"
            ></el-progress>
          </template>
        </el-table-column>
        <el-table-column label="健康评分" width="120">
          <template #default="scope">
            <el-tag :type="getHealthTagType(scope.row.healthScore)">
              {{ scope.row.healthScore || 'N/A' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="viewDetails(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="设备详情" width="800px">
      <div v-if="selectedDevice">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备名称">{{ selectedDevice.name }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ selectedDevice.ip }}</el-descriptions-item>
          <el-descriptions-item label="厂商">{{ selectedDevice.vendor }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ selectedDevice.model }}</el-descriptions-item>
          <el-descriptions-item label="协议">{{ selectedDevice.protocol }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedDevice.status === 'online' ? 'success' : 'danger'">
              {{ selectedDevice.status }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <h3 style="margin-top: 20px;">实时指标</h3>
        <el-row :gutter="20" style="margin-top: 10px;">
          <el-col :span="12">
            <el-card>
              <div>CPU使用率</div>
              <el-progress 
                :percentage="selectedDevice.cpuUsage || 0" 
                :color="getProgressColor(selectedDevice.cpuUsage)"
              ></el-progress>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <div>内存使用率</div>
              <el-progress 
                :percentage="selectedDevice.memoryUsage || 0" 
                :color="getProgressColor(selectedDevice.memoryUsage)"
              ></el-progress>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';

const devices = ref([]);
const stats = ref({
  totalDevices: 0,
  onlineDevices: 0,
  warningDevices: 0,
  faultDevices: 0
});
const detailDialogVisible = ref(false);
const selectedDevice = ref(null);

// 获取设备列表和统计数据
const loadData = async () => {
  try {
    const { data } = await axios.get('/api/device');
    devices.value = data.data || [];
    
    // 计算统计数据
    stats.value.totalDevices = devices.value.length;
    stats.value.onlineDevices = devices.value.filter(d => d.status === 'online').length;
    
    // 模拟获取设备指标（实际应该从采集服务获取）
    devices.value.forEach(device => {
      // 这里应该调用采集服务获取实时指标
      device.cpuUsage = Math.floor(Math.random() * 100);
      device.memoryUsage = Math.floor(Math.random() * 100);
      device.healthScore = 100 - Math.max(device.cpuUsage, device.memoryUsage);
    });
    
    stats.value.warningDevices = devices.value.filter(d => 
      d.cpuUsage > 80 || d.memoryUsage > 80
    ).length;
    stats.value.faultDevices = devices.value.filter(d => 
      d.healthScore < 60
    ).length;
  } catch (e) {
    ElMessage.error('获取设备数据失败');
  }
};

const refreshData = () => {
  loadData();
  ElMessage.success('数据已刷新');
};

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#67C23A';
  if (percentage < 80) return '#E6A23C';
  return '#F56C6C';
};

const getHealthTagType = (score) => {
  if (!score) return 'info';
  if (score >= 80) return 'success';
  if (score >= 60) return 'warning';
  return 'danger';
};

const viewDetails = (device) => {
  selectedDevice.value = device;
  detailDialogVisible.value = true;
};

onMounted(() => {
  loadData();
  // 定时刷新（每30秒）
  setInterval(loadData, 30000);
});
</script>

<style scoped>
.monitor-panel {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>

