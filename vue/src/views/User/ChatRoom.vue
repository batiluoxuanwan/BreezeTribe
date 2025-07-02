<template>
  <div class="flex flex-col h-screen bg-gray-50 px-4 md:px-8 pt-4">
    <!-- 顶部状态栏 -->
    <div class="flex items-center gap-4 mb-4">
      <button @click="goBack" class="text-blue-600 text-3xl p-2 hover:text-blue-800 transition">←</button>
      <img :src="friend.avatarUrl" class="w-10 h-10 rounded-full shadow" />
      <div class="flex flex-col">
        <span class="font-semibold text-lg">{{ friend.username }}</span>
        <span class="text-sm text-green-500">● 在线</span>
      </div>
    </div>
    <!-- 聊天内容 -->
    <div ref="messagesBox" class="flex-1 overflow-y-auto flex flex-col gap-4 pb-4">
      <div
          v-for="(msg, i) in messages"
          :key="i"
          class="flex items-end gap-2 mb-3"
          :class="{ 'justify-end': msg.from === currentUserId, 'justify-start': msg.from !== currentUserId }"
      >
        <!-- 对方消息 -->
        <template v-if="msg.from !== currentUserId">
          <img :src="friend.avatarUrl||defaultAvatar" class="w-9 h-9 rounded-full shadow" />
          <div class="bg-white text-black px-4 py-3 rounded-2xl rounded-bl-none shadow max-w-[70%]">
            {{ msg.content }}
          </div>
        </template>

        <!-- 自己消息 -->
        <template v-else>
          <div class="bg-blue-500 text-white px-4 py-3 rounded-2xl rounded-br-none shadow max-w-[70%]">
            {{ msg.content }}
          </div>
          <img :src="user.avatarUrl||defaultAvatar" class="w-9 h-9 rounded-full shadow" />
        </template>
      </div>
    </div>

    <!-- 底部输入框 -->
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
</template>

<script setup>
import {ref, onMounted, nextTick, computed, reactive} from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRoute,useRouter } from 'vue-router';
import { connectWebSocket, sendMessage } from '@/utils/websocket.js';
// import axios from 'axios';
// import { authAxios } from '@/utils/request'
import {authAxios} from "@/utils/request.js";
import {ElMessage} from "element-plus";
import defaultAvatar from '@/assets/NotFoundsonailong.jpg';


import ChatHeader from '@/components/chat/ChatHeader.vue'
import ChatMessage from '@/components/chat/ChatMessage.vue'



const authStore = useAuthStore();
const currentUserId = computed(() => authStore.userId);
// 在 setup() 里定义响应式对象
const user = reactive({
  id: '',
  username: '',
  avatarUrl: '',
  role: '',
  active: '',
});
//const friend = reactive({});       // 聊天对象
const isLoading = ref(true);       // 控制页面加载状态
const route = useRoute();
const router = useRouter();
const friendId = route.params.friendId;
const friend = reactive({
  id: '',
  username: '',
  avatarUrl: '',
  role: '',
  active: true,
});
const token = authStore.token;

console.log('当前用户ID:', currentUserId.value);
console.log('聊天对象ID:', friendId);

const messages = ref([]);
const newMessage = ref('');
const messagesBox = ref(null);

const fetchCurrentUser = async () => {
  try {
    const res = await authAxios.get('/auth/me',{
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      Object.assign(user, res.data.data);
      console.log('✅✅✅✅✅✅✅✅✅✅✅✅当前用户:', user);
      console.log('聊天对象头像:', user.avatarUrl);
    } else {
      ElMessage.error(res.data.message || '获取当前用户失败');
    }
  } catch (error) {
    ElMessage.error('获取当前用户失败');
    console.error(error);
  }
};

const fetchFriendInfo = async (friendId) => {
  try {
    //@Operation(summary = "获取指定用户发的基础信息")
    //@GetMapping("/users/{userId}/info")
    const res = await authAxios.get(`public/users/${friendId}/info`,{
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      Object.assign(friend, res.data.data);
      console.log('✅✅✅✅✅✅✅✅✅✅✅✅好友信息:', friend);
      console.log('聊天对象头像:', user.avatarUrl);
    } else {
      ElMessage.error(res.data.message || '获取好友信息失败');
    }
  } catch (error) {
    ElMessage.error('获取好友信息失败');
    console.error(error);
  }
};

onMounted(async () => {

  isLoading.value = true;
  await fetchCurrentUser();
  await fetchFriendInfo(friendId);
  isLoading.value = false;

  // 1. 拉取历史消息
  const res = await authAxios.get('/messages/' + friendId, {
    headers: { Authorization: `Bearer ${token}` }
  });
  console.log('token:', token);
  console.log(res.data);
  messages.value = res.data.map(m => ({
    from: m.fromAccountId,
    to: m.toAccountId,
    content: m.content,
    timestamp: m.timestamp
  }));

  // 2. 启动 WebSocket
  connectWebSocket(token,(msg) => {
    // 只接收当前聊天对象相关的消息
    console.log('🚀 收到消息:', msg);
    if (
        (msg.from === currentUserId.value && msg.to === friendId) ||
        (msg.from === friendId && msg.to === currentUserId.value)
    ) {
      messages.value.push(msg);
      scrollToBottom();
    }
  });
});

function send() {
  if (newMessage.value.trim() === '') return;

  const msg = {
    from: currentUserId.value,   // 注意这里要加 .value，获取原始数据
    to: friendId,
    content: newMessage.value.trim(),
    timestamp: new Date().toISOString()
  };

  sendMessage(msg);
  messages.value.push(msg);
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

const goBack = () => {
  router.push('/user/friends');
};

</script>


