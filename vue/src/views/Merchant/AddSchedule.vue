<template>
  <div class="add-schedule-page">
    <div class="header-section">
      <h2 class="page-title">
        <el-icon><Calendar /></el-icon> 管理团期
      </h2>
      <el-button type="info" :icon="ArrowLeft" class="back-to-profile-btn" @click="goToProfile">
        返回
      </el-button>
    </div>

    <div class="packages-grid-container" v-loading="loading">
      <el-empty v-if="!loading && travelPackages.length === 0" description="您还没有发布任何旅行团，快去发布第一个吧！">
      </el-empty>

      <div class="package-wrapper" v-for="pkg in travelPackages" :key="pkg.id">
        <el-card
          class="package-card"
          :class="{ 'is-active': activePackageId === pkg.id }"
          shadow="hover"
          @click="togglePackageExpand(pkg.id)"
        >
          <img :src="pkg.coverImageUrl || defaultCoverImage" class="package-cover-image" :alt="pkg.title" />
          <div class="package-info">
            <h3 class="package-title" :title="pkg.title">{{ pkg.title }}</h3>
            <div class="package-meta">
              <span class="duration-tag"><el-icon><Sunrise /></el-icon> {{ pkg.durationInDays }} 天</span>
            </div>
          </div>
          <div class="package-actions">
            <el-button
              :type="activePackageId === pkg.id ? 'danger' : 'primary'"
              :icon="activePackageId === pkg.id ? ArrowUp : ArrowDown"
              size="small"
              class="manage-btn"
            >
              {{ activePackageId === pkg.id ? '收起团期' : '查看/管理团期' }}
            </el-button>
          </div>
        </el-card>

        <el-collapse-transition>
          <div v-show="activePackageId === pkg.id" class="departure-management-area">
            <div class="current-package-title">
              正在管理：<strong>{{ pkg.title }}</strong> 的团期
            </div>
            <el-card class="form-card add-departure-card">
              <template #header>
                <div class="card-header">
                  <h3>批量添加新团期</h3>
                  <el-icon><Plus /></el-icon>
                </div>
              </template>
              <el-form :model="newDeparturesForm" label-width="100px">
                <el-form-item label="选择日期" required>
                  <el-date-picker
                    v-model="newDeparturesForm.dates"
                    type="dates"
                    placeholder="选择一个或多个出发日期"
                    :picker-options="{
                      disabledDate: (time) => time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
                    }"
                    style="width: 100%"
                  ></el-date-picker>
                </el-form-item>
                <el-form-item label="价格" required>
                  <el-input-number v-model="newDeparturesForm.price" :min="0" :precision="2" controls-position="right" style="width: 100%"></el-input-number>
                </el-form-item>
                <el-form-item label="库存" required>
                  <el-input-number v-model="newDeparturesForm.capacity" :min="1" controls-position="right" style="width: 100%"></el-input-number>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="addLoading" :icon="UploadFilled" @click="addDepartures(pkg.id)">
                    批量添加团期
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card class="data-card departure-list-card" style="margin-top: 20px;" v-loading="departuresLoading">
              <template #header>
                <div class="card-header">
                  <h3>现有团期列表</h3>
                  <el-icon><List /></el-icon>
                </div>
              </template>
              <el-empty v-if="!currentDepartures.length && !departuresLoading" description="该旅行团暂无团期"></el-empty>
              <div v-else>
                <el-table :data="currentDepartures" style="width: 100%">
                  <el-table-column prop="departureDate" label="出发日期" width="180">
                    <template #default="{ row }">
                      {{ dayjs(row.departureDate).format('YYYY-MM-DD') }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="price" label="价格" width="100"></el-table-column>
                  <el-table-column prop="capacity" label="库存" width="80"></el-table-column>
                  <el-table-column prop="participants" label="已报名" width="80"></el-table-column>
                  <el-table-column prop="status" label="状态" width="100"></el-table-column>
                  <el-table-column label="操作" width="180">
                    <template #default="{ row }">
                      <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                      <el-button size="small" type="danger" :icon="Delete" @click="confirmDeleteDeparture(row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination
                  v-if="departurePagination.totalElements > departurePagination.pageSize"
                  v-model:current-page="departurePagination.pageNumber"
                  v-model:page-size="departurePagination.pageSize"
                  :page-sizes="[5, 10, 20]"
                  :background="true"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="departurePagination.totalElements"
                  @size-change="handleDepartureSizeChange"
                  @current-change="handleDepartureCurrentChange"
                  class="departure-pagination"
                ></el-pagination>
              </div>
            </el-card>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <el-pagination
      v-if="travelPackages.length > 0 || (!loading && pagination.totalElements > pagination.pageSize)"
      v-model:current-page="pagination.pageNumber"
      v-model:page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :background="true"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.totalElements"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      class="pagination-bottom"
    ></el-pagination>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑团期信息"
      width="40%"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form :model="editDepartureForm" label-width="80px">
        <el-form-item label="出发日期">
          <el-input :value="dayjs(editDepartureForm.departureDate).format('YYYY-MM-DD')" disabled></el-input>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="editDepartureForm.price" :min="0" :precision="2" controls-position="right" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input-number v-model="editDepartureForm.capacity" :min="1" controls-position="right" style="width: 100%"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="updateLoading" @click="updateDeparture">确认更新</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { authAxios } from '@/utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Suitcase, Sunrise, Calendar, Plus, List, Delete, Edit, ArrowLeft, ArrowDown, ArrowUp, UploadFilled } from '@element-plus/icons-vue';
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc'; 

dayjs.extend(utc); // 扩展 Day.js，使其支持 UTC 功能
const router = useRouter();

const defaultCoverImage = 'https://via.placeholder.com/400x250?text=No+Image';

const travelPackages = ref([]);
const loading = ref(false); // 控制旅行团列表的加载状态

// 旅行团列表的分页
const pagination = reactive({
  pageNumber: 1,
  pageSize: 10,
  totalElements: 0,
  totalPages: 0,
  sortBy: 'createdTime',
  sortDirection: 'DESC',
});

// --- 旅行团列表相关方法 ---
const fetchTravelPackages = async () => {
  loading.value = true;
  try {
    const response = await authAxios.get('/dealer/travel-packages', { 
      params: {
        page: pagination.pageNumber,
        size: pagination.pageSize,
        sortBy: pagination.sortBy,
        sortDirection: pagination.sortDirection,
      },
    });

    if (response.data.code === 200) {
      travelPackages.value = response.data.data.content;
      pagination.totalElements = response.data.data.totalElements;
      pagination.totalPages = response.data.data.totalPages;
    } else {
      ElMessage.error(response.data.message || '获取旅行团列表失败。');
    }
  } catch (error) {
    console.error('获取旅行团列表失败:', error);
    ElMessage.error(`获取旅行团列表失败: ${error.response?.data?.message || error.message}`);
  } finally {
    loading.value = false;
  }
};

const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.pageNumber = 1;
  fetchTravelPackages();
};

const handleCurrentChange = (val) => {
  pagination.pageNumber = val;
  fetchTravelPackages();
};

const goToProfile = () => {
  router.push('/merchant/me');
};

// --- 团期管理部分 ---
const activePackageId = ref(null); // 当前展开的旅行团ID
const currentDepartures = ref([]); // 当前展开旅行团的团期列表
const departuresLoading = ref(false); // 团期列表的加载状态

const departurePagination = reactive({
  pageNumber: 1,
  pageSize: 5, // 团期列表每页显示数量可以少一点
  totalElements: 0,
  totalPages: 0,
});

// 批量添加团期表单
const newDeparturesForm = reactive({
  dates: [],
  price: 0.00,
  capacity: 1,
});
const addLoading = ref(false); // 批量添加团期按钮的加载状态

// 切换卡片展开/收起
const togglePackageExpand = async (pkgId) => {
  if (activePackageId.value === pkgId) {
    // 如果点击的是当前已展开的卡片，则收起
    activePackageId.value = null;
    currentDepartures.value = []; // 清空团期列表
    departurePagination.pageNumber = 1; // 重置分页
  } else {
    // 展开新的卡片
    activePackageId.value = pkgId;
    departurePagination.pageNumber = 1; // 切换产品时，团期列表回到第一页
    await fetchDepartures(pkgId); // 展开时立即加载该团的团期
  }
};

// 获取特定旅行团的团期列表
const fetchDepartures = async (pkgId) => {
  if (!pkgId) return; // 如果没有 packageId，则不请求

  departuresLoading.value = true;
  try {
    // 请确认这个接口路径，GET /api/merchant/packages/{packageId}/departures
    const response = await authAxios.get(`/merchant/packages/${pkgId}/departures`, {
      params: {
        page: departurePagination.pageNumber,
        size: departurePagination.pageSize,
        // 团期列表通常会按出发日期排序
        sortBy: 'departureDate',
        sortDirection: 'ASC',
      },
    });

    if (response.data.code === 200) {
      currentDepartures.value = response.data.data.content;
      departurePagination.totalElements = response.data.data.totalElements;
      departurePagination.totalPages = response.data.data.totalPages;
    } else {
      ElMessage.error(response.data.message || '获取团期列表失败。');
    }
  } catch (error) {
    console.error('获取团期列表失败:', error);
    ElMessage.error(`获取团期列表失败: ${error.response?.data?.message || error.message}`);
  } finally {
    departuresLoading.value = false;
  }
};

// 团期列表分页处理
const handleDepartureSizeChange = (val) => {
  departurePagination.pageSize = val;
  departurePagination.pageNumber = 1;
  fetchDepartures(activePackageId.value);
};

const handleDepartureCurrentChange = (val) => {
  departurePagination.pageNumber = val;
  fetchDepartures(activePackageId.value);
};

// 批量添加团期逻辑
const addDepartures = async (pkgId) => {
  if (!pkgId) {
    ElMessage.error('缺少旅行产品ID，无法添加团期。');
    return;
  }
  if (newDeparturesForm.dates.length === 0) {
    ElMessage.warning('请选择要添加的出发日期！');
    return;
  }
  if (newDeparturesForm.price < 0) {
    ElMessage.warning('价格不能为负数！');
    return;
  }
  if (newDeparturesForm.capacity < 1) {
    ElMessage.warning('库存必须大于0！');
    return;
  }
  const departuresToAdd = newDeparturesForm.dates.map(date => {
    const dayjsDate = dayjs(date);
    const formattedDate = dayjsDate.utc().millisecond(0).toISOString();
    return {
      departureDate: formattedDate,
      price: newDeparturesForm.price,
      capacity: newDeparturesForm.capacity,
    };
  });
  //console.log('即将发送的请求体:', departuresToAdd); 

  addLoading.value = true;
  try {
    const response = await authAxios.post(`/merchant/packages/${pkgId}/departures`, departuresToAdd);

    if (response.data.code === 200) {
      ElMessage.success('团期批量添加成功！');
      // 成功后清空表单
      newDeparturesForm.dates = [];
      newDeparturesForm.price = 0.00;
      newDeparturesForm.capacity = 1;
      // 重新加载团期列表
      await fetchDepartures(pkgId);
    } else {
      ElMessage.error(response.data.message || '批量添加团期失败。');
    }
  } catch (error) {
    console.error('批量添加团期失败:', error);
    ElMessage.error(`批量添加团期失败: ${error.response?.data?.message || error.message}`);
  } finally {
    addLoading.value = false;
  }
};

// --- 编辑团期相关 ---
const editDialogVisible = ref(false); // 控制编辑对话框的显示
const updateLoading = ref(false); // 控制更新按钮的加载状态
const editDepartureForm = reactive({
  id: '', // 团期ID
  departureDate: '', // 团期日期（用于显示，不可编辑）
  price: 0,
  capacity: 0,
});

// 打开编辑对话框并填充数据
const openEditDialog = (row) => {
  editDepartureForm.id = row.id;
  editDepartureForm.departureDate = row.departureDate; // 原始日期字符串
  editDepartureForm.price = row.price;
  editDepartureForm.capacity = row.capacity;
  editDialogVisible.value = true;
};

// 提交更新团期信息
const updateDeparture = async () => {
  // 基本校验
  if (editDepartureForm.price < 0) {
    ElMessage.warning('价格不能为负数！');
    return;
  }
  if (editDepartureForm.capacity < 1) {
    ElMessage.warning('库存必须大于0！');
    return;
  }

  updateLoading.value = true;
  try {
    const response = await authAxios.put(`/merchant/departures/${editDepartureForm.id}`,
      {
        price: editDepartureForm.price,
        capacity: editDepartureForm.capacity,
      }
    );

    if (response.data.code === 200) {
      ElMessage.success('团期更新成功！');
      editDialogVisible.value = false; // 关闭对话框
      await fetchDepartures(activePackageId.value); // 刷新当前团期列表
    } else {
      ElMessage.error(response.data.message || '团期更新失败。');
    }
  } catch (error) {
    console.error('更新团期失败:', error);
    ElMessage.error(`更新团期失败: ${error.response?.data?.message || error.message}`);
  } finally {
    updateLoading.value = false;
  }
};

// --- 删除团期相关 ---
// 弹出确认对话框
const confirmDeleteDeparture = (row) => {
  ElMessageBox.confirm(
    `确定要删除日期为 ${dayjs(row.departureDate).format('YYYY-MM-DD')} 的团期吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
  .then(() => {
    // 用户点击确定，执行删除操作
    deleteDeparture(row.id);
  })
  .catch(() => {
    // 用户点击取消或关闭对话框
    ElMessage.info('已取消删除');
  });
};

// 执行删除团期操作
const deleteDeparture = async (departureId) => {
  departuresLoading.value = true; // 可以显示团期列表的加载状态
  try {
    const response = await authAxios.delete(`/merchant/departures/${departureId}`);

    if (response.data.code === 200) {
      ElMessage.success('团期删除成功！');
      // 刷新当前团期列表
      await fetchDepartures(activePackageId.value);
    } else {
      ElMessage.error(response.data.message || '团期删除失败。');
    }
  } catch (error) {
    console.error('删除团期失败:', error);
    // 后端如果因为有报名而不允许删除，通常会返回 400 Bad Request
    const errorMessage = error.response?.data?.message || error.message;
    ElMessage.error(`删除团期失败: ${errorMessage}`);
  } finally {
    departuresLoading.value = false;
  }
};

onMounted(() => {
  fetchTravelPackages(); // 获取旅行团列表
});

// 监听 activePackageId 的变化，当它被设置时，清空批量添加表单
watch(activePackageId, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    newDeparturesForm.dates = [];
    newDeparturesForm.price = 0.00;
    newDeparturesForm.capacity = 1;
  }
});
</script>

<style scoped>
/* 整个页面容器 */
.add-schedule-page {
  padding: 40px;
  max-width: 1400px;
  margin: auto;
  font-family: 'Poppins', sans-serif;
  background-color: #f0f2f5;
  border-radius: 18px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  min-height: 80vh;
  display: flex;
  flex-direction: column;
}

/* 页面头部 */
.header-section {
  text-align: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

.page-title {
  font-size: 2.2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-title .el-icon {
  font-size: 2.2rem;
  margin-right: 10px;
  color: #00796b;
}

.back-to-profile-btn {
  position: absolute;
  top: 0;
  left: 0;
  padding: 10px 20px;
  font-size: 1rem;
  border-radius: 8px;
  background-color: #607d8b; /* Blue Grey */
  border-color: #607d8b;
  color: #fff;
  transition: all 0.3s ease;
}

.back-to-profile-btn:hover {
  background-color: #455a64;
  border-color: #455a64;
  transform: translateY(-2px);
}

/* 旅行团卡片网格容器 */
.packages-grid-container {
  display: grid;
  grid-template-columns: 1fr; /* 每行一个卡片，卡片内容纵向排列 */
  gap: 20px;
  margin-top: 10px;
  flex-grow: 1;
}

/* 单个旅行团的包装容器，包含卡片和下拉区域 */
.package-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0px; /* 紧贴在一起 */
}

/* --- 旅行团卡片样式 (主要修改部分) --- */
.package-card {
  display: flex; /* **关键：使卡片内部子元素横向排列** */
  align-items: center; /* 垂直居中对齐子元素 */
  justify-content: space-between; /* **关键：在子元素之间创建空间，使按钮靠右** */
  border-radius: 14px;
  overflow: hidden;
  padding: 16px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  border: 1px solid #ebebeb;
  cursor: pointer; /* 鼠标悬停显示手型，表示可点击 */
}

.package-card:hover {
  transform: translateY(-3px); /* 略微上浮 */
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
  border-color: #409EFF; /* 鼠标悬停时边框颜色变化 */
}

.package-card.is-active {
  border-color: #409EFF; /* 选中状态的边框颜色 */
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  transform: translateY(0); /* 激活时不浮动 */
}

.package-cover-image {
  width: 80px; /* **调整图片宽度，以适应横向空间** */
  height: 60px; /* **相应调整高度** */
  object-fit: cover;
  border-radius: 8px; /* 缩略图圆角 */
  margin-right: 15px; /* **减少图片和文字之间的间距** */
  flex-shrink: 0; /* 不缩小 */
}

.package-info {
  flex-grow: 1; /* **关键：让信息区域占据尽可能多的剩余空间** */
  text-align: left;
  display: flex; /* **关键：使标题和天数在信息区域内横向排列** */
  align-items: center; /* 垂直居中对齐标题和天数 */
  min-width: 0; /* **允许内部元素在空间不足时缩小** */
}

.package-title {
  margin: 0;
  font-size: 1.1rem; /* 标题字号 */
  color: #333;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1; /* 标题尽可能占据空间 */
}

.package-meta {
  margin-left: 10px; /* **天数和标题之间的间距** */
  color: #777;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  flex-shrink: 0; /* 天数部分不缩小 */
}

.duration-tag {
  display: flex;
  align-items: center;
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 3px 8px;
  border-radius: 4px;
}

.duration-tag .el-icon {
  margin-right: 5px;
  font-size: 0.9rem;
}

.package-actions {
  flex-shrink: 0; /* 不缩小 */
  margin-left: 15px; /* **按钮和信息区域之间的间距** */
}

.package-actions .manage-btn {
  border-radius: 8px;
  padding: 8px 12px; /* 稍微调整按钮内边距 */
  font-size: 0.9rem;
}

/* 团期管理展开区域 */
.departure-management-area {
  background-color: #ffffff;
  border: 1px solid #409EFF; /* 边框颜色与激活卡片一致 */
  border-top: none; /* 上边框取消，与卡片连接 */
  border-bottom-left-radius: 14px; /* 底部圆角 */
  border-bottom-right-radius: 14px;
  padding: 20px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  margin-top: -1px; /* 向上微调，消除边框间隙 */
}

.current-package-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #e0e0e0;
  text-align: center;
}

.current-package-title strong {
  color: #409EFF;
}

.form-card, .data-card {
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
  color: #333;
}

.card-header .el-icon {
  font-size: 1.3rem;
  color: #909399;
}

.add-departure-card .el-button {
  width: 100%;
  padding: 12px 0;
  font-size: 1rem;
  letter-spacing: 1px;
  border-radius: 8px;
  background-color: #409eff;
  border-color: #409eff;
  transition: all 0.3s ease;
}

.add-departure-card .el-button:hover {
  background-color: #337ecc;
  border-color: #337ecc;
  transform: translateY(-2px);
}

.departure-list-card .el-table {
  margin-top: 15px;
}

.departure-pagination {
  margin-top: 20px;
  justify-content: flex-end;
  display: flex;
}

/* 顶层旅行团分页 */
.pagination-bottom {
  margin-top: 30px;
  justify-content: flex-end;
  display: flex;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .add-schedule-page {
    padding: 20px;
  }
  .header-section {
    text-align: left;
    margin-bottom: 30px;
  }
  .back-to-profile-btn {
    position: static;
    margin-top: 20px;
    width: 100%;
  }
  .package-card {
    flex-direction: column; /* 小屏幕上垂直堆叠 */
    align-items: flex-start;
  }
  .package-cover-image {
    width: 100%;
    height: 150px;
    margin-right: 0;
    margin-bottom: 15px;
  }
  .package-info {
    width: 100%;
    flex-direction: column; /* 小屏幕上信息区域内部垂直堆叠 */
    align-items: flex-start;
  }
  .package-title {
    max-width: 100%;
    margin-bottom: 5px; /* 标题和天数之间增加间距 */
  }
  .package-meta {
    margin-left: 0; /* 移除横向间距 */
  }
  .package-actions {
    margin-left: 0;
    margin-top: 15px;
    width: 100%;
  }
  .package-actions .manage-btn {
    width: 100%;
  }
}
</style>