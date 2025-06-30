<template>
  <div class="user-profile-page">
    <aside class="sidebar">
      <el-button type="info" :icon="ArrowLeft" class="back-to-home-btn" @click="goToHome">
        返回
      </el-button>
      <el-avatar
        :src="user.avatarUrl || '/default-avatar.png'"
        :size="100"
        class="avatar"
      />
      <h2 class="username">{{ user.username }}</h2>
      <br>

      <div class="sidebar-menu">
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'collected' }"
          @click="activeTab = 'collected'"
        >
          <el-icon><Star /></el-icon>
          <span>我的收藏</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'joined' }"
          @click="activeTab = 'joined'"
        >
          <el-icon><Tickets /></el-icon>
          <span>我的报名</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'notes' }"
          @click="activeTab = 'notes'"
        >
          <el-icon><EditPen /></el-icon>
          <span>我的游记</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'reviews' }"
          @click="activeTab = 'reviews'"
        >
          <el-icon><Comment /></el-icon>
          <span>我的评价</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'notifications' }"
          @click="activeTab = 'notifications'"
        >
          <el-icon><Bell /></el-icon>
          <span>我的通知</span>
          <el-badge v-if="unreadNotifications > 0" :value="unreadNotifications" class="notification-badge" />
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
          <el-tab-pane label="我的收藏" name="collected">
          <div v-if="collectedTours.length > 0" class="card-grid">
            <el-card
              v-for="tour in collectedTours"
              :key="tour.itemid"
              class="tour-card hover-card"
              @click="goToTourDetail(tour)"
            >
              <img :src="getTourImage(tour.itemType)" class="card-img" alt="收藏图片" />
              <div class="card-info">
                <h3>{{ getTourTitle(tour) }}</h3>
                <p>{{ getTourLocation(tour) }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无收藏"></el-empty>

          <div v-if="pagination.totalElements > 0" class="pagination-container">
            <el-pagination
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page="pagination.pageNumber + 1"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="pagination.pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.totalElements"
            >
            </el-pagination>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的报名" name="joined">
          <div v-if="joinedTours.length > 0" class="card-grid">
            <el-card
              v-for="tour in joinedTours"
              :key="tour.id"
              class="tour-card hover-card"
            >
              <img :src="tour.image" class="card-img" />
              <div class="card-info">
                <h3>{{ tour.title }}</h3>
                <p>出发日期：{{ tour.date }}</p>
                <el-progress :percentage="tour.progress" color="#13ce66" />
                <p class="progress-text">当前进度：{{ tour.progress }}%</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无报名"></el-empty>
        </el-tab-pane>

        <el-tab-pane label="我的游记" name="notes">
          <div class="notes-header">
            <h3 class="notes-section-title">我的游记</h3>
            <el-button type="primary" :icon="Plus" @click="goToPublishTravelNote">发布新游记</el-button>
          </div>
          <div v-loading="noteLoading">
            <div v-if="notes.length > 0" class="card-grid">
              <el-card
                v-for="note in notes"
                :key="note.id"
                class="note-card hover-card"
                @click.stop="goToDetail(note.id)"
              >
                <img v-if="note.coverImageUrl" :src="note.coverImageUrl" class="note-img" />
                <div class="card-info">
                  <h3>{{ note.title }}</h3>
                </div>
              </el-card>
            </div>
            <el-empty v-else description="暂无游记"></el-empty>
          </div>

          <div class="load-more-container">
            <el-button
              v-if="hasMoreNotes"
              type="primary"
              :loading="noteLoading"
              @click="fetchNotes(false)"
              class="load-more-btn"
            >
              {{ noteLoading ? '加载中...' : '加载更多游记' }}
            </el-button>
            <p v-else-if="notes.length > 0 && noMoreNotes" class="no-more-text">
              已加载全部游记
            </p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的评价" name="reviews">
          <div v-if="reviews.length > 0" class="card-grid">
            <el-card
              v-for="review in reviews"
              :key="review.id"
              class="review-card hover-card"
            >
              <div class="review-info">
                <h3>{{ review.tourTitle }}</h3>
                <el-rate v-model="review.rating" disabled show-text />
                <p>{{ review.comment }}</p>
                <p class="review-date">评价日期：{{ review.date }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无评价"></el-empty>
        </el-tab-pane>

        <el-tab-pane label="我的通知" name="notifications">
          <div v-if="notifications.length > 0" class="notification-list">
            <el-card
              v-for="notification in notifications"
              :key="notification.id"
              class="notification-item hover-card"
            >
              <div class="notification-content">
                <p class="notification-title">{{ notification.title }}</p>
                <p class="notification-text">{{ notification.message }}</p>
                <p class="notification-date">{{ notification.date }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无通知"></el-empty>
        </el-tab-pane>
        <el-tab-pane label="系统设置" name="systemSettings">
          <div style="margin-bottom: 32px;">
            <AccountOverview @userUpdated="handleUserUpdated"/>
          </div>
        </el-tab-pane>
      </el-tabs>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref,computed,reactive,watch } from 'vue'
import { Star, Tickets, EditPen, Comment, Bell, ArrowLeft, Plus } from '@element-plus/icons-vue' 
import { ElTabs, ElTabPane, ElCard, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton, ElPagination, ElEmpty, ElMessage } from 'element-plus';
import AccountOverview from '@/components/AccountOverview.vue' 

import { useRoute,useRouter } from 'vue-router';
import { authAxios,publicAxios } from '@/utils/request';

const router = useRouter();
const route = useRoute();
const user = reactive({
  id: '',
  email: '',
  phone: '',
  role: '',
  username: '',
  avatarUrl: '',
  createdAt: '',
  updatedAt: '',
});

const collectedTours = ref([]);

const pagination = reactive({
  pageNumber: 0,
  pageSize: 10,
  totalElements: 0,
  totalPages: 0,
});

const searchParams = reactive({
  keyword: '',
  page: 1,
  size: 10,
  sortBy: 'createdTime',
  sortDirection: 'DESC',
});

const joinedTours = ref([]); // 存储我的报名旅行团列表
const joinedTotal = ref(0); // 我的报名总数
const joinedPageSize = 6; // 每页显示数量，你可以根据需要调整
const joinedCurrentPage = ref(1); // 当前页码

const loadingJoinedTours = ref(false); // 专门用于“我的报名”Tab 的加载状态

// 示例评价数据
const reviews = ref([
  { id: 1, tourTitle: '桂林山水团', rating: 4, comment: '风景很美，导游很热情！', date: '2025-06-20' },
  { id: 2, tourTitle: '成都美食团', rating: 5, comment: '火锅太棒了！', date: '2025-06-15' },
])

// 示例通知数据
const notifications = ref([
  { id: 1, title: '报名成功', message: '您已成功报名成都美食团，祝您旅途愉快！', date: '2025-06-10' },
  { id: 2, title: '游记审核通过', message: '您的游记“稻城亚丁旅行记”已通过审核。', date: '2025-06-05' },
])

const activeTab = ref('collected')

const unreadNotifications = ref(1);

const fetchUserProfile = async () => {
  try {
    const res = await authAxios.get('/auth/me'); 

    if (res.data.code === 200) {
      Object.assign(user, res.data.data);
      ElMessage.success('用户信息获取成功！');
      console.log('User Profile Data:', user); // 调试用
    } else {
      // 处理非200状态码，显示后端返回的错误信息
      ElMessage.error(res.data.message || '获取用户信息失败');
      console.error('Failed to fetch user profile:', res.data.message);
    }
  } catch (error) {
    // 处理网络请求错误或服务器无响应
    if (error.response) {
      // 服务器返回了状态码，但不在 2xx 范围内
      console.error('Error response data:', error.response.data);
      console.error('Error response status:', error.response.status);
      ElMessage.error(`获取用户信息失败：${error.response.data.message || '服务器错误'}`);
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('No response received:', error.request);
      ElMessage.error('获取用户信息失败：网络错误或服务器无响应');
    } else {
      // 其他错误
      console.error('Error setting up the request:', error.message);
      ElMessage.error(`获取用户信息失败：${error.message}`);
    }
  }
};

//获取用户收藏列表
const fetchCollectedTours = async () => {
  try {
    const response = await authAxios.get('/user/favorites', {
      params: {
        page: searchParams.page,
        size: searchParams.size,
        sortBy: searchParams.sortBy,
        sortDirection: searchParams.sortDirection
      },
    });

    if (response.data.code === 200 && response.data.data) {
      collectedTours.value = response.data.data.content; 
      pagination.pageNumber = response.data.data.pageNumber;
      pagination.pageSize = response.data.data.pageSize;
      pagination.totalElements = response.data.data.totalElements;
      pagination.totalPages = response.data.data.totalPages;
    } else {
      ElMessage.error(response.data.message || '获取收藏列表失败。');
    }
  } catch (error) {
    console.error('获取收藏列表时发生错误:', error);
    ElMessage.error('获取收藏列表时发生网络或服务器错误。');
  }
};

//处理每页显示数量变化的事件
const handleSizeChange = (newSize) => {
  searchParams.size = newSize;
  searchParams.page = 1; 
  fetchCollectedTours();
};

//处理当前页码变化的事件
const handleCurrentChange = (newPage) => {
  searchParams.page = newPage;
  fetchCollectedTours();
};

//根据收藏项类型获取图片URL
const getTourImage = (itemType) => {
  switch (itemType) {
    case 'PACKAGE':
      return 'https://via.placeholder.com/150/FF5733/FFFFFF?text=旅游套餐';
    case 'SCENIC_SPOT':
      return 'https://via.placeholder.com/150/33A8FF/FFFFFF?text=景点';
    default:
      return 'https://via.placeholder.com/150/CCCCCC/FFFFFF?text=默认';
  }
};

//根据收藏项获取标题
const getTourTitle = (tour) => {
  return `收藏ID: ${tour.itemid} (${tour.itemType})`;
};

//根据收藏项获取地点/简要描述
const getTourLocation = (tour) => {
  return `收藏时间: ${new Date(tour.createdTime).toLocaleDateString()}`;
};

//点击收藏卡片时的处理函数，例如跳转到详情页
const goToTourDetail = (tour) => {
  ElMessage.info(`点击了收藏项: ${tour.itemid}，类型: ${tour.itemType}`);
  console.log('Clicked tour:', tour);
};

// --- 获取我的报名（订单） ---
const fetchJoinedTours = async () => {
  loadingJoinedTours.value = true; 
  try {
    const response = await authAxios.get('/user/orders', {
      params: {
        page: joinedCurrentPage.value,
        size: joinedPageSize,
      },
    });

    if (response.data.code === 200) {
      joinedTours.value = response.data.data.content.map(order => {
        let progress = 0; 

        // 根据订单状态设置进度和日期（示例逻辑，请根据你的业务需求调整）
        switch (order.status) {
          case 'CONFIRMED': // 已确认
            progress = 50;
            tourDate = '即将出发'; // 或从其他地方获取实际出发日期
            break;
          case 'COMPLETED': // 已完成
            progress = 100;
            tourDate = '已完成';
            break;
          case 'PENDING': // 待处理/待确认
            progress = 20;
            tourDate = '等待确认';
            break;
        }
        
        let tourDate = new Date(order.orderTime).toLocaleDateString();

        return {
          id: order.orderId,
          image: order.packageCoverImageUrl,
          title: order.packageTitle,
          date: tourDate, 
          progress: progress, 
          orderId: order.packageId,
          status: order.status,
          travelerCount: order.travelerCount,
          totalPrice: order.totalPrice,
          orderTime: order.orderTime
        };
      });
      joinedTotal.value = response.data.data.totalElements || 0;
    } else {
      ElMessage.error(response.data.message || '获取我的报名数据失败！');
      joinedTours.value = [];
      joinedTotal.value = 0;
    }
  } catch (error) {
    console.error('获取我的报名失败:', error);
    ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    joinedTours.value = [];
    joinedTotal.value = 0;
  } finally {
    loadingJoinedTours.value = false; // 结束加载
  }
};

// 跳转首页
const goToHome = () => {
  router.push('/')
}

// 跳转到发布游记页面
const goToPublishTravelNote = () => {
  router.push('/user/publish-travel-note'); 
};

const notes = ref([]);
const currentPage = ref(1);
const pageSize = ref(9); // 每页记录数
const totalNotes = ref(0); // 总游记数量
const noteLoading = ref(false); // 加载状态
const noMoreNotes = ref(false); // 是否没有更多游记了

// 判断是否还有更多游记
const hasMoreNotes = computed(() => {
  return notes.value.length < totalNotes.value && !noMoreNotes.value;
});

const fetchNotes = async (reset = false) => {
  if (noteLoading.value) return; // 如果正在加载中，则跳过
  if (noMoreNotes.value && !reset) { // 如果已经没有更多，且不是重置操作，则跳过
    ElMessage.info('没有更多游记了。');
    return;
  }

  noteLoading.value = true;
  if (reset) {
    currentPage.value = 0; // 重置页码
    notes.value = []; // 清空现有游记
    noMoreNotes.value = false; // 重置没有更多的状态
  }

  const nextPage = currentPage.value + 1; // 下一页的页码

  try {
    const response = await authAxios.get('/user/posts', {
      params: {
        page: nextPage, // 请求下一页的数据
        size: pageSize.value,
        sortBy: 'createdTime',
        sortDirection: 'DESC',
      },
    });

    if (response.data.code === 200 && response.data.data) {
      const newNotes = response.data.data.content;
      totalNotes.value = response.data.data.totalElements;

      if (reset) {
        notes.value = newNotes; // 重置时直接替换
      } else {
        notes.value = [...notes.value, ...newNotes]; // 追加新数据
      }

      currentPage.value = nextPage; // 更新当前页码

      // 判断是否加载完所有数据
      if (notes.value.length >= totalNotes.value) {
        noMoreNotes.value = true;
        ElMessage.info('所有游记已加载完毕。');
      } else {
        // ElMessage.success(`成功加载第 ${nextPage} 页游记！`); // 加载成功提示
      }
    } else {
      ElMessage.warning('未能获取游记数据，请稍后再试。');
      noMoreNotes.value = true; 
    }
  } catch (error) {
    console.error('获取游记列表失败:', error);
    ElMessage.error('获取游记列表失败，请检查网络或稍后再试。');
    noMoreNotes.value = true; 
  } finally {
    noteLoading.value = false;
  }
};

const handleUserUpdated = () => {
  console.log('Parent: User update notification received from child. Re-fetching user profile...');
  fetchUserProfile();
};

onMounted(() => {
  fetchNotes(true);
  fetchUserProfile();
  fetchJoinedTours();
  fetchCollectedTours();
})

// 跳转详情页并传递游记id
const goToDetail = (id) => {
  console.log('查看游记详情:', id);
  router.push({ name: 'EditNote', params: { id: id } });
};

// --- 监听路由查询参数，用于激活对应的Tab ---
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab) {
      activeTab.value = newTab;
    }
  },
  { immediate: true } 
);


</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');

.user-profile-page {
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

/* 左侧栏 */
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
  top:10px;
  left:10px;
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

.edit-profile-btn {
  width: 80%;
  margin-bottom: 30px;
  border-radius: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 左侧功能导航菜单样式 */
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
  box-shadow: 0 4px 12px rgba(64, 255, 245, 0.3);
}

.menu-item.active .el-icon {
  color: #ffffff;
}

/* 消息通知徽标样式 */
.notification-badge {
  --el-badge-color: #f56c6c; /* 设置徽标颜色 */
  margin-left: 5px; /* 调整徽标位置 */
}

/* 主内容 */
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

/* 我的游记部分的标题和按钮容器 */
.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px; 
  padding-bottom: 10px; 
  border-bottom: 1px solid #eee; 
}

.notes-section-title {
  font-size: 1.5rem; 
  font-weight: 600;
  color: #333;
  margin: 0; 
}

/* 内容卡片网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 10px; 
}

/* 卡片 */
.tour-card,
.note-card,
.review-card,
.notification-item {
  display: flex;
  align-items: center;
  border-radius: 14px;
  overflow: hidden;
  padding: 16px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: 1px solid #ebebeb;
}

.hover-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(64, 255, 223, 0.15);
  border-color: #54c4b5;
}

.card-img,
.note-img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
  margin-right: 20px;
  flex-shrink: 0;
}

.card-info {
  flex-grow: 1;
  text-align: left;
}

.card-info h3 {
  margin: 0;
  font-size: 17px;
  color: #333;
  font-weight: 600;
}

.card-info p {
  margin: 5px 0 0;
  color: #777;
  font-size: 13px;
  line-height: 1.4;
}

/* 报名卡片进度条样式 */
.progress-text {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

/* 评价卡片样式 */
.review-card {
  flex-direction: column; 
  align-items: flex-start; 
}

.review-info h3 {
  margin-bottom: 8px;
  font-size: 1.1rem;
}

.review-info .el-rate {
  margin-bottom: 8px;
}

.review-date {
  font-size: 0.9rem;
  color: #999;
  margin-top: 8px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  flex-direction: column;
  align-items: flex-start;
}

.notification-content {
  flex-grow: 1;
}

.notification-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 4px;
}

.notification-text {
  font-size: 1rem;
  color: #555;
  line-height: 1.4;
}

.notification-date {
  font-size: 0.9rem;
  color: #999;
  margin-top: 8px;
}

.el-dialog {
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.el-dialog__header {
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
}

.el-dialog__footer {
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.el-input {
  border-radius: 8px;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.load-more-btn {
  width: 200px;
  padding: 12px 20px;
  font-size: 1rem;
  font-weight: 500;
  border-radius: 8px;
}


.no-more-text {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 15px;
}
</style>