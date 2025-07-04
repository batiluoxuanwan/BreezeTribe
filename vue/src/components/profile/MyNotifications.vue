<template>
    <div class="notification-center">
    <el-tabs v-model="activeNotificationTab" tab-position="left" class="notification-tabs-vertical beautiful-sidebar">
      <el-tab-pane name="all">
        <template #label>
          <div class="sidebar-item-content">
            <span class="item-text">全部通知</span>
            </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="likes">
        <template #label>
          <div class="sidebar-item-content">
            <span class="item-text">赞和收藏</span>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="comments">
        <template #label>
          <div class="sidebar-item-content">
            <span class="item-text">评论与@</span>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane name="system">
        <template #label>
          <div class="sidebar-item-content">
            <span class="item-text">系统消息</span>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="notificationLoading">
      <div v-if="notifications.length > 0" class="notification-list">
        <el-card
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item hover-card"
          :class="{ 'is-read': notification.isRead }"
          @click="markAsReadAndNavigate(notification)"
        >
          <div class="notification-content">
            <p class="notification-title">
              <el-tag v-if="!notification.isRead" type="danger" size="small" effect="dark" class="unread-dot"></el-tag>
              {{ notification.title }}
            </p>
            <p class="notification-text">{{ notification.message }}</p>
            <p class="notification-date">{{ notification.date }}</p>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无通知"></el-empty>
    </div>
  </div>
  <div class="load-more-container">
      <el-button
        v-if="hasMoreNotifications"
        type="text"
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
</template>

<script setup>
import { onMounted,ref,computed,watch } from 'vue'
import { ElTabs, ElTabPane, ElCard, ElButton, ElEmpty, ElMessage } from 'element-plus';
import { authAxios } from '@/utils/request';

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

//获取通知列表
const fetchNotifications = async (category, reset = false) => {
  if (notificationLoading.value) return
  if (noMoreNotifications.value && !reset) {
    ElMessage.info('没有更多通知了。')
    return
  }

  notificationLoading.value = true
  if (reset) {
    currentNotificationPage.value = 0
    notifications.value = []
    noMoreNotifications.value = false
  }

  const nextPage = currentNotificationPage.value + 1

  try {
    const params = {
      page: nextPage,
      size: pageNotificationSize.value,
      category: category === 'all' ? undefined : category,
    }

    const response = await authAxios.get('/notifications', { params })

    if (response.data.code === 200 && response.data.data) {
      const newNotificationsRaw = response.data.data.content
      totalNotifications.value = response.data.data.totalElements

      // 数据转换
      const formattedNewNotifications = newNotificationsRaw.map(item => ({
        id: item.id,
        isRead: item.isRead,
        title: item.description, 
        message: item.content, 
        date: new Date(item.createdTime).toLocaleString(), 
        triggerUsername: item.triggerUsername,
        relatedItemId: item.relatedItemId,
        type: item.type,
      }))

      if (reset) {
        notifications.value = formattedNewNotifications
      } else {
        notifications.value = [...notifications.value, ...formattedNewNotifications]
      }

      currentNotificationPage.value = nextPage

      if (notifications.value.length >= totalNotifications.value) {
        noMoreNotifications.value = true
      }
    } else {
      ElMessage.warning('未能获取通知数据，请稍后再试。')
      noMoreNotifications.value = true
    }
  } catch (error) {
    console.error("获取通知失败:", error)
    ElMessage.error('获取通知列表失败，请检查网络或稍后再试。')
    noMoreNotifications.value = true
  } finally {
    notificationLoading.value = false
  }
}

// 监听 activeNotificationTab 变化，重新加载数据
watch(activeNotificationTab, (newTab) => {
  console.log(`Tab changed to: ${newTab}`);
  fetchNotifications(newTab, true); // 切换 Tab 时，重置并获取新数据
});

onMounted(() => {
  fetchNotifications(activeNotificationTab.value, true);
});
</script>

<style>
.notification-center {
  display: flex;
  width: 100%; 
  gap: 20px; /* 二级导航和通知列表之间的间距 */
  background-color: transparent;
  padding: 0; 
  box-shadow: none; 
  border-radius: 0; 
}

.notification-tabs-vertical.beautiful-sidebar {
  flex-shrink: 0; /* 不会被压缩 */
  width: 100px; /* 固定宽度，可以根据需要调整 */
  background-color: #fff; 
  border-radius: 8px; 
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); /* 柔和阴影 */
  padding-top: 20px;
  padding-bottom: 10px;
  box-sizing: border-box;
  overflow: hidden;
}

.notification-item {
  display: flex;
  width:100%;
  align-items: center;
  border-radius: 14px;
  overflow: hidden;
  padding: 16px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: 1px solid #ebebeb;
   flex-direction: column;
  align-items: flex-start;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-center > div:not(.notification-tabs-vertical) {
  flex: 1; /* 保证右侧通知列表区域自动拉伸 */
  min-width: 0;
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

.no-more-text {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 15px;
}

.load-more-container {
  text-align: center;
  padding: 20px;
  margin-top: 30px; 
}

</style>