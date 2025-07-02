<script>
export default {
  name: "ChatMessage"
}
</script>

<template>
  <div
      class="flex items-end"
      :class="{ 'justify-end': isSelf, 'justify-start': !isSelf }"
  >
    <!-- 对方消息 -->
    <template v-if="!isSelf">
      <img :src="avatar" class="w-9 h-9 rounded-full mr-3 shadow" />
      <div
          class="bg-white text-black px-4 py-3 rounded-2xl rounded-bl-none shadow max-w-[70%]"
      >
        {{ msg.content }}
      </div>
    </template>

    <!-- 自己消息 -->
    <template v-else>
      <div
          class="bg-gradient-to-br from-blue-500 to-blue-600 text-white px-4 py-3 rounded-2xl rounded-br-none shadow max-w-[70%]"
      >
        {{ msg.content }}
      </div>
      <img :src="avatar" class="w-9 h-9 rounded-full ml-3 shadow" />
    </template>
  </div>
</template>

<script setup>
defineProps({
  msg: {
    type: Object,
    required: true
  },
  isSelf: {
    type: Boolean,
    required: true
  },
  avatar: {
    type: String,
    required: true
  }
});
</script>
