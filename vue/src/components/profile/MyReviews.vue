<template>
  <div class="my-reviews-container">
    <div class="review-filters">
      <span class="filter-label">评价状态:</span>
      <el-radio-group v-model="reviewStatus" size="default">
        <el-radio-button label="PENDING">待评价</el-radio-button>
        <el-radio-button label="REVIEWED">已评价</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="reviews.length > 0" class="card-grid">
      <el-card
        v-for="review in reviews"
        :key="review.id"
        class="review-card hover-card"
      >
        <div class="review-card-content">
          <img v-if="review.packageCoverImageUrl" :src="review.packageCoverImageUrl" class="review-package-image" alt="旅行团封面" />
          <div class="review-info">
            <h3 class="tour-title">{{ review.tourTitle }}</h3>
            <el-rate
              v-if="review.hasReviewed"
              v-model="review.rating"
              disabled
              show-text
              :texts="['很差', '差', '一般', '好', '很好']"
              class="review-rating"
            />
            <p v-else class="pending-review-prompt">等待您的评价</p>

            <p class="review-comment">{{ review.comment }}</p>
            <p class="review-date">订单日期：{{ review.date }}</p>
          </div>
        </div>
        <div class="review-actions">
          <el-button v-if="!review.hasReviewed" type="primary" size="small" @click="goToReview(review.packageId)">去评价</el-button>
          <el-button v-else type="info" size="small" @click="viewReview(review.orderId)">查看评价</el-button>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="暂无相关评价订单"></el-empty>

    <div v-if="!noMoreReviews && !reviewLoading && totalReviews > 0" class="load-more-container">
      <el-button @click="fetchReviews(false)" type="primary" plain class="load-more-btn">
        加载更多
      </el-button>
    </div>
    <div v-if="reviewLoading" class="load-more-container">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon> <span>加载中...</span>
    </div>
    <p v-if="noMoreReviews && reviews.length > 0" class="no-more-text">所有订单已加载完毕</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElCard, ElRate, ElEmpty, ElButton, ElRadioGroup, ElRadioButton, ElIcon } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';
import { useRouter } from 'vue-router';

const router = useRouter();

// 我的评价
const reviews = ref([]); // 用于存储评价（或待评价订单）数据的响应式引用
const reviewStatus = ref('PENDING'); // 控制评价状态筛选: 'PENDING' (待评价), 'REVIEWED' (已评价), 'ALL' (全部)
const currentReviewPage = ref(1); // 评价列表当前页码，从1开始
const reviewPageSize = ref(6); // 评价列表每页记录数
const totalReviews = ref(0); // 评价总数
const reviewLoading = ref(false); // 评价加载状态
const noMoreReviews = computed(() => {
  return reviews.value.length >= totalReviews.value && totalReviews.value > 0;
});

// 获取评价订单列表函数
const fetchReviews = async (reset = false) => {
  if (reviewLoading.value) return;
  if (noMoreReviews.value && !reset) {
    ElMessage.info('没有更多相关订单了。');
    return;
  }

  reviewLoading.value = true;
  if (reset) {
    currentReviewPage.value = 1;
    reviews.value = [];
  }

  try {
    const response = await authAxios.get('/user/orders/for-review', {
      params: {
        status: reviewStatus.value,
        page: currentReviewPage.value,
        size: reviewPageSize.value,
        sortBy: 'orderTime',
        sortDirection: 'DESC',
      },
    });

    console.log('获取评价列表',response)

    if (response.data.code === 200 && response.data.data) {
      const newOrders = response.data.data.content;
      totalReviews.value = response.data.data.totalElements;

      const mappedOrders = newOrders.map(order => ({
        packageId:order.packageId,
        orderId: order.orderId,
        tourTitle: order.packageTitle,
        packageCoverImageUrl: order.packageCoverImageUrl,
        rating: order.status === 'REVIEWED' ? 5 : 0, // 占位符：已评价默认5星
        comment: order.hasReviewed ? '您已对该订单进行评价。' : '该订单等待您的评价。',
        date: new Date(order.orderTime).toLocaleDateString(),
        orderStatus: order.status,
        content:order.content,
        hasReviewed:order.hasReviewed,
      }));

      if (reset) {
        reviews.value = mappedOrders;
      } else {
        reviews.value = [...reviews.value, ...mappedOrders];
      }

      currentReviewPage.value++;
    } else {
      ElMessage.warning(response.data.message || '未能获取订单数据。');
      if (reset) reviews.value = [];
    }
  } catch (error) {
    console.error('获取订单评价列表失败:', error);
    ElMessage.error('获取订单评价列表失败，请检查网络或稍后再试。');
    if (reset) reviews.value = [];
  } finally {
    reviewLoading.value = false;
  }
};

// 处理“去评价”点击事件
const goToReview = (packageId) => {
  router.push({ name: 'SubmitReviewPage', params: { packageId: packageId }});
  console.log('跳转至去评价页面，传入的参数',router)
};

// 处理“查看评价”点击事件**
const viewReview = (orderId) => {
  router.push({ name: 'ViewReviewPage', params: { packageCommentId: orderId } });
  console.log('跳转至查看评价页面，传入的参数',router)
};

// --- 生命周期钩子 ---
onMounted(() => {
  fetchReviews(true); // 组件挂载时立即获取评价数据
});

// --- 监听 reviewStatus 变化，重新获取评价数据 ---
watch(reviewStatus, () => {
  fetchReviews(true); // 筛选条件改变时，重置并重新获取数据
});
</script>

<style scoped>
.my-reviews-container {
  /* 可以添加一些容器样式，比如背景色、内边距等 */
  display: flex;
  flex-direction: column;
  height: 100%; /* 确保组件能够填充父容器 */
}

/* 评价筛选器样式 */
.review-filters {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px; /* 筛选器下方间距 */
  background-color: #f8f8f8;
  padding: 15px 20px;
  border-radius: 12px;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.05); /* 内阴影 */
  border: 1px solid #eee;
}

.filter-label {
  font-size: 1.05rem;
  color: #333;
  font-weight: 600;
  flex-shrink: 0;
}

/* 卡片网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); /* 调整卡片最小宽度 */
  gap: 20px;
  flex-grow: 1; /* 让网格占据剩余空间 */
}

/* 评价卡片样式 */
.review-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  border-radius: 14px;
  overflow: hidden;
  padding: 16px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: 1px solid #ebebeb;
  justify-content: space-between;
}

.hover-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(64, 255, 223, 0.15);
  border-color: #54c4b5;
}

.review-card-content {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  flex-grow: 1;
}

.review-package-image {
  width: 110px;
  height: 90px;
  object-fit: cover;
  border-radius: 10px;
  margin-right: 18px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.review-info {
  flex-grow: 1;
  text-align: left;
}

.review-info .tour-title {
  margin: 0;
  font-size: 1.15rem;
  color: #333;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 5px;
}

.review-rating {
  margin-bottom: 5px;
}

.pending-review-prompt {
  font-size: 0.95rem;
  color: #f56c6c;
  font-weight: 500;
  margin-bottom: 5px;
}

.review-comment {
  margin: 5px 0 0;
  color: #666;
  font-size: 0.95rem;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.review-date {
  font-size: 0.85rem;
  color: #999;
  margin-top: 8px;
  text-align: right;
  width: 100%;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.review-actions .el-button {
  padding: 8px 15px;
  font-size: 0.9rem;
  border-radius: 6px;
}

/* 加载更多样式 */
.load-more-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  gap: 10px;
  color: #666;
  font-size: 1rem;
}

.load-more-btn {
  width: 200px;
  padding: 12px 20px;
  font-size: 1rem;
  font-weight: 500;
  border-radius: 8px;
}

.loading-icon {
  font-size: 1.2rem;
}

.no-more-text {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 15px;
}
</style>