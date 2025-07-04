<template>
  <div class="notification-center">
    <el-tabs v-model="activeNotificationTab" tab-position="left" class="notification-tabs-vertical beautiful-sidebar">
      <el-tab-pane name="all">
        <template #label>
          <div class="sidebar-item-content">
            <el-icon><Bell /></el-icon> <span class="item-text">全部通知</span>
            <el-badge v-if="unreadCounts.all > 0" :value="unreadCounts.all" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="likes">
        <template #label>
          <div class="sidebar-item-content">
            <el-icon><Star /></el-icon> <span class="item-text">赞和收藏</span>
            <el-badge v-if="unreadCounts.likes > 0" :value="unreadCounts.likes" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="comments">
        <template #label>
          <div class="sidebar-item-content">
            <el-icon><ChatDotRound /></el-icon> <span class="item-text">评论与@</span>
            <el-badge v-if="unreadCounts.comments > 0" :value="unreadCounts.comments" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="system">
        <template #label>
          <div class="sidebar-item-content">
            <el-icon><Monitor /></el-icon> <span class="item-text">系统消息</span>
            <el-badge v-if="unreadCounts.system > 0" :value="unreadCounts.system" :max="99" class="notification-badge" />
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="notification-content-area">
      <div v-loading="notificationLoading">
        <div v-if="notifications.length > 0" class="notification-list">
          <el-card
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item hover-card"
            :class="{ 'is-read': notification.isRead }"
            @click="markAsReadAndNavigate(notification)"
          >
            <div class="notification-item-header">
              <span class="notification-source">{{ getNotificationSource(notification.type) }}</span>
              <span v-if="!notification.isRead" class="unread-dot"></span> </div>
            <p class="notification-title">{{ notification.title }}</p>
            <p class="notification-text">{{ notification.message }}</p>
            <p class="notification-date">{{ notification.date }}</p>
          </el-card>
        </div>
        <el-empty v-else description="暂无通知" :image-size="80"></el-empty>
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
          已加载全部通知
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, reactive } from 'vue';
import { ElTabs, ElTabPane, ElCard, ElButton, ElEmpty, ElMessage, ElBadge, ElIcon } from 'element-plus';
import { Bell, Star, ChatDotRound, Monitor } from '@element-plus/icons-vue'; // 引入新图标
import { authAxios } from '@/utils/request';
import { useRouter } from 'vue-router'; // 引入 useRouter 用于导航

const router = useRouter(); // 初始化 router

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

// 各类别未读数量
const unreadCounts = reactive({
  all: 0,
  likes: 0,
  comments: 0,
  system: 0,
});

// 计算属性，判断是否还有更多通知
const hasMoreNotifications = computed(() => {
  return notifications.value.length < totalNotifications.value && !noMoreNotifications.value;
});

// 获取通知列表
const fetchNotifications = async (category, reset = false) => {
 console.log('category:',category)
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
      page: nextPage, // 后端页码可能从1开始，或者0开始，请根据实际情况调整
      size: pageSize.value,
      category: category === 'all' ? undefined : category, // 根据分类传递类型参数
    };

    const response = await authAxios.get('/notifications', { params });

    if (response.data.code === 200 && response.data.data) {
      const newNotificationsRaw = response.data.data.content;
      totalNotifications.value = response.data.data.totalElements;

      // 数据转换与格式化
      const formattedNewNotifications = newNotificationsRaw.map(item => ({
        id: item.id,
        isRead: item.isRead,
        title: item.description || '无标题', // 确保有标题
        message: item.content || '无内容', // 确保有内容
        date: new Date(item.createdTime).toLocaleString(), // 格式化日期时间
        type: item.type, // 假设后端返回通知类型，例如 'LIKE', 'COMMENT', 'SYSTEM'
        relatedItemId: item.relatedItemId, // 用于跳转详情页的ID
      }));

      if (reset) {
        notifications.value = formattedNewNotifications;
      } else {
        notifications.value = [...notifications.value, ...formattedNewNotifications];
      }

      currentPage.value = nextPage; // 更新当前页码

      if (notifications.value.length >= totalNotifications.value) {
        noMoreNotifications.value = true;
      }
      // 首次加载或切换分类时，更新未读数量
      if (reset || category === 'all') {
         updateUnreadCounts(); // 仅在加载'all'或重置时更新总未读数
      }

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

// 获取各个分类的未读通知数量
const fetchUnreadCounts = async () => {
    try {
        const response = await authAxios.get('/notifications/unread-counts');
        if (response.data.code === 200 && response.data.data) {
            const counts = response.data.data; 
            Object.assign(unreadCounts, counts);
        }
    } catch (error) {
        console.error('获取未读通知数量失败:', error);
    }
};

// 更新未读数量（当后端没有提供直接的未读分类计数接口时，前端可以从“全部通知”中统计）
const updateUnreadCounts = () => {
  // 假设后端在 `/notifications?type=likes` 等接口中会返回 totalElements 和 content
  // 这里需要为每个分类单独请求未读数量，或者后端提供一个汇总接口。
  // 临时方案：如果后端只提供`all`的isRead状态，你需要在获取到所有通知后在前端进行分类统计
  // 最佳实践：后端提供一个 `/notifications/unreadCounts` 这样的接口
  // 目前先简化处理，实际开发中需调整
  fetchUnreadCounts(); // 调用获取所有分类未读数量的函数
};


// 标记通知为已读并导航
const markAsReadAndNavigate = async (notification) => {
  if (!notification.isRead) {
    try {
      // 假设后端有标记为已读的接口
      await authAxios.post(`/notifications/${notification.id}/read`);
      notification.isRead = true; // 乐观更新UI
      // 更新未读数量
      updateUnreadCounts();
    } catch (error) {
      console.error('标记通知为已读失败:', error);
      ElMessage.error('标记为已读失败。');
    }
  }

  // 根据通知类型导航到不同页面
  // 你需要根据实际业务逻辑定义通知类型和对应的路由
  if (notification.type === 'COMMENT' && notification.relatedItemId) {
    router.push(`/post/${notification.relatedItemId}#comment-${notification.id}`); // 示例：跳转到帖子详情页的特定评论
  } else if (notification.type === 'LIKE' && notification.relatedItemId) {
    router.push(`/post/${notification.relatedItemId}`); // 示例：跳转到帖子详情页
  } else if (notification.type === 'SYSTEM') {
    // 系统通知可能不需要跳转，或者跳转到公告页等
    ElMessage.info('这是一条系统通知，通常不需要跳转。');
  }
  // 如果通知没有相关链接，可以不跳转，或者显示详情弹窗
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
  fetchUnreadCounts(); // 页面加载时获取未读数量
});
</script>

<style scoped>
/* 整体布局容器 */
.notification-center {
  display: flex;
  width: 100%;
  gap: 20px; /* 侧边栏和内容区域的间距 */
  background-color: transparent; /* 背景透明 */
  padding: 20px; /* 增加整体内边距 */
  box-sizing: border-box; /* 包含 padding 和 border */
}

/* 左侧导航栏容器 */
.notification-tabs-vertical.beautiful-sidebar {
  flex-shrink: 0; /* 不会被压缩 */
  width: 180px; /* 增加宽度以容纳图标和文字 */
  background-color: #fff;
  border-radius: 12px; /* 更圆润的边角 */
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08); /* 柔和阴影 */
  padding: 20px 0; /* 上下内边距，左右由 tab-pane 自身控制 */
  box-sizing: border-box;
}

/* 隐藏 Element Plus 默认的 Tab 内容区域，由我们自己控制右侧内容 */
.notification-tabs-vertical :deep(.el-tabs__content) {
  display: none;
}

/* 调整 Tab 标签的样式 */
.notification-tabs-vertical :deep(.el-tabs__item) {
  height: 50px; /* 增加 Tab 项高度 */
  line-height: 50px;
  padding: 0 20px; /* 左右内边距 */
  justify-content: flex-start; /* 内容左对齐 */
  color: #555;
  font-weight: 500;
  transition: all 0.3s ease;
}

.notification-tabs-vertical :deep(.el-tabs__item.is-active) {
  background-color: #e6f7ff; /* 激活状态的背景色 */
  color: #1890ff; /* 激活状态的文字颜色 */
  font-weight: 600;
  border-right: 3px solid #1890ff; /* 激活状态的右侧边框 */
}

.notification-tabs-vertical :deep(.el-tabs__active-bar) {
  display: none; /* 隐藏默认的激活条 */
}

/* 侧边栏项内容 */
.sidebar-item-content {
  display: flex;
  align-items: center;
  gap: 10px; /* 图标和文字之间的间距 */
  width: 100%;
  position: relative; /* 用于徽标定位 */
}

.sidebar-item-content .el-icon {
  font-size: 1.2rem; /* 图标大小 */
  color: inherit; /* 继承父元素的颜色 */
}

.sidebar-item-content .item-text {
  flex-grow: 1; /* 文字占据剩余空间 */
}

/* 未读消息徽标 */
.notification-badge {
  /* margin-left: auto; 让徽标推到右边 */
  position: absolute;
  right: 0; /* 定位到最右侧 */
  top: 50%;
  transform: translateY(-50%);
}

/* 右侧通知内容区域 */
.notification-content-area {
  flex: 1; /* 自动填充剩余空间 */
  min-width: 0; /* 确保在 flex 布局中不会溢出 */
  background-color: #fff; /* 独立的白色背景 */
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  padding: 20px; /* 内容区域内边距 */
  box-sizing: border-box;
  display: flex; /* 让内容和加载更多按钮垂直排列 */
  flex-direction: column;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 15px; /* 通知卡片之间的间距 */
  padding-bottom: 20px; /* 防止加载更多按钮遮挡 */
}

/* 单个通知卡片 */
.notification-item {
  border-radius: 10px; /* 统一圆角 */
  overflow: hidden;
  padding: 15px; /* 调整内边距 */
  background-color: #f9f9f9; /* 稍微深一点的底色 */
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04); /* 更轻的阴影 */
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
  border: 1px solid #eee;
  cursor: pointer; /* 鼠标手型 */
  display: flex; /* 内部内容也使用 flex */
  flex-direction: column;
  align-items: flex-start;
}

.notification-item:hover {
  transform: translateY(-3px); /* 悬停效果 */
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
  background-color: #ffffff; /* 悬停时变为白色 */
}

.notification-item.is-read {
  opacity: 0.7; /* 已读通知稍微变淡 */
  background-color: #f0f0f0; /* 已读通知的背景色 */
}

/* 通知头，包含来源和未读点 */
.notification-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
}

.notification-source {
  font-size: 0.85rem;
  color: #606266;
  background-color: #e0e0e0;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
}

/* 未读圆点 */
.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background-color: #f56c6c; /* Element Plus danger color */
  border-radius: 50%;
  margin-left: 10px; /* 确保与来源文本有间距 */
  flex-shrink: 0; /* 防止被压缩 */
}

.notification-title {
  font-size: 1.05rem; /* 稍微调整标题大小 */
  font-weight: 600;
  color: #333;
  margin-bottom: 5px; /* 标题和正文间距 */
}

.notification-text {
  font-size: 0.95rem; /* 调整正文大小 */
  color: #666;
  line-height: 1.5;
  margin-bottom: 10px; /* 正文和日期间距 */
  /* Ellipsis for long text */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%; /* 防止超出行宽 */
}

.notification-date {
  font-size: 0.8rem; /* 调整日期大小 */
  color: #999;
  align-self: flex-end; /* 让日期靠右对齐 */
}

.load-more-container {
  text-align: center;
  padding-top: 20px;
  /* 确保在内容区域内部底部 */
  margin-top: auto; /* 将加载更多推到底部 */
}

.load-more-btn {
  width: 150px;
  height: 40px;
  font-size: 1rem;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.no-more-text {
  text-align: center;
  color: #aaa;
  font-size: 0.9rem;
  margin-top: 15px;
}
</style>