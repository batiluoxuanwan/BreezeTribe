<template>
  <div class="two-rank-lists-container">
    <el-card shadow="hover" class="rank-card">
      <template #header>
        <div class="card-header">
          <span>🏆 旅行团排行榜</span>
          <div class="filters">
            <el-select v-model="tripSortDirection" placeholder="排序方式" size="small" style="width: 100px;">
              <el-option label="降序" value="DESC" />
              <el-option label="升序" value="ASC" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="tripLoading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
      </div>
      <div v-else class="rank-list-content">
        <ul v-if="tripItems.length > 0" class="items-list">
          <li v-for="(item, index) in tripItems" :key="item.id" class="item-entry">
            <div class="rank-number" :class="getRankClass(index)">{{ (tripPage - 1) * tripPageSize + index + 1 }}</div>
            <img :src="item.coverImageUrl || defaultAvatar" alt="cover" class="item-avatar" />
            <div class="item-info">
              <p class="item-name">{{ item.title }}</p>
              <!-- <p class="item-stats">
                <span class="stat-item">浏览: {{ item.viewCount || 0 }}</span>
                <span class="stat-item">收藏: {{ item.favouriteCount || 0 }}</span>
                <span class="stat-item">评论: {{ item.commentCount || 0 }}</span>
                <span class="stat-item">评分: {{ item.averageRating ? item.averageRating.toFixed(1) : 'N/A' }}</span>
                <span class="stat-item">销量: {{ item.salesCount || 0 }}</span>
              </p> -->
            </div>
          </li>
        </ul>
        <el-empty v-else description="暂无旅行团数据" :image-size="80"></el-empty>

        <div class="pagination-container">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="tripTotalElements"
            :page-size="tripPageSize"
            :current-page="tripPage"
            @current-change="handleTripPageChange"
          />
        </div>
      </div>
    </el-card>

    <el-card shadow="hover" class="rank-card">
      <template #header>
        <div class="card-header">
          <span>🏅 商家评分排行榜</span>
          <div class="filters">
            <el-select v-model="merchantSortDirection" placeholder="排序方式" size="small" style="width: 100px; margin-right: 10px;">
              <el-option label="降序" value="DESC" />
              <el-option label="升序" value="ASC" />
            </el-select>
            <el-select v-model="merchantSortBy" placeholder="排序字段" size="small" style="width: 130px;">
              <el-option label="平均评分" value="averageRating" />
              <el-option label="创建时间" value="createdTime" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="merchantLoading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon> 正在加载中...
      </div>
      <div v-else class="rank-list-content">
        <ul v-if="merchantItems.length > 0" class="items-list">
          <li v-for="(item, index) in merchantItems" :key="item.id" class="item-entry">
            <div class="rank-number" :class="getRankClass(index)">{{ (merchantPage - 1) * merchantPageSize + index + 1 }}</div>
            <img :src="item.avatarUrl || defaultAvatar" alt="avatar" class="item-avatar" />
            <div class="item-info">
              <p class="item-name">{{ item.name }}</p>
              <p class="item-stats">
                <span class="stat-item">平均评分: {{ item.averageRating ? item.averageRating.toFixed(1) : 'N/A' }}</span>
              </p>
            </div>
          </li>
        </ul>
        <el-empty v-else description="暂无商家数据" :image-size="80"></el-empty>

        <div class="pagination-container">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="merchantTotalElements"
            :page-size="merchantPageSize"
            :current-page="merchantPage"
            @current-change="handleMerchantPageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ElCard, ElSelect, ElOption, ElIcon, ElPagination, ElEmpty, ElMessage } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';
import defaultAvatar from '@/assets/NotFoundsonailong.jpg';

// --- 旅行团排行榜状态 ---
const tripItems = ref([]);
const tripLoading = ref(false);
const tripPage = ref(1);
const tripPageSize = ref(5); // 默认每页5条
const tripTotalElements = ref(0);
// 移除 sortBy 的 ref，因为现在固定为综合排序
const tripSortDirection = ref('DESC');

// --- 商家排行榜状态 ---
const merchantItems = ref([]);
const merchantLoading = ref(false);
const merchantPage = ref(1);
const merchantPageSize = ref(5); // 默认每页5条
const merchantTotalElements = ref(0);
const merchantSortBy = ref('averageRating'); // 商家默认按平均评分
const merchantSortDirection = ref('DESC');

// --- 共同逻辑 ---

// 获取排行榜数据通用函数
const fetchRankData = async (type) => {
  let itemsRef, loadingRef, pageRef, sizeRef, currentSortBy, sortDirectionRef, totalElementsRef;
  let apiUrl;
  let params = {};

  if (type === 'trip') {
    itemsRef = tripItems;
    loadingRef = tripLoading;
    pageRef = tripPage;
    sizeRef = tripPageSize;
    // 旅行团固定为 COMPREHENSIVE 排序
    currentSortBy = 'COMPREHENSIVE';
    sortDirectionRef = tripSortDirection;
    totalElementsRef = tripTotalElements;
    apiUrl = '/admin/data/trip-rank';
    
    params = {
      page: pageRef.value,
      size: sizeRef.value,
      sortDirection: sortDirectionRef.value,
    };
    // 明确设置 rankType 为 COMPREHENSIVE，且不传 sortBy
    params.rankType = 'COMPREHENSIVE';

  } else if (type === 'merchant') {
    itemsRef = merchantItems;
    loadingRef = merchantLoading;
    pageRef = merchantPage;
    sizeRef = merchantPageSize;
    currentSortBy = merchantSortBy.value; // 商家仍可选择排序字段
    sortDirectionRef = merchantSortDirection;
    totalElementsRef = merchantTotalElements;
    apiUrl = '/admin/data/merchant-rank';

    params = {
      page: pageRef.value,
      size: sizeRef.value,
      sortBy: currentSortBy,
      sortDirection: sortDirectionRef.value,
    };
  } else {
    console.error("Invalid rank list type:", type);
    return;
  }

  loadingRef.value = true;
  try {
    const res = await authAxios.get(apiUrl, { params });

    if (res.data.code === 200) {
      itemsRef.value = res.data.data.content || [];
      totalElementsRef.value = res.data.data.totalElements || 0;

      if (pageRef.value > res.data.data.totalPages && res.data.data.totalPages > 0) {
        pageRef.value = res.data.data.totalPages;
        if (pageRef.value !== res.data.data.pageNumber + 1) { 
             fetchRankData(type);
        }
      }
    } else {
      ElMessage.error(res.data.message || `获取${type === 'trip' ? '旅行团' : '商家'}排行榜数据失败`);
      itemsRef.value = [];
      totalElementsRef.value = 0;
    }
  } catch (error) {
    ElMessage.error(`网络错误，无法获取${type === 'trip' ? '旅行团' : '商家'}排行榜数据`);
    console.error(`Error fetching ${type} rank data:`, error);
    itemsRef.value = [];
    totalElementsRef.value = 0;
  } finally {
    loadingRef.value = false;
  }
};

// 旅行团分页
const handleTripPageChange = (newPage) => {
  tripPage.value = newPage;
  fetchRankData('trip');
};

// 商家分页
const handleMerchantPageChange = (newPage) => {
  merchantPage.value = newPage;
  fetchRankData('merchant');
};

// 监听旅行团排序方向变化 (不再监听 sortBy)
watch(tripSortDirection, () => {
  tripPage.value = 1; // 重置页码
  fetchRankData('trip');
});

// 监听商家排序字段和方向变化 (保持不变)
watch([merchantSortBy, merchantSortDirection], () => {
  merchantPage.value = 1; // 重置页码
  fetchRankData('merchant');
});

// 计算前三名样式
const getRankClass = (index) => {
  if (index === 0) return 'rank-gold';
  if (index === 1) return 'rank-silver';
  if (index === 2) return 'rank-bronze';
  return '';
};

// 组件挂载时，同时获取两种排行榜数据
onMounted(() => {
  fetchRankData('trip');
  fetchRankData('merchant');
});
</script>

<style scoped>
/* 样式与之前版本保持一致 */
.two-rank-lists-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  width: 100%;
  height: 100%;
}

.rank-card {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  color: #999;
  min-height: 200px;
}
.loading-container .el-icon {
  font-size: 2em;
  margin-bottom: 10px;
}

.rank-list-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.items-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.item-entry {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.item-entry:last-child {
  border-bottom: none;
}

.item-entry:hover {
  background-color: #f9f9f9;
}

.rank-number {
  font-size: 1.2em;
  font-weight: bold;
  width: 30px;
  text-align: center;
  margin-right: 15px;
  color: #666;
}

.rank-gold {
  color: #ffd700;
  font-size: 1.4em;
}
.rank-silver {
  color: #c0c0c0;
  font-size: 1.3em;
}
.rank-bronze {
  color: #cd7f32;
  font-size: 1.25em;
}

.item-avatar {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  object-fit: cover;
  margin-right: 15px;
  flex-shrink: 0;
}

.item-info {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.item-name {
  font-size: 1.1em;
  font-weight: 500;
  color: #333;
  margin: 0 0 5px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-stats {
  font-size: 0.9em;
  color: #777;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
}

.stat-item {
  white-space: nowrap;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
  margin-top: 15px;
  flex-shrink: 0;
}

.el-pagination.is-background .el-pager li {
  min-width: 30px;
  height: 30px;
  line-height: 30px;
  border-radius: 4px;
}
.el-pagination.is-background .btn-prev,
.el-pagination.is-background .btn-next {
  min-width: 30px;
  height: 30px;
  line-height: 30px;
  border-radius: 4px;
}
</style>