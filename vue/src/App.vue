<script setup>
  import { useAuthStore } from './stores/auth'
  import { onMounted } from 'vue'

  const authStore = useAuthStore()

  onMounted(() => {
    authStore.checkAuth()
  })

</script>

<template>

  <div>
    <router-view />
  </div>

</template>

<style scoped>

</style>

