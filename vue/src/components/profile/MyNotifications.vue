<template>
  <div class="notification-container">
    <el-tabs v-model="activeNotificationTab" tab-position="top" class="notification-tabs-horizontal beautiful-navbar">
      <el-tab-pane name="all">
        <template #label>
          <div class="navbar-item-content">
            <el-icon><Bell /></el-icon>
            <span class="item-text">全部通知</span>
            <el-badge v-if="unreadCounts.all > 0" :value="unreadCounts.all" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="likes">
        <template #label>
          <div class="navbar-item-content">
            <el-icon><Star /></el-icon>
            <span class="item-text">赞和收藏</span>
            <el-badge v-if="unreadCounts.likes > 0" :value="unreadCounts.likes" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="comments">
        <template #label>
          <div class="navbar-item-content">
            <el-icon><ChatDotRound /></el-icon>
            <span class="item-text">评论回复</span>
            <el-badge v-if="unreadCounts.comments > 0" :value="unreadCounts.comments" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="system">
        <template #label>
          <div class="navbar-item-content">
            <el-icon><Monitor /></el-icon>
            <span class="item-text">系统消息</span>
            <el-badge v-if="unreadCounts.system > 0" :value="unreadCounts.system" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="notification-content-area-top">
      <div class="notification-actions">
        <el-button
          v-if="notifications.length > 0 || unreadCounts.all > 0"
          type="primary"
          plain
          size="small"
          @click="markCurrentCategoryAsRead()"
          class="mark-read-btn"
        >
          <el-icon><Bell /></el-icon>
          <span>{{ activeNotificationTab === 'all' ? '将所有通知标为已读' : '将本类别全部标为已读' }}</span>
        </el-button>
      </div>

      <div v-loading="notificationLoading" class="notification-list-wrapper">
        <div v-if="notifications.length > 0" class="notification-list">
          <el-card
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item hover-card"
            :class="{ 'is_read': notification.read }"
            @click="Turn(notification)"
          >
            <div class="notification-item-header">
              <span class="notification-source">{{ getNotificationSource(notification.type) }}</span>
              <span v-if="!notification.read" class="unread-dot" title="未读"></span>
            </div>
            <p class="notification-title">{{ notification.title }}</p>
            <p class="notification-text">{{ notification.message }}</p>
            <p class="notification-date">{{ notification.date }}</p>
          </el-card>
        </div>
        <el-empty v-else description="暂无通知" :image-size="120"></el-empty>
      </div>

      <div class="load-more-container">
        <el-button
          v-if="hasMoreNotifications"
          type="primary"
          plain
          :loading="notificationLoading"
          @click="fetchNotifications(activeNotificationTab, false)"
          class="load-more-btn"
        >
          {{ notificationLoading ? '加载中...' : '加载更多' }}
        </el-button>
        <p v-else-if="notifications.length > 0 && noMoreNotifications" class="no-more-text">
          已加载全部通知，没有更多了。
        </p>
        <p v-else-if="!notifications.length && !noMoreNotifications && !notificationLoading" class="no-more-text">
          您当前没有新的通知。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, reactive } from 'vue';
import { ElTabs, ElTabPane, ElCard, ElButton, ElEmpty, ElMessage, ElBadge, ElIcon } from 'element-plus';
import { Bell, Star, ChatDotRound, Monitor } from '@element-plus/icons-vue'; 
import { authAxios } from '@/utils/request';
import { useRouter } from 'vue-router'; 
import { useNotificationStore } from '@/stores/notificationStore';

const router = useRouter(); // 初始化 router

const notificationStore = useNotificationStore();
const unreadCounts = computed(() => notificationStore.unreadCounts);

// 当前激活的通知分类标签页
const activeNotificationTab = ref('all');
// 存放通知列表的数组
const notifications = ref([]);
// 分页信息
const currentPage = ref(0); // 从0开始的页码
const pageSize = ref(10);
const totalNotifications = ref(0);
// 状态标记
const notificationLoading = ref(false);
const noMoreNotifications = ref(false);

// 计算属性，判断是否还有更多通知
const hasMoreNotifications = computed(() => {
  return notifications.value.length < totalNotifications.value && !noMoreNotifications.value;
});

// 获取通知列表
const fetchNotifications = async (category, reset = false) => {
  if (notificationLoading.value) return;
  if (noMoreNotifications.value && !reset) {
    ElMessage.info('没有更多通知了。');
    return;
  }

  notificationLoading.value = true;
  if (reset) {
    currentPage.value = 0; // 重置页码为0
    notifications.value = [];
    noMoreNotifications.value = false;
  }

  const nextPage = currentPage.value + 1; // 请求下一页数据

  try {
    const params = {
      page: nextPage,
      size: pageSize.value,
      category: category === 'all' ? undefined : category,
    };

    console.log('category',category)

    const response = await authAxios.get('/notifications', { params });

    if (response.data.code === 200 && response.data.data) {
      const newNotificationsRaw = response.data.data.content;
      totalNotifications.value = response.data.data.totalElements;

      console.log('newNotificationsRaw',newNotificationsRaw)
      // 数据转换与格式化
      const formattedNewNotifications = newNotificationsRaw.map(item => ({
        id: item.id,
        read: item.read,
        title: item.description || '无标题',
        message: item.content || '无内容',
        date: new Date(item.createdTime).toLocaleString(),
        type: item.type, 
        relatedItemId: item.relatedItemId, 
      }));

      if (reset) {
        notifications.value = formattedNewNotifications;
      } else {
        notifications.value = [...notifications.value, ...formattedNewNotifications];
      }

      console.log('所有消息',notifications)

      currentPage.value = nextPage; // 更新当前页码

      if (notifications.value.length >= totalNotifications.value) {
        noMoreNotifications.value = true;
      }
      // 在加载完通知后，更新未读数量
      notificationStore.fetchUnreadCounts();

    } else {
      ElMessage.warning('未能获取通知数据，请稍后再试。');
      noMoreNotifications.value = true;
    }
  } catch (error) {
    console.error("获取通知失败:", error);
    ElMessage.error('获取通知列表失败，请检查网络或稍后再试。');
    noMoreNotifications.value = true;
  } finally {
    notificationLoading.value = false;
  }
};

const markCurrentCategoryAsRead = async () => {
  const currentCategory = activeNotificationTab.value;

  ElMessage.info(`正在将 ${currentCategory} 类别通知标记为已读...`);
  try {
    const response = await authAxios.post('/notifications/mark-as-read', {
      category: currentCategory, 
    });

    if (response.data.code === 200) {
      ElMessage.success(response.data.message || `“${currentCategory}”类别通知已标记为已读！`);
      await fetchNotifications(activeNotificationTab.value,true);
      notificationStore.fetchUnreadCounts(); 
    } else {
      ElMessage.error(response.data.message || `标记“${currentCategory}”类别通知为已读失败。`);
    }
  } catch (error) {
    console.error(`标记“${currentCategory}”类别通知为已读失败:`, error);
    ElMessage.error(`操作失败: ${error.response?.data?.message || '网络错误或服务器问题。'}`);
  }
};


const Turn = async (notification) => {
  let targetRoute = null;

  switch (notification.type) {
    case 'NEW_POST_COMMENT':
      if (notification.relatedItemId) {
        targetRoute = {
          name: 'TravelNoteDetail',
          params: { id: notification.relatedItemId }
        };
      } else {
        ElMessage.warning('评论回复通知无有效跳转链接。');
        return;
      }
      break;

    case 'NEW_POST_LIKE':
    case 'NEW_POST_FAVORITE':
      if (notification.relatedItemId) {
        targetRoute = {
          name: 'TravelNoteDetail',
          params: { id: notification.relatedItemId }
        };
      } else {
        ElMessage.warning('点赞/收藏通知无有效跳转链接。');
        return;
      }
      break;

    default:
      ElMessage.info('此类型通知不支持跳转。');
      console.log('阻止跳转的通知类型:', notification.type);
      return;
  }

  // 成功构建路由后，在新标签页打开
  const resolvedRoute = router.resolve(targetRoute);
  if (resolvedRoute && resolvedRoute.href) {
    window.open(resolvedRoute.href, '_blank');
  } else {
    ElMessage.error('无法解析跳转链接。');
    console.error('路由解析失败:', targetRoute);
  }
};



// 获取通知来源文本
const getNotificationSource = (type) => {
  switch (type) {
    case 'likes': return '点赞与收藏';
    case 'comments': return '评论与回复';
    case 'system': return '系统与订单';
    default: return '通知';
  }
};

// 监听 activeNotificationTab 变化，重新加载数据
watch(activeNotificationTab, (newTab) => {
  console.log(`Tab changed to: ${newTab}`);
  fetchNotifications(newTab, true); // 切换 Tab 时，重置并获取新数据
});

onMounted(() => {
  fetchNotifications(activeNotificationTab.value, true); // 首次加载默认“全部通知”
  notificationStore.fetchUnreadCounts();                 // 加载未读数字
});

// 通知类型映射
const NOTIFICATION_TYPE_MAP = {
  // --- 点赞和收藏类 (映射到前端的 'likes' 分类) ---
  // 游记相关的通知直接指向 'TravelNoteDetail' 路由，并通过 relatedItemId 传递 ID
  NEW_POST_LIKE: { category: 'likes', sourceText: '游记点赞', routeName: 'TravelNoteDetail' },
  NEW_POST_FAVORITE: { category: 'likes', sourceText: '游记收藏', routeName: 'TravelNoteDetail' },
  // 旅行团相关的通知直接指向 'TravelGroupDetail' 路由，并通过 relatedItemId 传递 ID
  NEW_PACKAGE_FAVORITE: { category: 'likes', sourceText: '旅行团收藏', routeName: 'TravelGroupDetail' },

  // --- 评论和回复类 (映射到前端的 'comments' 分类) ---
  // 游记评论指向 'TravelNoteDetail' 路由
  NEW_POST_COMMENT: { category: 'comments', sourceText: '游记评论', routeName: 'TravelNoteDetail' },
  // 旅行团评论指向 'TravelGroupDetail' 路由
  NEW_PACKAGE_COMMENT: { category: 'comments', sourceText: '旅行团评论', routeName: 'TravelGroupDetail' },
  // 评论回复需要特殊处理，因为其 relatedItemId 可能指向评论本身或父实体，因此 routeName 设为 null
  NEW_COMMENT_REPLY: { category: 'comments', sourceText: '评论回复', routeName: null },

  // --- 系统和订单类 (映射到前端的 'system' 分类) ---
  // 订单相关通知，都将跳转到商家个人主页的订单管理 Tab
  ORDER_CREATED: { category: 'system', sourceText: '订单创建', routeName: '团长个人主页', queryTab: 'orderManagement' },
  ORDER_PAID: { category: 'system', sourceText: '订单支付', routeName: '团长个人主页', queryTab: 'orderManagement' },
  ORDER_CANCELED: { category: 'system', sourceText: '订单取消', routeName: '团长个人主页', queryTab: 'orderManagement' },

  // 旅行团审核相关通知，都将跳转到商家个人主页的旅行团管理 Tab
  PACKAGE_APPROVED: { category: 'system', sourceText: '旅行团审核通过', routeName: '团长个人主页', queryTab: 'tourManagement' },
  PACKAGE_REJECTED: { category: 'system', sourceText: '旅行团审核驳回', routeName: '团长个人主页', queryTab: 'tourManagement' },

  // 用户已支付订单通知，直接跳转到商家个人主页 (概述 Tab)
  USER_PAID: { category: 'system', sourceText: '用户已支付一笔订单', routeName: '团长个人主页' },

  // 临行提醒需要特殊处理，因为您的路由中没有明确的用户订单详情页，可能需要跳到用户个人主页的订单 Tab 或新建路由
  DEPARTURE_REMINDER: { category: 'system', sourceText: '临行提醒', routeName: null },
};
</script>

<style scoped>
/* 整体布局容器 */
.notification-container {
  width: 100%;
  max-width: 1200px; /* 限制最大宽度，居中显示 */
  margin: 0px auto; /* 上下外边距，左右居中 */
  border-radius: 12px;
  padding-bottom: 20px; /* 底部留白 */
  box-sizing: border-box;
  display: flex; /* 让顶部导航和内容区域垂直排列 */
  flex-direction: column;
}

/* 顶部导航栏容器 */
.notification-tabs-horizontal.beautiful-navbar {
  border-bottom: 1px solid #e0e0e0; /* 底部细分隔线 */
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
  padding: 0 20px; /* 左右内边距 */
}

/* 隐藏 Element Plus 默认的 Tab 内容区域*/
.notification-tabs-horizontal :deep(.el-tabs__content) {
  display: none;
}

/* 调整 Tab 标签的样式 */
.notification-tabs-horizontal :deep(.el-tabs__header) {
  margin-bottom: 0; /* 移除 header 底部默认间距 */
}

.notification-tabs-horizontal :deep(.el-tabs__nav-wrap::after) {
  height: 0; /* 移除默认的底部线条 */
}

.notification-tabs-horizontal :deep(.el-tabs__item) {
  height: 50px; /* Tab 项高度 */
  line-height: 50px;
  padding: 0 20px; /* 左右内边距 */
  color: #606266;
  font-weight: 500;
  transition: all 0.3s ease;
  font-size: 1rem;
}

.notification-tabs-horizontal :deep(.el-tabs__item:hover) {
  color: #54b4a7; /* 悬停文字颜色 */
}

.notification-tabs-horizontal :deep(.el-tabs__item.is-active) {
  color: #52b8a7; /* 激活状态的文字颜色 */
  font-weight: 600;
  background-color: transparent; /* 激活时背景透明 */
  /* 我们可以通过 active-bar 来控制底部指示线，或者自定义 */
}

.notification-tabs-horizontal :deep(.el-tabs__active-bar) {
  height: 3px; /* 激活条高度 */
  background-color: #52b8a7; /* 激活条颜色 */
  border-radius: 2px; /* 激活条圆角 */
}

/* 顶部导航栏项内容 */
.navbar-item-content {
  display: flex;
  align-items: center;
  gap: 8px; /* 图标和文字之间的间距缩小 */
  position: relative; /* 用于徽标定位 */
}

.navbar-item-content .el-icon {
  font-size: 1.2rem; /* 图标大小 */
  color: inherit; /* 继承父元素的颜色 */
}

.navbar-item-content .item-text {
  /* 在水平导航中，文字不需要 flex-grow 来占据剩余空间 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 未读消息徽标 */
.notification-badge {
  /* 在水平导航中，徽章可以更紧凑地跟随文字 */
  margin-left: 5px; /* 徽章与文字的间距 */
  transform: translateY(-2px); /* 微调垂直位置 */
  --el-badge-bg-color: var(--el-color-danger);
  --el-badge-font-size: 10px;
  --el-badge-size: 18px;
}

/* 通知内容区域 - 调整为适应顶部导航 */
.notification-content-area-top {
  flex: 1; /* 自动填充剩余空间 */
  min-width: 0;
  background-color: #fff; /* 内容区域背景色 */
  border-bottom-left-radius: 12px; /* 底部圆角 */
  border-bottom-right-radius: 12px;
  padding: 25px; /* 内容区域内边距增加 */
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* 通知动作区域 */
.notification-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.mark-read-btn {
  border-radius: 8px;
  padding: 8px 15px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 5px;
}

.notification-list-wrapper {
  flex-grow: 1;
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 单个通知卡片 */
.notification-item {
  border-radius: 10px;
  overflow: hidden;
  padding: 18px;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
  border: 1px solid #ebeef5;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
  justify-content: space-between;
  width: 100%;
}

.notification-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  background-color: #f7f9fc;
}

.notification-item.is-read {
  opacity: 0.8;
  background-color: #f5f7fa;
}

/* 通知头，包含来源和未读点 */
.notification-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 10px;
}

.notification-source {
  font-size: 0.8rem;
  color: #606266;
  background-color: #e0e0e0;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 未读圆点 */
.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background-color: var(--el-color-primary);
  border-radius: 50%;
  margin-left: auto;
  flex-shrink: 0;
  animation: pulse-dot 1.5s infinite ease-out;
}

@keyframes pulse-dot {
  0% { transform: scale(0.8); opacity: 0.7; }
  50% { transform: scale(1.1); opacity: 1; }
  100% { transform: scale(0.8); opacity: 0.7; }
}

.notification-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
}

.notification-text {
  font-size: 0.9rem;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.notification-date {
  font-size: 0.8rem;
  color: #909399;
  align-self: flex-end;
}

/* 加载更多容器和按钮 */
.load-more-container {
  text-align: center;
  padding-top: 25px;
  margin-top: auto;
}

.load-more-btn {
  width: 160px;
  height: 42px;
  font-size: 1rem;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.no-more-text {
  text-align: center;
  color: #a8a8a8;
  font-size: 0.9rem;
  margin-top: 15px;
  padding-bottom: 10px;
}

/* ElEmpty 样式调整 */
.el-empty {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}
</style>