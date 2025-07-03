<template>
  <div class="merchant-dashboard-page">
    <aside class="sidebar">
      <el-button type="info" :icon="ArrowLeft" class="back-to-home-btn" @click="goToHome">
        返回
      </el-button>
      <el-avatar
        :src="merchantProfile.avatarUrl || '/default-merchant-avatar.png'"
        :size="100"
        class="avatar"
      />
      <h2 class="username">{{ merchantProfile.companyName || merchantProfile.username }}</h2>
      <div class="stats">
        <div><strong>{{ merchantOverview.totalTours }}</strong><p>发布团数</p></div>
        <div><strong>{{ merchantOverview.pendingTours }}</strong><p>待审团数</p></div>
        <div><strong>{{ merchantOverview.totalOrders }}</strong><p>累计订单</p></div>
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
          :class="{ 'menu-item': true, 'active': activeTab === 'tourManagement' }"
          @click="activeTab = 'tourManagement'"
        >
          <el-icon><Compass /></el-icon>
          <span>我的旅行团</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'reviewManagement' }"
          @click="activeTab = 'reviewManagement'"
        >
          <el-icon><Comment /></el-icon>
          <span>评价管理</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'messageCenter' }"
          @click="activeTab = 'messageCenter'"
        >
          <el-icon><Message /></el-icon>
          <span>消息中心</span>
          <el-badge v-if="merchantOverview.unreadMessages > 0" :value="merchantOverview.unreadMessages" class="notification-badge" />
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'accountSettings' }"
          @click="activeTab = 'accountSettings'"
        >
          <el-icon><Setting /></el-icon>
          <span>账户设置</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <el-tabs v-model="activeTab" class="hidden-tabs-header">
        <el-tab-pane label="概览" name="overview">
          <div class="stat-cards-grid">
            <el-card class="stat-card">
              <div class="card-icon"><el-icon><DocumentCopy /></el-icon></div>
              <div class="card-info">
                <p>待审核旅行团</p>
                <h2>{{ merchantOverview.pendingTours }}</h2>
              </div>
            </el-card>
            <el-card class="stat-card">
              <div class="card-icon"><el-icon><Calendar /></el-icon></div>
              <div class="card-info">
                <p>今日订单数</p>
                <h2>{{ merchantOverview.todayOrders }}</h2>
              </div>
            </el-card>
            <el-card class="stat-card">
              <div class="card-icon"><el-icon><TrendCharts /></el-icon></div>
              <div class="card-info">
                <p>本月营收</p>
                <h2>¥{{ merchantOverview.monthlyRevenue }}</h2>
              </div>
            </el-card>
            <el-card class="stat-card">
              <div class="card-icon"><el-icon><StarFilled /></el-icon></div>
              <div class="card-info">
                <p>整体评分</p>
                <h2>{{ merchantOverview.overallRating }}</h2>
              </div>
            </el-card>
            <el-card class="stat-card new-tour-card" @click="goToNewGroupPage">
              <div class="card-icon"><el-icon><Plus /></el-icon></div>
              <div class="card-info">
                <h2>发布新团</h2>
              </div>
              <el-button type="primary" :icon="Plus" circle class="add-tour-btn"></el-button>
            </el-card>
            <el-card class="stat-card add-schedule-card" @click="goToAddSchedulePage">
                <div class="card-icon"><el-icon><Calendar /></el-icon></div>
                <div class="card-info">
                    <h2>添加团期</h2>
                </div>
                <el-button type="primary" :icon="Calendar" circle class="add-schedule-btn"></el-button>
            </el-card>
          </div>

          <el-row :gutter="20" class="overview-sections">
            <el-col :span="12">
              <el-card class="recent-status-card">
                <template #header>
                  <div class="card-header-with-link">
                    <span>近期旅行团审核状态</span>
                    <el-link type="primary" @click="activeTab = 'tourManagement'">查看所有</el-link>
                  </div>
                </template>
                <el-table :data="recentTourReviews" :show-header="false" style="width: 100%">
                  <el-table-column prop="title" label="团名"></el-table-column>
                  <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="getTourReviewStatusType(row.status)">{{ getTourReviewStatusText(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="recentTourReviews.length === 0" description="暂无近期审核记录" :image-size="50"></el-empty>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="recent-status-card">
                <template #header>
                  <div class="card-header-with-link">
                    <span>近期订单概览</span>
                    <el-link type="primary" @click="activeTab = 'orderManagement'">查看所有</el-link>
                  </div>
                </template>
                <el-table :data="recentOrders" :show-header="false" style="width: 100%">
                  <el-table-column prop="tourTitle" label="旅行团"></el-table-column>
                  <el-table-column prop="orderStatus" label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="getOrderStatusType(row.orderStatus)">{{ getOrderStatusText(row.orderStatus) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="orderDate" label="日期" width="120"></el-table-column>
                </el-table>
                <el-empty v-if="recentOrders.length === 0" description="暂无近期订单" :image-size="50"></el-empty>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="我的旅行团" name="tourManagement">
          <el-table :data="myTours" v-loading="tourLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="title" label="团名"></el-table-column>
            <el-table-column prop="description" label="详细描述"></el-table-column>
            <el-table-column prop="startDate" label="出发日期" width="120"></el-table-column>
            <el-table-column prop="price" label="价格" width="100"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getTourStatusType(row.status)">{{ getTourStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewTourDetails(row)">详情</el-button>
                <el-button link type="warning" size="small" @click="editTour(row)">编辑</el-button>
                <el-button 
                  link 
                  type="info" 
                  size="small" 
                  @click="viewOrders(row)" 
                  :disabled="row.status === 'REJECTED' || row.status === 'PENDING_APPROVAL'">
                  查看订单
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="myToursTotal"
            :page-size="myToursPageSize"
            v-model:current-page="myToursCurrentPage"
            @current-change="fetchMyTours"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="评价管理" name="reviewManagement">
          <el-table :data="reviews" v-loading="reviewLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="tourTitle" label="旅行团"></el-table-column>
            <el-table-column prop="userName" label="评价用户"></el-table-column>
            <el-table-column prop="rating" label="评分" width="100">
              <template #default="{ row }">
                <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" score-template="{value} 星"></el-rate>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="评价内容" show-tooltip-when-overflow></el-table-column>
            <el-table-column prop="reviewDate" label="评价日期" width="120"></el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewReviewDetails(row)">详情</el-button>
                </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="reviewsTotal"
            :page-size="reviewsPageSize"
            v-model:current-page="reviewsCurrentPage"
            @current-change="fetchReviews"
            class="pagination-bottom"
          />
        </el-tab-pane>

        <el-tab-pane label="消息中心" name="messageCenter">
          <MyNotifications/>
          <!-- <el-table :data="messages" v-loading="messageLoading" style="width: 100%" class="admin-table">
            <el-table-column prop="title" label="标题"></el-table-column>
            <el-table-column prop="content" label="内容" show-tooltip-when-overflow></el-table-column>
            <el-table-column prop="sender" label="发送者" width="120"></el-table-column>
            <el-table-column prop="sendDate" label="发送日期" width="120"></el-table-column>
            <el-table-column prop="isRead" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : ''">{{ row.isRead ? '已读' : '未读' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewMessageDetails(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="messagesTotal"
            :page-size="messagesPageSize"
            v-model:current-page="messagesCurrentPage"
            @current-change="fetchMessages"
            class="pagination-bottom"
          /> -->
        </el-tab-pane>

        <el-tab-pane label="账户设置" name="accountSettings">
          <div style="margin-bottom: 32px;">
            <AccountOverview @userUpdated="handleUserUpdated"/>
          </div>            
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-dialog v-model="tourDetailsDialog" :title="`旅行团详情: ${selectedTour.title}`" width="800px">
      <el-descriptions border :column="2" class="detail-descriptions">
        <el-descriptions-item label="团名">{{ selectedTour.title }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ selectedTour.location }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ selectedTour.price }}</el-descriptions-item>
        <el-descriptions-item label="天数">{{ selectedTour.duration }}</el-descriptions-item>
        <el-descriptions-item label="出发日期">{{ selectedTour.startDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getTourStatusType(selectedTour.status)">{{ getTourStatusText(selectedTour.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="封面图" :span="2">
          <img :src="selectedTour.image" alt="封面" style="width: 100%; max-height: 250px; object-fit: cover; border-radius: 8px;">
        </el-descriptions-item>
        <el-descriptions-item label="详情描述" :span="2">
          <p>{{ selectedTour.description }}</p>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="tourDetailsDialog = false">关闭</el-button>
        <el-button type="warning" @click="editTour(selectedTour)">编辑</el-button>
        <el-button :type="selectedTour.status === 'PUBLISHED' ? 'danger' : 'success'" @click="toggleTourStatus(selectedTour)">
          {{ selectedTour.status === 'PUBLISHED' ? '下架' : '上架' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderDetailsDialog" :title="`订单详情: ${selectedOrder.orderId}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="订单号">{{ selectedOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="旅行团名">{{ selectedOrder.tourTitle }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ selectedOrder.customerName }}</el-descriptions-item>
        <el-descriptions-item label="客户电话">{{ selectedOrder.customerPhone }}</el-descriptions-item>
        <el-descriptions-item label="预订人数">{{ selectedOrder.paxCount }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ selectedOrder.totalPrice }}</el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ selectedOrder.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatusType(selectedOrder.status)">{{ getOrderStatusText(selectedOrder.status) }}</el-tag>
        </el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="orderDetailsDialog = false">关闭</el-button>
        <el-button v-if="selectedOrder.status === 'PENDING_PAYMENT'" type="warning" @click="markOrderPaid(selectedOrder)">标记付款</el-button>
        <el-button v-if="selectedOrder.status === 'PAID'" type="success" @click="markOrderCompleted(selectedOrder)">标记完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDetailsDialog" :title="`评价详情: ${selectedReview.tourTitle}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="旅行团名">{{ selectedReview.tourTitle }}</el-descriptions-item>
        <el-descriptions-item label="评价用户">{{ selectedReview.userName }}</el-descriptions-item>
        <el-descriptions-item label="评分">
          <el-rate v-model="selectedReview.rating" disabled show-score text-color="#ff9900" score-template="{value} 星"></el-rate>
        </el-descriptions-item>
        <el-descriptions-item label="评价内容">{{ selectedReview.comment }}</el-descriptions-item>
        <el-descriptions-item label="评价日期">{{ selectedReview.reviewDate }}</el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="reviewDetailsDialog = false">关闭</el-button>
        </template>
    </el-dialog>

    <el-dialog v-model="messageDetailsDialog" :title="`消息详情: ${selectedMessage.title}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="发送者">{{ selectedMessage.sender }}</el-descriptions-item>
        <el-descriptions-item label="发送日期">{{ selectedMessage.sendDate }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ selectedMessage.content }}</el-descriptions-item>
        </el-descriptions>
      <template #footer>
        <el-button @click="messageDetailsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick,reactive } from 'vue';
import { Monitor, Compass, Plus, Tickets, Comment, Message, Setting, DocumentCopy, Calendar, TrendCharts, StarFilled ,ArrowLeft} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { authAxios } from '@/utils/request';
import { useRouter } from 'vue-router';
import AccountOverview from '@/components/AccountOverview.vue' 
import MyNotifications from '@/components/profile/MyNotifications.vue';

const router = useRouter();

// --- 团长信息及侧边栏数据 ---
const merchantProfile = reactive({
  id: '',
  email: '',
  phone: '',
  role: '',
  companyName: '', // 经销商特有的公司名称
  avatarUrl: '',
  createdAt: '',
  updatedAt: '',
});


const merchantOverview = ref({
  totalTours: 0,
  pendingTours: 0,
  totalOrders: 0,
  todayOrders: 0,
  monthlyRevenue: 0.00,
  overallRating: 0.0,
  newOrders: 0, // 新订单数量（用于徽标）
  unreadMessages: 0, // 未读消息数量（用于徽标）
});

const activeTab = ref('overview'); // 默认激活的 Tab

const editProfileDialog = ref(false); // 编辑资料弹窗

// --- 概览页数据 ---
const recentTourReviews = ref([]); // 近期旅行团审核状态
const recentOrders = ref([]); // 近期订单概览

// --- 我的旅行团数据 ---
const myTours = ref([]);
const tourLoading = ref(false);
const myToursTotal = ref(0);
const myToursCurrentPage = ref(1);
const myToursPageSize = 10;
const tourSearchQuery = ref('');
const selectedTour = ref({}); 
const tourDetailsDialog = ref(false);

// --- 订单管理数据 ---
const orders = ref([]);
const orderLoading = ref(false);
const ordersTotal = ref(0);
const ordersCurrentPage = ref(1);
const ordersPageSize = 10;
const orderSearchQuery = ref('');
const selectedOrder = ref({});
const orderDetailsDialog = ref(false);

// --- 评价管理数据 ---
const reviews = ref([]);
const reviewLoading = ref(false);
const reviewsTotal = ref(0);
const reviewsCurrentPage = ref(1);
const reviewsPageSize = 10;
const selectedReview = ref({});
const reviewDetailsDialog = ref(false);

// --- 消息中心数据 ---
const messages = ref([]);
const messageLoading = ref(false);
const messagesTotal = ref(0);
const messagesCurrentPage = ref(1);
const messagesPageSize = 10;
const selectedMessage = ref({});
const messageDetailsDialog = ref(false);


// --- 账户设置数据 ---
const passwordForm = ref({
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

// 获取团长概览数据
const fetchMerchantOverview = async () => {
  try {
    const response = await authAxios.get('/api/merchant/overview'); // 假设后端接口
    if (response.data.code === 200 && response.data.data) {
      Object.assign(merchantOverview.value, response.data.data); // 更新概览数据
    }
  } catch (error) {
    console.error('获取团长概览失败:', error);
    ElMessage.error('获取概览数据失败。');
  }
};

// 获取团长个人资料
const fetchMerchantProfile = async () => {
  try {
    const res = await authAxios.get('/auth/me'); 

    if (res.data.code === 200) {
      Object.assign(merchantProfile, res.data.data);
      ElMessage.success('经销商信息获取成功！');
      console.log('Merchant Profile Data:', merchantProfile); // 调试用
    } else {
      ElMessage.error(res.data.message || '获取经销商信息失败');
      console.error('Failed to fetch merchant profile:', res.data.message);
    }
  } catch (error) {
    if (error.response) {
      console.error('Error response data:', error.response.data);
      ElMessage.error(`获取经销商信息失败：${error.response.data.message || '服务器错误'}`);
    } else if (error.request) {
      console.error('No response received:', error.request);
      ElMessage.error('获取经销商信息失败：网络错误或服务器无响应');
    } else {
      console.error('Error setting up the request:', error.message);
      ElMessage.error(`获取经销商信息失败：${error.message}`);
    }
  }
};


// 获取近期旅行团审核状态
const fetchRecentTourReviews = async () => {
  try {
    const response = await authAxios.get('/api/merchant/tours/recentReviews'); // 假设后端接口
    if (response.data.code === 200 && response.data.data) {
      recentTourReviews.value = response.data.data;
    }
  } catch (error) {
    console.error('获取近期旅行团审核状态失败:', error);
  }
};

// 获取近期订单概览
const fetchRecentOrders = async () => {
  try {
    const response = await authAxios.get('/api/merchant/orders/recent'); // 假设后端接口
    if (response.data.code === 200 && response.data.data) {
      recentOrders.value = response.data.data;
    }
  } catch (error) {
    console.error('获取近期订单概览失败:', error);
  }
};


// 获取我的旅行团列表
const fetchMyTours = async () => {
  tourLoading.value = true;
  try {
    const response = await authAxios.get('/dealer/travel-packages', {
      params: {
        page: myToursCurrentPage.value,
        size: myToursPageSize
      }
    });
    if (response.data.code === 200 && response.data.data) {
      myTours.value = response.data.data.content;
      myToursTotal.value = response.data.data.totalElements;
      console.log('获取旅行团列表成功', response.data.data )
    } else {
      ElMessage.error(response.data.message || '获取旅行团列表失败');
    }
  } catch (error) {
    console.error('获取我的旅行团列表时发生错误:', error);
    ElMessage.error('加载旅行团列表失败。');
  } finally {
    tourLoading.value = false;
  }
};
const debouncedSearchTours = debounce(fetchMyTours, 500);

// 查看旅行团详情
const viewTourDetails = (tourRow) => {
  selectedTour.value = { ...tourRow };
  tourDetailsDialog.value = true;
};

// 编辑旅行团 (可能跳转到发布新团页面并预填数据，或在弹窗内编辑)
const editTour = (tourRow) => {
  ElMessage.info(`跳转到编辑页面或打开编辑弹窗，编辑团：${tourRow.title}`);
  // 实际项目中，这里会填充 newTourForm 或打开一个专门的编辑弹窗
  // 例如：router.push({ name: 'PublishTour', query: { tourId: tourRow.id } });
  // 或者：selectedTour.value = { ...tourRow }; editTourDialog.value = true;
};

// 切换旅行团状态 (上架/下架)
const toggleTourStatus = async (tourRow) => {
  const newStatus = tourRow.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED';
  const actionText = newStatus === 'OFFLINE' ? '下架' : '上架';
  ElMessageBox.confirm(`确定要${actionText}旅行团 “${tourRow.title}” 吗？`, '提示', {
    type: newStatus === 'OFFLINE' ? 'warning' : 'success'
  })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/merchant/tours/${tourRow.id}/status`, { status: newStatus }); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success(`${actionText}成功！`);
          tourRow.status = newStatus; // 乐观更新UI
          // 如果弹窗打开，也更新弹窗中的状态
          if (tourDetailsDialog.value && selectedTour.value.id === tourRow.id) {
            selectedTour.value.status = newStatus;
          }
        } else {
          ElMessage.error(response.data.message || `${actionText}失败`);
        }
      } catch (error) {
        console.error(`${actionText}旅行团时发生错误:`, error);
        ElMessage.error(`操作失败，请稍后再试。`);
      }
    })
    .catch(() => { /* 用户取消 */ });
};

// 获取订单列表
const fetchOrders = async () => {
  orderLoading.value = true;
  try {
    const response = await authAxios.get('/api/merchant/orders', {
      params: {
        pageNum: ordersCurrentPage.value,
        pageSize: ordersPageSize,
        query: orderSearchQuery.value
      }
    });
    if (response.data.code === 200 && response.data.data) {
      orders.value = response.data.data.records;
      ordersTotal.value = response.data.data.total;
    } else {
      ElMessage.error(response.data.message || '获取订单列表失败');
    }
  } catch (error) {
    console.error('获取订单列表时发生错误:', error);
    ElMessage.error('加载订单列表失败。');
  } finally {
    orderLoading.value = false;
  }
};
const debouncedSearchOrders = debounce(fetchOrders, 500);

// 查看订单详情
const viewOrderDetails = (orderRow) => {
  selectedOrder.value = { ...orderRow };
  orderDetailsDialog.value = true;
};

// 标记订单为已付款
const markOrderPaid = async (orderRow) => {
  ElMessageBox.confirm(`确定要将订单 ${orderRow.orderId} 标记为“已付款”吗？`, '提示', { type: 'info' })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/merchant/orders/${orderRow.orderId}/mark-paid`); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success('订单已标记为已付款！');
          orderRow.status = 'PAID'; // 乐观更新
          fetchMerchantOverview(); // 更新概览
          // 如果弹窗打开，也更新弹窗中的状态
          if (orderDetailsDialog.value && selectedOrder.value.orderId === orderRow.orderId) {
            selectedOrder.value.status = 'PAID';
          }
        } else {
          ElMessage.error(response.data.message || '操作失败');
        }
      } catch (error) {
        console.error('标记订单已付款时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    });
};

// 标记订单为已完成
const markOrderCompleted = async (orderRow) => {
  ElMessageBox.confirm(`确定要将订单 ${orderRow.orderId} 标记为“已完成”吗？`, '提示', { type: 'success' })
    .then(async () => {
      try {
        const response = await authAxios.post(`/api/merchant/orders/${orderRow.orderId}/mark-completed`); // 假设后端接口
        if (response.data.code === 200) {
          ElMessage.success('订单已标记为已完成！');
          orderRow.status = 'COMPLETED'; // 乐观更新
          fetchMerchantOverview(); // 更新概览
          // 如果弹窗打开，也更新弹窗中的状态
          if (orderDetailsDialog.value && selectedOrder.value.orderId === orderRow.orderId) {
            selectedOrder.value.status = 'COMPLETED';
          }
        } else {
          ElMessage.error(response.data.message || '操作失败');
        }
      } catch (error) {
        console.error('标记订单已完成时发生错误:', error);
        ElMessage.error('操作失败，请稍后再试。');
      }
    });
};


// 获取评价列表
const fetchReviews = async () => {
  reviewLoading.value = true;
  try {
    const response = await authAxios.get('/api/merchant/reviews', {
      params: {
        pageNum: reviewsCurrentPage.value,
        pageSize: reviewsPageSize,
        // query: reviewSearchQuery.value // 如果需要搜索评价内容
      }
    });
    if (response.data.code === 200 && response.data.data) {
      reviews.value = response.data.data.records;
      reviewsTotal.value = response.data.data.total;
    } else {
      ElMessage.error(response.data.message || '获取评价列表失败');
    }
  } catch (error) {
    console.error('获取评价列表时发生错误:', error);
    ElMessage.error('加载评价列表失败。');
  } finally {
    reviewLoading.value = false;
  }
};

// 查看评价详情
const viewReviewDetails = (reviewRow) => {
  selectedReview.value = { ...reviewRow };
  reviewDetailsDialog.value = true;
};

// // 获取消息列表
// const fetchMessages = async () => {
//   messageLoading.value = true;
//   try {
//     const response = await authAxios.get('/api/merchant/messages', {
//       params: {
//         pageNum: messagesCurrentPage.value,
//         pageSize: messagesPageSize,
//       }
//     });
//     if (response.data.code === 200 && response.data.data) {
//       messages.value = response.data.data.records;
//       messagesTotal.value = response.data.data.total;
//       merchantOverview.value.unreadMessages = response.data.data.records.filter(m => !m.isRead).length; // 更新未读消息计数
//     } else {
//       ElMessage.error(response.data.message || '获取消息列表失败');
//     }
//   } catch (error) {
//     console.error('获取消息列表时发生错误:', error);
//     ElMessage.error('加载消息列表失败。');
//   } finally {
//     messageLoading.value = false;
//   }
// };

// // 查看消息详情
// const viewMessageDetails = async (messageRow) => {
//   selectedMessage.value = { ...messageRow };
//   messageDetailsDialog.value = true;
//   if (!messageRow.isRead) {
//     // 标记为已读
//     try {
//       await authAxios.post(`/api/merchant/messages/${messageRow.id}/mark-read`); // 假设后端接口
//       messageRow.isRead = true; // 乐观更新
//       fetchMerchantOverview(); // 重新获取概览以更新未读消息数
//     } catch (error) {
//       console.error('标记消息为已读失败:', error);
//     }
//   }
// };

// 查看指定旅行团的订单列表
const viewOrders = (tourRow) => {
  selectedTour.value = { ...tourRow }; // 存储当前选中的旅行团信息，方便弹窗标题显示
  ordersForSelectedPackage.pageNumber = 1; // 重置分页到第一页
  travelPackageOrdersDialog.value = true; // 打开弹窗
  fetchTravelPackageOrders(); // 调用新的函数来获取订单数据
};

// 跳转发布旅行团页
const goToNewGroupPage = () => {
  router.push('/merchant/newgroup');
};
//跳转添加团期页
const goToAddSchedulePage = () => {
  router.push('/merchant/addschedule');
};

// 跳转首页
const goToHome = () => {
  router.push('/')
}


// --- 辅助函数：状态标签和文本 ---
const getTourReviewStatusType = (status) => {
  switch (status) {
    case 'PENDING': return 'info';
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'danger';
    default: return '';
  }
};
const getTourReviewStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '审核中';
    case 'APPROVED': return '已通过';
    case 'REJECTED': return '已驳回';
    default: return '未知';
  }
};

const getOrderStatusType = (status) => {
  switch (status) {
    case 'PENDING_PAYMENT': return 'warning';
    case 'PAID': return '';
    case 'COMPLETED': return 'success';
    case 'CANCELLED': return 'danger';
    default: return 'info';
  }
};
const getOrderStatusText = (status) => {
  switch (status) {
    case 'PENDING_PAYMENT': return '待付款';
    case 'PAID': return '已付款';
    case 'COMPLETED': return '已完成';
    case 'CANCELLED': return '已取消';
    default: return '未知';
  }
};

const getTourStatusType = (status) => {
  switch (status) {
    case 'PENDING_REVIEW': return 'info';
    case 'PUBLISHED': return 'success';
    case 'OFFLINE': return 'danger';
    case 'REJECTED': return 'warning'; 
    default: return '';
  }
};
const getTourStatusText = (status) => {
  switch (status) {
    case 'PENDING_APPROVAL': return '待审核';
    case 'PUBLISHED': return '已发布';
    case 'OFFLINE': return '已下架';
    case 'REJECTED': return '已驳回';
    default: return '未知';
  }
};

const handleUserUpdated = () => {
  console.log('Parent: User update notification received from child. Re-fetching user profile...');
  fetchMerchantProfile();
};

// --- 初始化数据 ---
onMounted(() => {
  fetchMerchantProfile();    // 获取团长个人资料
  // fetchMerchantOverview();   // 获取概览数据
  // fetchRecentTourReviews(); // 获取近期旅行团审核状态
  // fetchRecentOrders();      // 获取近期订单概览

  fetchMyTours();
  // fetchOrders();
  // fetchReviews();
  // fetchMessages();
});

// 监听 activeTab 变化，动态加载对应数据 (按需开启，避免初始化加载过多)
// watch(activeTab, (newTab) => {
//   nextTick(() => { // 确保 DOM 更新后执行
//     if (newTab === 'tourManagement') fetchMyTours();
//     else if (newTab === 'orderManagement') fetchOrders();
//     else if (newTab === 'reviewManagement') fetchReviews();
//     else if (newTab === 'messageCenter') fetchMessages();
//   });
// });

</script>

<style scoped>
/* 引入 Google Fonts - Poppins */
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');

.merchant-dashboard-page {
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
  display: flex;
  flex-direction: column;
  align-items: center;
  top: 40px;
  align-self: flex-start;
  height: 100%;
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

/* 主内容 - 沿用用户/管理员主页风格 */
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

/* --- 团长特有样式 --- */
.tab-header {
  font-size: 1.6rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 25px;
  border-bottom: 2px solid #eee;
  padding-bottom: 15px;
}

.stat-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background-color: #fdfdfd;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-5px);
}

.card-icon {
  font-size: 2.5rem;
  color: rgb(77,182,172); /* 与主题色搭配 */
  margin-right: 20px;
}

.card-info h2 {
  margin: 0;
  font-size: 2.2rem;
  color: #333;
}

.card-info p {
  margin: 0;
  font-size: 0.95rem;
  color: #666;
}

.stat-card.new-tour-card {
  cursor: pointer;
  background: linear-gradient(45deg, #e0f2f1, #b2dfdb); /* 浅绿渐变 */
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}
.stat-card.new-tour-card .card-icon {
  color: #00796b; 
}
.stat-card.new-tour-card .card-info h2 {
  color: #004d40; 
}
.stat-card.new-tour-card .card-info p {
  color: #26a69a;
}

.stat-card.add-schedule-card {
  cursor: pointer;
  background: linear-gradient(45deg, #e0f2f1, #b2dfdb); /* 浅绿渐变 */
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}
.stat-card.add-schedule-card .card-icon {
  color: #00796b; 
}
.stat-card.add-schedule-card .card-info h2 {
  color: #004d40;  
}
.stat-card.add-schedule-card .card-info p {
  color: #26a69a;
}

.add-tour-btn {
  position: absolute;
  bottom: 15px;
  right: 15px;
  background-color: #00796b;
  border-color: #00796b;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease, background-color 0.2s ease;
}
.add-tour-btn:hover {
  transform: scale(1.1);
  background-color: #00695c;
  border-color: #00695c;
}

.add-schedule-btn {
  position: absolute;
  bottom: 15px;
  right: 15px;
  background-color: #00796b;
  border-color: #00796b;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease, background-color 0.2s ease;
}
.add-schedule-btn:hover {
  transform: scale(1.1);
  background-color: #00695c;
  border-color: #00695c;
}

.overview-sections {
  margin-top: 30px;
}

.recent-status-card {
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px; /* 确保卡片间距 */
}

.card-header-with-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.admin-table {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination-bottom {
  margin-top: 20px;
  justify-content: flex-end;
  display: flex;
}

.publish-tour-form {
  max-width: 700px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 12px;
  background-color: #fdfdfd;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
}

.publish-tour-form .el-form-item {
  margin-bottom: 22px;
}

.form-tip {
  font-size: 0.85rem;
  color: #999;
  margin-top: 5px;
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
  align-items: flex-start;
}
</style>