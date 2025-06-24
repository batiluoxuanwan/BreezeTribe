<template>
  <div class="register-background">
    <div class="register-container">
      <div class="register-form">
        <h2>BreezeTribe - 注册</h2>

        <!-- 身份选择 -->
        <el-radio-group v-model="registerForm.role" size="large">
          <el-radio-button label="user">用户</el-radio-button>
          <el-radio-button label="dealer">经销商</el-radio-button>
        </el-radio-group>

        <el-form :model="registerForm" :rules="rules" ref="formRef" label-width="100px" class="form-body">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input type="password" v-model="registerForm.password" placeholder="请输入密码" show-password />
          </el-form-item>

          <!-- 经销商专属信息 -->
          <el-form-item label="公司名称" v-if="registerForm.role === 'dealer'" prop="company">
            <el-input v-model="registerForm.company" placeholder="请输入公司名称" />
          </el-form-item>

          <el-form-item label="营业执照号" v-if="registerForm.role === 'dealer'" prop="license">
            <el-input v-model="registerForm.license" placeholder="请输入营业执照号" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleRegister">注册</el-button>
            <el-button type="text" @click="goLogin">已有账号？去登录</el-button>
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
      registerForm: {
        role: 'user',
        phone: '',
        email: '',
        password: '',
        company: '',
        license: ''
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
        company: [
          { required: true, message: '请输入公司名称', trigger: 'blur' }
        ],
        license: [
          { required: true, message: '请输入营业执照号', trigger: 'blur' }
        ]
      }
    };
  }
//   methods: {
//     handleRegister() {
//       this.$refs.formRef.validate((valid) => {
//         if (!valid) return;

//         const data = { ...this.registerForm };

//         // 可根据角色处理不同逻辑（如经销商待审核）
//         console.log('提交注册数据：', data);

//         // 示例接口调用
//         // axios.post('/api/register', data).then(...)
//       });
//     },
//     goLogin() {
//       this.$router.push('/login');
//     }
//   }
};
</script>

<style scoped>
.register-background {
  height: 100vh;
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}

.register-container {
  background: rgba(255, 255, 255, 0.95);
  padding: 32px;
  border-radius: 16px;
  max-width: 500px;
  width: 100%;
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.2);
}

.register-form {
  display: flex;
  flex-direction: column;
}

.form-body {
  margin-top: 20px;
}

.el-input__inner,
.el-button {
  border-radius: 12px;
}
</style>
