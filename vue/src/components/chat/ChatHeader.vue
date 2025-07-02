<script>
export default {
  name: "ChatHeader"
}
</script>

<template>
  <div class="flex items-center mb-4">
    <button
        class="text-blue-600 text-3xl p-2 mr-4 hover:text-blue-800 transition"
        @click="$emit('goBack')"
    >
      ←
    </button>
    <img :src="friend.avatar" class="w-10 h-10 rounded-full mr-3 shadow" />
    <div class="flex flex-col">
      <span class="font-semibold text-lg">{{ friend.nickname }}</span>
      <span class="text-sm text-green-500 flex items-center">
        <span class="animate-ping absolute inline-flex h-2 w-2 rounded-full bg-green-400 opacity-75"></span>
        <span class="relative inline-flex rounded-full h-2 w-2 bg-green-500 mr-1"></span>
        在线
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  friend: {
    type: Object,
    required: true
  }
});
</script>
