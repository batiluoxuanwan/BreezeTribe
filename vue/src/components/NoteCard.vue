<template>
  <div class="travel-note-card">
    <div class="image-wrapper">
      <img class="cover" :src="note.coverImageUrl" :alt="note.title" />
      <div v-if="note.category" class="category-badge">{{ note.category }}</div>
      <div class="stats-overlay">
        <span class="view-count">
          <i class="el-icon-view"></i> {{ formatNumber(note.views || 0) }}
        </span>
        <span class="like-count">
          <i class="el-icon-heart"></i> {{ formatNumber(note.likes || 0) }}
        </span>
      </div>
    </div>
    <div class="note-content">
      <h3 class="title">{{ note.title }}</h3>
      <p class="description">{{ note.description }}</p>
      <div class="author-info">
        <img class="avatar" :src="note.author.avatarUrl" :alt="note.author" />
        <span class="author-name">{{ note.author.username }}</span>
        <span class="publish-date">{{ formatDate(note.publishDate) }}</span>
      </div>
      <el-button type="primary" size="small" class="read-more-button" @click.stop="goToDetail">
        阅读详情 <i class="el-icon-arrow-right el-icon--right"></i>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';
import { ElButton } from 'element-plus'; // 确保导入 ElButton
import {useRouter} from 'vue-router';
const router = useRouter();
const props = defineProps({
  note: {
    type: Object,
    required: true,
  },
});

// 跳转详情页并传递游记id
const goToDetail = () => {
  console.log('查看游记详情:', props.note.id);
  router.push({ name: 'TravelNoteDetail', params: { id: props.note.id } });
};

// 辅助函数，用于格式化数字（如浏览量、点赞数）和日期
const formatNumber = (num) => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'; // 转换为千（k）单位
  }
  return num;
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const options = { year: 'numeric', month: 'short', day: 'numeric' };
  return new Date(dateString).toLocaleDateString('zh-CN', options);
};
</script>

<style scoped>
.travel-note-card {
  width: 280px; /* 卡片略宽 */
  background: #ffffff;
  border-radius: 12px; /* 更柔和的圆角 */
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(180, 220, 210, 0.3); /* 更明显、有活力的阴影 */
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative; /* 用于徽章/覆盖层定位 */
}

.travel-note-card:hover {
  transform: translateY(-8px); /* 更明显的上浮效果 */
  box-shadow: 0 12px 25px rgba(160, 200, 180, 0.4); /* 悬停时阴影增强 */
}

.image-wrapper {
  width: 100%;
  height: 180px; /* 图片略高 */
  overflow: hidden;
  position: relative;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease; /* 悬停时平滑缩放 */
}

.travel-note-card:hover .cover {
  transform: scale(1.08); /* 图片在悬停时轻微放大 */
}

.category-badge {
  position: absolute;
  top: 15px;
  left: 0; /* 从左侧定位 */
  background-color: #61b15c; /* 鲜艳的绿色 */
  color: white;
  padding: 6px 12px;
  border-radius: 0 8px 8px 0; /* 仅右侧圆角 */
  font-size: 0.8em;
  font-weight: 700;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.stats-overlay {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.5); /* 半透明深色背景 */
  color: white;
  padding: 5px 10px;
  border-radius: 5px;
  font-size: 0.85em;
  display: flex;
  gap: 10px;
  align-items: center;
  backdrop-filter: blur(3px); /* 磨砂玻璃效果 */
  -webkit-backdrop-filter: blur(3px);
  z-index: 2;
}

.stats-overlay i {
  margin-right: 3px;
}

.note-content {
  padding: 16px 20px; /* 内容区域增加内边距 */
  display: flex;
  flex-direction: column;
  gap: 10px; /* 间距略微增加 */
  flex-grow: 1; /* 使内容区域填充剩余空间 */
}

.title {
  font-size: 1.25rem; /* 标题字号更大 */
  color: #3f685c; /* 更深沉的绿/蓝绿色 */
  font-weight: 700; /* 更粗的字重 */
  line-height: 1.3;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 标题限制为2行 */
  -webkit-box-orient: vertical;
}

.description {
  font-size: 0.95rem; /* 描述文字略大 */
  color: #666;
  line-height: 1.5;
  flex-grow: 1; /* 允许描述占据可用空间 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3; /* 描述限制为3行 */
  -webkit-box-orient: vertical;
}

.author-info {
  display: flex;
  align-items: center;
  margin-top: 10px; /* 与描述的间距 */
  gap: 10px;
  font-size: 0.85rem;
  color: #888;
}

.avatar {
  width: 32px; /* 头像略大 */
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #a3e0b2; /* 头像的柔和边框 */
}

.author-name {
  font-weight: 600;
  color: #555;
}

.publish-date {
  margin-left: auto; /* 将日期推到最右侧 */
  font-size: 0.8em;
}

.read-more-button {
  width: 100%;
  margin-top: 15px; /* 与作者信息的间距 */
  background-color: #4caf91; 
  border-color: #4caf91;
  font-weight: 600;
  transition: all 0.3s ease;
}

/* 悬停 */
.read-more-button:hover {
  background-color: #35daa8;
  border-color: #35daa8;
  transform: translateY(-2px);
}
</style>