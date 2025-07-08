<template>
  <div class="admin-page">
    <aside class="sidebar">
      <el-button type="info" :icon="ArrowLeft" class="back-to-home-btn" @click="goToHome">
        返回
      </el-button>
      <el-avatar
        :src="adminInfo.avatarUrl || '/default-admin-avatar.png'"
        :size="100"
        class="avatar"
      />
      <h2 class="username">{{ adminInfo.username }}</h2>
      <div class="stats">
        <div><strong>{{ adminInfo.totalUsers }}</strong><p>注册用户</p></div>
        <div><strong>{{ adminInfo.totalMerchants }}</strong><p>注册团长</p></div>
        <div><strong>{{ adminInfo.pendingTours }}</strong><p>待审旅行团</p></div>
      </div>

      <div class="sidebar-menu">
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'overview' }"
          @click="activeTab = 'overview'"
        >
          <el-icon><Monitor /></el-icon>
          <span>概览</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'userManagement' }"
          @click="activeTab = 'userManagement'"
        >
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'merchantManagement' }"
          @click="activeTab = 'merchantManagement'"
        >
          <el-icon><Shop /></el-icon>
          <span>团长管理</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'tourReview' }"
          @click="activeTab = 'tourReview'"
        >
          <el-icon><PictureFilled /></el-icon>
          <span>旅行团审核</span>
          <el-badge v-if="pendingToursCount > 0" :value="pendingToursCount" class="notification-badge" />
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'reportHanding' }"
          @click="activeTab = 'reportHanding'"
        >
          <el-icon><MagicStick /></el-icon>
          <span>举报处理</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'friends' }"
          @click="activeTab = 'friends'"
        >
          <el-icon><User /></el-icon>
          <span>我的好友</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'systemSettings' }"
          @click="activeTab = 'systemSettings'"
        >
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <el-tabs v-model="activeTab" class="hidden-tabs-header">
        <el-tab-pane label="概览" name="overview">
          <AdminOverview/>
        </el-tab-pane>
        <el-tab-pane label="用户管理" name="userManagement">
          <h3 class="tab-header">用户列表</h3>
          <el-input v-model="userSearchQuery" placeholder="搜索用户ID、用户名或邮箱" clearable @input="debouncedSearchUsers" class="search-input"></el-input>
          <el-table :data="users" v-loading="userLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="username" label="用户名"></el-table-column>
            <el-table-column prop="email" label="邮箱"></el-table-column>
            <el-table-column prop="phone" label="电话"></el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getBanStatusType(row.banDurationDays)">
                  {{ getBanStatusText(row.banDurationDays) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewUserDetails(row)">查看详情</el-button>
                <el-button link :type="row.banDurationDays === 0 ? 'danger' : 'success'" size="small" @click="toggleBanStatus(row)">
                  {{ row.banDurationDays === 0 ? '封禁' : '解封' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="userTotal"
            :page-size="userPageSize"
            v-model:current-page="userCurrentPage"
            @current-change="fetchUsers"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="团长管理" name="merchantManagement">
          <h3 class="tab-header">团长列表</h3>
          <el-input v-model="merchantSearchQuery" placeholder="搜索团长ID、用户名或公司名" clearable @input="debouncedSearchMerchants" class="search-input"></el-input>
          <el-table :data="merchants" v-loading="merchantLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="username" label="用户名"></el-table-column>
            <el-table-column prop="companyName" label="公司名称"></el-table-column>
            <el-table-column prop="email" label="联系邮箱"></el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getBanStatusType(row.banDurationDays)">
                  {{ getBanStatusText(row.banDurationDays) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewMerchantDetails(row)">查看详情</el-button>
                <el-button link :type="row.banDurationDays === 0 ? 'danger' : 'success'" size="small" @click="toggleBanStatus(row)">
                  {{ row.banDurationDays === 0 ? '封禁' : '解封' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="merchantTotal"
            :page-size="merchantPageSize"
            v-model:current-page="merchantCurrentPage"
            @current-change="fetchMerchants"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="旅行团审核" name="tourReview">
          <h3 class="tab-header">待审核旅行团</h3>
          <el-table :data="pendingTours" v-loading="tourReviewLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="title" label="团名"></el-table-column>
            <el-table-column prop="merchant.username" label="发布团长"></el-table-column>
            <el-table-column prop="durationInDays" label="天数"></el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewTourDetails(row)">查看详情</el-button>
                <el-button link type="success" size="small" @click="approveTour(row.id)">通过</el-button>
                <el-button link type="danger" size="small" @click="rejectTour(row.id)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="pendingToursTotal"
            :page-size="tourReviewPageSize"
            v-model:current-page="tourReviewCurrentPage"
            @current-change="fetchPendingTours"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="举报处理" name="reportHanding">
          <div style="margin-bottom: 32px;">
            <ReportHanding/>
          </div>
          </el-tab-pane>


        <el-tab-pane label="我的好友" name="friends">
          <h3 class="tab-header">我的好友</h3>
          <div style="margin-bottom: 32px;">
            <MyFriends/>
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统设置" name="systemSettings">
          <h3 class="tab-header">系统配置</h3>
          <div style="margin-bottom: 32px;">
            <AccountOverview @userUpdated="handleUserUpdated"/>
          </div>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-dialog v-model="userDetailsDialog" :title="`用户详情: ${selectedUser.username}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="用户ID">{{ selectedUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selectedUser.email }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ selectedUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册日期">{{ formatTime(selectedUser.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedUser.banDurationDays === 0 ? 'success' : 'danger'">
            {{ selectedUser.banDurationDays === 0 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="userDetailsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="merchantDetailsDialog" :title="`团长详情: ${selectedMerchant.username}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="团长ID">{{ selectedMerchant.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedMerchant.username }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ selectedMerchant.email }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ selectedMerchant.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册日期">{{ formatTime(selectedMerchant.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedMerchant.banDurationDays === 0 ? 'success' : 'danger'">
            {{ selectedMerchant.banDurationDays === 0 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="merchantDetailsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tourDetailsDialog" :title="`旅行团审核: ${selectedTour.title}`" width="800px">
      <el-descriptions border :column="2" class="detail-descriptions">
        <el-descriptions-item label="团名">{{ selectedTour.title }}</el-descriptions-item>
        <el-descriptions-item label="发布团长">{{ selectedTour.merchant.username }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ selectedTour.price }}</el-descriptions-item>
        <el-descriptions-item label="天数">{{ selectedTour.durationInDays }}</el-descriptions-item>
        <el-descriptions-item label="封面图">
          <img :src="selectedTour.coverImageUrl" alt="封面" style="width: 120px; height: 90px; object-fit: cover; border-radius: 8px;">
        </el-descriptions-item>
        <el-descriptions-item label="详情描述" :span="2">
          <p>{{ selectedTour.description }}</p>
        </el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="tourDetailsDialog = false">取消</el-button>
        <el-button type="danger" @click="rejectTour(selectedTour.id)">拒绝</el-button>
        <el-button type="success" @click="approveTour(selectedTour.id)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick,reactive } from 'vue';
import { User, Shop, PictureFilled, Document, Setting, EditPen, ArrowLeft } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { authAxios } from '@/utils/request'; 
import { useRouter } from 'vue-router';
import AccountOverview from '@/components/AccountOverview.vue'  
import ReportHanding from '@/components/profile/ReportHanding.vue'  
import MyFriends from '@/components/profile/MyFriends.vue' ;
import AdminOverview from '@/components/profile/AdminOverview.vue';


const router = useRouter();

// --- 管理员信息及侧边栏数据 ---
const adminInfo = reactive({
  id: '',
  email: '',
  phone: '',
  role: '', 
  username: '',
  avatarUrl: '',
  createdAt: '',
  updatedAt: '',
});

const activeTab = ref('overview'); // 默认激活的 Tab

// --- 用户管理数据 ---
const users = ref([]);
const userLoading = ref(false);
const userTotal = ref(0);
const userCurrentPage = ref(1);
const userPageSize = 10;
const userSearchQuery = ref('');
const selectedUser = ref({});
const userDetailsDialog = ref(false);

// --- 团长管理数据 ---
const merchants = ref([]);
const merchantLoading = ref(false);
const merchantTotal = ref(0);
const merchantCurrentPage = ref(1);
const merchantPageSize = 10;
const merchantSearchQuery = ref('');
const selectedMerchant = ref({});
const merchantDetailsDialog = ref(false);

// --- 旅行团审核数据 ---
const pendingTours = ref([]);
const tourReviewLoading = ref(false);
const pendingToursTotal = ref(0);
const tourReviewCurrentPage = ref(1);
const tourReviewPageSize = 10;
const selectedTour = ref({}); // 用于查看详情和审核的旅行团对象
const tourDetailsDialog = ref(false);
const pendingToursCount = ref(0); // 侧边栏徽标显示数量

// --- 游记审核数据 ---
const pendingNotes = ref([]);
const noteReviewLoading = ref(false);
const pendingNotesTotal = ref(0);
const noteReviewCurrentPage = ref(1);
const noteReviewPageSize = 10;
const selectedNote = ref({}); // 用于查看详情和审核的游记对象
const noteDetailsDialog = ref(false);
const pendingNotesCount = ref(0); // 侧边栏徽标显示数量

// --- 系统设置数据 ---
const adminSettingsForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: '',
});

// --- API 请求函数 ---

// 辅助函数：防抖动
let timeout = null;
const debounce = (func, delay) => {
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), delay);
  };
};

// 辅助函数：格式化时间戳
const formatTime = (timeString) => {
  if (!timeString) return '';
  const date = new Date(timeString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

//获取管理员个人信息
const fetchAdminInfo = async () => {
  try {
    const res = await authAxios.get('/auth/me'); // 调用同一个接口

    if (res.data.code === 200) {
      Object.assign(adminInfo, res.data.data);
      //ElMessage.success('管理员信息获取成功！');
      console.log('Admin Info Data:', adminInfo); // 调试用
    } else {
      ElMessage.error(res.data.message || '获取管理员信息失败');
      console.error('Failed to fetch admin info:', res.data.message);
    }
  } catch (error) {
    if (error.response) {
      console.error('Error response data:', error.response.data);
      ElMessage.error(`获取管理员信息失败：${error.response.data.message || '服务器错误'}`);
    } else if (error.request) {
      console.error('No response received:', error.request);
      ElMessage.error('获取管理员信息失败：网络错误或服务器无响应');
    } else {
      console.error('Error setting up the request:', error.message);
      ElMessage.error(`获取管理员信息失败：${error.message}`);
    }
  }
};


// 获取管理员概览数据 (侧边栏统计)
const fetchAdminOverview = async () => {
  try {
    const response = await authAxios.get('/api/admin/overview'); 
    if (response.data.code === 200) {
      adminInfo.value.totalUsers = response.data.data.totalUsers;
      adminInfo.value.totalMerchants = response.data.data.totalMerchants;
      adminInfo.value.pendingTours = response.data.data.pendingTours;
      pendingToursCount.value = response.data.data.pendingTours; // 更新徽标
      pendingNotesCount.value = response.data.data.pendingNotes || 0;
    }
  } catch (error) {
    console.error('获取管理员概览失败:', error);
    ElMessage.error('获取管理员概览数据失败。');
  }
};

// 获取用户列表
const fetchUsers = async () => {
  userLoading.value = true;
  try {
    const response = await authAxios.get('/admin/users', {
      params: {
        role: 'ROLE_USER',
        page: userCurrentPage.value,
        size: userPageSize
      }
    });
    if (response.data.code === 200 && response.data.data) {
      users.value = response.data.data.content;
      userTotal.value = response.data.data.totalElements;
    } else {
      ElMessage.error(response.data.message || '获取用户列表失败');
    }
  } catch (error) {
    console.error('获取用户列表时发生错误:', error);
    ElMessage.error('加载用户列表失败。');
  } finally {
    userLoading.value = false;
  }
};
const debouncedSearchUsers = debounce(fetchUsers, 500); // 搜索防抖

// 切换用户状态 (禁用/启用)
const toggleUserStatus = async (userRow) => {
  const newStatus = userRow.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const actionText = newStatus === 'DISABLED' ? '禁用' : '启用';
  ElMessageBox.confirm(`确定要${actionText}用户 ${userRow.username} 吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/users/${userRow.id}/status`, { status: newStatus });
        if (response.data.code === 200) {
          ElMessage.success(`${actionText}用户成功！`);
          userRow.status = newStatus; // 乐观更新UI
        } else {
          ElMessage.error(response.data.message || `${actionText}用户失败`);
        }
      } catch (error) {
        console.error(`${actionText}用户时发生错误:`, error);
        ElMessage.error(`${actionText}用户失败，请稍后再试。`);
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 查看用户详情
const viewUserDetails = (userRow) => {
  selectedUser.value = { ...userRow }; // 复制一份数据
  userDetailsDialog.value = true;
  console.log('查看用户详情:',selectedUser.value)
};

// 获取团长列表
const fetchMerchants = async () => {
  merchantLoading.value = true;
  try {
    const response = await authAxios.get('/admin/users', {
      params: {
        role: 'ROLE_MERCHANT',
        page: merchantCurrentPage.value,
        size: merchantPageSize
      }
    });
    if (response.data.code === 200 && response.data.data) {
      merchants.value = response.data.data.content;
      merchantTotal.value = response.data.data.totalElements;
    } else {
      ElMessage.error(response.data.message || '获取团长列表失败');
    }
  } catch (error) {
    console.error('获取团长列表时发生错误:', error);
    ElMessage.error('加载团长列表失败。');
  } finally {
    merchantLoading.value = false;
  }
};
const debouncedSearchMerchants = debounce(fetchMerchants, 500); // 搜索防抖


// 查看团长详情
const viewMerchantDetails = (merchantRow) => {
  selectedMerchant.value = { ...merchantRow };
  merchantDetailsDialog.value = true;
  console.log('查看团长详情:',selectedMerchant.value )
};

// 获取待审核旅行团列表
const fetchPendingTours = async () => {
  tourReviewLoading.value = true;
  try {
    const response = await authAxios.get('/admin/approvals/travel-packages', { 
      params: {
        page: tourReviewCurrentPage.value,
        size: tourReviewPageSize
      }
    });
    if (response.data.code === 200 && response.data.data) {
      pendingTours.value = response.data.data.content;
      pendingToursTotal.value = response.data.data.totalElements;
      pendingToursCount.value = response.data.data.totalElements; 
    } else {
      ElMessage.error(response.data.message || '获取待审核旅行团失败');
    }
  } catch (error) {
    console.error('获取待审核旅行团时发生错误:', error);
    ElMessage.error('加载待审核旅行团失败。');
  } finally {
    tourReviewLoading.value = false;
  }
};

// 查看旅行团详情并弹窗审核
const viewTourDetails = (tourRow) => {
  selectedTour.value = { ...tourRow };
  tourDetailsDialog.value = true;
  console.log('查看旅行团详情:',selectedTour.value)
};

// 审核通过旅行团
const approveTour = async (tourId) => {
  ElMessageBox.confirm('确定要通过此旅行团的审核吗？', '提示', {
    type: 'success'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/admin/approvals/travel-packages/${tourId}/approve`); 
        if (response.data.code === 200) {
          ElMessage.success('旅行团审核通过！');
          // 移除已审核的旅行团并刷新列表
          await fetchPendingTours();
          tourDetailsDialog.value = false; // 关闭详情弹窗
        } else {
          ElMessage.error(response.data.message || '审核失败');
        }
      } catch (error) {
        console.error('审核旅行团时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 拒绝旅行团
const rejectTour = async (tourId) => {
  ElMessageBox.prompt('确定要拒绝此旅行团的审核吗？请输入拒绝原因:', '拒绝旅行团', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{2,200}$/, 
    inputErrorMessage: '拒绝原因不能为空，且长度需在10到200个字符之间。',
    type: 'warning' 
  })
    .then(async ({ value: rejectReason }) => { 
      try {
        const response = await authAxios.post(`/admin/approvals/travel-packages/${tourId}/reject`, {
          reason: rejectReason 
        });

        if (response.data.code === 200) {
          ElMessage.success('旅行团已拒绝！');
          await fetchPendingTours(); 
          tourDetailsDialog.value = false; 
        } else {
          ElMessage.error(response.data.message || '拒绝失败');
        }
      } catch (error) {
        console.error('拒绝旅行团时发生错误:', error);
        if (error.response && error.response.data && error.response.data.message) {
          ElMessage.error(error.response.data.message); // 显示后端返回的具体错误信息
        } else {
          ElMessage.error('操作失败，请稍后再试。');
        }
      }
    })
    .catch((action) => {
      // 用户点击取消或关闭弹窗
      if (action === 'cancel') {
        ElMessage.info('已取消拒绝操作。');
      } else {
        console.error('ElMessageBox.prompt 发生错误或被取消:', action);
      }
    });
};

// 获取封禁状态
const getBanStatusText = (banDurationDays) => {
  if (banDurationDays === -1) {
    return '永久封禁';
  } else if (banDurationDays === 0) {
    return '正常';
  } else if (banDurationDays > 0) {
    return `封禁 ${banDurationDays} 天`;
  }
  return '未知状态'; 
};

// 根据状态返回类型颜色
const getBanStatusType = (banDurationDays) => {
  if (banDurationDays === -1) {
    return 'danger'; // 永久封禁用红色
  } else if (banDurationDays === 0) {
    return 'success'; // 正常用绿色
  } else if (banDurationDays > 0) {
    return 'warning'; // 暂时封禁用黄色/橙色
  }
  return 'info'; // 未知状态用灰色
};

// 通用切换状态
const toggleBanStatus = async (user) => {
  const isCurrentlyBanned = user.banDurationDays !== 0; 
  let confirmTitle = '';
  let confirmMessage = '';
  let successMessage = '';
  let apiUrl = '';
  let targetBanDuration = 0; 

  if (isCurrentlyBanned) {
    // 当前已封禁，点击按钮是执行“解封”操作
    confirmTitle = '解封该账号';
    confirmMessage = `确定要解封 ${user.username} (ID: ${user.id}) 吗？解封后，该账号将恢复正常使用。`;
    apiUrl = `/admin/users/${user.id}/unban`; // 假设解封接口
    targetBanDuration = 0; // 解封即设置为0天
  } else {
    // 当前是正常状态，点击按钮是执行“封禁”操作
    confirmTitle = '封禁该账号';
    confirmMessage = `确定要封禁 ${user.username} (ID: ${user.id}) 吗？`;
    apiUrl = `/admin/users/${user.id}/ban`; 
    
    try {
      // 弹出一个输入框，让管理员输入封禁天数
      const { value: inputDays } = await ElMessageBox.prompt('请输入封禁天数 (-1 代表永久封禁，0 代表取消封禁或解封):', confirmTitle, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^-1|[0-9]\d*$/, // 允许输入 -1 或任意非负整数
        inputErrorMessage: '请输入 -1 或非负整数天数！'
      });
      
      const parsedDays = parseInt(inputDays, 10);
      if (isNaN(parsedDays)) {
        ElMessage.warning('无效的封禁天数输入。');
        return;
      }
      
      targetBanDuration = parsedDays; // 设置目标封禁天数

      if (targetBanDuration === 0 && !isCurrentlyBanned) {
        // 如果当前是正常状态，却输入了 0，提示用户这等同于不封禁
        ElMessage.info('您输入了 0 天，表示不执行封禁操作。');
        return; 
      }
      if (targetBanDuration > 0) {
         confirmMessage = `确定要封禁 ${user.username} (ID: ${user.id}) ${targetBanDuration} 天吗？`;
      } else if (targetBanDuration === -1) {
         confirmMessage = `确定要永久封禁 ${user.username} (ID: ${user.id}) 吗？`;
      }
      
    } catch (cancel) {
      ElMessage.info('已取消封禁操作。');
      return; 
    }
  }

  try {
    await ElMessageBox.confirm(confirmMessage, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: isCurrentlyBanned ? 'success' : 'warning', // 解封用绿色确认，封禁用黄色确认
      dangerouslyUseHTMLString: true 
    });

    const response = await authAxios.post(apiUrl, { durationInDays: targetBanDuration });

    if (response.data.code === 200) {
      successMessage = isCurrentlyBanned ? '已成功解封！' : '已成功封禁！';
      ElMessage.success(successMessage);
      user.banDurationDays = targetBanDuration;
    } else {
      ElMessage.error(response.data.message || '操作失败。');
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('操作已取消。');
    } else {
      console.error('更新状态失败:', error);
      ElMessage.error('操作失败，请稍后再试。');
    }
  }
};

const handleUserUpdated = () => {
  console.log('Parent: User update notification received from child. Re-fetching user profile...');
  fetchAdminInfo();
};

// --- 初始化数据 ---
onMounted(() => {
  fetchAdminInfo();  //获取管理员个人信息
  // fetchAdminOverview(); // 获取管理员概览数据
  fetchUsers();         // 加载用户列表
  fetchMerchants();     // 加载团长列表
  fetchPendingTours();  // 加载待审核旅行团
  //fetchPendingNotes();  // 加载待审核游记
});

// 跳转首页
const goToHome = () => {
  router.push('/')
}

</script>

<style scoped>
/* 引入 Google Fonts - Poppins */
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');

.admin-page {
  display: flex;
  padding: 40px;
  gap: 30px;
  min-height: 100vh;
  box-sizing: border-box;
  font-family: 'Poppins', sans-serif;
  background-image: url('@/assets/bg1.jpg'); 
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}

.sidebar {
  width: 280px;
  background: linear-gradient(135deg, #ffffff, #fdfdfd);
  border-radius: 18px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  height: 100vh; /* 固定高度，内部可滚动 */
  display: flex;
  flex-direction: column;
  align-items: center;
  top: 40px;
  align-self: flex-start;
}

.back-to-home-btn{
  position:absolute;
  top:50px;
  left:50px;
  padding:8px 15px;
  font-size:0.9rem;
  border-radius:8px;
  background-color:#607d8b;
  border-color:#607d8b;
  color:#fff;
  transition:all 0.3s ease;
}


.avatar {
  margin-bottom: 16px;
  border: 4px solid rgba(128,203,196,0.55);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.username {
  margin-top: 8px;
  font-size: 1.4rem;
  font-weight: 700;
  color: #333;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin: 25px 0 30px;
  width: 100%;
}

.stats div {
  flex: 1;
  text-align: center;
  font-size: 15px;
  color: rgb(0,100,110);
  padding: 5px 0;
  border-right: 1px solid #eee;
}

.stats div:last-child {
  border-right: none;
}

.stats strong {
  display: block;
  font-size: 20px;
  color: rgb(0,100,110);
  margin-bottom: 4px;
  font-weight: 600;
}

.sidebar-menu {
  width: 100%;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  margin-bottom: 8px;
  cursor: pointer;
  border-radius: 10px;
  transition: background-color 0.3s ease, color 0.3s ease, transform 0.2s ease;
  color: #555;
  font-size: 1.05rem;
  font-weight: 500;
}

.menu-item .el-icon {
  margin-right: 12px;
  font-size: 1.2rem;
  color: #777;
}

.menu-item:hover {
  background-color: rgb(224,242,241);
  color: rgb(0,100,110);
  transform: translateX(5px);
}

.menu-item.active {
  background: linear-gradient(45deg, rgb(77,182,172), rgb(178,223,219));
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.menu-item.active .el-icon {
  color: #ffffff;
}

.notification-badge {
  --el-badge-color: #f56c6c;
  margin-left: 5px;
}

/* 主内容 - 沿用用户主页风格 */
.main-content {
  flex: 1;
  background: #fff;
  border-radius: 18px;
  padding: 30px 40px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.hidden-tabs-header .el-tabs__header {
  display: none;
}

.hidden-tabs-header .el-tabs__content {
  padding: 0;
}

/* --- 管理员特有样式 --- */
.tab-header {
  font-size: 1.6rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 25px;
  border-bottom: 2px solid #eee;
  padding-bottom: 15px;
}

.search-input {
  width: 300px;
  margin-bottom: 20px;
}

.admin-table {
  border-radius: 10px;
  overflow: hidden; /* 确保圆角显示 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.setting-card {
  margin-top: 20px;
  padding: 20px;
  border-radius: 12px;
}

.setting-card h4 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.2rem;
  color: #444;
  border-bottom: 1px dashed #eee;
  padding-bottom: 10px;
}

.detail-descriptions {
  margin-top: 20px;
}
.detail-descriptions .el-descriptions-item__container {
  align-items: flex-start; /* 文本和内容在顶部对齐 */
}

.pagination-bottom {
  margin-top: 20px;
  justify-content: flex-end;
  display: flex;
}
</style>