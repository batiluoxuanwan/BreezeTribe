<template>
  <div class="chat-container">
    <h2>私聊聊天</h2>

    <!-- 打印ID -->
    <p>当前用户ID: {{ currentUserId }}</p>
    <p>聊天对象ID: {{ friendId }}</p>

    <div class="messages" ref="messagesBox">
      <div v-for="msg in messages" :key="msg.timestamp" :class="{'mine': msg.from === currentUserId, 'other': msg.from !== currentUserId}">
        <span class="sender">{{ msg.from === currentUserId ? '你' : '对方' }}：</span>
        <span class="content">{{ msg.content }}</span>
      </div>
    </div>
    <input v-model="newMessage" @keyup.enter="send" placeholder="输入消息，回车发送..." />
    <button @click="send">发送</button>
  </div>
</template>

<script setup>
import {ref, onMounted, nextTick, computed} from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRoute } from 'vue-router';
import { connectWebSocket, sendMessage } from '@/utils/websocket.js';

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.userId);
const route = useRoute();
const friendId = route.params.friendId;

console.log('当前用户ID:', currentUserId);
console.log('聊天对象ID:', friendId);

const messages = ref([]);
const newMessage = ref('');
const messagesBox = ref(null);

onMounted(() => {
  connectWebSocket((msg) => {
    messages.value.push(msg);
    scrollToBottom();
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
</script>
