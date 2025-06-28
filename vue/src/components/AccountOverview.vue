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
    <div class="account-item" @click="openStatusDialog('phone')">
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
    <div class="account-item" @click="openStatusDialog('email')">
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
      v-model="statusDialogVisible"
      :title="statusDialogTitle"
      width="30%"
      align-center
    >
      <div style="text-align: center; margin-bottom: 20px;">
        <p>{{ bindStatusText }}</p>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="statusDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleStatusDialogAction">
            {{ currentBindType === 'phone' ? (user.phone ? '修改' : '绑定') : (user.email ? '修改' : '绑定') }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindNewDialogVisible"
      :title="newBindDialogTitle"
      width="40%"
      align-center
      @closed="handleFormDialogClose" >
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
              @click="currentBindType === 'phone' ? sendPhoneCode() : sendEmailCode()"
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
      @closed="handleFormDialogClose" >
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
              @click="currentBindType === 'phone' ? sendPhoneCode() : sendEmailCode()"
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

const statusDialogVisible = ref(false);// 控制初始弹窗的显示与隐藏
const currentBindType = ref('');// 存储当前要绑定/修改的类型 ('phone' 或 'email')
const bindNewDialogVisible = ref(false);    // 控制“绑定新手机/邮箱”弹窗
const modifyDialogVisible = ref(false);    // 控制“修改手机/邮箱”弹窗
// --- 表单相关状态和引用 (现在 form 用于新的绑定/修改弹窗) ---
const formRef = ref(null); // 用于引用 <el-form>
const form = reactive({ // 表单数据
  phone: '',
  email: '',
  code: '',
});
// 邮箱和手机的倒计时和计时器
const emailCodeCountdown = ref(0);
let emailTimer = null;
const phoneCodeCountdown = ref(0);
let phoneTimer = null;

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
// 初始状态弹窗的标题
const statusDialogTitle = computed(() => { 
  if (currentBindType.value === 'phone') {
    return user.phone ? '修改手机号码' : '绑定手机号码';
  } else if (currentBindType.value === 'email') {
    return user.email ? '修改邮箱' : '绑定邮箱';
  }
  return '';
});
// 初始状态弹窗的提示文本
const bindStatusText = computed(() => { 
  if (currentBindType.value === 'phone') {
    return user.phone ? `你的手机号码：${user.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')}` : '还没有绑定手机号哦';
  } else if (currentBindType.value === 'email') {
    return user.email ? `你的邮箱：${user.email}` : '还没有绑定邮箱哦';
  }
  return '';
});
// 新绑定弹窗的标题
const newBindDialogTitle = computed(() => {
  return currentBindType.value === 'phone' ? '绑定手机号码' : '绑定邮箱';
});
// 修改弹窗的标题
const modifyDialogTitle = computed(() => {
  return currentBindType.value === 'phone' ? '修改手机号码' : '修改邮箱';
});

// 统一的表单验证规则，根据 currentBindType 动态生成
const formRules = computed(() => {
  const baseRules = { 
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],};

  if (currentBindType.value === 'phone') {
    return {
      phone: [
        { required: true, message: '请输入手机号码', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ],
      baseRules
    };
  } else if (currentBindType.value === 'email') {
    return {
      email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      baseRules
    };
  }
  return {};
});

// 打开初始的状态显示弹窗
const openStatusDialog = (type) => {
  currentBindType.value = type; // 设置当前要处理的类型
  statusDialogVisible.value = true; // 显示初始弹窗
  // 每次打开时，清空表单（避免数据残留影响后续弹窗）
  if (formRef.value) {
    formRef.value.resetFields();
  }
};

// 初始弹窗中“修改”或“绑定”按钮的点击事件
const handleStatusDialogAction = () => {
  statusDialogVisible.value = false; // 关闭初始弹窗

  if (currentBindType.value === 'phone') {
    if (user.phone) { // 已绑定手机号，进入修改流程
      form.phone = user.phone; // 预填充当前手机号
      modifyDialogVisible.value = true; // 打开修改弹窗
    } else { // 未绑定手机号，进入绑定流程
      form.phone = ''; // 清空手机号字段
      bindNewDialogVisible.value = true; // 打开绑定新弹窗
    }
  } else if (currentBindType.value === 'email') {
    if (user.email) { // 已绑定邮箱，进入修改流程
      form.email = user.email; // 预填充当前邮箱
      modifyDialogVisible.value = true; // 打开修改弹窗
    } else { // 未绑定邮箱，进入绑定流程
      form.email = ''; // 清空邮箱字段
      bindNewDialogVisible.value = true; // 打开绑定新弹窗
    }
  }
  // 确保新打开的表单弹窗清除了验证错误
  if (formRef.value) {
    formRef.value.clearValidate();
  }
};

// 所有绑定/修改弹窗关闭时通用的清理函数
const handleFormDialogClose = () => {
  if (formRef.value) {
    formRef.value.resetFields(); // 清空表单数据和验证状态
  }
  // 清除所有定时器
  if (emailTimer) {
    clearInterval(emailTimer);
    emailTimer = null;
    emailCodeCountdown.value = 0;
  }
  if (phoneTimer) {
    clearInterval(phoneTimer);
    phoneTimer = null;
    phoneCodeCountdown.value = 0;
  }
};

// // 电话发送验证码函数
// const sendPhoneCode = async () => {
//   formRef.value.validateField('phone', async (isValid) => {
//     if (!isValid) {
//       ElMessage.error('请先输入有效的手机号码');
//       return;
//     }
//     try {
//       const response = await publicAxios.post('/captcha/sendSms', null, {
//         params: { phone: form.phone }
//       });

//       if (response.data.code === 200) {
//         ElMessage.success(`手机验证码已发送至 ${form.phone}`);
//         if (phoneTimer) {
//           clearInterval(phoneTimer);
//         }
//         phoneCodeCountdown.value = 60;
//         phoneTimer = setInterval(() => {
//           if (phoneCodeCountdown.value > 0) {
//             phoneCodeCountdown.value--;
//           } else {
//             clearInterval(phoneTimer);
//             phoneTimer = null;
//           }
//         }, 1000);
//       } else {
//         ElMessage.error(response.data.message || '发送手机验证码失败');
//       }
//     } catch (error) {
//       console.error('发送手机验证码失败:', error);
//       ElMessage.error('发送手机验证码请求失败，请稍后再试');
//     }
//   });
// };

// 邮箱发送验证码函数 
const sendEmailCode = async () => {
  formRef.value.validateField('email', async (isValid) => {
    if (!isValid) {
      ElMessage.error('请先输入有效的邮箱地址');
      return;
    }
    try {
      const response = await publicAxios.post('/captcha/sendEmail', null, {
        params: { email: form.email }
      });

      if (response.data.code === 200) {
        ElMessage.success(`邮箱验证码已发送至 ${form.email}`);
        if (emailTimer) {
          clearInterval(emailTimer);
        }
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
      console.error('发送邮箱验证码失败:', error);
      ElMessage.error('发送邮箱验证码请求失败，请稍后再试');
    }
  });
};

// 处理绑定操作的提交
const handleBindSubmit = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请检查表单填写');
      return;
    }

    try {
      let apiEndpoint = '';
      let requestPayload = {
        code: form.code,
      };

      if (currentBindType.value === 'phone') {
        apiEndpoint = '/auth/bindPhone'; // 假设绑定手机号接口
        requestPayload.phone = form.phone;
      } else if (currentBindType.value === 'email') {
        apiEndpoint = '/auth/bindEmail'; // 假设绑定邮箱接口
        requestPayload.email = form.email;
      }

      const res = await publicAxios.post(apiEndpoint, requestPayload); // 假设是 POST 请求

      if (res.data.code === 200) {
        ElMessage.success(`绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}成功！`);
        // 刷新用户信息，或直接更新 user.phone/user.email
        await fetchUserBindInfo(); // 假设你有一个方法可以重新获取用户信息并更新user对象
        bindNewDialogVisible.value = false; // 关闭绑定弹窗
      } else {
        ElMessage.error(res.data.message || `绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}失败`);
      }
    } catch (error) {
      console.error(`绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败:`, error);
      ElMessage.error(`绑定${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败，请稍后再试`);
    }
  });
};

// 处理修改操作的提交
const handleModifySubmit = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请检查表单填写');
      return;
    }

    try {
      let apiEndpoint = '';
      let requestPayload = {
        code: form.code,
      };

      if (currentBindType.value === 'phone') {
        apiEndpoint = '/auth/updatePhone'; // 假设修改手机号接口
        requestPayload.oldPhone = user.phone; // 通常需要旧的作为验证
        requestPayload.newPhone = form.phone;
      } else if (currentBindType.value === 'email') {
        apiEndpoint = '/captcha/resetEmail'; // 假设修改邮箱接口
        requestPayload.oldEmail = user.email; // 通常需要旧的作为验证
        requestPayload.newEmail = form.email;
      }

      const res = await publicAxios.post(apiEndpoint, requestPayload); 

      if (res.data.code === 200) {
        ElMessage.success(`修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}成功！`);
        await fetchUserBindInfo(); // 刷新用户信息
        modifyDialogVisible.value = false; // 关闭修改弹窗
      } else {
        ElMessage.error(res.data.message || `修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}失败`);
      }
    } catch (error) {
      console.error(`修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败:`, error);
      ElMessage.error(`修改${currentBindType.value === 'phone' ? '手机' : '邮箱'}请求失败，请稍后再试`);
    }
  });
};

// 组件卸载时清除所有定时器
onUnmounted(() => {
  if (emailTimer) {
    clearInterval(emailTimer);
    emailTimer = null;
  }
  if (phoneTimer) {
    clearInterval(phoneTimer);
    phoneTimer = null;
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
