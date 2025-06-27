<template>
  <div class="admin-page">
    <aside class="sidebar">
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
      <el-button @click="editProfileDialog = true" type="primary" plain class="edit-profile-btn">编辑资料</el-button>

      <div class="sidebar-menu">
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
          :class="{ 'menu-item': true, 'active': activeTab === 'noteReview' }"
          @click="activeTab = 'noteReview'"
        >
          <el-icon><Document /></el-icon>
          <span>游记审核</span>
          <el-badge v-if="pendingNotesCount > 0" :value="pendingNotesCount" class="notification-badge" />
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
        <el-tab-pane label="用户管理" name="userManagement">
          <h3 class="tab-header">用户列表</h3>
          <el-input v-model="userSearchQuery" placeholder="搜索用户ID、用户名或邮箱" clearable @input="debouncedSearchUsers" class="search-input"></el-input>
          <el-table :data="users" v-loading="userLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="username" label="用户名"></el-table-column>
            <el-table-column prop="email" label="邮箱"></el-table-column>
            <el-table-column prop="phone" label="电话"></el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewUserDetails(row)">查看详情</el-button>
                <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'" size="small" @click="toggleUserStatus(row)">
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="username" label="用户名"></el-table-column>
            <el-table-column prop="companyName" label="公司名称"></el-table-column>
            <el-table-column prop="contactEmail" label="联系邮箱"></el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewMerchantDetails(row)">查看详情</el-button>
                <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'" size="small" @click="toggleMerchantStatus(row)">
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="title" label="团名"></el-table-column>
            <el-table-column prop="merchantName" label="发布团长"></el-table-column>
            <el-table-column prop="submitDate" label="提交日期"></el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewTourDetails(row)">查看详情</el-button>
                <el-button link type="success" size="small" @click="approveTour(row)">通过</el-button>
                <el-button link type="danger" size="small" @click="rejectTour(row)">拒绝</el-button>
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

        <el-tab-pane label="游记审核" name="noteReview">
          <h3 class="tab-header">待审核游记</h3>
          <el-table :data="pendingNotes" v-loading="noteReviewLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="title" label="游记标题"></el-table-column>
            <el-table-column prop="authorName" label="作者"></el-table-column>
            <el-table-column prop="submitDate" label="提交日期"></el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewNoteDetails(row)">查看详情</el-button>
                <el-button link type="success" size="small" @click="approveNote(row)">通过</el-button>
                <el-button link type="danger" size="small" @click="rejectNote(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="pendingNotesTotal"
            :page-size="noteReviewPageSize"
            v-model:current-page="noteReviewCurrentPage"
            @current-change="fetchPendingNotes"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="系统设置" name="systemSettings">
          <h3 class="tab-header">系统配置</h3>
          <el-card class="setting-card">
            <h4>管理员密码修改</h4>
            <el-form :model="adminSettingsForm" label-width="120px">
              <el-form-item label="旧密码">
                <el-input type="password" v-model="adminSettingsForm.oldPassword"></el-input>
              </el-form-item>
              <el-form-item label="新密码">
                <el-input type="password" v-model="adminSettingsForm.newPassword"></el-input>
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input type="password" v-model="adminSettingsForm.confirmNewPassword"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveAdminSettings">保存密码</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          </el-tab-pane>
      </el-tabs>
    </main>

    <el-dialog v-model="editProfileDialog" title="编辑管理员资料" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input type="password" v-model="editForm.password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editProfileDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDetailsDialog" :title="`用户详情: ${selectedUser.username}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="用户ID">{{ selectedUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selectedUser.email }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ selectedUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册日期">{{ selectedUser.registrationDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedUser.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ selectedUser.status === 'ACTIVE' ? '正常' : '禁用' }}
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
        <el-descriptions-item label="公司名称">{{ selectedMerchant.companyName }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ selectedMerchant.contactEmail }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ selectedMerchant.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="注册日期">{{ selectedMerchant.registrationDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedMerchant.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ selectedMerchant.status === 'ACTIVE' ? '正常' : '禁用' }}
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
        <el-descriptions-item label="发布团长">{{ selectedTour.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ selectedTour.location }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ selectedTour.price }}</el-descriptions-item>
        <el-descriptions-item label="天数">{{ selectedTour.duration }}</el-descriptions-item>
        <el-descriptions-item label="提交日期">{{ selectedTour.submitDate }}</el-descriptions-item>
        <el-descriptions-item label="封面图">
          <img :src="selectedTour.image" alt="封面" style="width: 120px; height: 90px; object-fit: cover; border-radius: 8px;">
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

    <el-dialog v-model="noteDetailsDialog" :title="`游记审核: ${selectedNote.title}`" width="800px">
      <el-descriptions border :column="2" class="detail-descriptions">
        <el-descriptions-item label="游记标题">{{ selectedNote.title }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ selectedNote.authorName }}</el-descriptions-item>
        <el-descriptions-item label="提交日期">{{ selectedNote.submitDate }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ selectedNote.category }}</el-descriptions-item>
        <el-descriptions-item label="封面图" :span="2">
          <img :src="selectedNote.image" alt="封面" style="width: 200px; height: 120px; object-fit: cover; border-radius: 8px;">
        </el-descriptions-item>
        <el-descriptions-item label="游记内容" :span="2">
          <p>{{ selectedNote.content }}</p>
        </el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="noteDetailsDialog = false">取消</el-button>
        <el-button type="danger" @click="rejectNote(selectedNote.id)">拒绝</el-button>
        <el-button type="success" @click="approveNote(selectedNote.id)">通过</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { User, Shop, PictureFilled, Document, Setting, EditPen } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { authAxios } from '@/utils/request'; // 假设你的认证请求实例

// --- 管理员信息及侧边栏数据 ---
const adminInfo = ref({
  username: '管理员',
  avatarUrl: 'https://fuss10.elemecdn.com/a/3f/3302e58f9a181d2509373dffb03dc.jpg', // 示例管理员头像
  totalUsers: 0,
  totalMerchants: 0,
  pendingTours: 0, // 待审核旅行团数量
  // 可以添加更多管理员维度的统计数据
});

const activeTab = ref('userManagement'); // 默认激活的 Tab

const editProfileDialog = ref(false);
const editForm = ref({ username: adminInfo.value.username, password: '' });

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

// 获取管理员概览数据 (侧边栏统计)
const fetchAdminOverview = async () => {
  try {
    const response = await authAxios.get('/api/admin/overview'); // 假设后端有此接口
    if (response.data.code === 200) {
      adminInfo.value.totalUsers = response.data.data.totalUsers;
      adminInfo.value.totalMerchants = response.data.data.totalMerchants;
      adminInfo.value.pendingTours = response.data.data.pendingTours;
      pendingToursCount.value = response.data.data.pendingTours; // 更新徽标
      // 假设后端也返回待审核游记数量
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
    const response = await authAxios.get('/api/admin/users', {
      params: {
        pageNum: userCurrentPage.value,
        pageSize: userPageSize,
        query: userSearchQuery.value // 传递搜索关键词
      }
    });
    if (response.data.code === 200 && response.data.data) {
      users.value = response.data.data.records;
      userTotal.value = response.data.data.total;
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
};

// 获取团长列表
const fetchMerchants = async () => {
  merchantLoading.value = true;
  try {
    const response = await authAxios.get('/api/admin/merchants', {
      params: {
        pageNum: merchantCurrentPage.value,
        pageSize: merchantPageSize,
        query: merchantSearchQuery.value // 传递搜索关键词
      }
    });
    if (response.data.code === 200 && response.data.data) {
      merchants.value = response.data.data.records;
      merchantTotal.value = response.data.data.total;
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

// 切换团长状态
const toggleMerchantStatus = async (merchantRow) => {
  const newStatus = merchantRow.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const actionText = newStatus === 'DISABLED' ? '禁用' : '启用';
  ElMessageBox.confirm(`确定要${actionText}团长 ${merchantRow.username} 吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/merchants/${merchantRow.id}/status`, { status: newStatus });
        if (response.data.code === 200) {
          ElMessage.success(`${actionText}团长成功！`);
          merchantRow.status = newStatus; // 乐观更新UI
        } else {
          ElMessage.error(response.data.message || `${actionText}团长失败`);
        }
      } catch (error) {
        console.error(`${actionText}团长时发生错误:`, error);
        ElMessage.error(`${actionText}团长失败，请稍后再试。`);
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 查看团长详情
const viewMerchantDetails = (merchantRow) => {
  selectedMerchant.value = { ...merchantRow };
  merchantDetailsDialog.value = true;
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
};

// 审核通过旅行团
const approveTour = async (tourId) => {
  ElMessageBox.confirm('确定要通过此旅行团的审核吗？', '提示', {
    type: 'success'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/tours/${tourId}/approve`); // 假设后端接口
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
  ElMessageBox.confirm('确定要拒绝此旅行团的审核吗？', '提示', {
    type: 'danger'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/tours/${tourId}/reject`); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success('旅行团已拒绝！');
          await fetchPendingTours();
          tourDetailsDialog.value = false; // 关闭详情弹窗
        } else {
          ElMessage.error(response.data.message || '拒绝失败');
        }
      } catch (error) {
        console.error('拒绝旅行团时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 获取待审核游记列表
const fetchPendingNotes = async () => {
  noteReviewLoading.value = true;
  try {
    const response = await authAxios.get('/api/admin/notes/pending', { // 假设后端有此接口
      params: {
        pageNum: noteReviewCurrentPage.value,
        pageSize: noteReviewPageSize
      }
    });
    if (response.data.code === 200 && response.data.data) {
      pendingNotes.value = response.data.data.records;
      pendingNotesTotal.value = response.data.data.total;
      pendingNotesCount.value = response.data.data.total; // 更新侧边栏徽标
    } else {
      ElMessage.error(response.data.message || '获取待审核游记失败');
    }
  } catch (error) {
    console.error('获取待审核游记时发生错误:', error);
    ElMessage.error('加载待审核游记失败。');
  } finally {
    noteReviewLoading.value = false;
  }
};

// 查看游记详情并弹窗审核
const viewNoteDetails = (noteRow) => {
  selectedNote.value = { ...noteRow };
  noteDetailsDialog.value = true;
};

// 审核通过游记
const approveNote = async (noteId) => {
  ElMessageBox.confirm('确定要通过此游记的审核吗？', '提示', {
    type: 'success'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/notes/${noteId}/approve`); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success('游记审核通过！');
          await fetchPendingNotes();
          noteDetailsDialog.value = false;
        } else {
          ElMessage.error(response.data.message || '审核失败');
        }
      } catch (error) {
        console.error('审核游记时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 拒绝游记
const rejectNote = async (noteId) => {
  ElMessageBox.confirm('确定要拒绝此游记的审核吗？', '提示', {
    type: 'danger'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/admin/notes/${noteId}/reject`); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success('游记已拒绝！');
          await fetchPendingNotes();
          noteDetailsDialog.value = false;
        } else {
          ElMessage.error(response.data.message || '拒绝失败');
        }
      } catch (error) {
        console.error('拒绝游记时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 保存管理员资料 (目前仅模拟，实际应调用后端接口)
const saveProfile = () => {
  // TODO: 调用后端 API 更新管理员信息，例如 /api/admin/profile
  ElMessage.success('管理员资料保存成功！');
  editProfileDialog.value = false;
};

// 保存系统设置 (目前仅模拟密码修改)
const saveAdminSettings = async () => {
  if (adminSettingsForm.value.newPassword !== adminSettingsForm.value.confirmNewPassword) {
    ElMessage.error('两次输入的新密码不一致！');
    return;
  }
  // TODO: 调用后端 API 更新管理员密码，例如 /api/admin/change-password
  // 确保后端有验证旧密码的逻辑
  ElMessage.success('管理员密码修改成功！');
  adminSettingsForm.value.oldPassword = '';
  adminSettingsForm.value.newPassword = '';
  adminSettingsForm.value.confirmNewPassword = '';
};

// --- 初始化数据 ---
onMounted(() => {
  // fetchAdminOverview(); // 获取管理员概览数据
  // fetchUsers();         // 加载用户列表
  // fetchMerchants();     // 加载团长列表
  fetchPendingTours();  // 加载待审核旅行团
  //fetchPendingNotes();  // 加载待审核游记
});

// 当 activeTab 改变时，重新加载对应数据 (按需)
// Vue 3 的 watch 可以在这里使用，但由于 onMounted 已经全部加载，这里仅作示例
// watch(activeTab, (newTab) => {
//   if (newTab === 'userManagement') fetchUsers();
//   else if (newTab === 'merchantManagement') fetchMerchants();
//   else if (newTab === 'tourReview') fetchPendingTours();
//   else if (newTab === 'noteReview') fetchPendingNotes();
// });

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
  position: sticky;
  top: 40px;
  align-self: flex-start;
  overflow-y: auto; /* 侧边栏内容过多时可滚动 */
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

.edit-profile-btn {
  width: 80%;
  margin-bottom: 30px;
  border-radius: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
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

.pagination-bottom {
  margin-top: 20px;
  justify-content: flex-end; /* 右对齐 */
  display: flex;
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
</style>