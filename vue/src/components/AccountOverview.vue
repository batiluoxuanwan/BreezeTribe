<template>
  <el-card class="account-settings-card" shadow="never">
    <!-- 头像 & 用户名 -->
    <h3 class="title" style="margin-bottom: 20px;margin-left: 20px;">  账号关联</h3>
    <div class="info-container" style="display: flex; align-items: center; gap: 16px;">
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

    <div class="username-info">
        <div v-if="!editMode" class="username-display" >
          <span class="username-text" @click="editMode = true">{{ user.username }}</span> 
          <el-icon class="edit-icon" @click="editMode = true"><EditPen /></el-icon> 
        </div>
        

        <div v-else class="username-edit-container">
          <el-input v-model="newUsername" size="small" class="username-input" @keyup.enter="updateUsername" />
          <el-button type="primary" size="small" @click="updateUsername">保存</el-button>
          <el-button size="small" @click="cancelEdit">取消</el-button>
        </div>
    </div>

    </div>

    <!-- 分割线 -->
    <el-divider />

    <!-- 手机号 -->
    <div class="account-item" @click="handleBindModifyClick('phone')">
    <el-icon><Iphone /></el-icon>
    <span>手机号</span>
    <span class="item-value">
        {{ user.phone ? user.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '去绑定' }}
    </span>
    <el-icon><ArrowRight /></el-icon>
    </div>

    <!-- 分割线 -->
    <el-divider />

    <!-- 邮箱行 -->
    <div class="account-item" @click="handleBindModifyClick('email')">
    <el-icon><Message /></el-icon>
    <span>邮箱</span>
    <span class="item-value">{{ user.email || '去绑定' }}</span>
    <el-icon><ArrowRight /></el-icon>
    </div>

    <!-- 分割线 -->
    <el-divider />

    <!--修改密码-->
    <div class="account-item" @click="changePasswordVisible = !changePasswordVisible">
      <el-icon><Key /></el-icon>
      <span>修改密码</span>
      <span class="item-value"></span>
      <el-icon :class="{'rotate-icon': changePasswordVisible}"><ArrowRight /></el-icon> </div>

    <transition name="el-fade-in-linear">
      <div v-if="changePasswordVisible" class="change-password-container">
        <ChangePassword @close-component="changePasswordVisible = false" />
      </div>
    </transition>

    <!-- 弹窗 -->
        <el-dialog
      v-model="bindNewDialogVisible"
      :title="newBindDialogTitle" width="40%"
      align-center
      @closed="handleFormDialogClose"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item v-if="currentBindType === 'phone'" label="手机号码" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码" />
        </el-form-item>
        <el-form-item v-else-if="currentBindType === 'email'" label="邮箱地址" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div style="display: flex; gap: 10px; align-items: center; width: 100%;">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              style="flex: 1; margin-right: 10px;"
            />
            <el-button
              type="primary"
              @click="currentBindType === 'phone' ? sendBindPhoneCode() : sendBindEmailCode()"
              :disabled="(currentBindType === 'phone' && phoneCodeCountdown > 0) || (currentBindType === 'email' && emailCodeCountdown > 0)"
            >
              {{ currentBindType === 'phone' ? (phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s 后重发` : '发送验证码') : (emailCodeCountdown > 0 ? `${emailCodeCountdown}s 后重发` : '发送验证码') }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="bindNewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleBindSubmit">
            确认绑定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="modifyDialogVisible"
      :title="modifyDialogTitle"
      width="40%"
      align-center
      @closed="handleFormDialogClose"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item v-if="currentBindType === 'phone'" label="新手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入新手机号码" />
        </el-form-item>
        <el-form-item v-else-if="currentBindType === 'email'" label="新邮箱地址" prop="email">
          <el-input v-model="form.email" placeholder="请输入新邮箱地址" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div style="display: flex; gap: 10px; align-items: center; width: 100%;">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              style="flex: 1; margin-right: 10px;"
            />
            <el-button
              type="primary"
              @click="currentBindType === 'phone' ? sendModifyPhoneCode() : sendModifyEmailCode()"
              :disabled="(currentBindType === 'phone' && phoneCodeCountdown > 0) || (currentBindType === 'email' && emailCodeCountdown > 0)"
            >
              {{ currentBindType === 'phone' ? (phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s 后重发` : '发送验证码') : (emailCodeCountdown > 0 ? `${emailCodeCountdown}s 后重发` : '发送验证码') }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="modifyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleModifySubmit">
            确认修改
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted ,reactive,computed,onUnmounted} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publicAxios,authAxios } from '@/utils/request'
import { Iphone, Message, ArrowRight,Key,EditPen } from '@element-plus/icons-vue'
import { watch } from 'vue'
import ChangePassword from '@/components/ChangePassword.vue';

const emit = defineEmits(['userUpdated']);

const router = useRouter()
const user = reactive({
  username: '',
  phone: '',
  email: '',
  avatarUrl: ''
})

//对 el-upload 组件的引用，用来手动触发上传操作（点击头像后模拟点击 input）
const uploadRef = ref(null)

const editMode = ref(false)
const newUsername = ref('')

const bindNewDialogVisible = ref(false);
const modifyDialogVisible = ref(false);

const currentBindType = ref('');

const phoneCodeCountdown = ref(0);
let phoneCodeTimer = null;
const emailCodeCountdown = ref(0);
let emailCodeTimer = null;

const formRef = ref(null);
const form = reactive({
  phone: '',
  email: '',
  code: '',
});

const changePasswordVisible = ref(false);

const fetchUserInfo = async () => {
  try {
    const res = await authAxios.get('auth/me')
    if (res.data.code === 200) {
        Object.assign(user, res.data.data);
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

        if (response.data.code === 200) {
            ElMessage.success('头像上传成功')
            console.log(response.data.data)
            user.avatarUrl = response.data.data.url || ''
            emit('userUpdated');
            console.log(user.value.avatarUrl)
        } else {
            ElMessage.error(response.data.message || '上传失败')
        }
    } catch (error) {
        ElMessage.error('上传失败，请稍后重试')
    }
}

const updateUsername = async () => {
  if (!newUsername.value.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }

  try {
    const response = await authAxios.put('/auth/updateUsername', {
      newusername: newUsername.value
    })

    if (response.data.code === 200) {
      ElMessage.success('用户名更新成功')
      user.username = newUsername.value
      emit('userUpdated');
      editMode.value = false
    } else {
      ElMessage.error(response.data.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('请求失败，请稍后重试')
  }
}
const cancelEdit = () => {
  editMode.value = false
  newUsername.value = user.username
}
watch(editMode, (val) => {
  if (val) {
    newUsername.value = user.username
  }
})

// --- 计算属性 ---
const newBindDialogTitle = computed(() => {
  return currentBindType.value === 'phone' ? '绑定手机号码' : '绑定邮箱';
});

const modifyDialogTitle = computed(() => {
  return currentBindType.value === 'phone' ? '修改手机号码' : '修改邮箱';
});

const formRules = computed(() => {
  const rules = {
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  };
  if (currentBindType.value === 'phone') {
    rules.phone = [
      { required: true, message: '请输入手机号码', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ];
  } else if (currentBindType.value === 'email') {
    rules.email = [
      { required: true, message: '请输入邮箱地址', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ];
  }
  return rules;
});

const handleBindModifyClick = (type) => {
  currentBindType.value = type;
  form.phone = '';
  form.email = '';
  form.code = '';
  formRef.value?.resetFields();

  if (currentBindType.value === 'phone') {
    if (user.phone) {
      modifyDialogVisible.value = true;
    } else {
      bindNewDialogVisible.value = true;
    }
  } else if (currentBindType.value === 'email') {
    if (user.email) {
      modifyDialogVisible.value = true;
    } else {
      bindNewDialogVisible.value = true;
    }
  }
};

const handleFormDialogClose = () => {
  formRef.value?.resetFields();
  if (phoneCodeTimer) { clearInterval(phoneCodeTimer); phoneCodeCountdown.value = 0; phoneCodeTimer = null; }
  if (emailCodeTimer) { clearInterval(emailCodeTimer); emailCodeCountdown.value = 0; emailCodeTimer = null; }
};

const startCodeCountdown = (type) => {
  let countdownRef;
  let setTimerRef;

  if (type === 'phone') {
    countdownRef = phoneCodeCountdown;
    setTimerRef = (val) => { phoneCodeTimer = val; };
  } else if (type === 'email') {
    countdownRef = emailCodeCountdown;
    setTimerRef = (val) => { emailCodeTimer = val; };
  } else {
    console.error('Invalid countdown type:', type);
    return;
  }

  if (phoneCodeTimer && type === 'phone') { clearInterval(phoneCodeTimer); setTimerRef(null); }
  if (emailCodeTimer && type === 'email') { clearInterval(emailCodeTimer); setTimerRef(null); }

  countdownRef.value = 60;
  const newTimer = setInterval(() => {
    if (countdownRef.value > 0) {
      countdownRef.value--;
    } else {
      clearInterval(newTimer);
      setTimerRef(null);
    }
  }, 1000);
  setTimerRef(newTimer);
};

// 辅助函数：当发送验证码请求失败时，停止倒计时并重置
const stopCountdownOnError = (type) => {
    if (type === 'phone' && phoneCodeTimer) {
        clearInterval(phoneCodeTimer);
        phoneCodeCountdown.value = 0;
        phoneCodeTimer = null;
    } else if (type === 'email' && emailCodeTimer) {
        clearInterval(emailCodeTimer);
        emailCodeCountdown.value = 0;
        emailCodeTimer = null;
    }
};

// 通用的发送验证码错误处理函数，统一提示错误信息
const handleSendCodeError = (error, typeName) => {
  if (error.response) {
    const errorMessage = error.response.data.message || error.response.statusText || '服务器错误。';
    ElMessage.error(`${typeName}验证码发送失败: ${errorMessage}`);
    if (error.response.status === 400 && (errorMessage.includes('频繁') || errorMessage.includes('too frequent'))) {
       ElMessage.warning('发送频率过高，请稍后再试。');
    } else if (error.response.status === 401) {
        ElMessage.error('您的登录已过期或未授权。');
    }
  } else if (error.request) {
    ElMessage.error(`${typeName}验证码发送失败：网络无响应，请检查您的网络连接。`);
  } else {
    ElMessage.error(`${typeName}验证码发送失败：发生未知错误。`);
  }
};


// 1. 绑定手机号发送验证码 
const sendBindPhoneCode = async () => {
  form.code = '';
  const phoneValidated = await formRef.value.validateField('phone', () => {});
  if (!phoneValidated) { ElMessage.warning('请先输入正确的手机号码！'); return; }
  startCodeCountdown('phone');
  try {
    const res = await authAxios.post('/captcha/bindSms', null, { params: { phone: form.phone } });
    if (res.data.code === 0) { 
      ElMessage.success(res.data.message || '手机验证码已发送，请注意查收！');
    } else {
      ElMessage.error(res.data.message || '手机验证码发送失败，请重试。');
      stopCountdownOnError('phone');
    }
  } catch (error) {
    console.error('发送绑定手机验证码请求失败:', error);
    stopCountdownOnError('phone');
    handleSendCodeError(error, '手机');
  }
};

// 2. 绑定邮箱发送验证码 
const sendBindEmailCode = async () => {
  form.code = '';
  const emailValidated = await formRef.value.validateField('email', () => {});
  if (!emailValidated) { ElMessage.warning('请先输入正确的邮箱地址！'); return; }
  startCodeCountdown('email');
  try {
    const res = await authAxios.post('/captcha/bindEmail', null, { params: { email: form.email } });
    if (res.data.code === 0) { 
      ElMessage.success(res.data.message || '邮箱验证码已发送，请注意查收！');
    } else {
      ElMessage.error(res.data.message || '邮箱验证码发送失败，请重试。');
      stopCountdownOnError('email');
    }
  } catch (error) {
    console.error('发送绑定邮箱验证码请求失败:', error);
    stopCountdownOnError('email');
    handleSendCodeError(error, '邮箱');
  }
};

// 3. 修改手机号发送验证码
const sendModifyPhoneCode = async () => {
  form.code = '';
  const phoneValidated = await formRef.value.validateField('phone', () => {});
  if (!phoneValidated) { ElMessage.warning('请先输入正确的新手机号码！'); return; }
  startCodeCountdown('phone');
  try {
    const res = await authAxios.post('/captcha/resetBySms', null, { params: { phone: form.phone } });
    if (res.data.code === 0) { 
      ElMessage.success(res.data.message || '新手机验证码已发送，请注意查收！');
    } else {
      ElMessage.error(res.data.message || '新手机验证码发送失败，请重试。');
      stopCountdownOnError('phone');
    }
  } catch (error) {
    console.error('发送修改手机验证码请求失败:', error);
    stopCountdownOnError('phone');
    handleSendCodeError(error, '新手机');
  }
};

// 4. 修改邮箱发送验证码
const sendModifyEmailCode = async () => {
  form.code = '';
  const emailValidated = await formRef.value.validateField('email', () => {});
  if (!emailValidated) { ElMessage.warning('请先输入正确的新邮箱地址！'); return; }
  startCodeCountdown('email');
  try {
    const res = await authAxios.post('/captcha/resetByEmail', null, { params: { email: form.email } });
    if (res.data.code === 0) { 
      ElMessage.success(res.data.message || '新邮箱验证码已发送，请注意查收！');
    } else {
      ElMessage.error(res.data.message || '新邮箱验证码发送失败，请重试。');
      stopCountdownOnError('email');
    }
  } catch (error) {
    console.error('发送修改邮箱验证码请求失败:', error);
    stopCountdownOnError('email');
    handleSendCodeError(error, '新邮箱');
  }
};


// 处理绑定操作的提交 
const handleBindSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请检查表单填写');
      return;
    }

    try {
      let requestPayload = {
        code: form.code,
        role: "ROLE_USER", 
      };

      if (currentBindType.value === 'phone') {
        requestPayload.newPhone = form.phone;
        requestPayload.newEmail = null; // 确保另一个字段为 null
      } else if (currentBindType.value === 'email') {
        requestPayload.newEmail = form.email;
        requestPayload.newPhone = null; // 确保另一个字段为 null
      }

      const res = await authAxios.put('/auth/rebind', requestPayload); 

      if (res.data.code === 0) { 
        ElMessage.success(`绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}成功！`);
        bindNewDialogVisible.value = false;
        emit('userUpdated');
        await fetchUserInfo(); 
      } else {
        ElMessage.error(res.data.message || `绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}失败`);
      }
    } catch (error) {
      console.error(`绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败:`, error);
      if (error.response && error.response.status === 400) {
        const errorMessageFromBackend = error.response.data.message || '提交的信息有误。';
        if (errorMessageFromBackend.includes('已注册')) {
          ElMessage.error(`${currentBindType.value === 'phone' ? '该手机号' : '该邮箱'}已被注册，请更换。`);
        } else {
          ElMessage.error(`提交信息有误: ${errorMessageFromBackend}`);
        }
      } else {
        ElMessage.error(`操作失败，请稍后再试。`);
      }
    }
  });
};

// 处理修改操作的提交 
const handleModifySubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请检查表单填写');
      return;
    }

    try {
      let requestPayload = {
        code: form.code,
        role: "ROLE_USER", 
      };

      if (currentBindType.value === 'phone') {
        requestPayload.newPhone = form.phone;
        requestPayload.newEmail = null; // 确保另一个字段为 null
      } else if (currentBindType.value === 'email') {
        requestPayload.newEmail = form.email;
        requestPayload.newPhone = null; // 确保另一个字段为 null
      }

      const res = await authAxios.put('/auth/rebind', requestPayload); 

      if (res.data.code === 0) {
        ElMessage.success(`修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}成功！`);
        modifyDialogVisible.value = false;
        emit('userUpdated');
        await fetchUserInfo(); 
      } else {
        ElMessage.error(res.data.message || `修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}失败`);
      }
    } catch (error) {
      console.error(`修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败:`, error);
      if (error.response && error.response.status === 400) {
        const errorMessageFromBackend = error.response.data.message || '提交的信息有误。';
        if (errorMessageFromBackend.includes('已注册')) {
          ElMessage.error(`${currentBindType.value === 'phone' ? '该手机号' : '该邮箱'}已被注册，请更换。`);
        } else {
          ElMessage.error(`提交信息有误: ${errorMessageFromBackend}`);
        }
      } else {
        ElMessage.error(`操作失败，请稍后再试。`);
      }
    }
  });
};

onUnmounted(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer);
    emailCodeTimer = null;
  }
  if (phoneCodeTimer) {
    clearInterval(phoneCodeTimer);
    phoneCodeTimer = null;
  }
});

onMounted(fetchUserInfo)
</script>

<style scoped>
.account-settings-card {
  border-radius: 50px;
  padding: 0;
}

.account-item {
  display: flex;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.account-item:hover {
  background-color: #f5f7fa;
}

.account-item i {
  margin-right: 12px;
  font-size: 18px;
  color: #606266;
}

.account-item span {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.item-value {
  color: #909399;
  text-align: right;
  font-size: 14px;
}

.avatar-item {
  padding: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username-info {
  margin-left: 16px;
}

.username-display {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  display: flex; /* 文本和图标在同一行 */
  align-items: center; /* 垂直居中 */
  gap: 12px; /* 文本和图标的间距 */
}

.username-display .username-text {
  font-size: 22px; /* 字体大小 */
  font-weight: bold; /* 加粗字体 */
  transition: color 0.2s; /* 保持过渡效果 */
}

.username-display .edit-icon {
  font-size: 16px;
  color: #909399;
  transition: color 0.2s;
}

.username-display:hover .edit-icon {
  color: #6da0b1;
}

.username-edit-container {
  display: flex;          
  align-items: center;   
  gap: 10px;              
}

.username-input {
  font-size: 18px;
  width: 200px; 
  height: 40px;
}

</style>
