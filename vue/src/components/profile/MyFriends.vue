<template>
  <div class="app-layout">
<div class="top-navbar">
      <div
        class="nav-item"
        :class="{ 'is-active': activeMainTab === 'chat' }"
        @click="activeMainTab = 'chat'"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span>好友列表</span>
      </div>
      <div
        class="nav-item"
        :class="{ 'is-active': activeMainTab === 'friends' }"
        @click="activeMainTab = 'friends'"
      >
        <el-icon><User /></el-icon>
        <span>好友管理</span>
      </div>
    </div>

    <div class="main-content-area">
      <FriendList v-if="activeMainTab === 'chat'" />
      <AddFriend v-if="activeMainTab === 'friends'" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ChatDotRound, User } from '@element-plus/icons-vue';
import FriendList from '@/components/chat/FriendList.vue';
import AddFriend from '@/views/User/AddFriend.vue';

const activeMainTab = ref('chat'); // 默认显示聊天模块
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column; /* 垂直方向布局：顶部导航在上面，内容在下面 */
  height: 100%; 
  width: 100%;
  gap:10px;
}

.top-navbar {
  display: flex; /* 让导航项水平排列 */
  justify-content: flex-start; /* 导航项从左侧开始排列 */
  align-items: center; /* 垂直居中 */
  height: 60px; /* 顶部导航栏的高度 */
  background-color: #ffffff; /* 导航栏背景 */
  padding: 0 20px; /* 左右内边距 */
  border-bottom: 1px solid #eee; /* 底部加一条细线，与内容区做区分 */
  gap: 30px; /* 导航项之间的间距 */
}

.nav-item {
  display: flex; /* 让图标和文字水平排列 */
  align-items: center; /* 垂直居中 */
  gap: 5px; /* 图标和文字之间的间距 */
  color: #b0b0b0;
  cursor: pointer;
  padding: 8px 12px; /* 导航项的内边距 */
  font-size: 14px; /* 字体大小 */
  border-radius: 4px; /* 轻微圆角 */
  transition: background-color 0.3s, color 0.3s; /* 平滑过渡效果 */
}

.nav-item:hover,
.nav-item.is-active {
  color: #6da0b1;
  background-color: #f0f0f0; /* 激活和悬停时的背景色 */
}

.nav-item.is-active {
  font-weight: bold; /* 激活时字体加粗 */
}

.main-content-area {
  flex-grow: 1; /* 占据剩余所有空间 */
  display: flex; /* 内部也可能是flex布局，取决于具体模块 */
  background-color: #f5f5f5; /* 右侧内容区域的背景色 */
}
</style>