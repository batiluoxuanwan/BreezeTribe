<template>
  <el-card shadow="hover" class="check-order-stats-card">
    <template #header>
      <div class="card-header">
        <span>📊 参与人数与收入流水统计</span>
      </div>
    </template>

    <div class="filter-controls">
      <el-form :inline="true" :model="form" class="demo-form-inline">
        <el-form-item label="时间周期">
          <el-select v-model="form.period" placeholder="选择周期" @change="handlePeriodChange">
            <el-option label="今天" value="day" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.period === 'custom'" label="自定义日期">
          <el-date-picker
            v-model="customDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleCustomDateChange"
            :clearable="false"
          />
        </el-form-item>

        <!-- <el-form-item>
          <el-button type="primary" @click="fetchStatsData" :loading="loading">查询</el-button>
        </el-form-item> -->
      </el-form>
    </div>

    <el-divider />

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon> 正在加载统计数据...
    </div>
    <div v-else-if="Object.keys(statsData).length === 0" class="empty-state">
      <el-empty description="当前时间范围内无统计数据" :image-size="100" />
    </div>
    <div v-else class="stats-display">
      <div class="stat-item">
        <div class="stat-label">参与总人数</div>
        <div class="stat-value">{{ statsData.participantCount !== undefined ? statsData.participantCount : 'N/A' }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">总收入流水</div>
        <div class="stat-value">¥ {{ statsData.revenue !== undefined ? formatCurrency(statsData.revenue) : 'N/A' }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { ElCard, ElSelect, ElOption, ElDatePicker, ElButton, ElForm, ElFormItem, ElMessage, ElIcon, ElEmpty, ElDivider } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request'; // 请确保此路径正确
import dayjs from 'dayjs'; // 用于日期计算
import 'dayjs/locale/zh-cn'; // 导入中文语言包
dayjs.locale('zh-cn'); // 设置 dayjs 为中文

// 响应式表单数据
const form = reactive({
  period: 'month', // 默认为 'month'
  startDate: null,
  endDate: null,
});

const customDateRange = ref([]); // 用于 El-date-picker 日期范围绑定
const statsData = ref({}); // 获取到的统计数据
const loading = ref(false); // 数据加载状态

// 在 setup 函数的根部初始化日期范围，确保在组件渲染前数据就已准备好
//calculateDateRange(form.period);

// --- 日期计算函数 ---
const calculateDateRange = (period) => {
  const now = dayjs();
  let start, end;

  switch (period) {
    case 'day':
      start = now.startOf('day');
      end = now.endOf('day');
      break;
    case 'week':
      // dayjs 默认一周开始是周日，如果你希望周一开始，可能需要插件
      // 这里使用默认的周日开始
      start = now.startOf('week');
      end = now.endOf('week');
      break;
    case 'month':
      start = now.startOf('month');
      end = now.endOf('month');
      break;
    case 'year':
      start = now.startOf('year');
      end = now.endOf('year');
      break;
    case 'custom':
      // 由 handleCustomDateChange 处理
      return;
    default:
      start = now.startOf('month'); // 未知周期默认为本月
      end = now.endOf('month');
  }
  form.startDate = start.format('YYYY-MM-DD');
  form.endDate = end.format('YYYY-MM-DD');
};

// --- 筛选条件变化的处理器 ---
const handlePeriodChange = (newPeriod) => {
  if (newPeriod !== 'custom') {
    calculateDateRange(newPeriod);
    customDateRange.value = []; // 如果不是自定义周期，清空自定义日期范围
  } else {
    // 对于 'custom'，日期将由 handleCustomDateChange 设置
    form.startDate = null;
    form.endDate = null;
  }
  fetchStatsData(); // 周期变化后自动获取数据
};

const handleCustomDateChange = (range) => {
  if (range && range.length === 2) {
    form.startDate = range[0];
    form.endDate = range[1];
  } else {
    form.startDate = null;
    form.endDate = null;
  }
  fetchStatsData(); // 自定义日期变化后自动获取数据
};

// --- 获取统计数据 ---
const fetchStatsData = async () => {
  // 确保非自定义周期时，日期范围已设置
  if (form.period !== 'custom' && (!form.startDate || !form.endDate)) {
      calculateDateRange(form.period); // 再次确保日期已设置
  }

  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择有效的时间范围。');
    statsData.value = {}; // 清空之前的数据
    return;
  }

  loading.value = true;
  try {
    const params = {
      period: form.period !== 'custom' ? form.period : undefined, // 仅在非自定义周期时发送 period
      startDate: form.startDate,
      endDate: form.endDate,
    };

    // 移除 params 中值为 undefined 的键
    Object.keys(params).forEach(key => params[key] === undefined && delete params[key]);

    const res = await authAxios.get('/admin/data/checkorder', { params });

    if (res.data.code === 200) {
      // 假设 data 对象直接包含 participantCount 和 revenue
      statsData.value = res.data.data || {};
      if (Object.keys(statsData.value).length === 0) {
        ElMessage.info('当前时间范围内无统计数据。');
      }
    } else {
      ElMessage.error(res.data.message || '获取统计数据失败');
      statsData.value = {};
    }
  } catch (error) {
    ElMessage.error('获取统计数据网络错误');
    console.error('Error fetching check order data:', error);
    statsData.value = {};
  } finally {
    loading.value = false;
  }
};

// --- 格式化货币 ---
const formatCurrency = (value) => {
  if (typeof value !== 'number') return value;
  // 格式化为人民币，保留两位小数
  return value.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};

onMounted(() => {
  calculateDateRange(form.period); 
  fetchStatsData();
});
</script>

<style scoped>
.check-order-stats-card {
  width: 100%;
  border-radius: 12px;
}

.card-header {
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}

.filter-controls {
  margin-bottom: 20px;
}

.el-form-item {
  margin-right: 20px;
  margin-bottom: 0; /* 移除默认底部外边距 */
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  color: #999;
}
.loading-container .el-icon {
  font-size: 2em;
  margin-bottom: 10px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 150px;
}

.stats-display {
  display: flex;
  justify-content: space-around; /* 使统计项均匀分布 */
  align-items: center;
  gap: 30px; /* 统计项之间的间距 */
  padding: 20px;
  flex-wrap: wrap; /* 允许换行 */
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 150px; /* 最小宽度 */
  padding: 15px 20px;
  border-radius: 10px;
  background-color: #e8f5e9; /* 浅绿色背景 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease-in-out;
}

.stat-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.12);
}

.stat-label {
  font-size: 1em;
  color: #666;
  margin-bottom: 8px;
  font-weight: bold;
}

.stat-value {
  font-size: 2.2em; /* 更大的字体 */
  font-weight: bold;
  color: #28a745; /* 绿色，代表成功或积极数据 */
}

/* 针对收入的特殊颜色 */
.stat-item:last-child .stat-value {
    color: #007bff; /* 蓝色，区分收入和人数 */
}
</style>