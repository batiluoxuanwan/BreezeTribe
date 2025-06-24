<template>
  <div class="login-background">
    <div class="login-container">
      <!-- 左侧 Logo -->
      <div class="login-left">
        <h2>Welcome to BreezeTribe</h2>
        <img src="/Logo.jpg" alt="Logo" class="login-logo" />
         <!-- 去注册 -->
          <el-form-item>
            <el-button type="text" @click="goRegister" style="width: 100%; text-align: center;">
              还没有账号？去注册
            </el-button>
          </el-form-item>
      </div>

      <!-- 右侧表单 -->
      <div class="login-right">
        
        <h1 style="text-align: center;">LOGIN</h1>
        <h4 style="text-align: center;">专业报团，放心出游，开启无忧旅行！</h4>
        <!-- 登录方式切换（用 Tabs 实现） -->
        <el-tabs v-model="mode" class="login-switch" stretch>
          <el-tab-pane label="手机号登录" name="phone"></el-tab-pane>
          <el-tab-pane label="邮箱登录" name="email"></el-tab-pane>
        </el-tabs>
        <h3 style="text-align: center;"></h3>
        <!-- 登录表单 -->
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
          <el-form-item v-show="mode === 'phone'" label="手机号" prop="phone">
            <el-input v-model="loginForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item v-show="mode === 'email'" label="邮箱" prop="email">
            <el-input v-model="loginForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>

          <el-form-item label="身份" prop="role">
            <el-select v-model="loginForm.role" placeholder="请选择登录身份">
              <el-option label="用户" value="user" />
              <el-option label="经销商" value="dealer" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </el-form-item>

          <!-- 登录与取消 -->
          <el-form-item>
            <div style="display: flex; gap: 10px; width: 100%;">
              <el-button
                type="primary"
                size="large"
                style="flex: 1;"
                @click="handleLogin"
              >
                登录
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
import { ElMessage } from 'element-plus'

const router = useRouter()

// 当前登录模式：phone 
const mode = ref('phone')

// 表单引用，用于调用 validate 方法
const loginFormRef = ref(null)

// 表单数据
const loginForm = reactive({
  phone: '',
  email: '',
  password: '',
  role: '',
})

// 表单校验规则
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择登录身份', trigger: 'change' }
  ]
}

// 登录处理逻辑
// const handleLogin = () => {
//   loginFormRef.value.validate((valid) => {
//     if (valid) {
//       // 模拟登录成功
//       ElMessage.success('登录成功')
//       // 登录后跳转主页或控制台
//       router.push('/dashboard')
//     } else {
//       ElMessage.error('请完善表单信息')
//     }
//   })
// }

// 取消处理逻辑
const handleCancel = () => {
  // 清空表单数据
  loginForm.phone = ''
  loginForm.email = ''
  loginForm.password = ''
  loginForm.role = ''
  ElMessage.info('已取消登录')
  router.push('/')
}

// 跳转注册页面
const goRegister = () => {
  router.push('/register')
}

</script>

<style scoped>
.login-background {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-position: center;
  padding: 16px; 
}

.login-container {
  width: 800px;
  height: 500px;
  display: flex;
  border-radius: 50px;
  background: white;
  overflow: visible; /* 关键：允许右侧卡片溢出 */
  position: relative;
}

.login-left {
  width: 250px;
  background: white;
  border-radius: 50px;
  padding: 40px;
  text-align: center;
}

.login-logo {
  max-width: 100%; /* 图片最大宽度100% */
  max-height: 80%; /* 图片最大高度80% */
  border-radius: 50px;
  object-fit: contain; /* 保持图片比例 */
}

.login-right {
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

.login-right .el-form-item {
  margin-bottom: 25px; /* 垂直间距*/
}

/* 取消最后一个 el-form-item 的下边距，避免按钮下方留白 */
.login-right .el-form-item:last-child {
  margin-bottom: 0;
}

.el-input__inner {
  border-radius: 16px; /* 输入框圆角 */
}

.el-button {
  border-radius: 12px; /* 按钮圆角 */
}
</style>