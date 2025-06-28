<template>
  <div class="login-background">
    <div class="login-container">
      <!-- 左侧 Logo -->
      <div class="login-left">
        <h2 style="text-align: center;font-size: 32px; font-weight: bold;">Welcome to BreezeTribe</h2>
        <img src="@/assets/Logo.jpg" alt="Logo" class="login-logo" />
         <!-- 去注册 -->
          <el-form-item>
            <el-button type="text" @click="goRegister" style="width: 100%; text-align: center;">
              还没有账号？去注册
            </el-button>
          </el-form-item>
      </div>

      <!-- 右侧表单 -->
      <div class="login-right">
        <h2 style="text-align: center;line-height: 2.0;">
          <span style="font-size: 32px; font-weight: bold;">LOGIN</span><br>
          <br>
        </h2>
        <!-- 登录方式切换（用 Tabs 实现） -->
        <el-tabs v-model="mode" class="login-switch" stretch>
          <el-tab-pane label="手机号登录" name="phone" ></el-tab-pane>
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
            <el-option label="普通用户" value="ROLE_USER" />
            <el-option label="经销商" value="ROLE_MERCHANT" />
            <el-option label="管理员" value="ROLE_ADMIN" />
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
import { ref, reactive, watch } from 'vue' // 引入 watch
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { publicAxios } from "@/utils/request"

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false) // 控制加载状态

// 当前登录模式：phone 或 email
const mode = ref('phone') // 确保这个ref被用来控制UI上的切换

// 表单引用，用于调用 validate 方法
const loginFormRef = ref(null)

// 表单数据
const loginForm = reactive({
  phone: '',
  email: '',
  password: '',
  role: '',
})

// 监听 mode 变化，清空不相关的字段值和校验状态
watch(mode, (newMode) => {
  if (newMode === 'phone') {
    loginForm.email = ''; // 切换到手机登录时清除邮箱值
    if (loginFormRef.value) {
      loginFormRef.value.clearValidate('email'); // 清除邮箱的校验信息
    }
  } else { // newMode === 'email'
    loginForm.phone = ''; // 切换到邮箱登录时清除手机号值
    if (loginFormRef.value) {
      loginFormRef.value.clearValidate('phone'); // 清除手机的校验信息
    }
  }
});


// 表单校验规则
const rules = {
  phone: [
    {
      validator: (rule, value, callback) => {
        if (mode.value === 'phone') {
          if (!value) {
            return callback(new Error('请输入手机号'));
          }
          if (!/^1[3-9]\d{9}$/.test(value)) {
            return callback(new Error('手机号格式不正确'));
          }
        }
        callback(); // 非 phone 模式，或验证通过
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (rule, value, callback) => {
        if (mode.value === 'email') {
          if (!value) {
            return callback(new Error('请输入邮箱'));
          }
          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
          if (!emailRegex.test(value)) {
            return callback(new Error('邮箱格式不正确'));
          }
        }
        callback(); 
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择登录身份', trigger: 'change' }
  ]
}

// 处理登录逻辑
const handleLogin = () => {
    if (!loginFormRef.value) {
        console.error('表单引用未初始化！请检查 <el-form> 上是否绑定了 ref="loginFormRef"。');
        ElMessage.error('表单组件未准备好，请稍后再试。');
        return;
    }

    loginFormRef.value.validate(async (valid) => { 
        if (valid) {
            loading.value = true;
            try {
                let payload = {};
                if (mode.value === 'phone') { 
                    payload = { identity: loginForm.phone, password: loginForm.password, role: loginForm.role };
                } else { 
                    payload = { identity: loginForm.email, password: loginForm.password, role: loginForm.role };
                }

                const response = await publicAxios.post('/auth/login', payload, {
                    headers: { 'Content-Type': 'application/json' }
                });

                if (response.data.code === 200) {
                    ElMessage.success('登录成功！');
                    console.log(response);
                    authStore.login(response.data.data.token,loginForm.role);
                    console.log(response.data.data,loginForm.role);
                    router.push('/');
                } else {
                    ElMessage.error(response.data.message || '登录失败');
                }
            } catch (error) {
                console.error('登录请求发生错误:', error);

                if (error.response) {
                  if (error.response.data && error.response.data.message) {
                    ElMessage.error(error.response.data.message);
                  } else if (error.response.status === 400) {
                    ElMessage.error('请求参数错误，请检查您的输入。');
                  } else if (error.response.status === 401) {
                    ElMessage.error('用户名或密码错误，或未授权访问。');
                  } else if (error.response.status === 403) {
                    ElMessage.error('您没有权限访问此资源。');
                  } else if (error.response.status === 500) {
                    ElMessage.error('服务器内部错误，请稍后再试。');
                  } else {
                    ElMessage.error(`登录失败: ${error.response.status} ${error.response.statusText}`);
                  }
                } else if (error.request) {
                  // 请求已发出但未收到响应 (例如网络断开，服务器未启动)
                  ElMessage.error('网络连接失败，请检查您的网络。');
                } else {
                  // 其他未知错误 (例如在设置请求时发生错误)
                  ElMessage.error('发生未知错误，请稍后再试。');
                }
            } finally {
                loading.value = false;
            }
        } else {
            ElMessage.error('请填写完整登录信息！'); // 表单校验不通过时提示
        }
    });
}

// 取消处理逻辑
const handleCancel = () => {
    if (loginFormRef.value) {
        loginFormRef.value.resetFields(); // 使用 resetFields() 清空表单并移除校验提示
    }
    ElMessage.info('已取消登录');
    router.push('/');
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
  background-image: url('@/assets/background.jpg');
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
  width: 340px;
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
  width: 400px;
  height:550px;
  padding: 50px 30px;
  background: #56899cce;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  z-index: 10;

  display: flex;
  flex-direction: column;
}

.login-right .el-form-item {
  margin-bottom: 30px; /* 垂直间距*/
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