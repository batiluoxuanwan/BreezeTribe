<template>
  <div class="travel-tour-card">
    <div class="image-container">
      <img :src="tour.coverImageUrl" :alt="tour.title" class="tour-image" />
      <div v-if="tour.isHot" class="hot-tag">热门</div>
    </div>

    <div class="tour-content">
      <h3 class="tour-title">{{ tour.title }}</h3>
      <!-- 固定在卡片底部 -->
      <div class="card-footer-fixed">
        <div class="tour-meta">
          <span class="duration">{{ tour.durationInDays }}天</span>
          <span class="departure-date">出发日期: {{ tour.startDate }}</span>
        </div>
        <div class="tour-price-wrapper">
          <span class="tour-price">¥{{ tour.price }}</span>
          <span class="price-unit">起</span>
        </div>
        <el-button type="primary" size="small" class="view-details-button" @click.stop="goToDetail">
          查看详情 
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';
import { ElButton } from 'element-plus'; // 确保导入 ElButton
import {useRouter} from 'vue-router';

const router = useRouter();

const props = defineProps({
  tour: {
    type: Object,
    required: true
  }
});

// 跳转详情页并传递旅行团id
const goToDetail = () => {
  console.log('查看旅行团详情:', props.tour.id);
  router.push({ name: 'TravelGroupDetail', params: { id: props.tour.id } });
};

</script>

<style scoped>
.travel-tour-card {
  width: 240px; /* 稍微增加宽度 */
  background: #ffffff;
  border-radius: 12px; /* 更圆润的边角 */
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); /* 更明显的阴影 */
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
  cursor: pointer;
}

.travel-tour-card:hover {
  transform: translateY(-8px); /* 悬停时向上浮动 */
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2); /* 悬停时阴影更深 */
}

.image-container {
  width: 100%;
  height: 150px; /* 增加图片高度 */
  overflow: hidden;
  position: relative;
}

.tour-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease; /* 悬停时图片缩放效果 */
}

.travel-tour-card:hover .tour-image {
  transform: scale(1.08); /* 悬停时图片轻微放大 */
}

.rating-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(255, 193, 7, 0.9); /* 亮黄色，略带透明 */
  color: white;
  padding: 4px 8px;
  border-radius: 5px;
  font-size: 0.85em;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
  z-index: 2;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.hot-tag {
  position: absolute;
  top: 10px;
  left: 0;
  background: linear-gradient(to right, #ff7e5f, #feb47b); /* 渐变色背景 */
  color: white;
  padding: 4px 10px;
  border-radius: 0 8px 8px 0;
  font-size: 0.8em;
  font-weight: 700;
  z-index: 2;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.2);
}

.tour-content {
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px; /* 增加内容间距 */
  flex-grow: 1;
}

.tour-title {
  font-size: 1.15rem; /* 标题字号略大 */
  color: #333;
  font-weight: 700; /* 加粗 */
  line-height: 1.3;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 标题限制为2行 */
  -webkit-box-orient: vertical;
}

.tour-location {
  font-size: 0.95rem; /* 地点信息字号 */
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.card-footer-fixed {
  margin-top: auto;
  display: flex;
  flex-direction: column; 
  width: 100%; 
  gap: 10px; 
  padding-top: 10px; 
  border-top: 1px solid #eee; 
}

.tour-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #888;
  margin-top: 5px;
}

.duration {
  background-color: rgb(229, 244, 242); /* Element Plus light blue */
  color: rgb(102, 162, 156);
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.departure-date {
  color: #909399;
}

.tour-price-wrapper {
  display: flex;
  align-items: baseline;
  margin-top: 10px; /* 与上方的间距 */
  margin-bottom: 10px;
}

.tour-price {
  font-size: 1.8rem; /* 价格字号更大 */
  color: #ff5722; /* 醒目的橙红色 */
  font-weight: 800; /* 超粗字体 */
  line-height: 1; /* 确保价格垂直居中 */
}

.price-unit {
  font-size: 0.9em;
  color: #ff5722;
  margin-left: 3px;
  font-weight: 600;
}

.view-details-button {
  width: 100%;
  background-color: #4caf91; 
  border-color: #4caf91;
  font-weight: 600;
  transition: all 0.3s ease;
}

/* 悬停 */
.view-details-button:hover {
  background-color: #35daa8; 
  border-color: #35daa8;
  transform: translateY(-2px);
}
</style>