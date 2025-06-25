<template>
  <div class="user-profile-page">
    <aside class="sidebar">
      <el-avatar
        :src="user.avatarUrl || '/default-avatar.png'"
        :size="100"
        class="avatar"
      />
      <h2 class="username">{{ user.username }}</h2>
      <div class="stats">
        <div><strong>{{ user.collectedCount }}</strong><p>收藏团</p></div>
        <div><strong>{{ user.joinedCount }}</strong><p>已报名</p></div>
        <div><strong>{{ user.noteCount }}</strong><p>游记</p></div>
      </div>
      <el-button @click="editProfileDialog = true" type="primary" plain class="edit-profile-btn">编辑资料</el-button>

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
      </div>
    </aside>

    <main class="main-content">
      <el-tabs v-model="activeTab" class="hidden-tabs-header">
        <el-tab-pane label="我的收藏" name="collected">
          <div v-if="collectedTours.length > 0" class="card-grid">
            <el-card
              v-for="tour in collectedTours"
              :key="tour.id"
              class="tour-card hover-card"
            >
              <img :src="tour.image" class="card-img" />
              <div class="card-info">
                <h3>{{ tour.title }}</h3>
                <p>{{ tour.location }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无收藏"></el-empty>
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
          <div v-if="notes.length > 0" class="card-grid">
            <el-card
              v-for="note in notes"
              :key="note.id"
              class="note-card hover-card"
            >
              <img v-if="note.image" :src="note.image" class="note-img" />
              <div class="card-info">
                <h3>{{ note.title }}</h3>
                <p>{{ note.description }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无游记"></el-empty>
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
      </el-tabs>
    </main>

    <el-dialog v-model="editProfileDialog" title="编辑资料" width="400px">
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Star, Tickets, EditPen, Comment, Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const user = ref({
  username: '旅行者小明',
  avatarUrl: 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg',
  collectedCount: 4,
  joinedCount: 2,
  noteCount: 3,
})

const collectedTours = ref([
  { id: 1, title: '桂林山水团', image: 'https://picsum.photos/id/10/120/90', location: '广西' },
  { id: 2, title: '巽寮湾之旅', image: 'https://picsum.photos/id/20/120/90', location: '广东' },
  { id: 3, title: '新疆伊犁风光', image: 'https://picsum.photos/id/30/120/90', location: '新疆' },
  { id: 4, title: '青海湖秘境', image: 'https://picsum.photos/id/40/120/90', location: '青海' },
])

const joinedTours = ref([
  { id: 3, title: '成都美食团', image: 'https://picsum.photos/id/50/120/90', date: '2025-07-10', progress: 50 }, // 添加进度
  { id: 4, title: '上海迪士尼乐园', image: 'https://picsum.photos/id/60/120/90', date: '2025-08-01', progress: 80 }, // 添加进度
])

const notes = ref([
  { id: 1, title: '稻城亚丁旅行记', description: '遇见最蓝的湖，洗涤心灵', image: 'https://picsum.photos/id/70/120/90' },
  { id: 2, title: '重庆火锅之旅', description: '麻辣鲜香，不虚此行', image: 'https://picsum.photos/id/80/120/90' },
  { id: 3, title: '徽州古村落探秘', description: '白墙黛瓦，如诗如画', image: 'https://picsum.photos/id/90/120/90' },
])

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

const editProfileDialog = ref(false)
const editForm = ref({ username: user.value.username, password: '' })
const unreadNotifications = ref(1);

function saveProfile() {
  user.value.username = editForm.value.username
  ElMessage.success('资料保存成功！')
  editProfileDialog.value = false
}
</script>

<style scoped>
/* 引入 Google Fonts - Poppins 用于更好的字体视觉效果 */
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');

.user-profile-page {
  display: flex;
  padding: 40px;
  gap: 30px;
  min-height: 100vh;
  box-sizing: border-box;
  font-family: 'Poppins', sans-serif;
  background-image: url('@/assets/background.png');
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
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: sticky;
  top: 40px;
  align-self: flex-start;
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

/* 新增：左侧功能导航菜单样式 */
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

/* 隐藏 Element Plus Tabs 的标签头 */
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
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.15);
  border-color: #409eff;
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
  flex-direction: column; /* 垂直排列 */
  align-items: flex-start; /* 左对齐 */
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

/* 通知卡片样式 */
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

/* Element Plus 对话框美化 */
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
</style>