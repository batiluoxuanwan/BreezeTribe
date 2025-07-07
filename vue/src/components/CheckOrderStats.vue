<template>
  <el-card shadow="hover" class="stats-card">
    <template #header>
      <div class="card-header">📊参与人数与收入流水统计</div>
    </template>

    <el-form :inline="true" class="filter-form">
      <el-form-item label="选择日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="fetchStats"
          :clearable="false"
        />
      </el-form-item>
    </el-form>

    <el-divider />

    <div v-loading="loading" class="stats-content">
      <div v-if="!hasData" class="empty">
        <el-empty description="暂无数据" />
      </div>
      <div v-else class="stats-display">
        <div class="stat-item">
          <div class="stat-label">参与人数</div>
          <div class="stat-value">{{ stats.participantCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">收入流水</div>
          <div class="stat-value">¥ {{ formatMoney(stats.revenue) }}</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { authAxios } from '@/utils/request';

const dateRange = ref([]);

const stats = reactive({
  participantCount: 0,
  revenue: 0,
});
const hasData = ref(false);
const loading = ref(false);

async function fetchStats() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    hasData.value = false;
    return;
  }

  loading.value = true;
  try {
    const params = {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
    };

    const res = await authAxios.get('/admin/data/checkorder', { params });
    if (res.data.code === 200 && res.data.data) {
      stats.participantCount = res.data.data.participantCount ?? 0;
      stats.revenue = res.data.data.revenue ?? 0;
      hasData.value = stats.participantCount > 0 || stats.revenue > 0;
    } else {
      hasData.value = false;
      ElMessage.info(res.data.message || '暂无数据');
    }
  } catch (err) {
    hasData.value = false;
    ElMessage.error('请求数据失败');
  } finally {
    loading.value = false;
  }
}

const formatMoney = (num) => {
  num = Number(num) || 0;
  return num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};
</script>

<style scoped>
.stats-card {
  width: 100%;
  border-radius: 12px;
}
.card-header {
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}
.filter-form {
  margin-bottom: 20px;
}
.el-form-item {
  margin-right: 20px;
  margin-bottom: 0;
}
.stats-content {
  min-height: 150px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
.stats-display {
  display: flex;
  justify-content: center;
  gap: 40px;
  flex-wrap: wrap;
}
.stat-item {
  background: #e8f5e9;
  padding: 20px 30px;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgb(0 0 0 / 0.08);
  text-align: center;
  min-width: 150px;
  transition: all 0.3s ease;
}
.stat-item:hover {
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.12);
  transform: translateY(-5px);
}
.stat-label {
  font-weight: 600;
  color: #666;
  margin-bottom: 10px;
  font-size: 1.1em;
}
.stat-value {
  font-weight: 700;
  font-size: 2.2em;
  color: #28a745;
}
</style>
