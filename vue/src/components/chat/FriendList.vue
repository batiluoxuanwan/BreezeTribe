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
          :class="['friend-item', 
                  { 'is-active': activeFriendId === getFriendId(friendItem) },
                  { 'show-delete': friendItem.showDelete } ]"
          @click="openChat(friendItem)"
          @contextmenu.prevent="toggleDeleteButton(friendItem)"
        >
          <img
            :src="friendAvatars[getFriendId(friendItem)] || defaultAvatar"
            alt="avatar"
            class="friend-avatar"
          />
          <span class="friend-username">{{ getFriend(friendItem).username }}</span>
          <div v-if="friendItem.showDelete" class="delete-button-container">
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              class="delete-button"
              @click.stop="confirmDeleteFriend(getFriendId(friendItem))"
            ></el-button>
          </div>
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
import { ElMessage, ElInput, ElButton, ElEmpty, ElIcon,ElMessageBox } from 'element-plus';
import { Search, Loading,Delete } from '@element-plus/icons-vue';
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
      size: 10, 
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };
    const res = await authAxios.post('/friend/friends', params, {
      headers: { Authorization: `Bearer ${token.value}` }
    });
    if (res.data.code === 200) {
      friends.value = res.data.data.content.map(friendItem => ({
        ...friendItem,
        showDelete: false 
      }));
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
const openChat = (friendItem) => {
  friends.value.forEach(f => {
    if (f.id !== friendItem.id) {
      f.showDelete = false;
    }
  });

  activeFriend.value = friendItem;
  activeFriendId.value = getFriendId(friendItem);
  chatVisible.value = true;
};

// 切换删除按钮的显示状态
const toggleDeleteButton = (friendItem) => {
  // 遍历所有好友，只显示当前点击好友的删除按钮，并隐藏其他好友的
  friends.value.forEach(f => {
    if (f.id === friendItem.id) {
      f.showDelete = !f.showDelete; // 切换当前好友的显示状态
    } else {
      f.showDelete = false; // 隐藏其他好友的删除按钮
    }
  });
};

// 确认删除好友
const confirmDeleteFriend = (friendId) => {
   console.log('即将删除的好友 ID:', friendId);
  ElMessageBox.confirm(
    '确定要删除这位好友吗？此操作不可逆！',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      await deleteFriend(friendId);
    })
    .catch(() => {
      ElMessage.info('已取消删除');
    });
};

// 删除好友
const deleteFriend = async (friendId) => {
  try {
    const response = await authAxios.delete(`/friend/friend/${friendId}`);

    if (response.data.code === 200) { 
      ElMessage.success('好友删除成功！');
      fetchFriends();
      friends.value = friends.value.filter(f => f.id !== friendId);
      // 如果删除的是当前活跃的聊天好友，需要重置 activeFriend
      if (activeFriendId.value === friendId) {
        activeFriend.value = null;
        activeFriendId.value = null;
        chatVisible.value = false;
      }
    } else {
      ElMessage.error('删除好友失败: ' + response.data.message); 
    }
  } catch (error) {
    ElMessage.error('删除好友异常: ' + (error.response?.data?.message || error.message));
    console.error('删除好友请求错误:', error);
  }
};

onMounted(async () => {
  await fetchFriends(); 
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
  position: relative; /* 允许子元素进行绝对定位 */
  overflow: hidden;  /* 隐藏超出边界的内容，实现滑动效果 */
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

.delete-button-container {
  position: absolute;
  right: -50px; /* 初始位置：完全在右侧外部 */
  top: 0;
  bottom: 0;
  width: 50px; /* 按钮宽度 */
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f56c6c; /* 与删除按钮颜色匹配 */
  transition: transform 0.3s ease-out; /* 滑动动画 */
  z-index: 1; /* 确保按钮在好友信息之上 */
}

.friend-item.show-delete .delete-button-container {
  transform: translateX(-50px); /* 滑入位置 */
}

.delete-button {
  box-shadow: none; /* 移除 Element Plus 按钮的默认阴影 */
}

</style>