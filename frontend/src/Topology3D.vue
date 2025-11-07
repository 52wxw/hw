<template>
  <div class="topology-container">
    <el-card>
      <div slot="header">3D网络拓扑图</div>
      <div id="three-container" style="width:100%;height:600px;"></div>
      <el-button @click="refresh">刷新拓扑</el-button>
    </el-card>
  </div>
</template>

<script setup>
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { onMounted, ref } from 'vue';
import axios from 'axios';

let scene, camera, renderer, controls;

// 初始化3D场景
const initScene = () => {
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x1e1e1e);
  
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / 600, 0.1, 1000);
  camera.position.set(0, 10, 20);
  
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(window.innerWidth, 600);
  document.getElementById('three-container').appendChild(renderer.domElement);
  
  // 光源
  const dirLight = new THREE.DirectionalLight(0xffffff, 1);
  dirLight.position.set(10, 10, 10);
  scene.add(dirLight);
  
  // 控制器
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  
  // 地面
  const groundGeo = new THREE.PlaneGeometry(50, 50);
  const groundMat = new THREE.MeshBasicMaterial({ color: 0x333333 });
  const ground = new THREE.Mesh(groundGeo, groundMat);
  ground.rotation.x = -Math.PI / 2;
  scene.add(ground);
  
  // 动画循环
  const animate = () => {
    requestAnimationFrame(animate);
    controls.update();
    renderer.render(scene, camera);
  };
  animate();
};

// 绘制设备节点
const drawDevices = async () => {
  const { data } = await axios.get('/api/device');
  const devices = data.data;
  
  // 设备模型（立方体+图标）
  const geo = new THREE.BoxGeometry(2, 2, 2);
  devices.forEach((dev, i) => {
    // 按厂商着色
    const colorMap = { "华为": 0x0066cc, "思科": 0x009900, "H3C": 0xff6600 };
    const mat = new THREE.MeshBasicMaterial({ color: colorMap[dev.vendor] || 0xffffff });
    const cube = new THREE.Mesh(geo, mat);
    cube.position.set(i * 5 - 10, 1, 0); // 横向排列
    scene.add(cube);
    
    // 设备标签（IP+状态）
    const div = document.createElement('div');
    div.className = 'device-label';
    div.style.cssText = `position:absolute; color:white; font-size:12px;`;
    div.innerHTML = `${dev.name}(${dev.ip})<br>${dev.status}`;
    document.body.appendChild(div);
    // 标签位置跟随3D节点
    const updateLabel = () => {
      const pos = new THREE.Vector3();
      pos.setFromMatrixPosition(cube.matrixWorld);
      pos.project(camera);
      const x = (pos.x * 0.5 + 0.5) * window.innerWidth;
      const y = (-pos.y * 0.5 + 0.5) * 600;
      div.style.left = `${x}px`;
      div.style.top = `${y}px`;
    };
    controls.addEventListener('change', updateLabel);
  });
};

// 绘制连接链路
const drawLinks = async () => {
  // 从LLDP协议获取设备连接关系
  const { data } = await axios.get('/api/links');
  const links = data.data;
  
  const lineGeo = new THREE.BufferGeometry();
  const lineMat = new THREE.LineBasicMaterial({ color: 0xcccccc });
  links.forEach(link => {
    // 起点和终点坐标（根据设备ID映射）
    const start = new THREE.Vector3(link.src_x, 1, link.src_z);
    const end = new THREE.Vector3(link.dst_x, 1, link.dst_z);
    lineGeo.setFromPoints([start, end]);
    const line = new THREE.Line(lineGeo, lineMat);
    scene.add(line);
  });
};

onMounted(() => {
  initScene();
  drawDevices();
  drawLinks();
});

const refresh = () => {
  // 清空场景并重绘
  scene.children.forEach(child => {
    if (child.type !== 'DirectionalLight' && child.type !== 'Mesh' && child.name !== 'ground') {
      scene.remove(child);
    }
  });
  drawDevices();
  drawLinks();
};
</script>
