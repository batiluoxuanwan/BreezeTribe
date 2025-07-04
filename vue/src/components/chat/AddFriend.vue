<template>
  <div class="add-friend-page max-w-2xl mx-auto mt-10 p-6 bg-white rounded-xl shadow-md">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">添加好友</h2>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名或手机号" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">查找</el-button>
      </el-form-item>
    </el-form>

    <div v-if="searchResult" class="mt-6 p-4 border rounded-lg bg-gray-50">
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <img :src="searchResult.avatar || defaultAvatar" class="w-12 h-12 rounded-full mr-4" />
          <div>
            <p class="text-lg font-semibold">{{ searchResult.username }}</p>
            <p class="text-sm text-gray-500">ID: {{ searchResult.id }}</p>
          </div>
        </div>
        <el-button type="success" @click="sendRequest">添加好友</el-button>
      </div>
    </div>

    <el-empty description="未搜索用户" v-else-if="searched" class="mt-10" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authAxios } from '@/utils/request'

const form = ref({ username: '' })
const formRef = ref()
const searchResult = ref(null)
const defaultAvatar = '/images/default-avatar.png'
const searched = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const handleSearch = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const response = await authAxios.get('/friends/search', {
        params: { username: form.value.username }
      })
      if (response.data.code === 200 && response.data.data) {
        searchResult.value = response.data.data
      } else {
        searchResult.value = null
        ElMessage.warning('未找到该用户')
      }
      searched.value = true
    } catch (err) {
      ElMessage.error('搜索失败，请稍后重试')
    }
  })
}

const sendRequest = async () => {
  try {
    const res = await authAxios.post('/friends/request', {
      targetUserId: searchResult.value.id
    })
    if (res.data.code === 200) {
      ElMessage.success('好友请求已发送')
    } else {
      ElMessage.warning(res.data.message || '请求失败')
    }
  } catch (err) {
    ElMessage.error('请求发送失败')
  }
}
</script>

<style scoped>
/* 可选：根据你的设计调整按钮、边框、头像等细节 */
</style>
