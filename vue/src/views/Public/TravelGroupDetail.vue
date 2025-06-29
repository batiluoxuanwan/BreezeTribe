<template>
  <div class="TravelGroupDetail">
    <div class="travel-group-detail-container">
      <el-button type="info" class="back-button" @click="goBack">
        <el-icon><ArrowLeftBold /></el-icon>
        <span>返回</span>
      </el-button>

      <div v-if="loading" class="loading-detail">
        <el-empty description="正在加载旅行团详情..."></el-empty>
      </div>

      <div v-else-if="travelGroupDetail" class="detail-content-wrapper">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <h2 class="group-title">{{ travelGroupDetail.title }}</h2>
              <div class="header-actions">
                <el-tag :type="getTourStatusTagType(travelGroupDetail.status)" size="large">
                  {{ getTourStatusText(travelGroupDetail.status) }}
                </el-tag>
                <el-button
                  :type="isFavorite ? 'danger' : 'primary'"
                  :icon="isFavorite ? StarFilled : Star"
                  circle
                  size="large"
                  class="favorite-button"
                  @click="toggleFavorite"
                  :loading="favoriteLoading"
                  :disabled="!authStore.isLoggedIn"
                >
                </el-button>
              </div>
            </div>
          </template>

          <el-row :gutter="30" class="main-info-section">
            <el-col :span="14">
              <el-carousel v-if="travelGroupDetail.coverImageUrls && travelGroupDetail.coverImageUrls.length > 1"
                           trigger="click" height="400px" indicator-position="outside" class="group-carousel">
                <el-carousel-item v-for="(imgUrl, idx) in travelGroupDetail.coverImageUrls" :key="idx">
                  <el-image :src="imgUrl" fit="cover" class="carousel-image"></el-image>
                </el-carousel-item>
              </el-carousel>
              <el-image v-else :src="travelGroupDetail.coverImageUrls && travelGroupDetail.coverImageUrls.length > 0 ? travelGroupDetail.coverImageUrls[0] : 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg'"
                        fit="cover" class="group-image-single">
                <template #error>
                  <div class="image-slot">
                    <el-icon><Picture /></el-icon>
                    <span>无图片</span>
                  </div>
                </template>
              </el-image>
            </el-col>
            <el-col :span="10">
              <div class="detail-info">
                <p class="price-display">
                  <strong>价格:</strong> <span class="price-value">¥{{ travelGroupDetail.price }}</span>
                </p>
                <p><strong>行程时长:</strong> <span class="info-value">{{ travelGroupDetail.durationInDays }} 天</span></p>
                <p v-if="travelGroupDetail.destination"><strong>目的地:</strong> <span class="info-value">{{ travelGroupDetail.destination }}</span></p>
                <p v-if="travelGroupDetail.startDate"><strong>出发日期:</strong> <span class="info-value">{{ formatDate(travelGroupDetail.startDate) }}</span></p>
                <p v-if="travelGroupDetail.members"><strong>当前成员:</strong> <span class="info-value">{{ travelGroupDetail.members }} 人</span></p>
                <p v-if="travelGroupDetail.rating">
                  <strong>用户评分:</strong>
                  <el-rate v-model="travelGroupDetail.rating" disabled show-score text-color="#ff9900" score-template="{value}" class="rating-display"></el-rate>
                </p>

                <div class="enroll-button-section">
                  <el-button
                    type="success"
                    size="large"
                    :disabled="travelGroupDetail.status !== 'PUBLISHED'"
                    @click="handleEnrollClick"
                  >
                    <el-icon><Tickets /></el-icon>
                    <span>立即报名</span>
                  </el-button>
                  <p v-if="travelGroupDetail.status !== 'PUBLISHED'" class="enroll-disabled-tip">
                    * 该旅行团未处于“已发布”状态，暂无法报名。
                  </p>
                </div>
              </div>
            </el-col>
          </el-row>

          <el-divider class="section-divider"></el-divider>

          <h3 class="section-title">旅行团介绍</h3>
          <p class="detailed-description-text" v-html="formatDescription(travelGroupDetail.detailedDescription)"></p>
          <p v-if="!travelGroupDetail.detailedDescription" class="placeholder-text">暂无详细介绍。</p>

          <el-divider class="section-divider"></el-divider>

          <h3 class="section-title">详细行程安排</h3>
          <div class="itinerary-section">
            <el-timeline v-if="travelGroupDetail.routes && travelGroupDetail.routes.length > 0">
              <el-timeline-item
                v-for="(route, index) in travelGroupDetail.routes"
                :key="route.id || index"
                :timestamp="`第 ${index + 1} 天`"
                placement="top"
                type="primary"
              >
                <el-card class="itinerary-card">
                  <h4>{{ route.name || `第 ${index + 1} 天行程` }}</h4>
                  <p class="route-description">{{ route.description || '暂无描述' }}</p>

                  <div v-if="route.spots && route.spots.length > 0" class="spot-list">
                    <h5>包含景点:</h5>
                    <el-tag v-for="spot in route.spots" :key="spot.id" class="spot-tag" type="info" effect="plain">
                      {{ spot.name }}
                    </el-tag>
                  </div>
                  <p v-else class="placeholder-text">暂无具体景点安排。</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无详细行程安排。"></el-empty>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="未找到该旅行团信息或加载失败"></el-empty>
    </div>
  </div>

  <el-dialog
    v-model="enrollDialogVisible"
    title="填写报名信息"
    width="500px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form :model="enrollForm" :rules="enrollRules" ref="enrollFormRef" label-width="100px">
      <el-form-item label="旅行团名称">
        <el-input :value="travelGroupDetail ? travelGroupDetail.title : ''" disabled></el-input>
      </el-form-item>
      <el-form-item label="报名人数" prop="numberOfParticipants">
        <el-input-number v-model="enrollForm.numberOfParticipants" :min="1" :max="99" style="width: 100%"></el-input-number>
      </el-form-item>
      <el-form-item label="联系人姓名" prop="contactName">
        <el-input v-model="enrollForm.contactName" placeholder="请输入联系人姓名"></el-input>
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="enrollForm.contactPhone" placeholder="请输入联系电话"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="enrollDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEnrollForm">确定报名</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive ,watch} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElCard, ElRow, ElCol, ElImage, ElDivider, ElTag, ElRate, ElEmpty, ElButton, ElCarousel, ElCarouselItem, ElTimeline, ElTimelineItem, ElIcon, ElMessageBox, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber } from 'element-plus';
import { ArrowLeftBold, Picture, Star, StarFilled, Tickets } from '@element-plus/icons-vue'; // Import Star and StarFilled icons
import { publicAxios, authAxios } from '@/utils/request';
import moment from 'moment';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const travelGroupDetail = ref(null);
const loading = ref(true);
const isFavorite = ref(false); 
const favoriteLoading = ref(false); 

const enrollDialogVisible = ref(false); // 控制报名弹窗的显示与隐藏
const enrollFormRef = ref(null); // 用于获取表单实例，进行表单校验

// 报名表单数据
const enrollForm = reactive({
  numberOfParticipants: 1,
  contactName: '',
  contactPhone: '',
});

// 报名表单校验规则
const enrollRules = reactive({
  numberOfParticipants: [
    { required: true, message: '请填写报名人数', trigger: 'blur' },
    { type: 'number', min: 1, message: '报名人数至少为1', trigger: 'blur' },
  ],
  contactName: [
    { required: true, message: '请填写联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }, // 简单的手机号正则
  ],
});

// --- 返回上一页 ---
const goBack = () => {
  router.push({ name: '旅行广场', query: { tab: 'groups' } });
};

// --- 获取特定旅行团的详细数据 ---
const fetchTravelGroupDetail = async (id) => {
  loading.value = true;
  try {
    const response = await publicAxios.get(`/public/travel-packages/${id}`);

    if (response.data.code === 200) {
      travelGroupDetail.value = response.data.data;
      if (authStore.isLoggedIn) {
        await checkFavoriteStatus(id);
      }
    } else {
      ElMessage.error(response.data.message || '获取旅行团详情失败！');
      travelGroupDetail.value = null;
    }
  } catch (error) {
    console.error(`获取旅行团详情 (ID: ${id}) 失败:`, error);
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(error.response.data.message);
    } else {
      ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    }
    travelGroupDetail.value = null;
  } finally {
    loading.value = false;
  }
};

const checkFavoriteStatus = async (itemId) => {
  if (!authStore.isLoggedIn) {
    isFavorite.value = false;
    return;
  }
  try {
    const response = await authAxios.get(`/user/favorites/status`, {
      params: { itemId: itemId, itemType: 'PACKAGE' }
    });
    if (response.data.code === 200) {
      isFavorite.value = response.data.data; 
    } else {
      console.warn('Failed to get favorite status:', response.data.message);
      isFavorite.value = false;
    }
  } catch (error) {
    console.error('Error checking favorite status:', error);
    isFavorite.value = false;
  }
};


// --- Toggle Favorite Status ---
const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    ElMessageBox.confirm('您尚未登录，请先登录才能收藏。', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    })
    .then(() => {
      router.push('/login');
    })
    .catch(() => {
      ElMessage.info('已取消收藏。');
    });
    return;
  }

  favoriteLoading.value = true;
  const itemId = travelGroupDetail.value.id;
  const itemType = 'PACKAGE'; 

  try {
    const method = isFavorite.value ? 'delete' : 'post';
    const url = `/user/favorites`;
    const data = { itemId, itemType };

    let response;
    if (method === 'post') {
      response = await authAxios.post(url, data);
    } else { 
      response = await authAxios.delete(url, data); 
    }

    if (response.data.code === 200) {
      isFavorite.value = !isFavorite.value;
      ElMessage.success(isFavorite.value ? '收藏成功！' : '取消收藏成功！');
    } else {
      ElMessage.error(response.data.message || '操作失败，请重试。');
    }
  } catch (error) {
    console.error('收藏/取消收藏失败:', error);
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(error.response.data.message);
    } else {
      ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    }
  } finally {
    favoriteLoading.value = false;
  }
};


// --- 辅助函数：根据状态获取文本 ---
const getTourStatusText = (status) => {
  switch (status) {
    case 'PENDING_APPROVAL': return '待审核';
    case 'PUBLISHED': return '已发布';
    case 'OFFLINE': return '已下架';
    case 'REJECTED': return '已驳回';
    default: return '未知状态';
  }
};

// --- 辅助函数：根据状态获取 El-Tag 类型（颜色） ---
const getTourStatusTagType = (status) => {
  switch (status) {
    case 'PENDING_APPROVAL': return 'info'; // 蓝色/灰色
    case 'PUBLISHED': return 'success'; // 绿色
    case 'OFFLINE': return 'danger'; // 红色
    case 'REJECTED': return 'warning'; // 黄色
    default: return 'info';
  }
};

// --- 辅助函数：格式化详细描述 (将换行符 \n 转换为 HTML 的 <br/>) ---
const formatDescription = (description) => {
  if (!description) return '';
  return description.replace(/\n/g, '<br/>');
};

// --- 辅助函数：格式化日期 (需要 moment.js 库) ---
const formatDate = (dateString) => {
  if (!dateString) return '未设置';
  return moment(dateString).format('YYYY年MM月DD日');
};

// --- 处理报名按钮点击 ---
const handleEnrollClick = async () => {
  // 检查登录状态
  if (!authStore.isLoggedIn) {
    ElMessageBox.confirm('您尚未登录，请先登录才能报名。', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    })
    .then(() => {
      router.push('/login');
    })
    .catch(() => {
      ElMessage.info('已取消报名。');
    });
    return;
  }

  // 检查用户角色
  if (authStore.role !== 'ROLE_USER') {
    ElMessageBox.alert('抱歉，只有普通用户才能报名旅行团。请使用用户账户登录或联系管理员。', '权限不足', {
      confirmButtonText: '知道了',
      type: 'warning',
      dangerouslyUseHTMLString: true,
    });
    return;
  }

  // 用户已登录且是 'USER' 角色，显示报名弹窗
  enrollForm.numberOfParticipants = 1;
  enrollForm.contactName = '';
  enrollForm.contactPhone = '';
  // 如果有表单引用，先清空校验状态
  if (enrollFormRef.value) {
    enrollFormRef.value.resetFields();
  }
  enrollDialogVisible.value = true;
};

// --- 提交报名表单 ---
const submitEnrollForm = async () => {
  try {
    // 表单校验
    await enrollFormRef.value.validate();

    // 发送报名请求
    const response = await authAxios.post('/user/orders', {
      packageId: travelGroupDetail.value.id,
      travelerCount: enrollForm.numberOfParticipants,
      contactName: enrollForm.contactName,
      contactPhone: enrollForm.contactPhone,
    });

    if (response.data.code === 200) {
      ElMessage.success(`成功报名 ${enrollForm.numberOfParticipants} 人！请等待后续通知。`);
      enrollDialogVisible.value = false;
    } else {
      ElMessage.error(response.data.message || '报名失败，请重试。');
    }
  } catch (error) {
    if (error === false) { // validate 校验失败会返回 false
      ElMessage.error('请完整填写报名信息并确保格式正确。');
    } else {
      console.error('报名提交失败:', error);
      if (error.response && error.response.data && error.response.data.message) {
        ElMessage.error(error.response.data.message);
      } else {
        ElMessage.error('报名操作失败，请稍后再试。');
      }
    }
  }
};

// 组件挂载时执行
onMounted(() => {
  // 从路由参数中获取旅行团 ID
  const groupId = route.params.id;
  if (groupId) {
    fetchTravelGroupDetail(groupId);
  } else {
    ElMessage.error('未获取到旅行团ID，无法加载详情！');
    loading.value = false;
  }
});

// 监听路由参数 id 的变化
watch(
  () => route.params.id, // 监听 route.params.id
  (newId, oldId) => {
    // 只有当 ID 确实发生变化且新 ID 不为空时才重新获取数据
    if (newId && newId !== oldId) {
      console.log(`路由 ID 发生变化：从 ${oldId} 变为 ${newId}，重新加载旅行团详情。`);
      fetchTravelGroupDetail(newId);
    }
  },
  { immediate: false } // 不需要立即执行，因为 onMounted 已经处理了首次加载
);
</script>

<style scoped>
.TravelGroupDetail{
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  min-height: 100vh;
  align-items: center;
  padding: 20px 0;
}

.travel-group-detail-container {
  padding: 30px;
  max-width: 1200px;
  margin: 30px auto;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

/* 固定在左上角的返回按钮 */
.back-button {
  position: fixed; /* 固定定位 */
  top: 20px;
  left: 20px;
  z-index: 1000; /* 确保在最顶层 */
  background-color: rgba(0, 0, 0, 0.5); /* 半透明背景 */
  border: none;
  color: #fff;
  padding: 10px 15px;
  border-radius: 20px; /* 更圆润 */
  font-size: 0.9em;
  transition: background-color 0.3s ease;
}
.back-button:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

/* 加载状态样式 */
.loading-detail {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

/* 详情内容容器，避免被返回按钮遮挡 */
.detail-content-wrapper {
  margin-top: 20px; /* Ensure content is below back button */
}

/* 卡片样式 */
.detail-card {
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* Softer, more pronounced shadow */
  border: none; /* Remove default border */
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px; /* Space between tag and button */
}

/* 旅行团标题 */
.group-title {
  font-size: 2.8em;
  color: #303133;
  margin: 0;
  font-weight: bold;
  letter-spacing: 0.5px;
}

/* Favorite button styles */
.favorite-button {
  transition: all 0.3s ease; /* Smooth transition for hover/active states */
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.favorite-button.el-button--primary {
  background-color: #409eff; /* Default blue for non-favorited */
  border-color: #409eff;
}

.favorite-button.el-button--danger {
  background-color: #f56c6c; /* Red for favorited */
  border-color: #f56c6c;
}

.favorite-button:hover {
  transform: translateY(-2px); /* Slight lift on hover */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}


/* 主信息区 */
.main-info-section {
  margin-top: 20px;
}

/* 图片轮播图样式 */
.group-carousel {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 单张图片样式 */
.group-image-single {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
/* 图片加载失败时的占位符样式 */
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f0f2f5;
  color: #909399;
  font-size: 2em;
  flex-direction: column;
}
.image-slot span {
  font-size: 0.5em;
  margin-top: 10px;
}

/* 详情信息文本样式 */
.detail-info {
  padding-left: 20px;
}
.detail-info p {
  font-size: 1.15em;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.6;
}
.detail-info strong {
  color: #303133;
  min-width: 80px;
  display: inline-block;
}
.info-value {
  color: #303133;
  font-weight: 500;
}
/* 价格显示样式 */
.price-display {
  font-size: 1.4em;
  font-weight: bold;
  color: #e6a23c;
}
.price-value {
  font-size: 1.8em;
  color: #e6a23c;
}
/* 评分组件对齐 */
.rating-display {
  vertical-align: middle;
}

/* 报名按钮部分 */
.enroll-button-section {
  margin-top: 30px;
  text-align: center; /* Center the button */
}

.enroll-button-section .el-button {
  padding: 12px 30px; /* Larger button */
  font-size: 1.2em; /* Larger text */
  border-radius: 30px; /* More rounded button */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1); /* Add shadow to button */
  transition: all 0.3s ease;
}

.enroll-button-section .el-button:hover {
  transform: translateY(-3px); /* Lift effect on hover */
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
}

.enroll-disabled-tip {
  color: #909399;
  font-size: 0.9em;
  margin-top: 10px;
}

/* 分隔线样式 */
.section-divider {
  margin: 40px 0;
  border-top: 2px dashed #ebeef5;
}

/* 各个内容区块的标题样式 */
.section-title {
  font-size: 1.8em;
  color: #303133;
  margin-bottom: 25px;
  border-left: 5px solid #4cb1a3;
  padding-left: 10px;
  font-weight: bold;
}

/* 详细描述文本样式 */
.detailed-description-text {
  font-size: 1.05em;
  line-height: 1.9;
  color: #303133;
  margin-bottom: 20px;
  text-align: justify;
  white-space: pre-wrap;
}
/* 占位符文本样式 */
.placeholder-text {
  color: #909399;
  font-style: italic;
  text-align: center;
  padding: 20px;
  border: 1px dashed #e4e7ed;
  border-radius: 5px;
  margin-top: 20px;
}

/* 行程区样式 */
.itinerary-section {
  padding: 15px 0;
}

/* 时间轴时间戳样式 */
.el-timeline-item__timestamp {
  font-size: 1.1em;
  color: #4bb6a8;
  font-weight: bold;
}

/* 时间轴中的卡片样式 */
.itinerary-card {
  margin-top: 5px;
  margin-left: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  border: none;
}
.itinerary-card h4 {
  font-size: 1.4em;
  color: #303133;
  margin-bottom: 10px;
  font-weight: bold;
}
.route-description {
  font-size: 1em;
  line-height: 1.7;
  color: #606266;
  margin-bottom: 15px;
}

/* 景点标签列表 */
.spot-list {
  margin-top: 10px;
}
.spot-list h5 {
  font-size: 1.05em;
  color: #303133;
  margin-bottom: 10px;
}
.spot-tag {
  margin-right: 8px;
  margin-bottom: 8px;
  border-radius: 4px;
  height: 32px;
  line-height: 30px;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .travel-group-detail-container {
    padding: 20px;
  }
  .main-info-section .el-col {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
  }
  .detail-info {
    padding-left: 0;
    margin-top: 20px;
  }
  .group-title {
    font-size: 2em;
  }
  .group-image-single, .group-carousel {
    height: 300px;
  }
  .back-button {
    top: 15px;
    left: 15px;
  }
}
</style>