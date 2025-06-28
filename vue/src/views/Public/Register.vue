<template>
  <div class="register-background">
    <div class="register-container">
      <div class="register-left">
        <h2 style="text-align: center;font-size: 32px; font-weight: bold;">Welcome to BreezeTribe</h2>
        <img src="@/assets//Logo.jpg" alt="Logo" class="register-logo" />
        <el-form-item>
          <el-button type="text" @click="goLogin" style="width: 100%; text-align: center;">
            已有账号？去登录
          </el-button>
        </el-form-item>
      </div>

      <div class="register-right">
        <h2 style="text-align: center;font-size: 32px; font-weight: bold;">REGISTER</h2>

        <el-form
          :model="registerForm"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          class="form-body"
        >
          <el-form-item label="注册角色" prop="role">
            <el-select v-model="registerForm.role" placeholder="请选择注册角色" style="width: 100%;">
              <el-option label="普通用户" value="ROLE_USER"></el-option>
              <el-option label="经销商" value="ROLE_MERCHANT"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="验证方式" prop="verificationMethod">
            <el-radio-group v-model="registerForm.verificationMethod">
              <el-radio label="phone">手机号验证</el-radio>
              <el-radio label="email">邮箱验证</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="手机号" prop="phone" v-if="registerForm.verificationMethod === 'phone'">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email" v-if="registerForm.verificationMethod === 'email'">
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="手机验证码" prop="phoneCode" v-if="registerForm.verificationMethod === 'phone'">
            <div class="verification-code-input">
              <el-input v-model="registerForm.phoneCode" placeholder="请输入手机验证码" />
              <el-button
                type="primary"
                @click="sendPhoneCode"
                :disabled="phoneCodeCountdown > 0 || !registerForm.phone"
              >
                {{ phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="邮箱验证码" prop="emailCode" v-if="registerForm.verificationMethod === 'email'">
            <div class="verification-code-input">
              <el-input v-model="registerForm.emailCode" placeholder="请输入邮箱验证码" />
              <el-button
                type="primary"
                @click="sendEmailCode"
                :disabled="emailCodeCountdown > 0 || !registerForm.email"
              >
                {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              type="password"
              v-model="registerForm.password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>

          <el-form-item
            label="公司名称"
            v-if="registerForm.role === 'ROLE_MERCHANT'"
            prop="company"
          >
            <el-input v-model="registerForm.company" placeholder="请输入公司名称" />
          </el-form-item>

          <el-form-item
            label="营业执照号"
            v-if="registerForm.role === 'ROLE_MERCHANT'"
            prop="license"
          >
            <el-input v-model="registerForm.license" placeholder="请输入营业执照号" />
          </el-form-item>

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
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {publicAxios} from '@/utils/request';

const router = useRouter();

// 移除了 activeTab，直接使用 registerForm.role
const registerForm = reactive({
  phone: '',
  email: '',
  password: '',
  role: 'ROLE_USER', // 默认是普通用户
  company: '',
  license: '',
  phoneCode: '',
  emailCode: '',
  verificationMethod: 'phone' // 默认验证方式
});

// 验证码倒计时状态
const phoneCodeCountdown = ref(0);
const emailCodeCountdown = ref(0);
let phoneTimer = null;
let emailTimer = null;

// 监听 verificationMethod 变化，清除不显示的字段值和校验
watch(() => registerForm.verificationMethod, (newVal) => {
  if (newVal === 'phone') {
    registerForm.email = '';
    if (formRef.value) {
      formRef.value.clearValidate('email');
      formRef.value.clearValidate('emailCode');
    }
  } else { // newVal === 'email'
    registerForm.phone = '';
    if (formRef.value) {
      formRef.value.clearValidate('phone');
      formRef.value.clearValidate('phoneCode');
    }
  }
});

// 监听 role 变化，清除经销商专属字段
watch(() => registerForm.role, (newVal) => {
  if (newVal !== 'ROLE_MERCHANT') { // 如果不是经销商，则清除公司名称和营业执照号
    registerForm.company = '';
    registerForm.license = '';
    if (formRef.value) {
      formRef.value.clearValidate(['company', 'license']);
    }
  }
});


// 校验规则
const rules = reactive({
  phone: [
    {
      required: () => registerForm.verificationMethod === 'phone',
      message: '请输入手机号',
      trigger: 'blur',
    },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: ['blur', 'change'],
    },
  ],
  email: [
    {
      required: () => registerForm.verificationMethod === 'email',
      message: '请输入邮箱',
      trigger: 'blur',
    },
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: ['blur', 'change'],
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  phoneCode: [
    {
      required: () => registerForm.verificationMethod === 'phone',
      message: '请输入手机验证码',
      trigger: 'blur',
    },
    {
      len: 6,
      message: '验证码为6位数字',
      trigger: 'blur',
    },
  ],
  emailCode: [
    {
      required: () => registerForm.verificationMethod === 'email',
      message: '请输入邮箱验证码',
      trigger: 'blur',
    },
    {
      len: 6,
      message: '验证码为6位数字',
      trigger: 'blur',
    },
  ],
  company: [
    {
      required: () => registerForm.role === 'ROLE_MERCHANT', // 根据 role 判断是否必填
      message: '请输入公司名称',
      trigger: 'blur',
    },
  ],
  license: [
    {
      required: () => registerForm.role === 'ROLE_MERCHANT', // 根据 role 判断是否必填
      message: '请输入营业执照号',
      trigger: 'blur',
    },
  ],
});

const formRef = ref();

// 发送手机验证码函数
const sendPhoneCode = () => {
  formRef.value.validateField('phone', async (isValid) => {
    if (isValid) {
      try {
        const response = await publicAxios.post('/api/send-phone-code', { phone: registerForm.phone });
        if (response.data.code === 200) {
          ElMessage.success(`手机验证码已发送至 ${registerForm.phone}`);
          phoneCodeCountdown.value = 60;
          phoneTimer = setInterval(() => {
            if (phoneCodeCountdown.value > 0) {
              phoneCodeCountdown.value--;
            } else {
              clearInterval(phoneTimer);
              phoneTimer = null;
            }
          }, 1000);
        } else {
          ElMessage.error(response.data.message || '发送手机验证码失败');
        }
      } catch (error) {
        ElMessage.error('发送手机验证码请求失败，请稍后再试。');
        console.error('发送手机验证码失败:', error);
      }
    } else {
      ElMessage.error('请先输入有效的手机号');
    }
  });
};

// 发送邮箱验证码函数
const sendEmailCode = () => {
  formRef.value.validateField('email', async (isValid) => {
    if (isValid) {
      try {
        const response = await publicAxios.post('/captcha/sendEmail', null, {params:{ email: registerForm.email }});
        if (response.data.code === 200) {
          ElMessage.success(`邮箱验证码已发送至 ${registerForm.email}`);
          emailCodeCountdown.value = 60;
          emailTimer = setInterval(() => {
            if (emailCodeCountdown.value > 0) {
              emailCodeCountdown.value--;
            } else {
              clearInterval(emailTimer);
              emailTimer = null;
            }
          }, 1000);
        } else {
          ElMessage.error(response.data.message || '发送邮箱验证码失败');
        }
      } catch (error) {
        ElMessage.error('发送邮箱验证码请求失败，请稍后再试。');
        console.error('发送邮箱验证码失败:', error);
      }
    } else {
      ElMessage.error('请先输入有效的邮箱地址');
    }
  });
};


const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      const registrationData = {
        password: registerForm.password,
        role: registerForm.role, 
        code: registerForm.verificationMethod === 'phone' ? registerForm.phoneCode : registerForm.emailCode
      };

      // 根据选择的验证方式添加相应的手机号或邮箱到注册数据中
      if (registerForm.verificationMethod === 'phone') {
        registrationData.phone = registerForm.phone;
      } else {
        registrationData.email = registerForm.email;
      }

      // 仅当角色为经销商时才添加公司名称和营业执照号
      if (registerForm.role === 'ROLE_MERCHANT') {
        registrationData.company = registerForm.company;
        registrationData.license = registerForm.license;
      }

      try {
        const response = await publicAxios.post('/auth/register', registrationData);

        if (response.data.code === 200) {
          ElMessage.success('注册成功！');
          router.push('/login');
        } else {
          ElMessage.error(response.data.message || '注册失败，请稍后再试。');
        }
      } catch (error) {
        ElMessage.error('用户已存在，请直接登录');
        console.error('注册失败:', error);
      }
    } else {
      ElMessage.error('请检查表单填写是否完整和正确！');
    }
  });
};

const handleCancel = () => {
  router.push('/');
};

const goLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-background {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/background.jpg');
  background-size: cover;
  background-position: center;
  padding: 16px;
  box-sizing: border-box;
}

.register-container {
  width: 800px;
  height: 500px;
  display: flex;
  border-radius: 50px;
  background: white;
  overflow: visible;
  position: relative;
}

.register-left {
  width: 340px;
  background: white;
  border-radius: 50px;
  padding: 40px;
  text-align: center;
}

.register-logo {
  max-width: 100%;
  max-height: 80%;
  border-radius: 50px;
  object-fit: contain;
}

.register-right {
  position: absolute;
  right: 60px;
  top: 50%;
  transform: translateY(-50%);
  width: 400px;
  height: 575px;
  padding: 30px 30px;
  background: #56899cce;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  z-index: 10;

  display: flex;
  flex-direction: column;
}

.register-right .el-form-item {
  margin-bottom: 25px;
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

.verification-code-input {
  display: flex;
  gap: 10px;
}
</style>