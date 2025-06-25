<template>
  <div class="dashboard-container">
    <!-- 顶部导航 -->
    <header class="dashboard-header">
      <div class="logo">团长个人中心</div>
      <div class="user-info">
        <span>敬旅旅行社</span>
        <el-avatar size="small">JL</el-avatar>
      </div>
    </header>

    <div class="dashboard-body">
      <!-- 左侧菜单 -->
      <el-menu class="sidebar" :default-active="'1'" router>
        <el-menu-item index="/Merchant/Profile">商户概览</el-menu-item>
        <el-sub-menu index="2">
          <template #title>旅游团管理</template>
          <el-menu-item index="/Merchant/Newgroup">发布新团</el-menu-item>
          <el-menu-item index="2-2">我的旅游团</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="3">订单管理</el-menu-item>
        <!-- <el-menu-item index="4">财务报表</el-menu-item>
        <el-menu-item index="5">客户消息</el-menu-item>
        <el-menu-item index="6">系统设置</el-menu-item> -->
      </el-menu>

      <!-- 主内容区 -->
      <main class="dashboard-main">
        <!-- 数据卡片 -->
        <div class="stat-cards">
          <el-card class="stat-card">
            <p>本月订单数</p>
            <h2>42</h2>
          </el-card>
          <el-card class="stat-card">
            <p>今日浏览量</p>
            <h2>1,245</h2>
          </el-card>
          <el-card class="stat-card">
            <p>整体评分</p>
            <h2>4.7</h2>
          </el-card>
        </div>

        <!-- 按钮 -->
        <!-- <div class="action-buttons">
          <el-button type="primary">发布新团</el-button>
          <el-button>管理旅游团</el-button>
          <el-button>查看订单</el-button>
        </div> -->

        <!-- 审核状态 -->
        <el-card class="status-card">
          <template #header>
            审核状态
            <el-link type="primary" class="float-right">查看所有</el-link>
          </template>
          <ul>
            <li>广西北海夏日游计划 - 审核中</li>
            <li>大理苍山洱海徒步三日游 - 已通过</li>
            <li>西双版纳热带雨林探索团 - 被驳回</li>
          </ul>
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ShoppingCart } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.dashboard-header {
  height: 60px;
  background: linear-gradient(to right, #6da0b1, #60a5fa);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  color: white;
}

.dashboard-body {
  display: flex;
  flex: 1;
}

.sidebar {
  width: 220px;
  height: 100%;
  border-right: 1px solid #eee;
}

.dashboard-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.stat-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.action-buttons {
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
}

.status-card ul {
  list-style: none;
  padding: 0;
}

.status-card li {
  margin-bottom: 10px;
}

.stat-card {
  width: 100%;
  max-width: 300px;
  padding: 20px;
}

.card-content {
  display: flex;
  gap: 16px;
}

.icon-box {
  background-color: #f0f4ff;
  padding: 10px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.text-box {
  flex: 1;
}

.title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.desc {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.highlight {
  color: #409EFF;
}
</style>
