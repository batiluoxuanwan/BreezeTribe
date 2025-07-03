<script setup>
  import { useAuthStore } from './stores/auth'
  import { onMounted } from 'vue'

  const authStore = useAuthStore();

  onMounted(async () => {
    await authStore.checkAuth();
    const token = authStore.accessToken;
  })

</script>

<template>

  <div>
    <router-view />
  </div>

</template>

<style scoped>

</style>

