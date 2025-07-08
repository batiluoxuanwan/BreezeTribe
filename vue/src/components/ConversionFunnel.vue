<template>
  <el-card shadow="hover" class="funnel-card">
    <template #header>
      <div class="card-header">
        <span>📈旅游团转化漏斗分析</span>
      </div>
    </template>

    <el-form :inline="true" @submit.prevent>
      <el-form-item label="选择时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :unlink-panels="true"
        />
      </el-form-item>

      <el-form-item label="选择旅行团">
        <el-select
          v-model="selectedPackage"
          filterable
          remote
          clearable
          :remote-method="searchPackages"
          placeholder="请输入关键词搜索"
          :loading="searching"
          value-key="id"  >
          <el-option
            v-for="pkg in searchResults"
            :key="pkg.id"
            :label="pkg.title"
            :value="pkg"
          >
            <div style="display: flex; align-items: center;">
              <img :src="pkg.coverImageUrl" alt="" style="width: 30px; height: 30px; object-fit: cover; margin-right: 8px;" />
              <span>{{ pkg.title }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="fetchFunnelData">查询</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <div v-loading="loading" style="margin-top: 20px;">
      <v-chart v-if="funnelData.length" :option="chartOption" autoresize style="height: 400px;" />
      <div v-else class="empty">暂无数据</div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue';
import { authAxios,publicAxios } from '@/utils/request';
import { ElMessage } from 'element-plus';
import 'echarts/lib/chart/funnel';
import VChart from 'vue-echarts';
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";

use([CanvasRenderer]);

const dateRange = ref([]);
const loading = ref(false);
const funnelData = ref([]);

// 图表配置
const chartOption = computed(() => ({
  title: { text: '旅游团转化漏斗图', left: 'center' },
  tooltip: { trigger: 'item', formatter: '{b} : {c}' },
  series: [
    {
      name: '转化漏斗',
      type: 'funnel',
      left: '10%',
      width: '80%',
      label: { show: true, position: 'inside' },
      data: funnelData.value,
    },
  ],
}));

const selectedPackage = ref(null);
const searchResults = ref([]);
const searching = ref(false);

// 搜索旅行团
const searchPackages = async (query) => {
  if (!query) return;
  searching.value = true;
  try {
    const res = await publicAxios.get('/public/travel-packages/search', {
      params: {
        keyword: query,
        page: 1,
        size: 10,
        sortBy: 'createdTime',
        sortDirection: 'DESC',
      },
    });
    if (res.data.code === 200) {
      // console.log('正在搜索关键词：', query);
      // console.log('返回结果：', res.data);
      searchResults.value = res.data.data.content || [];
    }
  } catch (e) {
    ElMessage.error('搜索旅行团失败');
  } finally {
    searching.value = false;
  }
};

// 获取接口数据
const fetchFunnelData = async () => {
  if (!selectedPackage.value || dateRange.value.length !== 2) {
    ElMessage.warning('请先选择旅行团和时间范围');
    return;
  }

  loading.value = true;
  try {
    const res = await authAxios.get('/admin/data/conversion-funnel', {
      params: {
        travelPackageId: selectedPackage.value.id,
        startDate: dateRange.value[0],
        endDate: dateRange.value[1],
      },
    });

    if (res.data.code === 200) {
      const raw = res.data.data || {};
      funnelData.value = [
        { name: '浏览量', value: raw.viewCount || 0 },
        { name: '收藏量', value: raw.favoriteCount || 0 },
        { name: '订单量', value: raw.orderCount || 0 },
      ];
    } else {
      ElMessage.error(res.data.message || '获取数据失败');
    }
  } catch (e) {
    ElMessage.error('请求失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

</script>

<style scoped>
.funnel-card {
  width: 100%;
  border-radius: 12px;
}

.card-header {
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}

.el-form-item {
  margin-right: 20px;
  margin-bottom: 0;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
  font-size: 1.1em;
}
</style>

