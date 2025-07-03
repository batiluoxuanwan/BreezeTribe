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
          @click="selectFriend(friendItem)"
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

    <div class="chat-window-content">
      <div v-if="activeFriendId">
        <div class="chat-header">
          <span class="chat-title">{{ getFriendName(activeFriendId) }}</span>
          </div>

        <div class="message-list" ref="messageListRef">
          <div v-for="message in chatMessages" :key="message.id"
               :class="['message-item', { 'message-self': message.senderId === userId }]">
            <img
              :src="getSenderAvatar(message.senderId)"
              alt="avatar"
              class="message-avatar"
            />
            <div class="message-bubble">
              <p class="message-content">{{ message.content }}</p>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
          </div>
          <el-empty v-if="chatMessages.length === 0" description="暂无消息" :image-size="80"></el-empty>
        </div>

        <div class="message-input-area">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            @keyup.enter.native="sendMessage"
          ></el-input>
          <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()">发送</el-button>
        </div>
      </div>
      <div v-else class="no-chat-selected">
        <el-empty description="选择一个好友开始聊天" :image-size="150"></el-empty>
      </div>
    </div>
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

// 辅助函数：从原始好友数据中获取对方好友的对象
const getFriend = (friendRelation) => {
  return friendRelation.account1.id === userId.value ? friendRelation.account2 : friendRelation.account1;
};

// 辅助函数：获取对方好友的ID
const getFriendId = (friendRelation) => {
  return getFriend(friendRelation).id;
};

// 辅助函数：获取对方好友的用户名
const getFriendName = (friendId) => {
  const friendRelation = friends.value.find(f => getFriendId(f) === friendId);
  return friendRelation ? getFriend(friendRelation).username : '未知用户';
};

// 辅助函数：获取发送者头像
const getSenderAvatar = (senderId) => {
  if (senderId === userId.value) {
    return authStore.userAvatarUrl || defaultAvatar; 
  }
  return friendAvatars.value[senderId] || defaultAvatar;
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
const selectFriend = async (friendItem) => { 
  const newFriendId = getFriendId(friendItem);
  if (activeFriendId.value !== newFriendId) {
    activeFriendId.value = newFriendId;
    messages.value = []; 
    await loadChatHistory(newFriendId); // 调用实际的加载历史消息函数
    scrollToBottom(); // 加载完消息后，滚动到底部
  }
};

const loadChatHistory = async (targetFriendId) => {
  try {
    const res = await authAxios.get(`/messages/${targetFriendId}`, {
      headers: { Authorization: `Bearer ${token.value}` }
    });
    if (res.data.code === 200) {
      messages.value = res.data.data.map(m => ({ 
        from: m.fromAccountId, 
        to: m.toAccountId,     
        content: m.content,
        timestamp: m.timestamp
      }));
      scrollToBottom();
    } else {
      ElMessage.error(res.data.message || '获取聊天记录失败');
    }
  } catch (error) {
    ElMessage.error('获取聊天记录失败');
    console.error(error);
  }
};

// connectWebSocket(token,(msg) => {
//     // 只接收当前聊天对象相关的消息
//     console.log('✅ 收到消息:', msg);
//     if (
//         (msg.from === currentUserId.value && msg.to === friendId) || // msg.from 是当前用户 && msg.to 是当前聊天对象
//         (msg.from === friendId && msg.to === currentUserId.value)   // msg.from 是当前聊天对象 && msg.to 是当前用户
//     ) {
//       messages.value.push(msg); // 将相关消息添加到消息列表中
//       scrollToBottom();         // 滚动到最新消息
//     }
// });

function scrollToBottom() {
  nextTick(() => {
    if (messagesBox.value) {
      messagesBox.value.scrollTop = messagesBox.value.scrollHeight;
    }
  });
}

// --- 聊天窗口相关数据和逻辑 ---
const messageListRef = ref(null); // 用于滚动到底部的引用
const chatMessages = ref([]); // 当前聊天窗口的消息列表
const newMessage = ref(''); // 新消息输入框内容

// 格式化时间戳
const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString();
};


// 发送消息
const sendMessage = () => {
  if (newMessage.value.trim() && activeFriendId.value) {
    const messageToSend = {
      id: Date.now(), // 实际应由后端生成
      senderId: userId.value,
      content: newMessage.value.trim(),
      timestamp: Date.now(),
    };
    chatMessages.value.push(messageToSend);
    newMessage.value = '';

    // 滚动到底部
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
      }
    });

    // 实际应将消息发送到后端 WebSocket 或 API
    console.log('发送消息:', messageToSend);
    // 模拟对方回复
    setTimeout(() => {
        chatMessages.value.push({
            id: Date.now(),
            senderId: activeFriendId.value,
            content: `[自动回复] 已收到你的消息: "${messageToSend.content}"`,
            timestamp: Date.now() + 1000
        });
         nextTick(() => {
            if (messageListRef.value) {
                messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
            }
        });
    }, 1000);
  }
};


onMounted(async () => {
  await fetchFriends(); // 获取好友列表
});

// 监听 activeFriendId 变化，加载对应聊天记录
// watch(activeFriendId, (newId) => {
//   if (newId) {
//     loadChatHistory(newId); // 实际的加载逻辑
//   }
// });
</script>

<style scoped>
.chat-module-container {
  height: 100%; 
  width: 100%;
  display: flex;
  height: calc(100vh - 60px); /* 假设顶部有导航栏，减去其高度 */
  background-color: #f0f2f5; /* 整体背景色，类似微信/QQ */
  border-radius: 8px;
  overflow: hidden; /* 防止内容溢出 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* --- 左侧好友列表样式 --- */
.friend-list-sidebar {
  flex-shrink: 0;
  width: 200px; /* 宽度 */
  background-color: #e6e6e6; /* 好友列表背景色 */
  border-right: 1px solid #ccc;
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


/* --- 右侧聊天窗口样式 --- */
.chat-window-content {
  flex-grow: 1; /* 占据剩余所有空间 */
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.no-chat-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px; /* 固定头部高度 */
}

.chat-title {
  font-size: 1.3em;
  font-weight: bold;
  color: #333;
}

.message-list {
  flex-grow: 1; /* 消息列表占据大部分空间 */
  overflow-y: auto; /* 允许消息滚动 */
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px; /* 消息间距 */
  background-color: #f0f2f5; /* 聊天背景 */
}

.message-item {
  display: flex;
  align-items: flex-start; /* 消息内容顶部对齐 */
  gap: 10px;
}

.message-item.message-self {
  justify-content: flex-end; /* 自己发的消息靠右 */
  flex-direction: row-reverse; /* 头像在右侧 */
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0; /* 防止头像被压缩 */
}

.message-bubble {
  max-width: 70%; /* 消息气泡最大宽度 */
  padding: 10px 15px;
  border-radius: 10px;
  position: relative;
  word-wrap: break-word; /* 自动换行 */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-item:not(.message-self) .message-bubble {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  color: #333;
}

.message-item.message-self .message-bubble {
  background-color: #007bff; /* 蓝色气泡 */
  color: #fff;
  border: 1px solid #007bff;
}

.message-content {
  margin: 0;
  font-size: 1em;
  line-height: 1.4;
}

.message-time {
  display: block;
  font-size: 0.75em;
  color: #999;
  margin-top: 5px;
  text-align: right;
  /* 调整时间颜色以适应气泡背景 */
  .message-item.message-self & {
    color: rgba(255, 255, 255, 0.7);
  }
}

.message-input-area {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 覆盖 Element Plus textarea 默认样式，使其回车不发送，Ctrl+Enter发送 */
.message-input-area .el-textarea__inner {
  resize: none; /* 禁止手动调整大小 */
  padding-right: 80px; /* 为发送按钮留出空间 */
}
.message-input-area .el-button {
  align-self: flex-end; /* 按钮靠右对齐 */
  width: 80px; /* 固定按钮宽度 */
}
</style>