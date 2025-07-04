<template>
  <el-card class="travel-card group-card" shadow="hover">
    <div class="image-wrapper">
      <img :src="group.coverImageUrl" class="card-image" alt="旅行团图片" />
      <el-tag v-if="group.isHot" type="danger" size="small" class="card-badge hot-badge">Hot</el-tag>
      <el-tag v-if="group.isNew" type="success" size="small" class="card-badge new-badge">New</el-tag>
    </div>
    <div class="card-content">
      <h3 class="card-title">{{ group.title }}</h3>
      <p class="card-price">¥{{ group.price }} 起</p>
      <div class="card-info">
        <span>时长: {{ group.durationInDays }}天</span>
        <!-- <span>人数: {{ group.members }}人</span> -->
        <span>评分: {{ group.rating }}</span>
      </div>
      <el-button type="primary" size="small" class="detail-button" @click.stop="goToDetail">查看详情</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { defineProps } from 'vue';
import { ElCard, ElButton, ElTag } from 'element-plus'; 
import {useRouter} from 'vue-router';

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
});

const router = useRouter();

// 跳转详情页并传递旅行团id
const goToDetail = () => {
  console.log('查看旅行团详情:', props.group.id);
  router.push({ name: 'TravelGroupDetail', params: { id: props.group.id } });
};

</script>

<style scoped>
.travel-card {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}
.travel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden; /* 隐藏图片放大超出部分 */
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease; /* 添加过渡效果 */
}

.travel-card:hover .card-image {
  transform: scale(1.05); /* 鼠标悬停时图片轻微放大 */
}

.card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 0.8em;
  font-weight: bold;
  z-index: 10; /* 确保在图片上方 */
}

.card-badge.hot-badge {
  background-color: #ee9a9a; 
  color: white;
}
.card-badge.new-badge {
  background-color: #80d8a4; 
  color: white;
}

.card-content {
  padding: 15px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}
.card-title {
  font-size: 1.2em;
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
  line-height: 1.4;
}
.card-meta {
  font-size: 0.9em;
  color: #666;
  margin-bottom: 5px;
}
.card-price {
  font-size: 1.4em;
  color: #ff5722;
  font-weight: bold;
  margin-bottom: 10px;
}
.card-info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.85em;
  color: #999;
  margin-top: auto;
  margin-bottom: 10px;
}
.detail-button {
  width: 100%;
  margin-top: 10px;
}
</style>