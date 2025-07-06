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
          <el-badge :value="notificationStore.unreadCounts.all" v-if="notificationStore.unreadCounts.all > 0" class="notification-badge" />
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'friends' }"
          @click="activeTab = 'friends'"
        >
          <el-icon><User /></el-icon>
          <span>我的好友</span>
        </div>
        <div
          :class="{ 'menu-item': true, 'active': activeTab === 'images' }"
          @click="activeTab = 'images'"
        >
          <el-icon><PictureFilled /></el-icon>
          <span>我的素材库</span>
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
      <MyFavorites v-if="activeTab === 'collected'" />

      <MyJoinedTours v-if="activeTab === 'joined'" />

      <MyNotes v-if="activeTab === 'notes'" />

      <MyReviews v-if="activeTab === 'reviews'"/>

      <MyNotifications v-if="activeTab === 'notifications'" />

      <MyFriends v-if="activeTab === 'friends'" />

      <MyMediaLibrary v-if="activeTab === 'images'" />

      <div v-if="activeTab === 'systemSettings'" class="settings-section">
        <AccountOverview @userUpdated="handleUserUpdated" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref,computed,reactive,watch } from 'vue'
import { User,Star, Tickets, EditPen, Comment, Bell, ArrowLeft, Plus } from '@element-plus/icons-vue' 
import { ElMessageBox,ElTabs, ElTabPane, ElCard, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton, ElPagination, ElEmpty, ElMessage } from 'element-plus';


import { useRoute,useRouter } from 'vue-router';
import { authAxios,publicAxios } from '@/utils/request';
import { useNotificationStore } from '@/stores/notificationStore';

import MyNotes from '@/components/profile/MyNotes.vue' 
import MyFavorites from '@/components/profile/MyFavorites.vue' 
import MyJoinedTours from '@/components/profile/MyJoinedTours.vue' 
import MyNotifications from '@/components/profile/MyNotifications.vue';
import MyFriends from '@/components/profile/MyFriends.vue' 
import MyReviews from '@/components/profile/MyReviews.vue' 
import AccountOverview from '@/components/AccountOverview.vue' 
import MyMediaLibrary from '@/components/profile/MyMediaLibrary.vue' 
const router = useRouter();
const route = useRoute();
const notificationStore = useNotificationStore();

// 用户资料
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

// 我的评价
const reviews = ref([]); // 用于存储评价（或待评价订单）数据的响应式引用
const reviewStatus = ref('PENDING'); // 控制评价状态筛选: 'PENDING' (待评价), 'REVIEWED' (已评价), 'ALL' (全部)
const currentReviewPage = ref(1); // 评价列表当前页码，从1开始
const reviewPageSize = ref(6); // 评价列表每页记录数，改为6，这样在卡片网格中更美观
const totalReviews = ref(0); // 评价总数
const reviewLoading = ref(false); // 评价加载状态
const noMoreReviews = computed(() => {
  // 如果已加载的评价数量大于等于总数，且总数大于0（避免空列表时显示“没有更多”）
  return reviews.value.length >= totalReviews.value && totalReviews.value > 0;
});

const activeTab = ref('collected')

// 获取用户资料
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


// 当前激活的通知分类标签页
const activeNotificationTab = ref('all') 

// 存放通知列表的数组
const notifications = ref([])

// 分页信息
const currentNotificationPage = ref(0) 
const pageNotificationSize = ref(10)
const totalNotifications = ref(0)

// 状态标记
const notificationLoading = ref(false)
const noMoreNotifications = ref(false)

// 计算属性，判断是否还有更多通知
const hasMoreNotifications = computed(() => {
  return notifications.value.length < totalNotifications.value && !noMoreNotifications.value;
});


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

// 获取游记
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

// 更新个人资料
const handleUserUpdated = () => {
  console.log('Parent: User update notification received from child. Re-fetching user profile...');
  fetchUserProfile();
};

// **处理“去评价”点击事件**
const goToReview = (orderId) => {
  // 导航到评价提交页面，并传递订单ID
  router.push({ name: 'SubmitReviewPage', params: { orderId: orderId } });
  // 你需要确保你的路由中定义了名为 'SubmitReviewPage' 的路由
  // 例如： { path: '/review/:orderId', name: 'SubmitReviewPage', component: SubmitReviewComponent }
  ElMessage.info(`跳转到订单 ${orderId} 的评价页面`);
};

// **处理“查看评价”点击事件**
const viewReview = (orderId) => {
  router.push({ name: 'ViewReviewPage', params: { orderId: orderId } });
  ElMessage.info(`查看订单 ${orderId} 的评价详情`);
};

onMounted(() => {
  fetchNotes(true);
  fetchUserProfile();
  notificationStore.fetchUnreadCounts();
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

.progress-row {
  display: flex;
  gap: 135px;
  align-items: center;
}
.progress-text {
  margin: 0;
}

.payment-actions {
  display: flex;
  gap: 8px;
}

</style>