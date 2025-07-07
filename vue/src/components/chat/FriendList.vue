<template>
  <div class="chat-module-container">
    <div class="friend-list-sidebar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索好友"
        :prefix-icon="Search"
        clearable
        class="search-input"
      ></el-input>

      <ul class="friend-items-list">
        <li
          v-for="friendItem in filteredFriends"
          :key="getFriendId(friendItem)"
          :class="['friend-item', { 'is-active': activeFriendId === getFriendId(friendItem) }]"
          @click="openChat(friendItem)"
        >
          <img
            :src="friendAvatars[getFriendId(friendItem)] || defaultAvatar"
            alt="avatar"
            class="friend-avatar"
          />
          <span class="friend-username">{{ getFriend(friendItem).username }}</span>
        </li>
        <el-empty v-if="filteredFriends.length === 0 && !friendLoading" description="暂无好友" :image-size="50"></el-empty>
      </ul>
      <div v-if="friendLoading" class="loading-overlay">
        <el-icon class="is-loading"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
    </div>
    <ChatRoom
      v-if="activeFriend"
      :friend-id="getFriend(activeFriend).id"
      v-model:visible="chatVisible"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { authAxios } from '@/utils/request.js';
import { ElMessage, ElInput, ElButton, ElEmpty, ElIcon } from 'element-plus';
import { Search, Loading } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import defaultAvatar from '@/assets/NotFoundsonailong.jpg'; // 确保路径正确
import ChatRoom from '@/views/User/ChatRoom.vue';

const router = useRouter();
const authStore = useAuthStore();

// --- 好友列表相关数据和逻辑 ---
const friends = ref([]); // 存储所有好友的原始数据
const friendAvatars = ref({}); // 好友ID -> 头像URL 映射
const friendLoading = ref(false); // 好友列表加载状态
const activeFriendId = ref(null); // 当前选中聊天的好友ID
const searchQuery = ref(''); // 搜索好友的查询文本

const userId = computed(() => authStore.userId); // 当前登录用户ID
const token = computed(() => authStore.token); // 用户认证token

const messages = ref([]);
const messagesBox = ref(null);

const chatVisible = ref(false);
const activeFriend = ref(null);

// 辅助函数：从原始好友数据中获取对方好友的对象
const getFriend = (friendRelation) => {
  return friendRelation.account1.id === userId.value ? friendRelation.account2 : friendRelation.account1;
};

// 辅助函数：获取对方好友的ID
const getFriendId = (friendRelation) => {
  return getFriend(friendRelation).id;
};

// 搜索过滤后的好友列表
const filteredFriends = computed(() => {
  const query = searchQuery.value.toLowerCase().trim();
  if (!query) {
    return friends.value;
  }
  return friends.value.filter(friendItem => {
    const friendData = getFriend(friendItem);
    return friendData.username.toLowerCase().includes(query);
  });
});

// 获取单个好友信息（主要用于获取头像）
const fetchFriendInfo = async (friendId) => {
  if (friendAvatars.value[friendId]) { 
    return;
  }
  try {
    const res = await authAxios.get(`public/users/${friendId}/info`, {
      headers: { Authorization: `Bearer ${token.value}` }
    });
    if (res.data.code === 200) {
      friendAvatars.value[friendId] = res.data.data.avatarUrl || defaultAvatar;
    } else {
      console.warn(`获取好友 ${friendId} 信息失败:`, res.data.message);
    }
  } catch (error) {
    console.error(`获取好友 ${friendId} 信息失败:`, error);
  }
};

// 获取好友列表
const fetchFriends = async () => {
  friendLoading.value = true;
  try {
    const params = {
      page: 1,
      size: 100, 
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };
    const res = await authAxios.post('/friend/friends', params, {
      headers: { Authorization: `Bearer ${token.value}` }
    });
    if (res.data.code === 200) {
      friends.value = res.data.data.content;
      // 并发请求所有好友的头像
      await Promise.all(
        friends.value.map(friendItem => fetchFriendInfo(getFriendId(friendItem)))
      );
    } else {
      ElMessage.error(res.data.message || '获取好友列表失败');
    }
  } catch (error) {
    ElMessage.error('获取好友列表失败');
    console.error(error);
  } finally {
    friendLoading.value = false;
  }
};

// 选择好友（点击好友列表项）
const openChat = (friend) => {
  activeFriend.value = friend;
  chatVisible.value = true;
};

onMounted(async () => {
  await fetchFriends(); // 获取好友列表
});
</script>

<style scoped>
.chat-module-container {
  width: 100%;
  height: 100%;
  background-color: #ffffff; /* 整体背景色 */
  border-radius: 8px;
  overflow: hidden; /* 防止内容溢出 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* 保留阴影效果 */

  display: flex; 
  justify-content: center; 
  align-items: center; 
}
/* --- 好友列表样式 --- */
.friend-list-sidebar {
  width: 100%;
  height: 100%;
  flex-shrink: 0;
  background-color: #ffffff; /* 好友列表背景色 */
  border-right: none; /* 移除右侧边框，因为现在是唯一的容器了 */
  display: flex;
  flex-direction: column;
  padding: 15px;
  overflow-y: auto; /* 允许滚动 */
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.search-input {
  margin-bottom: 15px;
}

.friend-items-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex-grow: 1; /* 占据剩余空间 */
}

.friend-item {
  display: flex;
  align-items: center;
  height:55px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  background-color: #f7f7f7;
  transition: background-color 0.2s ease, transform 0.1s ease;
}

.friend-item:hover {
  background-color: #e0e0e0;
}

.friend-item.is-active {
  background-color: #a6cad5; /* 激活背景色 */
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.friend-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
  border: 1px solid #ddd; /* 给头像加个边框 */
}

.friend-username {
  font-size: 1.1em;
  font-weight: 500;
  color: #333;
  flex-grow: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #999;
}
.loading-overlay .el-icon {
  font-size: 2em;
  margin-bottom: 10px;
}

</style>