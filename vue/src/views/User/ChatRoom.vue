<template>
  <el-dialog
    :model-value="visible"
    @close="$emit('update:visible', false)"
    draggable
    width="600px"
    top="10vh"
    :close-on-click-modal="false"
  >
    <div class="flex flex-col h-screen bg-gray-50 px-4 md:px-8 pt-4">
      <div class="flex items-center gap-4 mb-4">
        <img :src="friend.avatarUrl || defaultAvatar" class="w-10 h-10 rounded-full shadow" />
        <div class="flex flex-col">
          <span class="font-semibold text-lg">{{ friend.username }}</span>
          <span class="text-sm text-green-500">● 在线</span>
        </div>
      </div>
      <div ref="messagesBox" class="flex-1 overflow-y-auto flex flex-col gap-4 pb-4">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="flex items-end gap-2 mb-3"
          :class="{ 'justify-end': msg.from === currentUserId, 'justify-start': msg.from !== currentUserId }"
        >
          <template v-if="msg.from !== currentUserId">
            <img :src="friend.avatarUrl || defaultAvatar" class="w-9 h-9 rounded-full shadow" />
            <div class="bg-white text-black px-4 py-3 rounded-2xl rounded-bl-none shadow max-w-[70%]">
              {{ msg.content }}
            </div>
          </template>

          <template v-else>
            <div class="bg-blue-500 text-white px-4 py-3 rounded-2xl rounded-br-none shadow max-w-[70%]">
              {{ msg.content }}
            </div>
            <img :src="user.avatarUrl || defaultAvatar" class="w-9 h-9 rounded-full shadow" />
          </template>
        </div>
      </div>

      <div class="flex items-center py-4 bg-gray-50 border-t mt-2">
        <input
          v-model="newMessage"
          placeholder="请输入消息..."
          @keydown.enter="send"
          class="flex-1 border border-gray-300 rounded-full px-5 py-3 mr-4 focus:outline-none focus:ring-2 focus:ring-blue-400 shadow-sm"
        />
        <button
          class="bg-blue-500 text-white px-6 py-3 rounded-full shadow hover:bg-blue-600 transition"
          @click="send"
        >
          发送
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, reactive, watch } from 'vue'; // 引入 watch
import { useAuthStore } from '@/stores/auth';
import { useRoute, useRouter } from 'vue-router';
import { connectWebSocket, sendMessage } from '@/utils/websocket.js';
import { authAxios } from "@/utils/request.js";
import { ElMessage } from "element-plus";
import defaultAvatar from '@/assets/NotFoundsonailong.jpg';
import { useChatStore } from '@/stores/chatStore';

// 由于 ChatHeader 和 ChatMessage 组件在模板中未被直接使用，可以移除导入。
// import ChatHeader from '@/components/chat/ChatHeader.vue';
// import ChatMessage from '@/components/chat/ChatMessage.vue';

const props = defineProps({
  friendId: String,
  visible: Boolean
});

const emit = defineEmits(['update:visible']); // 声明 emit

const authStore = useAuthStore();
const chatStore = useChatStore();
const currentUserId = computed(() => authStore.userId);

const user = reactive({
  id: '',
  username: '',
  avatarUrl: '',
  role: '',
  active: '',
});

const friend = reactive({
  id: '',
  username: '',
  avatarUrl: '',
  role: '',
  active: true,
});

const isLoading = ref(true);
const token = authStore.token;

const messages = computed(() => chatStore.getMessages(props.friendId));
const newMessage = ref('');
const messagesBox = ref(null);


// --- 新增：监听 friendId 变化并加载数据 ---
watch(() => props.friendId, async (newFriendId, oldFriendId) => {
  if (newFriendId && newFriendId !== oldFriendId) { // 确保 friendId 有效且确实发生了变化
    console.log(`Friend ID changed from ${oldFriendId} to ${newFriendId}`);
    isLoading.value = true;
    await fetchFriendInfo(newFriendId); // 重新获取好友信息
    await fetchHistoryMessages(newFriendId); // 重新获取历史消息
    isLoading.value = false;
    scrollToBottom(); // 滚动到底部
  }
}, { immediate: true }); // immediate: true 确保组件初次渲染时也会执行一次

// 优化：封装历史消息拉取逻辑
const fetchHistoryMessages = async (targetFriendId) => {
  try {
    const res = await authAxios.get('/messages/' + targetFriendId, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log(`Fetched history for friend ${targetFriendId}:`, res.data);
    const history = res.data.map(m => ({
      from: m.fromAccountId,
      to: m.toAccountId,
      content: m.content,
      timestamp: m.timestamp
    }));
    chatStore.setHistory(targetFriendId, history);
  } catch (error) {
    ElMessage.error('获取历史消息失败');
    console.error('获取历史消息失败:', error);
    chatStore.setHistory(targetFriendId, []); // 失败时清空或设置为默认空数组
  }
};


const fetchCurrentUser = async () => {
  try {
    const res = await authAxios.get('/auth/me', {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      Object.assign(user, res.data.data);
      console.log('✅✅✅✅✅✅✅✅✅✅✅✅当前用户:', user);
    } else {
      ElMessage.error(res.data.message || '获取当前用户失败');
    }
  } catch (error) {
    ElMessage.error('获取当前用户失败');
    console.error(error);
  }
};

const fetchFriendInfo = async (friendIdToFetch) => { // 接受参数 friendIdToFetch
  try {
    const res = await authAxios.get(`public/users/${friendIdToFetch}/info`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      Object.assign(friend, res.data.data);
      console.log('✅✅✅✅✅✅✅✅✅✅✅✅好友信息:', friend);
    } else {
      ElMessage.error(res.data.message || '获取好友信息失败');
      // 如果获取失败，清空或重置 friend 信息，避免显示旧数据
      Object.assign(friend, {
        id: friendIdToFetch,
        username: '未知用户',
        avatarUrl: defaultAvatar,
        role: '',
        active: false,
      });
    }
  } catch (error) {
    ElMessage.error('获取好友信息失败');
    console.error(error);
    // 如果获取失败，清空或重置 friend 信息，避免显示旧数据
    Object.assign(friend, {
      id: friendIdToFetch,
      username: '未知用户',
      avatarUrl: defaultAvatar,
      role: '',
      active: false,
    });
  }
};

onMounted(async () => {
  // 只在挂载时获取当前用户信息
  await fetchCurrentUser();
  // friendInfo 和历史消息会在 watch 中根据 props.friendId 首次触发并加载
  // 确保 WebSocket 连接只建立一次
  connectWebSocket(authStore.token, chatStore); 
});

function send() {
  if (!newMessage.value.trim()) return;

  const msg = {
    from: currentUserId.value,
    to: props.friendId,
    content: newMessage.value.trim(),
    timestamp: new Date().toISOString()
  };

  sendMessage(msg);
  chatStore.addMessage(msg);
  newMessage.value = '';
  scrollToBottom();
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesBox.value) {
      messagesBox.value.scrollTop = messagesBox.value.scrollHeight;
    }
  });
}
</script>

<style scoped>
/* 确保这些 Tailwind CSS 类在你的项目中已配置并正常工作 */
.h-screen { height: 100vh; }
.bg-gray-50 { background-color: #f9fafb; }
.px-4 { padding-left: 1rem; padding-right: 1rem; }
.md\:px-8 { /* for medium screens and up */ padding-left: 2rem; padding-right: 2rem; }
.pt-4 { padding-top: 1rem; }
.flex { display: flex; }
.flex-col { flex-direction: column; }
.items-center { align-items: center; }
.gap-4 { gap: 1rem; }
.mb-4 { margin-bottom: 1rem; }
.w-10 { width: 2.5rem; }
.h-10 { height: 2.5rem; }
.rounded-full { border-radius: 9999px; }
.shadow { box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1); }
.font-semibold { font-weight: 600; }
.text-lg { font-size: 1.125rem; line-height: 1.75rem; }
.text-sm { font-size: 0.875rem; line-height: 1.25rem; }
.text-green-500 { color: #22c55e; }
.flex-1 { flex: 1 1 0%; }
.overflow-y-auto { overflow-y: auto; }
.pb-4 { padding-bottom: 1rem; }
.gap-2 { gap: 0.5rem; }
.mb-3 { margin-bottom: 0.75rem; }
.justify-end { justify-content: flex-end; }
.justify-start { justify-content: flex-start; }
.w-9 { width: 2.25rem; }
.h-9 { height: 2.25rem; }
.bg-white { background-color: #fff; }
.text-black { color: #000; }
.px-4 { padding-left: 1rem; padding-right: 1rem; }
.py-3 { padding-top: 0.75rem; padding-bottom: 0.75rem; }
.rounded-2xl { border-radius: 1rem; }
.rounded-bl-none { border-bottom-left-radius: 0px; }
.max-w-\[70\%\] { max-width: 70%; } /* Custom utility, ensure it works with your Tailwind config */
.bg-blue-500 { background-color: #3b82f6; }
.text-white { color: #fff; }
.rounded-br-none { border-bottom-right-radius: 0px; }
.border-t { border-top-width: 1px; border-color: #e5e7eb; }
.mt-2 { margin-top: 0.5rem; }
.border { border-width: 1px; }
.border-gray-300 { border-color: #d1d5db; }
.rounded-full { border-radius: 9999px; }
.px-5 { padding-left: 1.25rem; padding-right: 1.25rem; }
.py-3 { padding-top: 0.75rem; padding-bottom: 0.75rem; }
.mr-4 { margin-right: 1rem; }
.focus\:outline-none { outline: 2px solid transparent; outline-offset: 2px; }
.focus\:ring-2 { --tw-ring-offset-width: 0px; --tw-ring-offset-color: #fff; --tw-ring-color: #3b82f6; ring-width: 2px; }
.focus\:ring-blue-400 { --tw-ring-opacity: 1; ring-color: rgb(96 165 250 / var(--tw-ring-opacity)); }
.hover\:bg-blue-600:hover { background-color: #2563eb; }
.transition { transition-property: color, background-color, border-color, text-decoration-color, fill, stroke, opacity, box-shadow, transform, filter, backdrop-filter; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1); transition-duration: 150ms; }

/* Custom styles for the dialog's content area to ensure h-screen works */
.el-dialog__body {
  padding: 0 !important; /* Remove default padding of El-dialog body */
  height: calc(100vh - 10vh - 54px - 16px); /* Adjust based on dialog top and header height if any, 54px for default header height, 16px for potential default padding */
  display: flex;
  flex-direction: column;
}
</style>