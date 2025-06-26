<template>
  <div class="register-background">
    <div class="register-container">
      <!-- 左侧 Logo -->
      <div class="register-left">
        <h2>Welcome to BreezeTribe</h2>
        <img src="@/assets//Logo.jpg" alt="Logo" class="register-logo" />
        <!-- 去登录 -->
          <el-form-item>
            <el-button type="text" @click="goLogin" style="width: 100%; text-align: center;">
              已有账号？去登录
            </el-button>
          </el-form-item>
      </div>

      <!-- 右侧表单 -->
      <div class="register-right">
        <h2 style="text-align: center;">REGISTER</h2>

        <!-- 注册身份 Tab 切换 -->
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="用户注册" name="user" />
          <el-tab-pane label="经销商注册" name="dealer" />
        </el-tabs>

        <el-form
          :model="registerForm"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          class="form-body"
        >
          <!-- 共用字段 -->
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              type="password"
              v-model="registerForm.password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>

          <!-- 经销商专属字段 -->
          <el-form-item
            label="公司名称"
            v-if="activeTab === 'dealer'"
            prop="company"
          >
            <el-input v-model="registerForm.company" placeholder="请输入公司名称" />
          </el-form-item>

          <el-form-item
            label="营业执照号"
            v-if="activeTab === 'dealer'"
            prop="license"
          >
            <el-input v-model="registerForm.license" placeholder="请输入营业执照号" />
          </el-form-item>

          <!-- 注册与取消 -->
          <el-form-item>
            <div style="display: flex; gap: 10px; width: 100%;">
              <el-button
                type="primary"
                size="large"
                style="flex: 1;"
                @click="handleRegister"
              >
                注册
              </el-button>
              <el-button
                size="large"
                style="flex: 1;"
                @click="handleCancel"
              >
                取消
              </el-button>
            </div>
          </el-form-item>

          
        </el-form>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const activeTab = ref('user') // 默认是用户注册

const registerForm = reactive({
  phone: '',
  email: '',
  password: '',
  role: 'user', 
  company: '',
  license: ''
})

const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  company: [
    {
      required: () => activeTab.value === 'dealer',
      message: '请输入公司名称',
      trigger: 'blur'
    }
  ],
  license: [
    {
      required: () => activeTab.value === 'dealer',
      message: '请输入营业执照号',
      trigger: 'blur'
    }
  ]
}

const formRef = ref()

const handleRegister = () => {
  registerForm.role = activeTab.value
  formRef.value.validate((valid) => {
    if (valid) {
      console.log('注册数据：', registerForm)
      // 提交逻辑
    }
  })
}

const handleCancel = () => {
  router.push('/')  
}

const goLogin = () => {
  router.push('/login')
}

</script>


<style scoped>
.register-background {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-position: center;
  padding: 16px; /* 防止在小屏设备挤边 */
  box-sizing: border-box;
}

.register-container {
  width: 800px;
  height: 500px;
  display: flex;
  border-radius: 50px;
  background: white;
  overflow: visible; /* 关键：允许右侧卡片溢出 */
  position: relative;
}

.register-left {
  width: 250px;
  background: white;
  border-radius: 50px;
  padding: 40px;
  text-align: center;
}

.register-logo {
  max-width: 100%; /* 图片最大宽度100% */
  max-height: 80%; /* 图片最大高度80% */
  border-radius: 50px;
  object-fit: contain; /* 保持图片比例 */
}

.register-right {
  position: absolute;
  right: 60px;  
  top: 50%;
  transform: translateY(-50%);
  width: 350px;
  height:460px;
  padding: 40px 30px;
  background: #84adbc;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  z-index: 10;

  display: flex;
  flex-direction: column;
}

.register-right .el-form-item {
  margin-bottom: 25px; /* 垂直间距*/
}

/* 取消最后一个 el-form-item 的下边距，避免按钮下方留白 */
.register-right .el-form-item:last-child {
  margin-bottom: 0;
}


.form-body {
  margin-top: 20px;
}

.el-input__inner,
.el-button {
  border-radius: 12px;
}
</style>
