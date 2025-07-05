<!-- 旅行广场的游记卡片 -->
 <template>
  <el-card class="note-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <img :src="note.coverImageUrl || defaultNoteCover" alt="游记封面" class="card-cover-image">
      </div>
    </template>
    <div class="card-body">
      <h3 class="card-title">{{ note.title }}</h3>
      <div class="card-author">
        <el-avatar :src="note.author.avatarUrl || defaultAvatar" :size="24" class="author-avatar"></el-avatar>
        <span class="author-name">{{ note.author.username }}</span>
        <span class="publish-date">{{ formatTime(note.createdTime) }}</span>
      </div>
      <div class="card-footer">
        <span class="meta-item">
          <el-icon><View /></el-icon> {{ note.viewCount || 0 }}
        </span>
        <span class="meta-item">
          <el-icon><ChatDotRound /></el-icon> {{ note.commentCount || 0 }}
        </span>
        <span class="meta-item">
          <el-icon><Pointer /></el-icon> {{ note.likeCount || 0 }}
        </span>
        <el-button type="primary" link @click.stop="goToDetail">阅读全文</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { defineProps } from 'vue';
import { ElCard, ElAvatar, ElButton, ElIcon } from 'element-plus';
import { View, ChatDotRound, Pointer } from '@element-plus/icons-vue';
import {useRouter} from 'vue-router';
const router = useRouter();

const props = defineProps({
  note: {
    type: Object,
    required: true,
  },
});

const defaultNoteCover = '/assets/images/default_note_cover.jpg';
const defaultAvatar = '/assets/images/default_avatar.png';

/**
 * 格式化时间戳为更友好的日期时间字符串
 * @param {string} timeString - ISO 格式的时间字符串
 * @returns {string} 格式化后的时间字符串
 */
const formatTime = (timeString) => {
  if (!timeString) return '';
  const date = new Date(timeString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 获取互动状态
const fetchInteractionStatus = async (itemId) => {
    if (note.value) { 
        note.value.isLiked = false;
        note.value.isFavorited = false;
    }

    if (!authStore.isLoggedIn) { 
        console.log('用户未登录，跳过获取互动状态。');
        return;
    }

    if (!itemId) {
        console.warn('缺少项目ID，无法获取互动状态。');
        return;
    }
    const itemType = 'POST'; 
    const statusMapKey = `${itemType}_${itemId}`; 
    try {
        const response = await authAxios.post('/user/interactions/status', {
            items: [
                {
                    id: itemId,
                    type: 'POST' 
                }
            ]
        });

        if (response.data.code === 200 && response.data.data.statusMap) {
            const status = response.data.data.statusMap[statusMapKey];
            if (status) {
                note.value.isLiked = status.liked;
                note.value.isFavorited = status.favorited; 
            }
            console.log('status ',response.data.data)
        } else {
            console.warn('获取互动状态失败:', response.data.message);
        }
    } catch (error) {
        console.error('获取互动状态请求失败:', error);
        if (error.response && error.response.status === 401) {
            ElMessage.warning('您未登录，无法获取点赞和收藏状态。');
        } else {
           ElMessage.error('获取点赞/收藏状态网络错误！');
        }
    }
};

// 跳转详情页并传递游记id
const goToDetail = () => {
  console.log('查看游记详情:', props.note.id);
  router.push({ name: 'TravelNoteDetail', params: { id: props.note.id } });
};
</script>

<style scoped>
.note-card {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}
.card-header {
  height: 180px;
  overflow: hidden;
}
.card-cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.note-card:hover .card-cover-image {
  transform: scale(1.05);
}
.card-body {
  padding: 15px;
}
.card-title {
  font-size: 1.2em;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-author {
  display: flex;
  align-items: center;
  font-size: 0.85em;
  color: #909399;
  margin-bottom: 10px;
}
.author-avatar {
  margin-right: 8px;
}
.author-name {
  margin-right: 10px;
  color: #606266;
}
.publish-date {
  flex-grow: 1;
  text-align: right;
}
.card-description {
  font-size: 0.9em;
  color: #666;
  line-height: 1.5;
  height: 45px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 15px;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}
.meta-item {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 0.85em;
  margin-right: 15px;
}
.meta-item .el-icon {
  margin-right: 4px;
}
</style>