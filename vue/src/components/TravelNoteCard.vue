<!-- 旅行广场的游记卡片 -->
 <template>
  <el-card class="note-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <img :src="note.coverImage || defaultNoteCover" alt="游记封面" class="card-cover-image">
      </div>
    </template>
    <div class="card-body">
      <h3 class="card-title">{{ note.title }}</h3>
      <div class="card-author">
        <el-avatar :src="note.authorAvatar || defaultAvatar" :size="24" class="author-avatar"></el-avatar>
        <span class="author-name">{{ note.authorName }}</span>
        <span class="publish-date">{{ note.publishDate }}</span>
      </div>
      <p class="card-description">{{ note.summary }}</p>
      <div class="card-footer">
        <span class="meta-item">
          <el-icon><View /></el-icon> {{ note.views || 0 }}
        </span>
        <span class="meta-item">
          <el-icon><ChatDotRound /></el-icon> {{ note.comments || 0 }}
        </span>
        <span class="meta-item">
          <el-icon><Star /></el-icon> {{ note.likes || 0 }}
        </span>
        <el-button type="primary" link @click="viewNoteDetail(note.id)">阅读全文</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { defineProps } from 'vue';
import { ElCard, ElAvatar, ElButton, ElIcon } from 'element-plus';
import { View, ChatDotRound, Star } from '@element-plus/icons-vue';

const props = defineProps({
  note: {
    type: Object,
    required: true,
  },
});

const defaultNoteCover = '/assets/images/default_note_cover.jpg';
const defaultAvatar = '/assets/images/default_avatar.png';

const viewNoteDetail = (id) => {
  // 导航到游记详情页
  console.log('查看游记详情:', id);
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