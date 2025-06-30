<!-- 游记详情 -->
 <template>
  <div class="my-travel-note-detail-container">
    <el-button type="info" class="back-button" @click="goBackToMyNotes">
      <el-icon><ArrowLeftBold /></el-icon>返回
    </el-button>

    <el-card v-if="loading" class="loading-card">
      <el-skeleton animated :rows="6"></el-skeleton>
      <div class="loading-text">正在加载游记详情...</div>
    </el-card>

    <el-card v-else-if="error" class="error-card">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetchNoteDetail(route.params.id)">重试</el-button>
        </template>
      </el-result>
    </el-card>

    <el-card v-else-if="!note" class="empty-card">
      <el-empty description="未找到该游记或游记不存在"></el-empty>
    </el-card>

    <el-card v-else class="note-detail-card">
      <div class="author-actions">
        <el-button type="primary" :icon="Edit" @click="editNote">编辑游记</el-button>
        <el-popconfirm
          title="确定要删除这篇游记吗？删除后将无法恢复！"
          confirm-button-text="确认删除"
          cancel-button-text="取消"
          @confirm="deleteNote"
        >
          <template #reference>
            <el-button type="danger" :icon="Delete">删除游记</el-button>
          </template>
        </el-popconfirm>
      </div>

      <h1 class="note-title">{{ note.title }}</h1>
      <div class="note-meta">
        <el-avatar :src="note.author.avatarUrl || '/default-avatar.png'" :size="40"></el-avatar>
        <span class="author-name">{{ note.author.username || '匿名用户' }}</span>
        <span class="publish-date">发布于：{{ formatDateTime(note.createdTime) }}</span>
        <span class="views">
          <el-icon><View /></el-icon>{{ note.viewCount || 0 }} 阅读
        </span>
        <span class="likes">
          <el-icon><Pointer /></el-icon>{{ note.likeCount || 0 }} 喜欢
        </span>
        <span class="comments">
          <el-icon><ChatDotRound /></el-icon>{{ note.commentCount || 0 }} 评论
        </span>
      </div>

      <el-divider></el-divider>

      <div class="note-content" v-html="note.content">
        </div>

      <div v-if="note.imageUrls && note.imageUrls.length > 0" class="image-gallery">
          <img
            v-for="(imageUrl, index) in note.imageUrls"
            :key="index"
            :src="imageUrl"
            alt="游记图片"
            class="gallery-image"
            @click="previewImage(imageUrl, note.imageUrls)"
          />
      </div>

      <div class="note-tags" v-if="note.tags && note.tags.length">
        <el-tag v-for="(tag, index) in note.tags" :key="index" size="large" class="note-tag">{{ tag }}</el-tag>
      </div>
      
      <el-divider></el-divider>

      <div class="comment-section">
        <h2>评论</h2>
        <el-empty description="暂无评论" v-if="note.commentsCount === 0"></el-empty>
        <div v-if="note.comments && note.comments.length">
          <div v-for="comment in note.comments" :key="comment.id" class="comment-item">
            <el-avatar :src="comment.user?.avatarUrl || '/default-avatar.png'" :size="30"></el-avatar>
            <div class="comment-info">
              <span class="comment-user">{{ comment.user?.name || '匿名用户' }}</span>
              <span class="comment-date">{{ formatDateTime(comment.commentDate) }}</span>
              <p class="comment-text">{{ comment.text }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElCard, ElButton, ElMessage, ElLoading, ElSkeleton, ElResult, ElEmpty, ElAvatar, ElIcon, ElDivider, ElImage, ElTag, ElPopconfirm } from 'element-plus';
import { ArrowLeftBold, Edit, Delete, View, ChatDotRound, Picture } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request'; 

const route = useRoute();
const router = useRouter();

const note = ref(null);
const loading = ref(true);
const error = ref(null);

// --- 格式化日期时间 ---
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '未知时间';
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};


// --- 获取游记详情数据 ---
const fetchNoteDetail = async (noteId) => {
  if (!noteId) {
    error.value = '游记ID无效，无法加载。';
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = null;
  note.value = null; // 清空旧数据

  try {
    const response = await authAxios.get(`/user/posts/${noteId}`); 
    if (response.data.code === 200) {
      note.value = response.data.data;
      ElMessage.success('游记详情加载成功！');
    } else {
      error.value = response.data.message || '获取游记详情失败。';
      ElMessage.error(error.value);
    }
  } catch (err) {
    console.error('Error fetching travel note detail:', err);
    error.value = '网络请求失败或服务器错误，请稍后再试。';
    ElMessage.error(error.value);
  } finally {
    loading.value = false;
  }
};

// --- 返回我的游记列表 ---
const goBackToMyNotes = () => {
  router.push({ name: '用户个人主页', query: { tab: 'notes' } }); 
};

// --- 编辑游记 ---
const editNote = () => {
  if (note.value && note.value.id) {
    // 跳转到编辑游记页面，并带上游记ID
    router.push({ name: '用户编辑游记', params: { id: note.value.id } }); 
  } else {
    ElMessage.warning('无法编辑，游记信息不完整。');
  }
};

// --- 删除游记 ---
const deleteNote = async () => {
  if (!note.value || !note.value.id) {
    ElMessage.warning('无法删除，游记ID无效！');
    return;
  }
  try {
    const response = await authAxios.delete(`/user/posts/${note.value.id}`); 
    if (response.data.code === 200) {
      ElMessage.success('游记删除成功！');
      router.push({ name: '用户个人主页', query: { tab: 'notes' } });
    } else {
      ElMessage.error(response.data.message || '删除游记失败！');
    }
  } catch (err) {
    console.error('Error deleting travel note:', err);
    ElMessage.error('网络请求失败，删除游记失败！');
  }
};


// --- 生命周期钩子 ---
onMounted(() => {
  const noteId = route.params.id;
  fetchNoteDetail(noteId);
});

watch(
  () => route.params.id, // 监听路由参数 id 的变化
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      console.log(`MyTravelNoteDetail: 路由 ID 变化，从 ${oldId} 变为 ${newId}。重新加载数据。`);
      fetchNoteDetail(newId);
    }
  }
);
</script>

<style scoped>
.my-travel-note-detail-container {
  min-height: 100vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center; 
  font-family: 'Noto Sans SC', sans-serif, 'Helvetica Neue', Helvetica, Arial, sans-serif;
  position: relative; 
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

/* 固定在左上角的返回按钮 */
.back-button {
  position: fixed; /* 固定定位 */
  top: 20px;
  left: 20px;
  z-index: 1000; /* 确保在最顶层 */
  background-color: rgba(0, 0, 0, 0.5); /* 半透明背景 */
  border: none;
  color: #fff;
  padding: 10px 15px;
  border-radius: 20px; /* 更圆润 */
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}
.back-button:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

.loading-card, .error-card, .empty-card {
  padding: 40px;
  text-align: center;
  border-radius: 8px;
}

.loading-text {
  margin-top: 20px;
  color: #606266;
}

.note-detail-card {
  max-width: 700px; 
  width: 100%; 
  margin: 20px auto; 
  border-radius: 10px; 
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); 
  background-color: #ffffff; 
  padding: 20px 25px; 
}

.author-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  gap: 10px; /* 按钮间距 */
}

.note-title {
  font-size: 2.5em;
  color: #333;
  text-align: center;
  margin-bottom: 20px;
  line-height: 1.3;
}

.note-meta {
  display: flex;
  align-items: center;
  justify-content: center; /* 居中显示元信息 */
  gap: 15px; /* 各个元信息项的间距 */
  color: #666;
  font-size: 0.95em;
  margin-bottom: 30px;
  flex-wrap: wrap; /* 允许在小屏幕上换行 */
}

.note-meta .el-avatar {
  flex-shrink: 0; /* 防止头像被压缩 */
}

.note-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.note-meta .el-icon {
  font-size: 1.1em;
  color: #909399;
}

.note-cover-wrapper {
  margin: 30px auto;
  max-width: 700px; /* 控制封面图最大宽度 */
  height: 400px; /* 固定高度，保持比例 */
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.note-cover {
  width: 100%;
  height: 100%;
  display: block; /* 移除图片底部空白 */
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 1.2em;
}

.note-content {
  line-height: 1.8;
  font-size: 1.1em;
  color: #333;
  min-height: 100px; 
  margin-top: 30px;
  padding: 0 20px; 
  word-wrap: break-word; 
}

/* 针对富文本内容的基础样式重置，防止外部CSS影响 */
.note-content :deep(p) {
  margin-bottom: 1em;
}
.note-content :deep(h1), .note-content :deep(h2), .note-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  color: #2c3e50;
}
.note-content :deep(img) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1em auto;
  border-radius: 4px;
}
.note-content :deep(pre), .note-content :deep(code) {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
}

/* 图片画廊（图片列表）样式 */
.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); /* 更灵活的列数，小图显示更多 */
  gap: 10px; /* 图片之间的间距 */
  margin-top: 20px;
  margin-bottom: 25px;
  max-width: 100%; /* 确保不超过父容器 */
  justify-content: center; /* 图片少时居中显示 */
}

.gallery-image {
  width: 100%;
  height: 150px; /* 固定高度，保持图片大小一致 */
  object-fit: cover; /* 裁剪图片以填充容器 */
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: zoom-in; /* 鼠标悬停时显示放大镜图标 */
  transition: transform 0.2s ease-in-out;
}

.gallery-image:hover {
  transform: scale(1.02); /* 悬停放大效果 */
}

.note-tags {
  margin-top: 30px;
  text-align: center;
  padding: 0 20px;
}

.note-tag {
  margin: 5px;
  font-size: 0.9em;
}

.comment-section {
  margin-top: 40px;
  padding: 0 20px;
}

.comment-section h2 {
  font-size: 1.8em;
  color: #333;
  margin-bottom: 20px;
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.comment-info {
  flex-grow: 1;
}

.comment-user {
  font-weight: bold;
  color: #555;
  margin-right: 10px;
}

.comment-date {
  font-size: 0.85em;
  color: #999;
}

.comment-text {
  margin-top: 5px;
  color: #333;
  line-height: 1.6;
}
</style>