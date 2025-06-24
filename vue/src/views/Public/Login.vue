<template>
  <div class="login-background">
  <div class="login-container">
    <!-- 左侧 Logo -->
      <div class="login-left">
        <img src="/Logo.jpg" alt="Logo" class="login-logo" />
      </div>
      <!-- 右侧表单 -->
      <div class="login-right">
      <h2>BreezeTribe-登录</h2>
      <!-- 登录方式切换 -->
      <div class="login-switch">
        <el-button :type="mode === 'phone' ? 'primary' : 'text'" @click="mode = 'phone'">手机号登录</el-button>
        <el-button :type="mode === 'email' ? 'primary' : 'text'" @click="mode = 'email'">邮箱登录</el-button>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
        <el-form-item v-if="mode === 'phone'" label="手机号" prop="phone">
          <el-input v-model="loginForm.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item v-if="mode === 'email'" label="邮箱" prop="email">
          <el-input v-model="loginForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>

        <el-form-item label="身份" prop="role">
          <el-select v-model="loginForm.role" placeholder="请选择登录身份">
            <el-option label="用户" value="user" />
            <el-option label="经销商" value="dealer" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleLogin">登录</el-button>
          <el-button type="text" @click="goRegister">还没有账号？去注册</el-button>
        </el-form-item>
      </el-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      mode: 'phone', // 当前登录模式：phone 或 email
      loginForm: {
        phone: '',
        email: '',
        password: '',
        role: ''
      },
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择登录身份', trigger: 'change' }
        ]
      }
    };
  }
  // methods: {
  //   handleLogin() {
  //     const activeProp = this.mode === 'phone' ? 'phone' : 'email';
  //     this.$refs.loginFormRef.validateField(activeProp, (validPhoneOrEmail) => {
  //       if (!validPhoneOrEmail) return;

  //       this.$refs.loginFormRef.validateField('password', (validPwd) => {
  //         if (!validPwd) return;

  //         this.$refs.loginFormRef.validateField('role', (validRole) => {
  //           if (!validRole) return;

  //           const account = this.mode === 'phone' ? this.loginForm.phone : this.loginForm.email;
  //           console.log('登录信息:', {
  //             account,
  //             password: this.loginForm.password,
  //             role: this.loginForm.role
  //           });

  //           // 发起 axios 登录请求等逻辑
  //         });
  //       });
  //     });
  //   }
  // }
};
</script>

<style scoped>
.login-background {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/loginbackground.png');
  background-size: cover;
  background-position: center;
  padding: 16px; /* 防止在小屏设备挤边 */
  box-sizing: border-box;
}

.login-container {
  display: flex;
  width: 800px;
  max-width: 90%;
  height: 500px; /* 固定容器高度 */
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
}

.login-left {
  flex: 1;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.login-logo {
  max-width: 100%;
  max-height: 80%; /* 限制图片高度不超过容器 */
  object-fit: contain;
}

.login-right {
  flex: 1;
  padding: 40px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: auto;
}

.login-switch {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.el-input__inner {
  border-radius: 16px; /* 输入框圆角 */
}

.el-button {
  border-radius: 12px; /* 按钮圆角 */
}
</style>