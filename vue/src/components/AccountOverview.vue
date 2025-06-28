<template>
  <el-card class="account-overview-card" shadow="hover">
    <h3 class="title">个人资料</h3>
    <div class="info-container">
      <el-avatar
        :src="user.avatarUrl || '/default-avatar.png'"
        :size="100"
        class="avatar"
        @click="handleAvatarClick"
        style="cursor: pointer;"
      />
      <el-upload
        ref="uploadRef"
        class="hidden-uploader"
        :http-request="customUpload"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        accept="image/*"
        style="display: none;"
        >
        <el-button>上传</el-button>
        </el-upload>
      <div class="user-info">
        <p><strong>用户名：</strong>{{ user.username }}</p>
        <p>
          <strong>手机号：</strong>
          <span v-if="user.phone">{{ user.phone }}</span>
          <el-button v-else type="text" @click="goBind('phone')">还未绑定手机号，去绑定</el-button>
        </p>
        <p>
          <strong>邮箱：</strong>
          <span v-if="user.email">{{ user.email }}</span>
          <el-button v-else type="text" @click="goBind('email')">还未绑定邮箱，去绑定</el-button>
        </p>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted ,reactive} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authAxios } from '@/utils/request'

const router = useRouter()
const user = reactive({
  username: '',
  phone: '',
  email: '',
  avatarUrl: ''
})
//对 el-upload 组件的引用，用来手动触发上传操作（点击头像后模拟点击 input）
const uploadRef = ref(null)
const showBindDialog = ref(false)
const bindType = ref('phone') 

const fetchUserInfo = async () => {
  try {
    const res = await authAxios.get('auth/me')
    if (res.data.code === 200) {
        const newUser = res.data.data
        Object.assign(user, newUser)
    } else {
      ElMessage.error(res.data.message || '获取用户信息失败')
    }
  } catch (err) {
    console.error('获取信息失败:', err)
    ElMessage.error('获取用户信息失败，请稍后再试')
  }
}

const handleAvatarClick = () => {
  uploadRef.value.$el.querySelector('input').click()
}
const beforeAvatarUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt8M = file.size / 1024 / 1024 < 8

    if (!isImage) ElMessage.error('只能上传图片格式！')
    if (!isLt8M) ElMessage.error('图片不能超过 8MB！')

    return isImage && isLt8M
}
const customUpload = async ({ file }) => {
    const formData = new FormData()
    formData.append('file', file)

    try {
        const response = await authAxios.put('/auth/updateAvatar', formData, {headers: {'Content-Type': 'multipart/form-data'}})

        if (response.data.code === 0) {
            ElMessage.success('头像上传成功')
            user.avatarUrl = response.data.data.url || ''
        } else {
            ElMessage.error(response.data.message || '上传失败')
        }
    } catch (error) {
        ElMessage.error('上传失败，请稍后重试')
    }
}

const goBind = (type) => {
  if (type === 'phone') {
    router.push({ name: '绑定手机' })
  } else if (type === 'email') {
    router.push({ name: '绑定邮箱' })
  }
}

onMounted(fetchUserInfo)
</script>

<style scoped>
.account-overview-card {
  max-width: auto;
  margin: 0 auto;
  padding: 20px;
  border-radius: 50px;
}
.title {
  text-align: center;
  margin-bottom: 20px;
}
.info-container {
  display: flex;
  gap: 20px;
  align-items: center;
}
.user-info p {
  margin: 6px 0;
}
</style>
