<template>
  <div class="merchant-reviews-component">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>我的旅行团评价概览</h3>
        </div>
      </template>

      <el-table
        :data="travelPackages.content"
        v-loading="travelPackages.loading"
        style="width: 100%"
        empty-text="您还没有创建任何旅行团或暂无评价数据。"
        class="admin-table"
      >
        <el-table-column prop="title" label="旅行团名称" min-width="180"></el-table-column>
        <el-table-column label="平均评分" width="120">
          <template #default="{ row }">
            <el-rate
              v-model="row.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </template>
        </el-table-column>
        <el-table-column label="评论数量" width="100">
          <template #default="{ row }">
            {{ row.commentCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <template v-if="row.status === 'REJECTED'">
                <el-tag type="info" size="small">{{ getTourStatusText(row.status) }}</el-tag>
            </template>
            <template v-else>
                <el-button link type="primary" size="small" @click="viewPackageComments(row)">
                    查看评论
                </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="travelPackages.totalElements > 0"
        background
        layout="prev, pager, next"
        :total="travelPackages.totalElements"
        :page-size="travelPackages.pageSize"
        v-model:current-page="travelPackages.pageNumber"
        @current-change="fetchTravelPackages"
        class="pagination-bottom"
      />
    </el-card>

    <el-dialog
      v-model="commentDetailDialogVisible"
      :title="`“${selectedPackageTitle}” 的评论`"
      width="800px"
      top="5vh"
    >
      <div class="comments-list-container" v-loading="comments.loading">
        <el-empty v-if="comments.content.length === 0 && !comments.loading" description="该旅行团暂无评论"></el-empty>
        <ul v-else class="comment-list">
          <li v-for="comment in comments.content" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <el-avatar :src="comment.author.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2646a77b777c0dcimg.jpeg'"></el-avatar>
              <div class="comment-info">
                <span class="comment-author">{{ comment.author.username }}</span>
                <el-rate v-model="comment.rating" disabled size="small"></el-rate>
              </div>
              <span class="comment-time">{{ formatTime(comment.createdTime) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            <div v-if="comment.totalReplies > 0" class="comment-replies">
                <el-tag size="small" type="info">共 {{ comment.totalReplies }} 条回复</el-tag>
                <div v-if="comment.repliesPreview && comment.repliesPreview.length" class="reply-preview">
                    <p v-for="(reply, index) in comment.repliesPreview" :key="index" class="reply-item">{{ reply }}</p>
                </div>
            </div>
          </li>
        </ul>
        <el-pagination
          v-if="comments.totalElements > 0"
          background
          layout="prev, pager, next"
          :total="comments.totalElements"
          :page-size="comments.pageSize"
          v-model:current-page="comments.pageNumber"
          @current-change="fetchCommentsForPackage"
          class="pagination-bottom"
        />
      </div>
      <template #footer>
        <el-button @click="commentDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { authAxios } from '@/utils/request'; // 假设你的认证请求工具

// --- 旅行团列表状态 ---
const travelPackages = reactive({
  content: [],
  totalElements: 0,
  pageNumber: 1,
  pageSize: 10,
  loading: false,
});

// --- 评论详情弹窗状态 ---
const commentDetailDialogVisible = ref(false);
const selectedPackageId = ref(null);
const selectedPackageTitle = ref('');

// --- 某个旅行团的评论列表状态 ---
const comments = reactive({
  content: [],
  totalElements: 0,
  pageNumber: 1,
  pageSize: 5, // 评论列表每页显示数量可以少一点
  loading: false,
});

onMounted(() => {
  fetchTravelPackages(); // 组件挂载时获取旅行团列表
});

// 获取团长创建的旅行团列表
const fetchTravelPackages = async () => {
  travelPackages.loading = true;
  try {
    const response = await authAxios.get('/dealer/travel-packages', {
      params: {
        page: travelPackages.pageNumber,
        size: travelPackages.pageSize,
        sortBy: 'createdTime', 
        sortDirection: 'DESC', 
      },
    });
    if (response.data.code === 200 && response.data.data) {
        console.log('拿到的旅行团数据',response.data)
      travelPackages.content = response.data.data.content.map(pkg => ({
        ...pkg,
        averageRating: pkg.averageRating || 0, // 确保有默认值
        commentCount: pkg.commentCount !== undefined ? pkg.commentCount : 0 
      }));
      travelPackages.totalElements = response.data.data.totalElements;
    } else {
      ElMessage.error(response.data.message || '获取旅行团列表失败');
    }
  } catch (error) {
    console.error('获取旅行团列表时发生错误:', error);
    ElMessage.error('加载旅行团列表失败。');
  } finally {
    travelPackages.loading = false;
  }
};

// 查看某个旅行团的评论
const viewPackageComments = (packageRow) => {
  selectedPackageId.value = packageRow.id;
  selectedPackageTitle.value = packageRow.title;
  comments.pageNumber = 1; 
  commentDetailDialogVisible.value = true;
  fetchCommentsForPackage(); 
};

const getTourStatusText = (status) => {
  console.log('status',status);
  switch (status) {
    case 'PENDING_APPROVAL': return '待审核';
    case 'PUBLISHED': return '已发布';
    case 'OFFLINE': return '已下架';
    case 'REJECTED': return '已驳回';
    default: return '未知';
  }
};

// 获取指定旅行团的评论列表
const fetchCommentsForPackage = async () => {
  if (!selectedPackageId.value) {
    return; 
  }
  comments.loading = true;
  try {
    const response = await authAxios.get(`/public/packages/comments/${selectedPackageId.value}`, {
      params: {
        page: comments.pageNumber,
        size: comments.pageSize,
        sortBy: 'createdTime', // 示例排序
        sortDirection: 'DESC', // 示例排序
      },
    });
    if (response.data.code === 200 && response.data.data) {
      comments.content = response.data.data.content;
      comments.totalElements = response.data.data.totalElements;
    } else {
      ElMessage.error(response.data.message || '获取评论列表失败');
      comments.content = [];
      comments.totalElements = 0;
    }
  } catch (error) {
    console.error('获取评论列表时发生错误:', error);
    ElMessage.error('加载评论列表失败。');
    comments.content = [];
    comments.totalElements = 0;
  } finally {
    comments.loading = false;
  }
};

// 辅助函数：格式化时间戳
const formatTime = (timeString) => {
  if (!timeString) return '';
  const date = new Date(timeString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};
</script>

<style scoped>
.merchant-reviews-component {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-bottom {
  margin-top: 20px;
  justify-content: flex-end;
  display: flex;
}

/* 评论详情弹窗内的样式 */
.comments-list-container {
  min-height: 200px; /* 确保加载时有高度 */
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  border-bottom: 1px solid #ebeef5;
  padding: 15px 0;
}
.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.comment-header .el-avatar {
  margin-right: 10px;
}

.comment-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.comment-author {
  font-weight: bold;
  color: #303133;
  margin-bottom: 3px;
}

.comment-time {
  font-size: 0.85em;
  color: #909399;
  margin-left: auto;
}

.comment-content {
  color: #606266;
  line-height: 1.5;
  margin-top: 0;
  margin-bottom: 10px;
}

.comment-replies {
    margin-top: 10px;
    padding-left: 15px;
    border-left: 3px solid #eee;
}

.reply-preview {
    margin-top: 5px;
    background-color: #f8f8f8;
    padding: 8px 12px;
    border-radius: 4px;
}

.reply-preview .reply-item {
    font-size: 0.9em;
    color: #6a6a6a;
    margin-bottom: 5px;
}

.reply-preview .reply-item:last-child {
    margin-bottom: 0;
}
</style>