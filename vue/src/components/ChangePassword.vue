<template>
  <el-card class="reset-password-card" shadow="hover">
     <h3 class="title">修改密码</h3>
    <el-tabs v-model="activeTab" class="full-width-tabs" stretch>
    <el-tab-pane label="手机" name="phone">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="200px">
        <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" style="width: 315px;"/>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
        <div style="display: flex; gap: 10px; align-items: center;">
            <el-input
            v-model="form.code"
            placeholder="请输入验证码"
            style="flex: 1; max-width: 215px; font-size: 15px;" />
            <el-button
            type="primary"
            :disabled="phoneCodeCountdown > 0"
            @click="sendPhoneCode"
            >
            {{ phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s后重试` : '发送验证码' }}
            </el-button>
        </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
        <div style="display: flex; gap: 10px; align-items: center;">
            <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            style="flex: 1; max-width: 225px; font-size: 15px;" />
            <el-button type="primary" @click="submit">保存新密码</el-button>
        </div>
        </el-form-item>
    </el-form>
    </el-tab-pane>

      <el-tab-pane label="邮箱" name="email">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="200px">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" style="width:315px;" />
          </el-form-item>

          <el-form-item label="验证码" prop="code">
        <div style="display: flex; gap: 10px; align-items: center;">
            <el-input
            v-model="form.code"
            placeholder="请输入验证码"
            style="flex: 1; max-width: 215px; font-size: 15px;" />
            <el-button
            type="primary"
            :disabled="emailCodeCountdown > 0"
            @click="sendEmailCode"
            >
            {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s后重试` : '发送验证码' }}
            </el-button>
        </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
        <div style="display: flex; gap: 10px; align-items: center;">
            <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            style="flex: 1; max-width: 225px; font-size: 15px;" />
            <el-button type="primary" @click="submit">保存新密码</el-button>
        </div>
        </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { publicAxios, authAxios } from '@/utils/request'

const emailCodeCountdown = ref(0)
let emailTimer = null
const phoneCodeCountdown = ref(0)
let phoneTimer = null

const props = defineProps({
  role: { type: String, required: true }
})

const activeTab = ref('phone')
const formRef = ref()

const form = ref({
  phone: '',
  email: '',
  code: '',
  newPassword: ''
})

const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

// 统一提交方法
const submit = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    const account = activeTab.value === 'phone' ? form.value.phone : form.value.email

    try {
      const res = await publicAxios.put('/auth/resetPassword', {
        phone: activeTab.value === 'phone' ? account : '',
        email: activeTab.value === 'email' ? account : '',
        newpassword: form.value.newPassword,
        code: form.value.code,
        role: props.role
      })

      if (res.data.code === 200) {
        ElMessage.success('密码修改成功')
        form.value.phone = ''
        form.value.email = '' 
        form.value.code = ''
        form.value.newPassword = ''
      } else {
        ElMessage.error(res.data.message || '修改失败')
      }
    } catch (err) {
      console.error(err)
      ElMessage.error('网络请求失败')
    }
  })
}

// 验证绑定状态 & 发送手机验证码
const sendPhoneCode = async () => {
  // 1. 验证手机号输入框
  // validateField('phone', ...) 会根据 el-form 的 rules 验证 form.value.phone 字段
  formRef.value.validateField('phone', async (isValid) => {
    if (!isValid) {
      ElMessage.error('请先输入有效的手机号码');
      return;
    }

    try {
      // 2. 验证当前用户是否已绑定该手机号
      // 调用 /auth/me 接口获取当前用户的信息
      const res = await authAxios.get('/auth/me');
      if (res.data.code !== 200) {
        return ElMessage.error('无法验证绑定状态');
      }

      // 假设 /auth/me 接口返回的数据中，当前绑定的手机号字段是 data.phone
      const boundPhone = res.data.data.phone;
      if (form.value.phone !== boundPhone) {
        return ElMessage.warning('请输入当前绑定的手机号码');
      }

      // 3. 调用发送短信验证码接口
      // 使用 publicAxios 发送 POST 请求到 /captcha/sendSms
      // 将手机号作为 params 发送
      const response = await publicAxios.post('/captcha/resetBySms', null, {
        params: { phone: form.value.phone }
      });

      if (response.data.code === 200) {
        ElMessage.success(`手机验证码已发送至 ${form.value.phone}`);

        // 确保在启动新定时器前，清除任何旧的定时器，避免重复计时
        if (phoneTimer) {
          clearInterval(phoneTimer);
          phoneTimer = null;
        }

        // 启动倒计时
        phoneCodeCountdown.value = 60; // 倒计时从60秒开始

        phoneTimer = setInterval(() => {
          if (phoneCodeCountdown.value > 0) {
            phoneCodeCountdown.value--; // 每秒递减
          } else {
            clearInterval(phoneTimer); // 倒计时结束，清除定时器
            phoneTimer = null; // 重置定时器变量
          }
        }, 1000); // 每1000毫秒（1秒）执行一次
      } else {
        // 后端返回的错误信息
        ElMessage.error(response.data.message || '发送手机验证码失败');
      }
    } catch (error) {
      // 网络请求或未知错误
      console.error('发送手机验证码失败:', error);
      ElMessage.error('发送手机验证码请求失败，请稍后再试');
    }
  });
};

// 验证绑定状态 & 发送邮箱验证码
const sendEmailCode = async () => {
    formRef.value.validateField('email', async (isValid) => {
    if (!isValid) {
      ElMessage.error('请先输入有效的邮箱地址')
      return
    }
    try {
        const res = await authAxios.get('/auth/me')
        if (res.data.code !== 200) return ElMessage.error('无法验证绑定状态')

        const boundEmail = res.data.data.email
        if (form.value.email !== boundEmail) return ElMessage.warning('请输入当前绑定的邮箱')

        const response = await publicAxios.post('/captcha/resetByEmail', null, {
            params: { email: form.value.email }
        })

        if (response.data.code === 200) {
            ElMessage.success(`邮箱验证码已发送至 ${form.value.email}`)
            emailCodeCountdown.value = 60

            emailTimer = setInterval(() => {
            if (emailCodeCountdown.value > 0) {
                emailCodeCountdown.value--
            } else {
                clearInterval(emailTimer)
                emailTimer = null
            }
            }, 1000)
        } else {
            ElMessage.error(response.data.message || '发送邮箱验证码失败')
        }
        } catch (error) {
        console.error('发送邮箱验证码失败:', error)
        ElMessage.error('发送邮箱验证码请求失败，请稍后再试')
        }
    })
}

</script>

<style scoped>
.reset-password-card {
  max-width: auto;
  margin: 0 auto;
  padding: 20px;
  border-radius: 50px;
}

.title {
  text-align: center;
  margin-bottom: 20px;
}
</style>
