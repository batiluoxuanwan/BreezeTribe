<template>
  <el-card shadow="hover" class="conversion-funnel-card">
    <template #header>
      <div class="card-header">
        <span>📈 旅游团转化漏斗分析</span>
      </div>
    </template>

    <div class="filter-controls">
      <el-form :inline="true" :model="form" class="demo-form-inline">
        <el-form-item label="选择旅游团">
          <el-select
            v-model="form.travelPackageId"
            placeholder="请选择旅游团"
            filterable
            remote
            :remote-method="searchTravelPackages"
            :loading="travelPackagesLoading"
            clearable
            @change="fetchFunnelData"
            style="width: 250px;"
          >
            <el-option
              v-for="item in travelPackages"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间周期">
          <el-select v-model="form.period"  @change="handlePeriodChange">
            <el-option label="今天" value="day" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
          </el-select>
        </el-form-item>

        <!-- <el-form-item>
          <el-button type="primary" @click="fetchFunnelData" :loading="loading">查询</el-button>
        </el-form-item> -->
      </el-form>
    </div>

    <el-divider />

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon> 正在加载漏斗数据...
    </div>
    <div v-else-if="!form.travelPackageId" class="empty-state">
      <el-empty description="请选择一个旅游团以查看转化漏斗数据" :image-size="100" />
    </div>
    <div v-else-if="processedFunnelData.length === 0" class="empty-state">
      <el-empty description="当前时间范围内无转化漏斗数据" :image-size="100" />
    </div>
    <div v-else class="funnel-chart-container">
      <div ref="funnelChart" class="echarts-chart"></div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue';
import { ElCard, ElSelect, ElOption, ElDatePicker, ElButton, ElForm, ElFormItem, ElMessage, ElIcon, ElEmpty, ElDivider } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';
import dayjs from 'dayjs';

// 导入 ECharts
import * as echarts from 'echarts';

// 响应式表单数据
const form = reactive({
  travelPackageId: null,
  period: 'month',
  startDate: null,
  endDate: null,
});

const customDateRange = ref([]);
const travelPackages = ref([]);
const travelPackagesLoading = ref(false);
const funnelData = ref({}); // 后端原始数据
const loading = ref(false);

const funnelChart = ref(null); // ECharts 容器的引用
let myChart = null; // ECharts 实例

// 定义漏斗步骤的顺序和对应的中文名称
const funnelStepsMap = {
  'viewCount': '浏览量',
  'favoriteCount': '收藏量',
  'commentCount': '评论量',
  'joinCount': '参与量'
};
// 定义漏斗步骤的顺序
const funnelOrder = ['viewCount', 'favoriteCount', 'commentCount', 'joinCount'];

// --- 旅游团搜索 ---
const searchTravelPackages = async (query) => {
  if (query) {
    travelPackagesLoading.value = true;
    try {
      const res = await authAxios.get('/public/travel-packages/search', {
        params: {
          query: query,
          page: 1,
          size: 10
        }
      });
      if (res.data.code === 200) {
        travelPackages.value = res.data.data.content || [];
      } else {
        ElMessage.error(res.data.message || '搜索旅游团失败');
      }
    } catch (error) {
      ElMessage.error('搜索旅游团网络错误');
      console.error('Error searching travel packages:', error);
    } finally {
      travelPackagesLoading.value = false;
    }
  } else {
    travelPackages.value = [];
  }
};

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
      return;
    default:
      start = now.startOf('month');
      end = now.endOf('month');
  }
  form.startDate = start.format('YYYY-MM-DD');
  form.endDate = end.format('YYYY-MM-DD');
};

// --- 筛选条件变化的处理器 ---
const handlePeriodChange = (newPeriod) => {
  if (newPeriod !== 'custom') {
    calculateDateRange(newPeriod);
    customDateRange.value = [];
  } else {
    form.startDate = null;
    form.endDate = null;
  }
  if (form.travelPackageId) {
    fetchFunnelData();
  }
};

const handleCustomDateChange = (range) => {
  if (range && range.length === 2) {
    form.startDate = range[0];
    form.endDate = range[1];
  } else {
    form.startDate = null;
    form.endDate = null;
  }
  if (form.travelPackageId) {
    fetchFunnelData();
  }
};

// --- 获取漏斗数据 ---
const fetchFunnelData = async () => {
  if (!form.travelPackageId) {
    ElMessage.warning('请先选择一个旅游团。');
    funnelData.value = {};
    return;
  }

  if (form.period !== 'custom' && (!form.startDate || !form.endDate)) {
      calculateDateRange(form.period);
  }

  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择有效的时间范围。');
    funnelData.value = {};
    return;
  }

  loading.value = true;
  try {
    const params = {
      travelPackageId: form.travelPackageId,
      period: form.period !== 'custom' ? form.period : undefined,
      startDate: form.startDate,
      endDate: form.endDate,
    };

    Object.keys(params).forEach(key => params[key] === undefined && delete params[key]);

    const res = await authAxios.get('/admin/data/conversion-funnel', { params });

    if (res.data.code === 200) {
      funnelData.value = res.data.data || {};
      if (Object.keys(funnelData.value).length === 0) {
        ElMessage.info('当前时间范围内无转化漏斗数据。');
      }
      updateChart(); // 数据更新后更新图表
    } else {
      ElMessage.error(res.data.message || '获取转化漏斗数据失败');
      funnelData.value = {};
      updateChart(); // 清空数据后也要更新图表
    }
  } catch (error) {
    ElMessage.error('获取转化漏斗数据网络错误');
    console.error('Error fetching conversion funnel data:', error);
    funnelData.value = {};
    updateChart(); // 错误发生后也要更新图表
  } finally {
    loading.value = false;
  }
};


// --- 处理并排序漏斗数据为 ECharts 格式 ---
const processedFunnelData = computed(() => {
  const data = funnelData.value;
  if (!data || Object.keys(data).length === 0) {
    return [];
  }

  const result = [];
  funnelOrder.forEach(key => {
    if (data.hasOwnProperty(key) && funnelStepsMap[key]) { // 确保键存在且有对应的中文名称
      result.push({
        value: data[key] || 0, // 确保值为数字，没有则默认为0
        name: funnelStepsMap[key]
      });
    }
  });

  result.sort((a, b) => b.value - a.value);
  return result;
});


// --- 初始化和更新 ECharts 图表 ---
const initChart = () => {
  if (funnelChart.value && !myChart) {
    myChart = echarts.init(funnelChart.value);
    window.addEventListener('resize', resizeChart); // 监听窗口大小变化，调整图表
  }
};

const updateChart = () => {
  if (!myChart) {
    initChart(); // 如果chart未初始化，先初始化
    if (!myChart) return; // 如果初始化失败，直接返回
  }

  const chartData = processedFunnelData.value;

  // 如果没有数据，清空图表并显示空状态
  if (chartData.length === 0) {
    myChart.clear();
    return;
  }

  const option = {
    title: {
      text: '旅游团转化漏斗',
      left: 'center',
      top: '20px' // 调整标题位置
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)' // 显示名称、数值和百分比
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: chartData.map(item => item.name) // 漏斗图的图例是步骤名称
    },
    series: [
      {
        name: '转化漏斗',
        type: 'funnel',
        left: '10%',
        top: 60, // 调整漏斗图的顶部位置
        bottom: 60, // 调整漏斗图的底部位置
        width: '80%',
        gap: 2, // 漏斗图节之间的间隔
        minSize: '0%', // 漏斗最小宽度（如果数值为0，显示为0）
        maxSize: '100%', // 漏斗最大宽度

        // 漏斗图的数据排序方式 (可选 'ascending', 'descending' 或不填按数据顺序)
        // 这里的数据是经过 processedFunnelData 排序的，所以按数据顺序即可
        sort: 'descending', // ECharts 漏斗图通常要求数据从大到小排序

        label: {
          show: true,
          position: 'inside', // 标签显示在漏斗内部
          formatter: '{b} {c}', // 显示名称和数值
          color: '#fff', // 内部标签颜色
          fontSize: 14,
          fontWeight: 'bold'
        },
        labelLine: {
          length: 10,
          lineStyle: {
            width: 1,
            type: 'solid'
          }
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1
        },
        emphasis: { // 鼠标悬浮时的样式
          label: {
            fontSize: 20
          }
        },
        data: chartData // 绑定处理后的数据
      }
    ]
  };
  myChart.setOption(option);
};

const resizeChart = () => {
  myChart?.resize();
};

// --- 生命周期钩子 ---
onMounted(() => {
  initChart(); // 初始化 ECharts 实例
  calculateDateRange(form.period);
  fetchFunnelData();
});

// 监听 travelPackageId 的变化并重新获取数据
watch(() => form.travelPackageId, (newVal, oldVal) => {
  if (newVal !== oldVal && newVal) {
    fetchFunnelData();
  } else if (!newVal && oldVal) {
    funnelData.value = {};
    updateChart(); // 如果取消选择套餐，清空数据并更新图表
  }
});

// 在组件卸载时销毁图表实例，防止内存泄漏
import { onBeforeUnmount } from 'vue';
onBeforeUnmount(() => {
  if (myChart) {
    myChart.dispose();
    window.removeEventListener('resize', resizeChart);
    myChart = null;
  }
});

</script>

<style scoped>
.conversion-funnel-card {
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
  margin-bottom: 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px; /* 增加高度以适应图表 */
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
  min-height: 300px; /* 增加高度以适应图表 */
}

.funnel-chart-container {
  width: 100%;
  /* 确保容器有足够的最小高度来显示图表 */
  min-height: 400px; /* 根据需要调整高度 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.echarts-chart {
  width: 100%;
  height: 400px; /* ECharts 图表的高度 */
}

</style>