<template>
  <div class="admin-dashboard">

    <div class="grid-row">
        <!--用户增长趋势-->
        <el-card shadow="hover" class="chart-card">
            <template #header>
            <div class="header-controls">
                <span>📈 用户增长趋势</span>
                <div class="filters">
                    <div style="display: flex; gap: 10px; margin-bottom: 10px;">
                        <el-select v-model="selectedPeriodUser" placeholder="选择周期" size="small" style="width: 120px" @change="fetchData">
                            <el-option label="日" value="day" />
                            <el-option label="周" value="week" />
                            <el-option label="月" value="month" />
                        </el-select>
                        <el-select v-model="selectedRole" placeholder="选择用户类型" size="small" style="width: 150px" @change="fetchData">
                            <el-option label="普通用户" value="ROLE_USER" />
                            <el-option label="商家" value="ROLE_MERCHANT" />
                            <el-option label="管理员" value="ROLE_ADMIN" />
                        </el-select>
                    </div>
                    <el-date-picker
                        v-model="userDateRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        size="small"
                        @change="fetchData"
                        value-format="YYYY-MM-DD"
                        style="width: 240px;"
                        clearable
                    />
                </div>
            </div>
            </template>

            <v-chart :option="userChartOption" autoresize v-if="!userLoading" style="height: 360px;" />
            <div v-else class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
            </div>
        </el-card>
        <!--旅行团增长趋势-->
        <el-card shadow="hover" class="chart-card">
            <template #header>
            <div class="header-controls">
                <span>✈️ 旅行团增长趋势</span>
                <div class="filters">
                <div style="display: flex; gap: 10px; margin-bottom: 10px;">
                <el-select v-model="selectedPeriodTrip" placeholder="选择周期" size="small" style="width: 120px" @change="fetchTripData">
                    <el-option label="日" value="day" />
                    <el-option label="周" value="week" />
                    <el-option label="月" value="month" />
                </el-select>
                </div>
                <el-date-picker
                    v-model="tripDateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    size="small"
                    @change="fetchTripData"
                    value-format="YYYY-MM-DD"
                    style="width: 240px;"
                    clearable
                />
                </div>
            </div>
            </template>

            <v-chart :option="tripChartOption" autoresize v-if="!tripLoading" style="height: 360px;" />
            <div v-else class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
            </div>
        </el-card>
    </div>

    <div>
      <RankList />
    </div>

    <div class="full-row">
    <!--参与人数数量与收入流水统计-->
      <el-card shadow="hover" class="order-stats-card">
        <template #header>
          <div class="card-header">
            <div class="filters">
              <div style="display: flex; gap: 10px; margin-bottom: 10px;">
                <span>📊 订单统计概览</span>
                <el-date-picker
                    v-model="orderStatsDateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    size="small"
                    @change="fetchOrderStatsData"
                    value-format="YYYY-MM-DD"
                    style="width: 240px;"
                    clearable
                />
              </div>
              </div>
          </div>
        </template>

        <div v-if="orderStatsLoading" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
        </div>
        <div v-else class="stats-content">
          <div class="stat-item">
            <span class="stat-label">总参与人数：</span>
            <span class="stat-value">{{ totalParticipants }} <span class="unit">人</span></span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总收入流水：</span>
            <span class="stat-value">¥ {{ totalRevenue.toFixed(2) }} <span class="unit">元</span></span>
          </div>
          <el-empty v-if="!hasOrderStatsData" description="暂无统计数据" :image-size="50"></el-empty>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElCard, ElSelect, ElOption, ElIcon, ElMessage, ElDatePicker } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { use } from 'echarts/core';
import VChart from 'vue-echarts';
import { LineChart } from 'echarts/charts';
import {TitleComponent,TooltipComponent,GridComponent,LegendComponent,} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { authAxios } from '@/utils/request';
import RankList from '@/components/RankList.vue';

use([LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer]);

// 用户增长图表
const selectedPeriodUser = ref('month');
const selectedRole = ref('ROLE_USER');
const userChartOption = ref({});
const userLoading = ref(false); 
const userDateRange = ref([]);
// 旅行团增长图表
const selectedPeriodTrip = ref('month');
const tripChartOption = ref({});
const tripLoading = ref(false); 
const tripDateRange = ref([]);
//参与人数数量与收入流水统计
const totalParticipants = ref(0); // 订单统计：总参与人数
const totalRevenue = ref(0.0);   // 订单统计：总收入流水
const orderStatsPeriod = ref('month'); // 订单统计：选择的周期
const orderStatsMerchantId = ref(''); // 订单统计：商家ID
const orderStatsDateRange = ref([]); // 订单统计：日期范围
const orderStatsLoading = ref(false); // 订单统计：加载状态

//用户增长趋势
const fetchUserData = async () => {
  userLoading.value = true;
  try {
    const params = {
      period: selectedPeriodUser.value, // period 始终发送
      role: selectedRole.value, // 角色参数总是需要
    };

    // 只有当 userDateRange 有值且包含两个日期时才添加 startDate 和 endDate
    if (userDateRange.value && userDateRange.value.length === 2) {
      params.startDate = userDateRange.value[0]; // YYYY-MM-DD 格式
      params.endDate = userDateRange.value[1];   // YYYY-MM-DD 格式
    }
    // 如果 userDateRange 为空，则不发送 startDate 和 endDate，后端将使用默认逻辑

    const res = await authAxios.get('/admin/data/user-growth', { params });
    if (res.data.code === 200) {
        //console.log("后端返回的数据：",res.data.data);
        const backendData = res.data.data; 
        const xData = backendData.xAxis;   
        const yData = backendData.yAxis;

      // 额外的检查：确保数据存在且长度匹配
      if (!xData || !yData || xData.length === 0 || xData.length !== yData.length) {
          ElMessage.warning('用户增长数据不完整或为空。');
          userChartOption.value = {}; // 清空图表配置，显示空白
          return;
      }

      userChartOption.value = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: xData }, 
        yAxis: { type: 'value' },
        series: [{
            name: '用户数',
            type: 'line',
            data: yData, 
            smooth: true,
            areaStyle: {}
        }]
    };}else {
      ElMessage.error(res.data.message || '获取数据失败');
    }
  } catch (err) {
    ElMessage.error('网络错误，无法获取用户增长数据');
    console.error(err);
  } finally {
    userLoading.value = false;
  }
};

// 确保在周期或角色改变时也调用 fetchData
const fetchData = () => {
    fetchUserData();
};

const fetchTripData = async () => {
  tripLoading.value = true;
  try {
    const params = {
      period: selectedPeriodTrip.value,
    };

     if (tripDateRange.value && tripDateRange.value.length === 2) {
      params.startDate = tripDateRange.value[0];
      params.endDate = tripDateRange.value[1];
    }

    const res = await authAxios.get('/admin/data/trip-growth', { params });

    if (res.data.code === 200) {
      const backendData = res.data.data;

      const xData = backendData.xAxis || [];
      const yData = backendData.yAxis || [];

      if (!xData || xData.length === 0 || xData.length !== yData.length) {
        ElMessage.warning('旅行团增长数据不完整或为空。');
        tripChartOption.value = {}; // 清空图表配置，显示空白
        return;
      }

      tripChartOption.value = {
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: xData,
          axisLabel: {
            formatter: function (value) {
              if (selectedPeriodTrip.value === 'day') {
                return value.substring(5); // 截取 MM-DD
              } else if (selectedPeriodTrip.value === 'week') {
                 return value.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'); // 假设周是 YYYYWW 格式，这里需要后端提供更友好的周格式
              }
              return value; // 默认返回原始值（如 YYYY-MM）
            }
          }
        },
        yAxis: { type: 'value', name: '旅行团数量' },
        series: [
          {
            name: '旅行团数',
            type: 'line',
            data: yData,
            smooth: true,
            areaStyle: {}
          }
        ]
      };
    } else {
      ElMessage.error(res.data.message || '获取旅行团增长数据失败');
    }
  } catch (err) {
    ElMessage.error('网络错误，无法获取旅行团增长数据');
    console.error(err);
  } finally {
    tripLoading.value = false;
  }
};

// 计算属性，用于判断订单统计是否有数据
const hasOrderStatsData = computed(() => totalParticipants.value >= 0 || totalRevenue.value >= 0);

// 获取订单统计数据
const fetchOrderStatsData = async () => {
  orderStatsLoading.value = true;
  try {
    const params = {
      period: orderStatsPeriod.value,
    };

    if (orderStatsMerchantId.value) {
      params.merchantId = orderStatsMerchantId.value;
    }

    if (orderStatsDateRange.value && orderStatsDateRange.value.length === 2) {
      params.startDate = orderStatsDateRange.value[0];
      params.endDate = orderStatsDateRange.value[1];
    }

    const res = await authAxios.get('/admin/data/orders-stats', { params });

    if (res.data.code === 200) {
      totalParticipants.value = res.data.data.totalParticipants || 0;
      totalRevenue.value = res.data.data.totalRevenue || 0.0;
    } else {
      ElMessage.error(res.data.message || '获取订单统计失败');
      totalParticipants.value = 0;
      totalRevenue.value = 0.0;
    }
  } catch (error) {
    ElMessage.error('网络错误，无法获取订单统计数据');
    console.error('Error fetching order stats:', error);
    totalParticipants.value = 0;
    totalRevenue.value = 0.0;
  } finally {
    orderStatsLoading.value = false;
  }
};

onMounted(() => {
  fetchUserData();
  fetchTripData();
  fetchOrderStatsData();
});
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background-color: #eefaf83a;
}

.grid-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.full-row {
  margin-top: 32px;
}
/*公用样式*/
.chart-card ,
.order-stats-card{
  width: 100%;
  height: 100%;
  border-radius: 12px;
}
.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}
.filters {
  display: flex;
  flex-direction: column; /* 垂直排列 */
  gap: 10px;
}
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}

/* --- 订单统计概览特有样式 --- */
.order-stats-card .card-header {
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}

.order-stats-card .card-header .filters {
  display: flex; 
  flex-grow: 1;  
  align-items: center; 
  justify-content: space-between; 
}

.order-stats-card .loading-container {
  min-height: 120px; /* 确保订单统计加载时也有一定高度 */
  font-size: 0.9em;
}

.order-stats-card .loading-container .el-icon {
  margin-right: 8px;
  font-size: 1.2em;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 10px 0;
}

.stat-item {
  display: flex;
  align-items: center;
  font-size: 1.2em;
  color: #555;
}

.stat-label {
  font-weight: 500;
  color: #777;
  min-width: 120px;
}

.stat-value {
  font-weight: bold;
  color: #6da0b1;
  font-size: 1.4em;
}

.stat-value .unit {
  font-size: 0.7em;
  font-weight: normal;
  margin-left: 5px;
  color: #999;
}
</style>