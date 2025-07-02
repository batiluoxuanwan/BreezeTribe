<template>
  <div class="add-friend p-4 bg-white rounded-xl shadow-md max-w-3xl mx-auto mt-10">
    <!-- 顶部标题栏 -->
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-800">添加好友</h2>
      <button @click="goBack" class="text-gray-600 hover:text-gray-900 text-xl">✖</button>
    </div>

    <!-- 搜索框 -->
    <div class="flex items-center mb-6">
      <input
          v-model="searchKeyword"
          @keyup.enter="searchUsers"
          type="text"
          placeholder="搜索用户名..."
          class="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
          @click="searchUsers"
          class="ml-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
      >
        搜索
      </button>
    </div>

    <!-- 搜索结果 -->
    <ul class="space-y-4">
      <li
          v-for="user in searchResults"
          :key="user.id"
          class="flex items-center justify-between p-4 bg-gray-100 rounded-lg shadow hover:bg-gray-200 transition"
      >
        <div class="flex items-center">
          <img
              :src="user.avatar || defaultAvatar"
              alt="avatar"
              class="w-12 h-12 rounded-full mr-4 object-cover"
          />
          <span class="text-lg font-medium text-gray-800">{{ user.username }}</span>
        </div>
        <button
            @click="sendFriendRequest(user.id)"
            class="text-blue-600 hover:text-blue-800 text-2xl"
        >
          ➕
        </button>
      </li>
    </ul>

    <!-- 空状态 -->
    <div v-if="searchResults.length === 0 && searched" class="text-center text-gray-500 mt-10">
      没有找到匹配的用户
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { authAxios } from '@/utils/request';
import defaultAvatar from '@/assets/NotFoundsonailong.jpg';
import { ElMessage } from 'element-plus';

// 1. 路由与状态
const router = useRouter();
const authStore = useAuthStore();
const token = authStore.token;

// 2. 搜索相关状态
const searchKeyword = ref('');
const searchResults = ref([]);
const searched = ref(false);

// 3. 返回好友列表页
function goBack() {
  router.push('/user/friends');
}

// 4. 搜索用户（根据关键词）
async function searchUsers() {
  if (!searchKeyword.value.trim()) return;

  try {
    const res = await authAxios.get('/public/users/search', {
      params: { keyword: searchKeyword.value },
      headers: { Authorization: `Bearer ${token}` }
    });

    if (res.data.code === 200) {
      searchResults.value = res.data.data;
      searched.value = true;
    } else {
      ElMessage.error(res.data.message || '搜索失败');
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('搜索失败，请稍后再试');
    searchResults.value = [];
    searched.value = true;
  }
}

// 5. 发送好友申请
async function sendFriendRequest(friendId) {
  try {
    const res = await authAxios.post('/friend/sendRequest', {
      params: {
        toAccountId: friendId}
    }, {
      headers: { Authorization: `Bearer ${token}` }
    });

    if (res.data.code === 200) {
      ElMessage.success('好友申请已发送！');
    } else {
      ElMessage.warning(res.data.message || '发送失败');
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('发送失败，请稍后再试');
  }
}
</script>

<style scoped>
/* 可选：根据实际需求添加过渡动画或样式 */
</style>
