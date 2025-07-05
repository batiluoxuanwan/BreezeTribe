<template>
  <div class="app-layout">
    <div class="main-sidebar">
      <div
        class="main-nav-item"
        :class="{ 'is-active': activeMainTab === 'chat' }"
        @click="activeMainTab = 'chat'"
      >
        <el-icon :size="24"><ChatDotRound /></el-icon>
      </div>
      <div
        class="main-nav-item"
        :class="{ 'is-active': activeMainTab === 'friends' }"
        @click="activeMainTab = 'friends'"
      >
        <el-icon :size="24"><User /></el-icon> 
      </div>
      </div>

    <div class="main-content-area">
      <ChatRoom v-if="activeMainTab === 'chat'" />
      <FriendManagementModule v-if="activeMainTab === 'friends'" />
      </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ChatDotRound, User } from '@element-plus/icons-vue';
import ChatRoom from '@/components/chat/ChatRoom.vue'; 
//import FriendManagementModule from './FriendManagementModule.vue'; // 假设的好友管理模块组件

const activeMainTab = ref('chat'); // 默认显示聊天模块
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100%; 
  width: 100%;
}

.main-sidebar {
  width: 40px; /* 导航栏宽度 */
  background-color: #ffffff; /* 导航栏背景 */
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
  gap: 15px; /* 导航项之间间距 */
}

.main-nav-item {
  color: #b0b0b0;
  cursor: pointer;
  padding: 10px 0;
  font-size: 12px;
  text-align: center;
  width: 100%; /* 让导航项占据整个侧边栏宽度 */
}

.main-nav-item:hover, .main-nav-item.is-active {
  color: #6da0b1;
  background-color: #ffffff; /* 激活和悬停时的背景色 */
}

.main-content-area {
  flex-grow: 1; /* 占据剩余所有空间 */
  display: flex; /* 内部也可能是flex布局，取决于具体模块 */
  background-color: #f5f5f5; /* 右侧内容区域的背景色 */
}
</style>