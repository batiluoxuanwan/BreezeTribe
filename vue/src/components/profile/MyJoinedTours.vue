<template>
<div class="joined-tours-display">
    <div v-if="joinedTours.length > 0" class="card-grid">
        <el-card
          v-for="tour in joinedTours"
          :key="tour.id"
          class="tour-card hover-card"
          @click="goToTourDetail(tour.id)" >
          <img :src="tour.image" class="card-img" alt="旅行团图片" />
          <div class="card-info">
            <h3>{{ tour.title }}</h3>
            <p>出发日期：{{ tour.date }}</p>
            <p class="progress-text">当前状态：{{ tour.progressText }}</p>
            <div class="progress-row">
              <div class="payment-actions">
                <el-button
                  v-if="tour.showPayButton"
                  type="primary"
                  size="small"
                  @click.stop="confirmPayment(tour.orderId)"
                >
                  去支付
                </el-button>
                <el-button
                  v-if="tour.showCancelButton"
                  type="danger"
                  size="small"
                  @click.stop="cancelOrder(tour.orderId)"
                >
                  取消订单
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无报名"></el-empty>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue' 
import { ElMessageBox, ElCard, ElButton, ElEmpty, ElMessage } from 'element-plus';
import { authAxios } from '@/utils/request';

const joinedTours = ref([]); // 存储我的报名旅行团列表
const joinedTotal = ref(0); // 我的报名总数
const joinedPageSize = 9; // 每页显示数量，你可以根据需要调整
const joinedCurrentPage = ref(1); // 当前页码

const loadingJoinedTours = ref(false); // 专门用于“我的报名”Tab 的加载状态

// --- 获取我的报名（订单） ---
const fetchJoinedTours = async () => {
  loadingJoinedTours.value = true;
  try {
    const response = await authAxios.get('/user/orders', {
      params: {
        page: joinedCurrentPage.value,
        size: joinedPageSize,
      },
    });

    if (response.data.code === 200) {
      joinedTours.value = response.data.data.content.map(order => {
        let progress = 0;
        let tourDate = ''; 
        let progressText = ''; 
        let showPayButton = false; // 支付按钮
        let showCancelButton = false; //取消按钮

        // 设置进度和状态文字
        switch (order.status) {
          case 'PENDING_PAYMENT': // 待支付
            progress = 20;
            progressText = '待支付';
            showPayButton = true; 
            showCancelButton = true;
            tourDate = new Date(order.orderTime).toLocaleDateString(); 
            break;
          case 'PAID': // 已支付（可以认为是已确认，即将出发）
            progress = 50;
            progressText = '已支付 (即将出发)';
            showCancelButton = true;
            tourDate = new Date(order.orderTime).toLocaleDateString();
            break;
          case 'ONGOING': // 正在进行
            progress = 75; // 可以调整进度条值
            progressText = '旅行中';
            tourDate = new Date(order.orderTime).toLocaleDateString();
            break;
          case 'COMPLETED': // 已完成
            progress = 100;
            progressText = '已完成';
            tourDate = new Date(order.orderTime).toLocaleDateString();
            break;
          case 'CANCELED': // 已取消
            progress = 0; // 或者一个表示取消的特定值
            progressText = '已取消';
            tourDate = new Date(order.orderTime).toLocaleDateString();
            break;
          default:
            progress = 0;
            progressText = '未知状态';
            tourDate = new Date(order.orderTime).toLocaleDateString();
        }

        return {
          orderId: order.orderId,
          image: order.packageCoverImageUrl,
          title: order.packageTitle,
          date: tourDate,
          progress: progress,
          progressText: progressText, 
          packageId: order.packageId,
          status: order.status,
          travelerCount: order.travelerCount,
          totalPrice: order.totalPrice,
          orderTime: order.orderTime,
          showPayButton: showPayButton,
          showCancelButton:showCancelButton
        };
      });

      joinedTotal.value = response.data.data.totalElements || 0;
    } else {
      ElMessage.error(response.data.message || '获取我的报名数据失败！');
      joinedTours.value = [];
      joinedTotal.value = 0;
    }
  } catch (error) {
    console.error('获取我的报名失败:', error);
    ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    joinedTours.value = [];
    joinedTotal.value = 0;
  } finally {
    loadingJoinedTours.value = false;
  }
};

//支付订单
const confirmPayment = async (orderId) => {
  try {
    const response = await authAxios.post(`/user/orders/${orderId}/confirm-payment`);
    if (response.data.code === 200) {
      ElMessage.success('支付成功！');
      fetchJoinedTours(); 
    } else {
      ElMessage.error(response.data.message || '支付失败');
    }
  } catch (error) {
    console.error('支付接口调用失败:', error);
    ElMessage.error('网络错误，请稍后再试');
  }
};

// 取消订单
const cancelOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('您确定要取消这笔订单吗？取消后将无法恢复。', '确认取消', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    const response = await authAxios.post(`/user/orders/${orderId}/cancel`);

    if (response.data.code === 200) { 
      ElMessage.success('订单已成功取消！');
      fetchJoinedTours();
    } else {
      ElMessage.error(response.data.message || '取消订单失败，请稍后再试。');
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消订单操作。');
    } else {
      console.error('取消订单时发生错误:', error);
      if (error.response) {
        ElMessage.error(`取消订单失败：${error.response.data.message || '服务器错误'}`);
      } else {
        ElMessage.error('取消订单失败：网络错误或服务器无响应');
      }
    }
  }
};

//跳转旅行团详情
const goToTourDetail = (id) => {
  router.push({ name: 'TravelGroupDetail', params: { id } });
};

onMounted(() => {
  fetchJoinedTours();
});
</script>

<style>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 10px; 
}

.tour-card {
  display: flex;
  align-items: center;
  border-radius: 14px;
  overflow: hidden;
  padding: 16px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: 1px solid #ebebeb;
}

.hover-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(64, 255, 223, 0.15);
  border-color: #54c4b5;
}

.card-img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
  margin-right: 20px;
  flex-shrink: 0;
}

.card-info {
  flex-grow: 1;
  text-align: left;
}

.progress-row {
  display: flex;
  gap: 135px;
  align-items: center;
}
.progress-text {
  margin: 0;
}

.payment-actions {
  display: flex;
  gap: 8px;
}
</style>