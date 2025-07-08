<template>
  <div class="smart-tour-advisor">
    <el-card class="box-card advisor-card">
      <template #header>
        <div class="card-header">
          <span>智能旅行团顾问</span>
        </div>
      </template>
      <div class="card-content">
        <p class="description">
          您想去哪里旅行？有什么偏好？尽管提问，我们的智能顾问将为您推荐最合适的旅行团！
        </p>

        <el-input
          v-model="userQuestion"
          :rows="3"
          type="textarea"
          placeholder="例如：有没有适合家庭亲子游的东南亚海岛团？或者推荐一个文化探索之旅。"
          class="question-input"
          resize="none"
          @keyup.enter="handleAskQuestion"
        ></el-input>

        <el-button
          type="primary"
          @click="handleAskQuestion"
          :loading="loading"
          :disabled="loading || !userQuestion.trim()"
          class="ask-button"
        >
          <el-icon><MagicStick /></el-icon>
          {{ loading ? '正在思考...' : '获取智能推荐' }}
        </el-button>

        <el-divider v-if="recommendations.length > 0 || loading">推荐结果</el-divider>

        <div v-if="loading" class="loading-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在为您寻找最佳推荐...</span>
        </div>

        <div v-else-if="recommendations.length > 0" class="recommendations-list">
          <el-collapse v-model="activeRecommendation" accordion>
            <el-collapse-item
              v-for="(rec, index) in recommendations"
              :key="rec.packageSummaryDto.id"
              :name="index"
            >
              <template #title>
                <div class="recommendation-header">
                  <h4 class="tour-title">{{ rec.packageSummaryDto.title }}</h4>
                  <el-tag type="info" size="small" class="ml-2">¥ {{ rec.packageSummaryDto.price }}</el-tag>
                  <el-tag type="info" size="small" class="ml-2">{{ rec.packageSummaryDto.durationInDays }} 天</el-tag>
                </div>
              </template>
              <div class="recommendation-detail">
                <el-row :gutter="20">
                  <el-col :span="8">
                    <img :src="rec.packageSummaryDto.coverImageUrl || 'placeholder.jpg'" alt="旅行团封面" class="cover-image">
                  </el-col>
                  <el-col :span="16">
                    <p class="tour-description">{{ rec.packageSummaryDto.description }}</p>
                    <div class="tour-meta">
                      <span>评分: <el-rate v-model="rec.packageSummaryDto.averageRating" disabled size="small" text-color="#ff9900" /></span>
                      <span>评论: {{ rec.packageSummaryDto.commentCount }}</span>
                      <span>收藏: {{ rec.packageSummaryDto.favouriteCount }}</span>
                    </div>
                    <el-tag class="mt-2">{{ getTourStatusText(rec.packageSummaryDto.status) }}</el-tag>
                    <p class="merchant-info">由 “{{ rec.packageSummaryDto.merchant.username }}” 提供</p>
                  </el-col>
                </el-row>
                <p class="recommendation-reason">
                  推荐理由:  {{ rec.reason }}
                </p>
                <el-button type="text" @click="viewTourDetail(rec.packageSummaryDto.id)" class="view-detail-button">
                  查看旅行团详情 <el-icon><Right /></el-icon>
                </el-button>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <el-empty v-else-if="!loading && hasSearched && recommendations.length === 0" description="抱歉，未能找到符合您要求的旅行团推荐。请尝试更换问题。"></el-empty>
        <el-empty v-else-if="!hasSearched" description="输入您的问题，获取智能推荐！"></el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { MagicStick, Loading, Right } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';
import { useRouter } from 'vue-router'; 

const userQuestion = ref('');
const recommendations = ref([]);
const loading = ref(false);
const hasSearched = ref(false); // 标记是否已经进行过搜索
const activeRecommendation = ref(0); // 用于控制 el-collapse 默认展开第一个

const router = useRouter();

const handleAskQuestion = async () => {
  if (!userQuestion.value.trim()) {
    ElMessage.warning('请输入您的问题！');
    return;
  }

  loading.value = true;
  hasSearched.value = true;
  recommendations.value = []; // 清空之前的推荐
  activeRecommendation.value = null; // 关闭所有折叠项

  try {
    const response = await authAxios.post('/chat/retrieve-ids', userQuestion.value.trim(), {
      headers: {
        'Content-Type': 'application/json' 
      }
    });

    if (response.data.code === 200 && response.data.data) {
      console.log('AI推荐旅行团',response.data)
      recommendations.value = response.data.data;
      if (recommendations.value.length > 0) {
        ElMessage.success(`为您找到 ${recommendations.value.length} 个推荐旅行团！`);
        activeRecommendation.value = 0; 
      } else {
        ElMessage.info('未能找到相关的旅行团推荐，请尝试更具体的问题。');
      }
    } else {
      ElMessage.error(`获取推荐失败: ${response.data.message || '未知错误'}`);
    }
  } catch (error) {
    console.error('调用智能问答接口失败:', error);
    ElMessage.error('获取推荐失败，请检查网络或稍后再试。');
  } finally {
    loading.value = false;
  }
};

// --- 辅助函数：根据状态获取文本 ---
const getTourStatusText = (status) => {
  switch (status) {
    case 'PENDING_APPROVAL': return '待审核';
    case 'PUBLISHED': return '已发布';
    case 'OFFLINE': return '已下架';
    case 'REJECTED': return '已驳回';
    default: return '未知状态';
  }
};

const viewTourDetail = (id) => {
  // 使用 router.resolve 来生成完整的 URL
  const routeData = router.resolve({
    name: 'TravelGroupDetail',
    params: { id: id },
    query: {
      from: 'smartAdvisor'
    }
  });
  // 使用 window.open 在新标签页打开解析后的 URL
  window.open(routeData.href, '_blank');
};
</script>

<style scoped>
.smart-tour-advisor {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.advisor-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.description {
  color: #606266;
  margin-bottom: 20px;
  line-height: 1.6;
}

.question-input {
  margin-bottom: 20px;
}

.ask-button {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  margin-bottom: 20px;
}

.ask-button .el-icon {
  margin-right: 8px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #909399;
  font-size: 16px;
}

.loading-indicator .el-icon {
  margin-right: 10px;
  font-size: 24px;
}

.recommendations-list {
  margin-top: 20px;
}

.recommendation-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  color: #303133;
}

.tour-title {
  margin: 0;
  font-size: 18px;
  color: #60aa9a;
  flex-grow: 1; /* 标题占据剩余空间 */
}

.ml-2 {
  margin-left: 8px;
}

.recommendation-detail {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.cover-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 10px;
}

.tour-description {
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
  font-size: 14px;
}

.tour-meta span {
  margin-right: 15px;
  color: #909399;
  font-size: 13px;
}

.merchant-info {
  margin-top: 10px;
  font-size: 14px;
  color: #303133;
}

.recommendation-reason {
  margin-top: 15px;
  padding: 10px;
  background-color: #e6fff9; 
  border-left: 4px solid #5aaaa3; 
  border-radius: 4px;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.view-detail-button {
  margin-top: 15px;
  float: right; 
}

.el-divider {
  margin-top: 30px;
  margin-bottom: 30px;
}

/* 覆盖 Element Plus 样式，确保折叠面板箭头显示 */
:deep(.el-collapse-item__header) {
  padding-left: 15px; /* 给折叠面板标题留出一些内边距 */
  font-weight: bold;
}

:deep(.el-collapse-item__content) {
  padding: 0 15px 15px 15px; /* 内容内边距 */
}
</style>