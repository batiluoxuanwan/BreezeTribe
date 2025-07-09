<template>
  <div class="add-friend">
    <!-- 顶部标题栏 -->
    <div class="title">
      <h2>添加好友</h2>
    </div>

    <!-- 搜索框 -->
    <div class="flex items-center gap-3 mb-6">
      <el-input
        v-model="searchKeyword"
        @keyup.enter="searchUsers"
        placeholder="搜索用户名..."
        clearable
        :prefix-icon="Search"
        class="flex-1"
        size="large"
      />
      <el-button type="primary" size="large" @click="searchUsers">
        搜索
      </el-button>
    </div>

  <!-- 搜索结果 -->
  <ul class="search-results">
    <li
      v-for="user in searchResults"
      :key="user.id"
      class="search-result-item"
    >
      <div class="user-info">
        <img
          :src="user.avatarUrl || defaultAvatar"
          alt="avatar"
          class="user-avatar"
        />
        <span class="user-name">{{ user.username }}</span>
      </div>
      <button
        @click="sendFriendRequest(user.id)"
        class="add-button"
      >
        <el-icon><Plus /></el-icon>
      </button>
    </li>
  </ul>

    <!-- 空状态 -->
    <div v-if="searchResults.length === 0 && searched" class="text-center text-gray-500 mt-10">
      没有找到匹配的用户
    </div>

  
   <div class="sent-requests-section"> <div class="section-header" @click="toggleExpand">
        <span class="title-text">我发送的好友请求</span>
        <el-icon :class="{ rotated: isExpanded }"><ArrowRight /></el-icon>
      </div>

      <el-collapse-transition>
        <div v-show="isExpanded">
          <ul v-if="sentRequests.length > 0" class="request-list">
            <li
              v-for="item in sentRequests"
              :key="item.id"
              class="request-item"
            >
              <img :src="item.to.avatarUrl || defaultAvatar" class="avatar" />
              <span class="username">{{ item.to.username }}</span>
              <el-tag
                :type="item.status === 'PENDING' ? 'warning' : item.status === 'ACCEPTED' ? 'success' : 'info'"
              >
                {{
                  item.status === 'PENDING'
                    ? '等待验证'
                    : item.status === 'ACCEPTED'
                    ? '已通过'
                    : '已拒绝'
                }}
              </el-tag>
            </li>
          </ul>
          <div v-else class="empty-requests-message">
            暂无发送的请求
          </div>
        </div>
      </el-collapse-transition>
    </div>

    <div class="received-requests-section">
      <div class="section-header" @click="toggleExpandReceived">
        <span class="title-text">我收到的好友请求</span>
        <el-icon :class="{ rotated: isExpandedReceived }"><ArrowRight /></el-icon>
      </div>

      <el-collapse-transition>
        <div v-show="isExpandedReceived">
          <ul v-if="receivedRequests.length > 0" class="request-list">
            <li
              v-for="item in receivedRequests"
              :key="item.id"
              class="request-item"
            >
              <img :src="item.from.avatarUrl || defaultAvatar" class="avatar" />
              <span class="username">{{ item.from.username }}</span>
              <div class="actions">
                <el-button type="success" size="small" @click="acceptFriendRequest(item.id)">接受</el-button>
                <el-button type="danger" size="small" @click="rejectFriendRequest(item.id)">拒绝</el-button>
              </div>
            </li>
          </ul>
          <div v-else class="empty-requests-message">
            暂无收到的请求
          </div>
        </div>
      </el-collapse-transition>
    </div>
  </div>
</template>

<script setup>
import { ref ,onMounted } from 'vue';
//import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { authAxios } from '@/utils/request';
import defaultAvatar from '@/assets/default.jpg';
import { ElMessage } from 'element-plus';
import { Search ,Plus ,ArrowRight } from '@element-plus/icons-vue';


// 1. 路由与状态
//const router = useRouter();
const authStore = useAuthStore();
const token = authStore.token;

// 2. 搜索相关状态
const searchKeyword = ref('');
const searchResults = ref([]);
const searched = ref(false);

const isExpanded = ref(false);
const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};

// 搜索用户（根据关键词）
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

// 发送好友申请
async function sendFriendRequest(friendId) {
  try {
    const res = await authAxios.post(
      '/friend/sendRequest',
      null, 
      {
        params: { 
          toAccountId: friendId
        },
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json', 
        },
      }
  );

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

const sentRequests = ref([]);
const loading = ref(false);

const fetchSentRequests = async () => {
  loading.value = true;
  try {
    const res = await authAxios.post(
      '/friend/sentRequests',
      {
        page: 1,
        size: 10,
        sortBy: 'createdTime',
        sortDirection: 'DESC'
      },
      {
        headers: {
          Authorization: `Bearer ${token}`,
           'Content-Type': 'application/json'
        }
      }
    );

    if (res.data.code === 200) {
      sentRequests.value = res.data.data.content;
    } else {
      ElMessage.warning(res.data.message || '获取好友请求失败');
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('请求发送失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

// 我收到的请求
const isExpandedReceived = ref(false); 
const toggleExpandReceived = () => { 
  isExpandedReceived.value = !isExpandedReceived.value;
};
const receivedRequests = ref([]); 
const fetchReceivedRequests = async () => { 
  try {
    const res = await authAxios.post(
      '/friend/receivedRequests', 
      {
        page: 1,
        size: 10,
        sortBy: 'createdAt',
        sortDirection: 'DESC'
      },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (res.data.code === 200) {
      receivedRequests.value = res.data.data.content.filter(req => req.status === 'PENDING');
    } else {
      ElMessage.warning(res.data.message || '获取收到的好友请求失败');
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('获取收到请求失败，请稍后再试');
  }
};

// 接受好友请求
const acceptFriendRequest = async (requestId) => {
  if (!requestId) {
    ElMessage.error('无法接受好友请求：缺少请求ID。');
    console.error('acceptFriendRequest: requestId is missing.');
    return;
  }

  try {
    const res = await authAxios.post(
      '/friend/acceptRequest',
      null, 
      {
        params: { 
          requestId: requestId
        },
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json' 
        }
      }
    );

    console.log('接受好友请求API响应:', res); 

    if (res.data && res.data.code === 200) {
      //ElMessage.success('已接受好友请求！');
      fetchReceivedRequests();
    } else {
      ElMessage.warning(res.data.message || '接受请求失败');
    }
  } catch (error) {
    console.error('接受请求失败:', error);
    if (error.response) {
      console.error('错误响应数据:', error.response.data);
      console.error('错误响应状态码:', error.response.status);
      ElMessage.error(`接受请求失败: ${error.response.data.message || error.response.statusText}`);
    } else if (error.request) {
      console.error('未收到服务器响应:', error.request);
      ElMessage.error('无法连接到服务器，请检查网络。');
    } else {
      console.error('请求设置时出错:', error.message);
      ElMessage.error('发送请求时发生未知错误。');
    }
  }
};

// 拒绝好友请求
const rejectFriendRequest = async (requestId) => {
  try {
    const res = await authAxios.post(
      '/friend/rejectRequest',
      null, 
      {
        params: { 
          requestId: requestId
        },
        headers: {
          Authorization: `Bearer ${token}`, 
          'Content-Type': 'application/json' 
        }
      }
    ); if (res.data.code === 200) {
      //ElMessage.success('已拒绝好友请求！');
      fetchReceivedRequests(); 
    } else {
      if (res.data.code === 400 && res.data.message.includes("找不到")) {
          ElMessage.warning('请求不存在或已处理。');
      } else {
          ElMessage.warning(res.data.message || '拒绝请求失败');
      }
    }
  } catch (error) {
    console.error('拒绝请求时发生错误:', error);
    if (error.response) {
      if (error.response.status === 400) {
        ElMessage.error(error.response.data.message || '请求无效，请检查ID。');
      } else if (error.response.status === 401) {
        ElMessage.error('未授权，请登录。');
      } else {
        ElMessage.error('网络或服务器错误，请稍后再试。');
      }
    } else {
      ElMessage.error('拒绝请求失败，请检查网络连接。');
    }
  }
};

onMounted(async () => {
  fetchSentRequests();
  fetchReceivedRequests();
});
</script>

<style scoped>
.add-friend {
  height: 100%;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  padding: 0;         /* 移除边距 */
  margin: 0;
  border-radius: 0;   /* 去掉圆角 */
  overflow-y: auto;
}

.title {
  margin: 16px 0;           /* 上下间距 */
  padding: 0 16px;           /* 左右内边距 */
}

.title h2 {
  font-size: 1.75rem;        /* 字号小 */
  font-weight: 600;          /* 字体加粗 */
  color: #333;               /* 深灰色字体 */
  margin: 0;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 1rem; /* 等效于 space-y-4 */
  margin-top: 24px;
}

.search-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #f3f4f6; 
  border-radius: 0.5rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: background-color 0.3s;
}

.search-result-item:hover {
  background-color: #e5e7eb; 
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 9999px;
  margin-right: 1rem;
  object-fit: cover;
}

.user-name {
  font-size: 1.125rem;
  font-weight: 500;
  color: #1f2937; 
}

.add-button {
  font-size: 1.5rem;
  color: #62b3b7; 
  transition: color 0.3s;
  background: none;
  border: none;
  cursor: pointer;
}

.add-button:hover {
  color: #1e40af; 
}

.sent-requests-section{
  margin-top: 20px; /* 与上方的距离 */
  border-top: 1px solid #eee; /* 上边线 */
  padding-top: 15px; /* 上边线与标题的距离 */
  padding-bottom: 15px; /* 标题与列表的距离，或整个section底部内边距 */
  margin-bottom: 0px; /* 整个 section 下方的外边距 */
}

.received-requests-section {
  margin-top: 0px; /* 与上方的距离 */
  padding-bottom: 15px; /* 标题与列表的距离，或整个section底部内边距 */
  margin-bottom: 20px; /* 整个 section 下方的外边距 */
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.125rem;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  padding: 0 0 10px 0; /* 标题底部内边距 */
  transition: background-color 0.3s;
  border-bottom: 1px solid #eee; /* 标题下方的分隔线 */
  margin-bottom: 10px; /* 标题下边线与列表的距离 */
}

.section-header .title-text { /* 确保标题文字有自己的类名，避免与全局title冲突 */
  flex-grow: 1;
}

.section-header:hover {
  background-color: transparent; /* 保持透明，如果想有hover效果可以在这里加 */
}

.section-header .el-icon {
  transition: transform 0.3s ease;
}

.section-header .rotated {
  transform: rotate(90deg);
}

.request-list {
  padding: 0; /* 移除 ElCard 带来的默认内边距 */
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border-bottom: 1px solid #eee; /* 列表底部的分隔线 */
  padding-bottom: 15px; /* 列表内容与底部线的距离 */
}

.request-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 5px 0; /* 列表项的上下内边距 */
}

.request-item .avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.request-item .username {
  font-weight: 500;
  color: #333;
  flex: 1;
}

.request-item .actions {
  display: flex;
  gap: 8px; /* 按钮之间的间距 */
}

.empty-requests-message {
  text-align: center;
  color: #999;
  padding: 15px 0;
  font-size: 0.95rem;
}
</style>
