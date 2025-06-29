<template>
  <div class="travel-note-detail-page">
    <el-button type="info" :icon="ArrowLeft" class="back-button" @click="goBack">
      返回
    </el-button>

    <el-card v-if="loading" v-loading="loading" class="detail-card is-loading">
      <div class="loading-placeholder">正在加载游记详情...</div>
    </el-card>

    <el-card v-else-if="note" class="detail-card">
      <div class="card-header">
        <div class="author-profile">
          <el-avatar :src="note.authorAvatarUrl || '/default-avatar.png'" :size="50" class="author-avatar"></el-avatar>
          <div class="author-info-text">
            <span class="author-name">{{ note.author.username || '匿名用户' }}</span>
            <span class="publish-time">{{ formatTime(note.createdTime) }}</span>
          </div>
        </div>
        <h1 class="note-title">{{ note.title }}</h1>
      </div>

      <div class="note-content">

        <div class="note-text-content" v-html="note.content">
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

        <div v-if="note.spot" class="note-spot">
          <el-icon><Location /></el-icon>
          <span class="spot-name">{{ note.spot.name }}</span>
          <span v-if="note.spot.address" class="spot-address">({{ note.spot.address }})</span>
        </div>

        <div v-if="note.tags && note.tags.length > 0" class="note-tags">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>

        <div class="note-stats">
          <span class="stat-item"><el-icon><View /></el-icon> {{ note.viewCount || 0 }}</span>
          <span class="stat-item"><el-icon><Star /></el-icon> {{ note.favoriteCount || 0 }}</span>
          <span class="stat-item"><el-icon><Pointer /></el-icon> {{ note.likeCount || 0 }}</span>
          <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ note.commentCount || 0 }}</span>
        </div>

        <div class="note-actions">
          </div>
      </div>
    </el-card>

    <el-empty v-else description="游记不存在或已删除" class="empty-state"></el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Star, ChatDotRound } from '@element-plus/icons-vue'; // 引入图标
import { publicAxios } from '@/utils/request'; // 确保您有这个封装好的公共请求实例

const route = useRoute(); // 获取当前路由信息
const router = useRouter(); // 获取路由器实例

const note = ref(null); // 存储游记详情数据
const loading = ref(true); // 加载状态
const postId = ref(null); // 存储从路由获取的 postId

/**
 * 格式化时间戳为更友好的日期时间字符串
 * @param {string} timeString - ISO 格式的时间字符串
 * @returns {string} 格式化后的时间字符串
 */
const formatTime = (timeString) => {
  if (!timeString) return '';
  const date = new Date(timeString);
  // 使用 toLocaleString 格式化为本地化的日期和时间，精确到分钟
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

/**
 * 获取游记详情的函数
 */
const fetchNoteDetail = async () => {
  loading.value = true;
  try {
    // 调用 GET /api/public/posts/{postId} 接口
    const response = await publicAxios.get(`/public/posts/${postId.value}`);
    
    if (response.data && response.data.code === 200 && response.data.data) {
      note.value = response.data.data;
      note.value.authorAvatarUrl = note.value.authorAvatarUrl || '/default-avatar.png'; 
      ElMessage.success('游记详情加载成功！');
      console.log(note.value)
    } else {
      note.value = null;
      ElMessage.error('未能获取游记详情，请检查游记ID或稍后再试。');
    }
  } catch (error) {
    console.error('获取游记详情失败:', error);
    note.value = null;
    ElMessage.error('获取游记详情失败，请检查网络或稍后再试。');
  } finally {
    loading.value = false;
  }
};

/**
 * 返回上一页
 */
const goBack = () => {
  router.back(); // 使用 router.back() 返回上一页
};

// 组件挂载时执行
onMounted(() => {
  // 从路由参数中获取 postId
  // 请根据您的路由配置调整参数名，例如：route.params.id 或 route.params.postId
  postId.value = route.params.id; 
  if (postId.value) {
    fetchNoteDetail();
  } else {
    loading.value = false;
    ElMessage.error('缺少游记ID，无法加载详情。');
  }
});
</script>

<style scoped>
/* 引入 Google Fonts - Noto Sans SC 用于更现代的中文显示 */
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

/* 页面整体容器 */
.travel-note-detail-page {
  min-height: 100vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center; /* 让卡片居中 */
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
}
.back-button:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

/* 游记详情卡片 */
.detail-card {
  max-width: 700px; 
  width: 100%; 
  margin: 20px auto; 
  border-radius: 10px; 
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); 
  background-color: #ffffff; 
  padding: 20px 25px; 
}

/* 加载状态时的卡片高度，避免抖动 */
.detail-card.is-loading {
  min-height: 300px; 
}

/* 加载占位符样式 */
.loading-placeholder {
  min-height: 150px; 
  display: flex;
  justify-content: center;
  align-items: center;
  color: #909399;
  font-size: 1.1em;
}

/* 卡片头部 - 模拟朋友圈的用户信息 */
.card-header {
  padding-bottom: 20px; /* 增加底部间距 */
  border-bottom: none; /* 移除底部分割线 */
}

.author-profile {
  display: flex;
  align-items: center;
  margin-bottom: 15px; /* 头像和标题之间的间距 */
}

.author-avatar {
  margin-right: 15px;
  border: 2px solid #eee; /* 浅色边框 */
}

.author-info-text {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 1.15em;
  font-weight: 600;
  color: #333;
  margin-bottom: 3px;
}

.publish-time {
  font-size: 0.85em;
  color: #909399; /* 浅灰色时间 */
}

/* 游记标题样式 */
.note-title {
  font-size: 1.6em; /* 标题稍小，更像动态 */
  color: #2c3e50; /* 深色文字 */
  margin: 0; /* 移除默认外边距 */
  line-height: 1.4;
  font-weight: 700;
  text-align: left; /* 标题左对齐 */
}

/* 游记内容区域 */
.note-content {
  padding: 10px 0; /* 调整内容内边距 */
}

/* 封面图片容器 */
.cover-image-container {
  margin-bottom: 25px; /* 封面图下方间距 */
}

/* 封面图片样式 */
.cover-image {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

/* 游记正文富文本内容样式，使用 :deep() 穿透 Element Plus 默认样式 */
.note-text-content {
  line-height: 1.8; /* 更舒适的行高 */
  font-size: 1.05em; /* 文本大小 */
  color: #303133;
  text-align: justify; /* 文本两端对齐 */
  margin-bottom: 20px;

  :deep(p) {
    margin-bottom: 0.8em; /* 段落间距 */
    word-break: break-word; /* 单词换行 */
  }
  :deep(img) {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 15px auto;
    border-radius: 6px;
    box-shadow: 0 1px 5px rgba(0, 0, 0, 0.1);
  }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin-top: 1.5em;
    margin-bottom: 0.8em;
    color: #34495e; /* 更柔和的标题颜色 */
    font-weight: 600;
  }
  :deep(ul), :deep(ol) {
    margin-left: 25px; /* 列表缩进 */
    margin-bottom: 1em;
  }
  :deep(li) {
    margin-bottom: 0.5em;
  }
  :deep(a) {
    color: #409EFF;
    text-decoration: none;
  }
  :deep(pre) { /* 代码块 */
    background-color: #f8f8f8;
    border-left: 3px solid #409EFF;
    padding: 10px 15px;
    margin-bottom: 1em;
    overflow-x: auto;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
    font-size: 0.9em;
  }
  :deep(blockquote) { /* 引用块 */
    border-left: 4px solid #ccc;
    padding-left: 15px;
    color: #666;
    margin-bottom: 1em;
    font-style: italic;
  }
}

/* 图片画廊（图片列表）样式 */
.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); /* 响应式列，至少180px宽 */
  gap: 10px; /* 图片之间的间距 */
  margin-top: 20px;
  margin-bottom: 25px;
  max-width: 100%; /* 确保不超过父容器 */
  justify-content: center; /* 图片少时居中显示 */
}

.gallery-image {
  width: 100%;
  height: 180px; /* 固定高度，保持图片大小一致 */
  object-fit: cover; /* 裁剪图片以填充容器 */
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: zoom-in; /* 鼠标悬停时显示放大镜图标 */
  transition: transform 0.2s ease-in-out;
}

.gallery-image:hover {
  transform: scale(1.02); /* 悬停放大效果 */
}

/* 地点信息样式 */
.note-spot {
  display: flex;
  align-items: center;
  margin-top: 15px;
  margin-bottom: 20px;
  color: #606266;
  font-size: 0.95em;
  padding: 8px 12px;
  background-color: #f2f6fc;
  border-left: 3px solid #44beae;
  border-radius: 4px;
}

.note-spot .el-icon {
  margin-right: 8px;
  font-size: 1.1em;
}

.spot-name {
  font-weight: 500;
  margin-right: 5px;
}

.spot-address {
  color: #909399;
}

/* 标签样式 */
.note-tags {
  margin-top: 25px; /* 标签顶部间距 */
  padding-top: 15px;
  border-top: 1px dashed #e4e7ed; /* 虚线分隔 */
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.el-tag {
  background-color: #ecf5ff;
  border-color: #d9ecff;
  font-weight: 500;
  padding: 0 10px; /* 调整标签内边距 */
  height: 28px; /* 调整标签高度 */
  line-height: 26px;
  border-radius: 14px; /* 更圆润的标签 */
}

/* 互动统计数据样式 */
.note-stats {
  display: flex;
  justify-content: space-around; /* 统计数据均匀分布 */
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee; /* 顶部细线 */
  color: #606266;
  font-size: 0.9em;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px; /* 图标和文字间距 */
}

.stat-item .el-icon {
  font-size: 1.1em;
  color: #909399;
}

/* 互动按钮区 */
.note-actions {
  display: flex;
  justify-content: flex-end; /* 按钮靠右 */
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee; /* 顶部细线 */
  gap: 20px; /* 按钮间距 */
}

.note-actions .el-button.el-button--text {
  color: #606266; /* 文本按钮颜色 */
  font-size: 0.95em;
  padding: 0 5px;
}
.note-actions .el-button.el-button--text .el-icon {
  font-size: 1.1em;
  margin-right: 5px;
}

/* 空状态样式 */
.empty-state {
  margin-top: 50px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .travel-note-detail-page {
    padding: 10px;
  }
  .detail-card {
    margin: 10px auto;
    padding: 15px;
  }
  .back-button {
    top: 10px;
    left: 10px;
    padding: 8px 12px;
  }
  .card-header {
    padding-bottom: 15px;
  }
  .note-title {
    font-size: 1.4em;
  }
  .note-text-content {
    font-size: 0.95em;
  }
  .author-avatar {
    width: 40px;
    height: 40px;
  }
  .author-name {
    font-size: 1.05em;
  }
  .publish-time {
    font-size: 0.8em;
  }
}
</style>