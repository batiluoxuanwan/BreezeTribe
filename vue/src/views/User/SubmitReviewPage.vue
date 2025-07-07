<template>
  <div class="package-review-page">
    <el-skeleton :loading="loading" animated class="review-skeleton" v-if="loading">
      <template #template>
        <el-skeleton-item variant="rect" style="width: 100%; height: 60px; margin-bottom: 20px;" />
        <div style="padding: 10px;">
          <el-skeleton-item variant="image" style="width: 120px; height: 90px; margin-right: 20px;" />
          <el-skeleton-item variant="text" style="width: 70%;" />
          <el-skeleton-item variant="text" style="width: 50%;" />
          <el-skeleton-item variant="text" style="width: 80%; margin-top: 20px;" />
          <el-skeleton-item variant="text" style="width: 90%; height: 100px;" />
          <el-skeleton-item variant="button" style="width: 100px; margin-right: 10px;" />
          <el-skeleton-item variant="button" style="width: 100px;" />
        </div>
      </template>
    </el-skeleton>

    <el-card class="review-form-card" v-else>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="goBack" class="back-button"></el-button>
          <span>
            {{ isViewMode ? '查看旅行团评价' : '评价旅行团' }}
          </span>
          <span class="tour-title-header">{{ packageDetail.packageTitle || '加载中...' }}</span>
          <el-button
            v-if="isViewMode"
            type="danger"
            :icon="Delete"
            circle
            @click="deleteReview"
            class="delete-button"
            title="删除评价"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </template>

      <div class="package-info" v-if="packageDetail.packageTitle">
        <img v-if="packageDetail.packageCoverImageUrl" :src="packageDetail.packageCoverImageUrl" class="package-cover-image" alt="旅行团封面" />
        <div class="package-text-info">
          <h3>旅行团：{{ packageDetail.packageTitle }}</h3>
          <p v-if="packageDetail.packageId">旅行团ID：{{ packageDetail.packageId }}</p>
          <p v-if="isViewMode && reviewDetail.createdTime">评价日期：{{ formatReviewDate(reviewDetail.createdTime) }}</p>
          <p v-if="isViewMode && reviewDetail.author">评价人：{{ reviewDetail.author.username }}</p>
        </div>
      </div>
      <el-alert v-else :title="errorMessage || '未找到旅行团或评价信息。'" type="warning" show-icon :closable="false" />

      <el-form :model="reviewForm" :rules="rules" ref="reviewFormRef" label-position="top" class="review-form" v-if="packageDetail.packageTitle && !errorMessage">
        <el-form-item label="您的评分：" prop="rating">
          <el-rate
            v-model="reviewForm.rating"
            :max="5"
            show-text
            :texts="['很差', '差', '一般', '好', '很好']"
            :disabled="isViewMode" /> </el-form-item>

        <el-form-item label="您的评价：" prop="comment">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="5"
            :placeholder="isViewMode ? '无评价内容' : '请分享您的旅行体验，例如行程安排、服务质量、导游表现等...'"
            maxlength="500"
            show-word-limit
            :disabled="isViewMode" /> </el-form-item>

        <el-form-item v-if="!isViewMode"> <el-button type="primary" :loading="submitting" @click="submitReview">
            提交评价
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
        <el-form-item v-else> <el-button type="info" @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElCard, ElButton, ElForm, ElFormItem, ElInput, ElRate, ElSkeleton, ElSkeletonItem, ElAlert ,ElMessageBox} from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { authAxios, publicAxios } from '@/utils/request'; // 确保你导出了 publicAxios 用于公共接口请求

const route = useRoute();
const router = useRouter();

// 路由参数：
// packageId 用于提交新评价的旅行团ID
// packageCommentId 用于查看已有评价的评论ID
const packageId = ref(route.params.packageId);
const packageCommentId = ref(route.params.commentId);

const reviewFormRef = ref(null); // 表单引用

const loading = ref(true); // 加载状态
const submitting = ref(false); // 提交加载状态
const errorMessage = ref(''); // 错误信息

const reviewDetail = ref({}); // 存储从后端获取的评价详情（用于查看模式）
const packageDetail = ref({}); // 存储旅行团的基本信息（标题、封面图等）

// reviewForm 用于表单绑定，它的值会在获取到评价后被填充（查看模式），或供用户输入（发布模式）
const reviewForm = reactive({
  rating: 0,
  comment: '', // 对应 API 的 'content' 字段
});

// 计算属性，判断当前是“查看模式”还是“发布模式”
// 如果 packageCommentId 存在，则为查看模式；否则为发布模式
const isViewMode = computed(() => !!packageCommentId.value);

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

// --- 格式化评价日期 ---
const formatReviewDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
};

// --- 返回上一页 ---
const goBack = () => {
  router.push({ name: '用户个人主页', query: { tab: 'reviews' } }); 
};

// --- 重置表单 (仅在发布模式下可用) ---
const resetForm = () => {
  if (reviewFormRef.value) {
    reviewFormRef.value.resetFields();
    reviewForm.rating = 0; // 手动重置评分
  }
};

// --- 获取数据：评价详情（查看模式）或旅行团基本信息（发布模式） ---
const fetchData = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    if (isViewMode.value) {
      // --- 查看模式：获取已有评价详情 ---
      const response = await publicAxios.get(`/public/packages/comments/detail/${packageCommentId.value}`);
      if (response.data.code === 200 && response.data.data) {
        reviewDetail.value = response.data.data;
        // 填充表单字段以供显示
        reviewForm.rating = reviewDetail.value.rating;
        reviewForm.comment = reviewDetail.value.content;

        // 从评价详情中获取旅行团的基本信息
        packageDetail.value = {
          packageId: reviewDetail.value.packageId,
          packageTitle: reviewDetail.value.packageTitle,
        };

      } else {
        errorMessage.value = response.data.message || '获取评价详情失败。';
      }
    } else {
      // --- 发布模式：获取旅行团基本信息（供用户评价时参考） ---
      const response = await publicAxios.get(`/public/travel-packages/${packageId.value}`); 
      if (response.data.code === 200 && response.data.data) {
        packageDetail.value = {
          packageId: response.data.data.id,
          packageTitle: response.data.data.title,
          packageCoverImageUrl: response.data.data.imageUrls[0],
        };
        // 在发布模式下，初始时 reviewForm 保持空或默认值
        reviewForm.rating = 0;
        reviewForm.comment = '';
      } else {
        errorMessage.value = response.data.message || '获取旅行团详情失败。';
      }
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    errorMessage.value = `网络请求失败: ${error.message || '请检查网络或稍后再试。'}`;
  } finally {
    loading.value = false;
  }
};

// --- 提交评价 ---
const submitReview = async () => {
  if (!reviewFormRef.value) return;

  await reviewFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        // 确保 packageId 在发布模式下可用
        if (!packageId.value) {
          ElMessage.error('无法获取旅行团ID，请刷新页面重试。');
          submitting.value = false;
          return;
        }

        const response = await authAxios.post(`/user/packages/comments/${packageId.value}`, {
          rating: reviewForm.rating,
          content: reviewForm.comment, // 将 reviewForm.comment 映射到 API 的 'content' 字段
          parentId: null // 对于新的顶层评价，parentId 为 null
        });

        if (response.data.code === 200) {
          ElMessage.success('评价提交成功！感谢您的宝贵反馈。');
          // 跳转回用户“我的评价”列表页
          router.push({ name: '用户个人主页', query: { tab: 'reviews' } });
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

// 删除评价
const deleteReview = async () => {
  if (!packageCommentId.value) {
    ElMessage.error('无法获取评价ID，请刷新页面重试。');
    return;
  }

  try {
    // 弹出确认框
    await ElMessageBox.confirm('此操作将永久删除该评价, 是否继续?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    // 用户确认删除
    loading.value = true; // 删除过程中显示加载状态
    const response = await authAxios.delete(`/user/packages/comments/package-comments/${packageCommentId.value}`);

    if (response.data.code === 200) {
      ElMessage.success('评价删除成功！');
      // 成功删除后，跳转回用户的“我的评价”列表页
      router.push({ name: '用户个人主页', query: { tab: 'reviews' } });
    } else {
      ElMessage.error(response.data.message || '评价删除失败，请重试。');
    }
  } catch (error) {
    // 如果用户点击取消，会抛出 'cancel' 字符串
    if (error === 'cancel') {
      ElMessage.info('已取消删除操作。');
    } else {
      console.error('删除评价失败:', error);
      ElMessage.error(`删除评价失败: ${error.message || '请检查网络或稍后再试。'}`);
    }
  } finally {
    loading.value = false;
  }
};

// 组件挂载时触发数据加载
onMounted(() => {
  // 如果既没有 packageId 也没有 packageCommentId，则无法加载页面
  if (!packageId.value && !packageCommentId.value) {
    errorMessage.value = '无效的旅行团或评价ID，无法加载页面。';
    loading.value = false;
    return;
  }
  fetchData();
});
</script>

<style scoped>
.package-review-page { /* 页面根元素类名更改 */
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px;
  min-height: calc(100vh - 80px);
  background-color: #f5f7fa;
}

.review-form-card {
  width: 100%;
  max-width: 700px;
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  padding: 20px 30px;
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
  margin-left: 15px;
}

.tour-title-header {
  flex-grow: 1;
  text-align: right;
  font-size: 1.2rem;
  color: #606266;
  font-weight: normal;
}

.delete-button {
  margin-left: 15px; 
  background-color: #F56C6C; 
  border-color: #F56C6C;
  color: #fff;
}

.delete-button:hover {
  background-color: #E6A23C; /* 悬停时颜色略深 */
  border-color: #E6A23C;
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

.package-info { /* 类名从 .order-details 更改 */
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

.package-cover-image { /* 类名从 .order-cover-image 更改 */
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.package-text-info h3 { /* 类名从 .order-text-info 更改 */
  margin: 0 0 8px 0;
  font-size: 1.3rem;
  color: #333;
  font-weight: 600;
}

.package-text-info p { /* 类名从 .order-text-info 更改 */
  margin: 4px 0;
  font-size: 0.9rem;
  color: #666;
}

.review-form {
  padding-top: 20px;
}

.el-form-item {
  margin-bottom: 25px;
}

.el-form-item__label {
  font-size: 1rem !important;
  font-weight: bold;
  color: #555;
  margin-bottom: 8px !important;
}

.el-rate {
  margin-top: 5px;
}

.el-textarea__inner {
  min-height: 120px !important;
  line-height: 1.6;
  font-size: 0.95rem;
}

.el-button {
  padding: 12px 25px;
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

.review-skeleton {
  width: 100%;
  max-width: 700px;
  padding: 20px;
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  background-color: #fff;
}
</style>