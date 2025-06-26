<template>
  <div class="travel-group-detail-container">
    <el-button type="primary" class="back-button" @click="goBack"><el-icon><ArrowLeftBold /></el-icon>返回</el-button>
    <div v-if="loading" class="loading-detail">
      <el-spinner size="large"></el-spinner>
      <p>正在加载旅行团详情...</p>
    </div>
    <div v-else-if="travelGroupDetail">
      <el-card class="detail-card">
        <template #header>
          <div class="card-header">
            <h2 class="group-title">{{ travelGroupDetail.title }}</h2>
            <el-tag type="success" v-if="travelGroupDetail.isHot">热门</el-tag>
            <el-tag type="info" v-if="travelGroupDetail.isNew">新品</el-tag>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-image :src="travelGroupDetail.imageUrl" fit="cover" class="group-image"></el-image>
          </el-col>
          <el-col :span="12">
            <div class="detail-info">
              <p><strong>目的地:</strong> {{ travelGroupDetail.destination }}</p>
              <p><strong>价格:</strong> ¥{{ travelGroupDetail.price }}</p>
              <p><strong>出发日期:</strong> {{ travelGroupDetail.startDate }}</p>
              <p><strong>行程时长:</strong> {{ travelGroupDetail.duration }}天</p>
              <p><strong>当前成员:</strong> {{ travelGroupDetail.members }}人</p>
              <p><strong>用户评分:</strong> <el-rate v-model="travelGroupDetail.rating" disabled show-score text-color="#ff9900" score-template="{value}"></el-rate></p>
              <p><strong>浏览量:</strong> {{ travelGroupDetail.views }}</p>
            </div>
          </el-col>
        </el-row>
        <el-divider></el-divider>
        <h3>行程介绍</h3>
        <p class="description-text">{{ travelGroupDetail.description }}</p>

        <el-divider></el-divider>
        <h4>详细行程安排</h4>
        <div class="itinerary-section">
          <div v-for="(day, index) in travelGroupDetail.itinerary" :key="index" class="itinerary-item">
            <h5>第 {{ day.dayNumber }} 天: {{ day.title }}</h5>
            <p>{{ day.details }}</p>
          </div>
          <p v-if="!travelGroupDetail.itinerary || travelGroupDetail.itinerary.length === 0">暂无详细行程安排。</p>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="未找到该旅行团信息或加载失败"></el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElCard, ElRow, ElCol, ElImage, ElDivider, ElTag, ElRate, ElEmpty, ElButton } from 'element-plus';
import { publicAxios } from '@/utils/request'; // 确保路径正确
import { ArrowLeftBold } from '@element-plus/icons-vue';

const route = useRoute(); // 获取当前路由信息
const router = useRouter(); // 获取路由实例

const travelGroupDetail = ref(null);
const loading = ref(true);

// --- 返回上一页 ---
const goBack = () => {
  router.back(); // 返回上一个历史记录页面
};

// --- 获取特定旅行团的详细数据 ---
const fetchTravelGroupDetail = async (id) => {
  loading.value = true;
  try {
    // 假设你的后端获取单个旅行团详情的接口是 /public/travel-packages/{id}
    // {id} 会被替换为实际的旅行团 ID
    const response = await publicAxios.get(`/public/travel-packages/${id}`);

    if (response.data.code === 200) {
      travelGroupDetail.value = response.data.data; // 后端返回的特定旅行团详情
      // 注意：这里假设后端直接返回一个旅行团对象，而不是一个包含 list 和 total 的分页对象
    } else {
      ElMessage.error(response.data.message || '获取旅行团详情失败！');
      travelGroupDetail.value = null; // 清空数据以显示 Empty 状态
    }
  } catch (error) {
    console.error(`获取旅行团详情 (ID: ${id}) 失败:`, error);
    ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    travelGroupDetail.value = null; // 清空数据以显示 Empty 状态
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  // 从路由参数中获取旅行团 ID
  const groupId = route.params.id; 
  if (groupId) {
    fetchTravelGroupDetail(groupId);
  } else {
    ElMessage.error('未获取到旅行团ID！');
    loading.value = false;
  }
});
</script>

<style scoped>
.travel-group-detail-container {
  padding: 20px 40px;
  max-width: 1000px;
  margin: 20px auto;
  background-color: #f9f9f9;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative; /* 确保返回按钮定位 */
}

.back-button {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
}

.loading-detail {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

.detail-card {
  margin-top: 50px; /* 留出返回按钮的空间 */
  border-radius: 8px;
  box-shadow: none; /* 内部卡片不再需要额外阴影 */
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.group-title {
  font-size: 2.2em;
  color: #303133;
  margin: 0;
}

.group-image {
  width: 100%;
  height: 350px; /* 固定高度，保持图片一致性 */
  border-radius: 8px;
  object-fit: cover;
}

.detail-info p {
  font-size: 1.1em;
  color: #606266;
  margin-bottom: 10px;
  line-height: 1.6;
}
.detail-info strong {
  color: #303133;
}

.description-text {
  font-size: 1.1em;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 20px;
  white-space: pre-wrap; /* 保留文本中的换行符 */
}

.itinerary-section {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 5px;
}

.itinerary-item {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e4e7ed;
}

.itinerary-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.itinerary-item h5 {
  font-size: 1.1em;
  color: #409eff;
  margin-bottom: 5px;
}

.itinerary-item p {
  font-size: 0.95em;
  color: #606266;
  line-height: 1.5;
}
</style>