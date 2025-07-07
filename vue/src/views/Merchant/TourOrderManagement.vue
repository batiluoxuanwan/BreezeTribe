<template>
  <div class="tour-order-management-page">
    <el-card class="tour-selection-card">
        <el-button type="info" :icon="ArrowLeft" @click="goBackToDashboard">返回</el-button>
      <template #header>
        <div class="card-header">
          <div class="header-content">
            <h3>{{ tourTitle }} 团期订单管理</h3>
            <transition name="el-fade-in-linear">
              <p v-if="selectedSchedule" class="current-schedule-info">
                当前团期：{{ formatScheduleLabel(selectedSchedule) }}
                <el-tag type="info" size="small" effect="plain" class="schedule-count-tag" v-if="schedules.length > 1">
                  共 {{ schedules.length }} 个团期
                </el-tag>
              </p>
            </transition>
          </div>
        </div>
      </template>

      <div v-if="schedules.length > 0" class="schedule-navigation-bar">
        <el-button-group>
          <el-button
            v-for="schedule in schedules"
            :key="schedule.id"
            :type="selectedScheduleId === schedule.id ? 'primary' : 'default'"
            :plain="selectedScheduleId !== schedule.id"
            @click="setSelectedScheduleId(schedule.id)"
            size="small"
          >
            {{ new Date(schedule.departureDate).toLocaleDateString('zh-CN') }}
            <el-tag size="small" :type="selectedScheduleId === schedule.id ? 'light' : 'info'" round style="margin-left: 5px;">
              {{ schedule.participants !== undefined ? `${schedule.participants}人 ` : '未知订单' }}
            </el-tag>
          </el-button>
        </el-button-group>
        <el-divider v-if="schedules.length > 0 && selectedScheduleId" />
      </div>

      <el-alert
        v-if="schedules.length === 0 && !loadingSchedules"
        title="该旅行团暂无团期或团期信息加载失败"
        type="info"
        show-icon
        :closable="false"
        class="no-schedule-alert"
      />

      <div v-else-if="selectedScheduleId" class="order-list-section">
        <el-table
          :data="orders.content"
          v-loading="orders.loading"
          style="width: 100%"
          class="admin-table"
          empty-text="当前团期暂无订单"
        >
          <el-table-column prop="id" label="订单号" width="150"></el-table-column>
          <el-table-column prop="contactName" label="客户名称" width="120"></el-table-column>
          <el-table-column prop="contactPhone" label="客户电话" width="150"></el-table-column>
          <el-table-column prop="travelerCount" label="预订人数" width="100"></el-table-column>
          <el-table-column prop="totalPrice" label="总金额" width="100"></el-table-column>
          <el-table-column prop="orderTime" label="下单日期" width="160">
            <template #default="{ row }">
              {{ formatTime(row.orderTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusType(row.status)">{{ getOrderStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewOrderDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="orders.totalElements > 0"
          background
          layout="prev, pager, next"
          :total="orders.totalElements"
          :page-size="orders.pageSize"
          v-model:current-page="orders.pageNumber"
          @current-change="fetchOrdersForSchedule"
          class="pagination-bottom"
        />
      </div>
      <el-skeleton v-else-if="loadingSchedules" :rows="5" animated />
    </el-card>

    <el-dialog v-model="orderDetailDialogVisible" :title="`订单详情: ${selectedOrder.id}`" width="600px">
      <el-descriptions border :column="1">
        <el-descriptions-item label="订单号">{{ selectedOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ selectedOrder.contactName }}</el-descriptions-item>
        <el-descriptions-item label="客户电话">{{ selectedOrder.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="预订人数">{{ selectedOrder.travelerCount }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ selectedOrder.totalPrice }}</el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ formatTime(selectedOrder.orderTime) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatusType(selectedOrder.status)">{{ getOrderStatusText(selectedOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" v-if="selectedOrder.notes">{{ selectedOrder.notes }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="orderDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElSkeleton } from 'element-plus'; // 导入 ElSkeleton
import { ArrowLeft } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';

const route = useRoute();
const router = useRouter();

const tourId = ref(null);
const tourTitle = ref('');
const fromPage = ref(1);
const schedules = ref([]);
const loadingSchedules = ref(false);
const selectedScheduleId = ref(null);

// 计算属性，用于获取当前选中的团期对象
const selectedSchedule = computed(() => {
  return schedules.value.find(s => s.id === selectedScheduleId.value);
});

const orders = reactive({
  content: [],
  totalElements: 0,
  pageNumber: 1,
  pageSize: 10, // 每页显示10条订单
  loading: false,
});

const selectedOrder = ref({});
const orderDetailDialogVisible = ref(false);

onMounted(() => {
  tourId.value = route.params.tourId;
  tourTitle.value = route.query.tourTitle || '未知旅行团';
  fromPage.value = route.query.fromPage;

  if (!tourId.value) {
    ElMessage.error('缺少旅行团 ID，无法加载订单信息。');
    router.back();
    return;
  }
  fetchSchedulesForTour();
});

// 监听 selectedScheduleId 的变化，当选择不同团期时重新加载订单
watch(selectedScheduleId, (newVal, oldVal) => {
  if (newVal !== oldVal && newVal) {
    orders.pageNumber = 1; // 切换团期时重置页码
    fetchOrdersForSchedule();
  } else if (!newVal) {
    orders.content = [];
    orders.totalElements = 0;
  }
});

// 分页加载某个旅行团的全部团期
const fetchSchedulesForTour = async () => {
  if (!tourId.value) {
    ElMessage.warning('未提供旅行团ID');
    return;
  }

  loadingSchedules.value = true;
  schedules.value = []; // 清空旧数据

  let currentPage = 1;
  const pageSize = 50; // 可自调
  let hasMore = true;

  try {
    while (hasMore) {
      const response = await authAxios.get(`/merchant/packages/${tourId.value}/departures`, {
        params: {
          page: currentPage,
          size: pageSize,
          sortBy: 'departureDate',
          sortDirection: 'ASC'
        }
      });

      if (response.data.code === 200 && response.data.data) {
        const pageData = response.data.data.content || [];
        schedules.value.push(...pageData);

        const totalPages = response.data.data.totalPages || 1;
        if (currentPage >= totalPages || pageData.length === 0) {
          hasMore = false;
        } else {
          currentPage++;
        }
      } else {
        ElMessage.error(response.data.message || '获取团期失败');
        hasMore = false;
      }
    }

    console.log('获取全部团期列表:', schedules.value);

    // 自动选中第一个团期
    if (schedules.value.length > 0) {
      selectedScheduleId.value = schedules.value[0].id;
    }

  } catch (error) {
    console.error('获取团期时出错:', error);
    ElMessage.error('加载团期信息失败。');
    schedules.value = [];
  } finally {
    loadingSchedules.value = false;
  }
};

// 手动设置选中的团期 ID
const setSelectedScheduleId = (id) => {
  selectedScheduleId.value = id;
};

// 获取指定团期的订单列表
const fetchOrdersForSchedule = async () => {
  if (!selectedScheduleId.value) {
    orders.content = [];
    orders.totalElements = 0;
    return;
  }
  orders.loading = true;
  try {
    const response = await authAxios.get(`/merchant/departures/${selectedScheduleId.value}/orders`, {
      params: {
        page: orders.pageNumber,
        size: orders.pageSize,
      },
    });
    if (response.data.code === 200 && response.data.data) {
      orders.content = response.data.data.content;
      orders.totalElements = response.data.data.totalElements;
      console.log('获取团期订单',response.data.data)
    } else {
      ElMessage.error(response.data.message || '获取团期订单失败');
      orders.content = [];
      orders.totalElements = 0;
    }
  } catch (error) {
    console.error('获取团期订单时发生错误:', error);
    ElMessage.error('加载团期订单失败。');
    orders.content = [];
    orders.totalElements = 0;
  } finally {
    orders.loading = false;
  }
};

const viewOrderDetail = (orderRow) => {
  selectedOrder.value = { ...orderRow };
  orderDetailDialogVisible.value = true;
};

const goBackToDashboard = () => {
  router.push({ path: '/merchant/me', query: { activeTab: 'tourManagement' ,fromPage: fromPage.value} }); 
  console.log('跳转回旅行团页面，传入的参数：',router)
};

// 辅助函数：格式化团期显示
const formatScheduleLabel = (schedule) => {
  if (!schedule) return '';
  const departureDate = new Date(schedule.departureDate).toLocaleDateString('zh-CN');
  // 增加对 undefined 的安全检查
  const participants = schedule.participants !== undefined && schedule.participants !== null ? schedule.participants : 0;
  const price = schedule.price !== undefined && schedule.price !== null ? schedule.price.toFixed(2) : '0.00';
  return `${departureDate}出发 (已参团人数: ${participants}, 价格: ¥${price})`;
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

// 辅助函数：获取订单状态标签类型和文本 (与原 Dashboard 保持一致)
const getOrderStatusType = (status) => {
  switch (status) {
    case 'PENDING_PAYMENT': return 'warning';
    case 'PAID': return ''; // 默认
    case 'COMPLETED': return 'success';
    case 'ONGOING': return 'primary'; // 正在进行用 primary 更合适
    case 'CANCELED': return 'danger';
    default: return 'info';
  }
};

const getOrderStatusText = (status) => {
  switch (status) {
    case 'PENDING_PAYMENT': return '待付款';
    case 'PAID': return '已付款';
    case 'COMPLETED': return '已完成';
    case 'CANCELED': return '已取消';
    case 'ONGOING': return '正在进行';
    default: return '未知';
  }
};
</script>

<style scoped>
.tour-order-management-page {
  padding: 40px;
  background-image: url('@/assets/bg1.jpg'); /* Ensure path is correct */
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: 'Poppins', sans-serif;
  overflow-y: auto; /* Allow page content to scroll */
}

.tour-selection-card {
  width: 90%;
  max-width: 1200px;
  border-radius: 18px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  padding: 30px;
  background-color: #fff;
  opacity: 0.98; /* Slightly transparent to let background show */
  transition: all 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap; /* Allow wrapping on smaller screens */
}

.header-content {
  flex-grow: 1; /* Title and schedule info take more space */
  margin-right: 20px; /* Spacing from the back button */
}

.card-header h3 {
  font-size: 2.2rem; /* Larger title */
  color: #2c3e50; /* Darker title color */
  margin: 0 0 10px 0; /* Space below title */
  font-weight: 700;
}

.current-schedule-info {
  font-size: 1.1rem;
  color: #444; /* Adjust color for better contrast with bold text */
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px; /* Spacing between elements */
  background-color: #f9f9f9;
  padding: 8px 15px;
  border-radius: 8px;
  border: 1px solid #eee;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.05);
  font-weight: 500; /* Slightly bolder for readability */
}

.schedule-count-tag {
  margin-left: 10px;
  font-size: 0.9rem;
}

.schedule-navigation-bar {
  margin-top: 15px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column; /* Button group and divider stacked vertically */
  align-items: flex-start; /* Left-aligned */
}

.schedule-hint-alert {
    margin-bottom: 15px; /* Spacing between hint and button group */
    width: 100%; /* Full width */
}

.schedule-button-group {
  margin-bottom: 15px; /* Spacing below button group */
  flex-wrap: wrap; /* Allow buttons to wrap */
  gap: 8px; /* Spacing between buttons */
}

.el-button-group .el-button {
  border-radius: 8px; /* Rounded buttons */
  padding: 8px 15px;
  display: flex; /* Center internal elements */
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease-in-out; /* Smooth transition on hover/active */
}

.el-button-group .el-button:hover {
  transform: translateY(-2px); /* Slight lift effect on hover */
}

.el-button-group .el-button.is-plain {
  border-color: var(--el-color-info-light-7);
  color: var(--el-color-info);
  background-color: #fcfcfc; /* Lighter background for plain buttons */
}

.el-button-group .el-button.is-primary {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff; /* White text for primary button */
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.2); /* Subtle shadow for active button */
}

.el-button-group .el-button .el-tag {
    background-color: rgba(255, 255, 255, 0.15); /* Slightly transparent tag background */
    border: none;
    color: inherit; /* Inherit text color from button */
    font-weight: 600;
}

.el-button-group .el-button.is-primary .el-tag {
    background-color: var(--el-color-primary-light-5); /* Lighter tag for primary button */
    color: var(--el-color-primary-dark-2); /* Darker text for primary tag */
}


.no-schedule-alert {
  margin-top: 20px;
  font-size: 1.1rem;
}

.order-list-section {
  margin-top: 20px;
}

/* Override Element Plus styles */
.el-card__header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 20px;
}

.el-divider {
  margin: 20px 0;
  border-top: 1px dashed #e0e0e0; /* Dashed divider */
}

.admin-table {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.admin-table ::v-deep .el-table__header-wrapper th {
  background-color: #f5f7fa;
  color: #555;
  font-weight: 600;
}

.admin-table ::v-deep .el-table__row:hover {
  background-color: #f7f9fc;
}

.pagination-bottom {
  margin-top: 25px;
  justify-content: center;
}

.el-dialog__header {
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
}

.el-descriptions__title {
  font-weight: bold;
}
</style>