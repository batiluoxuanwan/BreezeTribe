<template>
  <div class="submit-review-page">
    <el-card class="review-form-card">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="goBack" class="back-button"></el-button>
          <span>评价订单：**{{ orderId }}**</span>
        </div>
      </template>

      <div v-if="orderInfo.packageTitle" class="order-details">
        <img :src="orderInfo.packageCoverImageUrl" alt="旅行团封面" class="order-cover-image">
        <div class="order-text-info">
          <h3>{{ orderInfo.packageTitle }}</h3>
          <p class="order-time">订单日期：{{ new Date(orderInfo.orderTime).toLocaleDateString() }}</p>
          <p class="departure-time">出发时间：{{ new Date(orderInfo.departureTime).toLocaleDateString() }}</p>
        </div>
      </div>
      <el-skeleton v-else :rows="3" animated></el-skeleton>


      <el-form :model="reviewForm" :rules="rules" ref="reviewFormRef" label-position="top" class="review-form">
        <el-form-item label="您的评分：" prop="rating">
          <el-rate v-model="reviewForm.rating" :max="5" show-text :texts="['很差', '差', '一般', '好', '很好']" />
        </el-form-item>

        <el-form-item label="您的评价：" prop="comment">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="5"
            placeholder="请分享您的旅行体验，例如行程安排、服务质量、导游表现等..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitReview">
            提交评价
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElCard, ElButton, ElForm, ElFormItem, ElInput, ElRate, ElIcon, ElSkeleton } from 'element-plus';
import { ArrowLeft, Plus } from '@element-plus/icons-vue'; // 引入图标
import { authAxios } from '@/utils/request'; // 假设你的认证请求实例

const route = useRoute();
const router = useRouter();

const orderId = ref(route.params.orderId); // 从路由参数获取订单ID
const reviewFormRef = ref(null); // 表单引用

const reviewForm = reactive({
  rating: 0, // 评分，默认为0
  comment: '', // 评论内容
  // imageUrls: [] // 如果需要图片上传
});

const orderInfo = ref({}); // 存储订单详情，用于显示在页面上
const submitting = ref(false); // 提交加载状态

// 表单验证规则
const rules = reactive({
  rating: [
    { type: 'number', min: 1, message: '请选择至少一颗星进行评分', trigger: 'change' }
  ],
  comment: [
    { required: true, message: '评价内容不能为空', trigger: 'blur' },
    { min: 5, message: '评价内容至少需要5个字符', trigger: 'blur' }
  ]
});

// 返回上一页
const goBack = () => {
  router.go(-1); // 返回上一个历史记录页面
  // 或者 router.push({ name: 'UserProfilePage', query: { tab: 'reviews' } });
};

// 获取订单详情（用于展示）
const fetchOrderDetails = async () => {
  if (!orderId.value) {
    ElMessage.error('订单ID缺失，无法加载详情。');
    return;
  }
  try {
    // 假设你有一个获取单个订单详情的API
    // 注意：这个API可能需要你自己根据后端实际情况调整
    const response = await authAxios.get(`/api/user/orders/${orderId.value}`);
    if (response.data.code === 200 && response.data.data) {
      orderInfo.value = response.data.data;
    } else {
      ElMessage.error(response.data.message || '获取订单详情失败。');
    }
  } catch (error) {
    console.error('获取订单详情失败:', error);
    ElMessage.error('加载订单详情失败，请稍后再试。');
  }
};


// 提交评价
const submitReview = async () => {
  if (!reviewFormRef.value) return;

  await reviewFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        // 假设你的评价提交API是 POST /api/reviews
        // 并且需要 orderId, rating, comment 等字段
        const response = await authAxios.post('/api/reviews', {
          orderId: orderId.value,
          rating: reviewForm.rating,
          comment: reviewForm.comment,
          // imageUrls: reviewForm.imageUrls // 如果有图片
        });

        if (response.data.code === 200) {
          ElMessage.success('评价提交成功！感谢您的宝贵反馈。');
          // 提交成功后返回我的评价页面
          router.push({ name: 'UserProfilePage', query: { tab: 'reviews' } });
        } else {
          ElMessage.error(response.data.message || '评价提交失败，请重试。');
        }
      } catch (error) {
        console.error('提交评价失败:', error);
        ElMessage.error('提交评价失败，请检查网络或稍后再试。');
      } finally {
        submitting.value = false;
      }
    } else {
      ElMessage.warning('请检查您的输入，确保评分和评论内容符合要求。');
    }
  });
};

onMounted(() => {
  fetchOrderDetails(); // 页面加载时获取订单详情
});
</script>

<style scoped>
.submit-review-page {
  display: flex;
  justify-content: center;
  align-items: flex-start; /* 调整为flex-start，让内容从顶部开始 */
  padding: 40px;
  min-height: calc(100vh - 80px); /* 假设头部有高度 */
  background-color: #f5f7fa; /* 稍微深一点的背景色 */
}

.review-form-card {
  width: 100%;
  max-width: 700px; /* 控制卡片最大宽度 */
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1); /* 更明显的阴影 */
  padding: 20px 30px; /* 增加内边距 */
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.card-header span {
  margin-left: 15px; /* 标题与按钮的间距 */
}

.back-button {
  color: #606266;
  border-color: #dcdfe6;
}

.back-button:hover {
  background-color: #f2f6fc;
  border-color: #c6e2ff;
  color: #409eff;
}

.order-details {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  margin-bottom: 30px;
  background-color: #f9f9f9;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  box-shadow: inset 0 1px 5px rgba(0,0,0,0.03);
}

.order-cover-image {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.order-text-info h3 {
  margin: 0 0 8px 0;
  font-size: 1.3rem;
  color: #333;
  font-weight: 600;
}

.order-text-info p {
  margin: 4px 0;
  font-size: 0.9rem;
  color: #666;
}

.review-form {
  padding-top: 20px;
}

.el-form-item {
  margin-bottom: 25px; /* 增大表单项间距 */
}

.el-form-item__label {
  font-size: 1rem !important; /* 确保label字体大小 */
  font-weight: bold;
  color: #555;
  margin-bottom: 8px !important; /* label和输入框的间距 */
}

.el-rate {
  margin-top: 5px; /* 评分组件与label的间距 */
}

.el-textarea__inner {
  min-height: 120px !important; /* 增大文本域高度 */
  line-height: 1.6;
  font-size: 0.95rem;
}

.el-button {
  padding: 12px 25px; /* 按钮内边距 */
  font-size: 1rem;
  border-radius: 8px;
  font-weight: 500;
}

.el-button--primary {
  background-color: #4fc0b5;
  border-color: #4fc0b5;
}

.el-button--primary:hover {
  background-color: #3aa89f;
  border-color: #3aa89f;
}

/* 骨架屏样式微调 */
.el-skeleton {
  margin-bottom: 30px;
  padding: 15px;
  border-radius: 12px;
  background-color: #fefefe;
  border: 1px solid #f0f0f0;
}
</style>