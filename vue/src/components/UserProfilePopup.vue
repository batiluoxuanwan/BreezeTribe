<template>
  <el-dialog
    v-model="dialogVisible"
    width="500px"
    destroy-on-close
    align-center
  >
    <div v-if="loading" class="text-center py-5">
      <el-icon class="is-loading" :size="30"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    <div v-else-if="userProfile.id" class="user-profile-content">
      <div class="user-info-container">
        <img :src="userProfile.avatarUrl || defaultAvatar" alt="Avatar" class="avatar" />
        <div class="user-info-text">
          <h3 class="username">{{ userProfile.username }}</h3>
          <div class="status-info">
            <p class="role">
              {{ userProfile.role === 'ROLE_USER' ? '普通用户' : userProfile.role }}
            </p>
            <p class="status">
              <el-tag :type="userProfile.active ? 'success' : 'info'" size="small">
                {{ userProfile.active ? '在线' : '离线' }}
              </el-tag>
            </p>
          </div>
        </div>
      </div>
   
     <div class="actions" v-if="!isSelf">
        <el-button
          v-if="!isFriend"
          type="primary"
          @click="addFriend"
          :loading="addFriendLoading"
          plain
        >
          添加好友
        </el-button>
        <el-button
          v-else
          type="success"
          @click="sendMessage"
          plain
        >
          发送消息
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { authAxios } from '@/utils/request';
import defaultAvatar from '@/assets/NotFoundsonailong.jpg'; 
import { Loading } from '@element-plus/icons-vue';

// Props
const props = defineProps({
  userId: {
    type: String,
    default: null
  },
  modelValue: { // 控制弹窗显示隐藏的 prop
    type: Boolean,
    default: false
  },
  isFriend: { // 表示当前 userId 是否是登录用户的好友
    type: Boolean,
    default: false
  },
  currentUserId: { // 登录用户的ID，用于判断是否是自己
    type: String,
    default: null
  }
});

const dialogVisible = ref(props.modelValue);
const userProfile = ref({});
const loading = ref(false);
const addFriendLoading = ref(false);
const authStore = useAuthStore();
const token = authStore.token;

const emit = defineEmits(['update:modelValue',
  'friend-request-sent', 
  'send-message'
]);

const isSelf = computed(() => {
  return props.userId === props.currentUserId;
});

// 获取用于基本信息
const fetchUserProfile = async (userId) => {
  loading.value = true;
  userProfile.value = {}; // Clear previous data
  try {
    const res = await authAxios.get(`/public/users/${userId}/info`);
    if (res.data.code === 200) {
      userProfile.value = res.data.data;
    } else {
      ElMessage.error(res.data.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error('Failed to fetch user profile:', error);
    ElMessage.error('获取用户信息失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

// 发送好友请求
const addFriend = async () => {
  // 确保用户已登录
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录才能发送好友请求！');
    return;
  }
  // 确保有目标用户ID
  if (!userProfile.value.id) {
    ElMessage.error('无法获取目标用户ID，无法发送好友请求。');
    return;
  }
  // 防止重复点击
  if (addFriendLoading.value) return;

  addFriendLoading.value = true;
  try {
     const res = await authAxios.post(
      '/friend/sendRequest',
      null, 
      {
        params: { 
          toAccountId: userProfile.value.id
        },
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json', 
        },
      }
  );
    if (res.data.code === 200) {
      ElMessage.success('好友申请已发送！');
      emit('friend-request-sent', userProfile.value.id);
      dialogVisible.value = false; 
    } else {
      ElMessage.warning(res.data.message || '发送失败');
    }
  } catch (error) {
    console.error('Failed to send friend request:', error);
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录信息失效，请重新登录！');
    } else {
      ElMessage.error('发送好友请求失败，请稍后再试');
    }
  } finally {
    addFriendLoading.value = false;
  }
};

// 发消息
const sendMessage = () => {
  // 确保用户已登录
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录才能发送消息！');
    return;
  }
  // 确保有目标用户ID
  if (!userProfile.value.id) {
    ElMessage.error('无法获取目标用户ID，无法发送消息。');
    return;
  }

  //ElMessage.info(`即将与 ${userProfile.value.username} 发送消息`);
  emit('send-message', userProfile.value.id, userProfile.value.username); 
  dialogVisible.value = false; 
};

// Expose some methods if needed by parent
defineExpose({
  fetchUserProfile, // Optionally allow parent to trigger fetch
  dialogVisible // Expose dialogVisible to allow parent to control
});


watch(() => props.modelValue, (newVal) => {
  dialogVisible.value = newVal;
  if (newVal && props.userId) {
    fetchUserProfile(props.userId);
  } else if (!newVal) {
    userProfile.value = {};
  }
},{ immediate: true });

watch(dialogVisible, (newVal) => {
  emit('update:modelValue', newVal);
});
</script>

<style scoped>
.user-profile-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.user-info-container {
  display: flex;
  align-items: center; /* 让头像和文本垂直居中对齐 */
  width: 100%;
  margin-bottom: 20px;
}

.avatar {
  width: 80px; /* 适当调整头像大小 */
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #6da0b1;
  margin-right: 15px; /* 调整头像和文本的间距 */
}

.user-info-text {
  flex-grow: 1; /* 让文本区域占据剩余空间 */
  display: flex;
  flex-direction: column; /* 让用户名和状态信息垂直排列 */
}

.username {
  margin-top: 0; /* 移除默认的 margin-top */
  font-size: 1.3rem; /* 稍微调整字体大小 */
  color: #333;
  margin-bottom: 5px; /* 调整用户名和状态信息的间距 */
}

.status-info {
  display: flex; /* 让身份和状态水平排列 */
  align-items: center; /* 让身份和状态垂直居中对齐 */
}

.role {
  margin-right: 10px; /* 调整身份和状态标签的间距 */
  margin-bottom: 0; /* 移除默认的 margin-bottom */
  font-size: 0.9rem;
  color: #555;
}

.status {
  margin-bottom: 0; /* 移除默认的 margin-bottom */
  font-size: 0.9rem;
  color: #555;
}

.details {
  display: none; /* 隐藏原来的 details 区域 */
}

.details p {
  margin-bottom: 8px;
  font-size: 0.95rem;
  color: #555;
}

.details strong {
  color: #333;
}

.actions {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 20px; /* 调整 actions 和上方内容的间距 */
}
</style>