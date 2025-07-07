<template>
  <div class="admin-dashboard">

    <div class="grid-row">
        <!--用户增长趋势-->
        <el-card shadow="hover" class="chart-card">
            <template #header>
            <div class="header-controls">
                <span>📈 用户增长趋势</span>
                <div class="filters">
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
            </div>
            </template>

            <v-chart :option="chartOption" autoresize v-if="!loading" style="height: 360px;" />
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
                <el-select v-model="selectedPeriodTrip" placeholder="选择周期" size="small" style="width: 120px" @change="fetchTripData">
                    <el-option label="日" value="day" />
                    <el-option label="周" value="week" />
                    <el-option label="月" value="month" />
                </el-select>
                </div>
            </div>
            </template>

            <v-chart :option="chartOption" autoresize v-if="!loading" style="height: 360px;" />
            <div v-else class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
            </div>
        </el-card>
    </div>

    <div class="grid-row">
      <TripRankList />
      <MerchantRankList />
    </div>

    <div class="full-row">
      <OrderStatsChart />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElCard, ElSelect, ElOption, ElIcon, ElMessage } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { use } from 'echarts/core';
import VChart from 'vue-echarts';
import { LineChart } from 'echarts/charts';
import {TitleComponent,TooltipComponent,GridComponent,LegendComponent,} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { authAxios } from '@/utils/request';

use([LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer]);

const selectedPeriodUser = ref('month');
const selectedPeriodTrip = ref('month');
const selectedRole = ref('ROLE_USER');
const chartOption = ref({});
const loading = ref(false);
//用户增长趋势
const fetchUserData = async () => {
  loading.value = true;
  try {
    const res = await authAxios.get('/admin/data/user-growth', {
      params: {
        period: selectedPeriodUser.value,
        role: selectedRole.value,
      }
    });
    if (res.data.code === 200) {
        //console.log("后端返回的数据：",res.data.data);
        const backendData = res.data.data; 
        const xData = backendData.xAxis;   
        const yData = backendData.yAxis;

      // 额外的检查：确保数据存在且长度匹配
      if (!xData || !yData || xData.length === 0 || xData.length !== yData.length) {
          ElMessage.warning('用户增长数据不完整或为空。');
          chartOption.value = {}; // 清空图表配置，显示空白
          return;
      }

      chartOption.value = {
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
    loading.value = false;
  }
};

// 确保在周期或角色改变时也调用 fetchData
const fetchData = () => {
    fetchUserData();
};

const fetchTripData = async () => {
  loading.value = true;
  try {
    const params = {
      period: selectedPeriodTrip.value,
    };

    // 如果启用日期范围选择器，添加 startDate 和 endDate
    // if (dateRange.value && dateRange.value.length === 2) {
    //   params.startDate = dateRange.value[0].toISOString().split('T')[0]; // 格式化为 YYYY-MM-DD
    //   params.endDate = dateRange.value[1].toISOString().split('T')[0];
    // }

    const res = await authAxios.get('/admin/data/trip-growth', { params });

    if (res.data.code === 200) {
      const backendData = res.data.data;

      const xData = backendData.xAxis || [];
      const yData = backendData.yAxis || [];

      if (!xData || xData.length === 0 || xData.length !== yData.length) {
        ElMessage.warning('旅行团增长数据不完整或为空。');
        chartOption.value = {}; // 清空图表配置，显示空白
        return;
      }

      chartOption.value = {
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: xData,
          axisLabel: {
            formatter: function (value) {
              // 根据周期格式化 X 轴标签，例如：
              // 如果是月份，显示 'YYYY-MM'
              // 如果是日期，显示 'MM-DD'
              if (selectedPeriod.value === 'day') {
                return value.substring(5); // 截取 MM-DD
              } else if (selectedPeriod.value === 'week') {
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
    loading.value = false;
  }
};

onMounted(() => {
  fetchUserData();
  fetchTripData();
});
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background-color: #f5f5f5;
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
/*用户增长趋势*/
.chart-card {
  width: 100%;
  height: 100%;
  border-radius: 12px;
}
.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filters {
  display: flex;
  gap: 10px;
}
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}
</style>