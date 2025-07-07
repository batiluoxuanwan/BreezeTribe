<template>
<div class="collected-tours-display">
    <div v-if="collectedTours.length > 0" class="card-grid">
        <el-card
          v-for="tour in collectedTours"
          :key="tour.itemid"
          class="tour-card hover-card"
          @click="goToTourDetail(tour.itemid)"
        >
          <img
            v-if="tour.coverImageUrls && tour.coverImageUrls.length > 0"
            :src="tour.coverImageUrls[0]"
            class="card-img"
            alt="收藏图片"
          />
          <div class="card-info">
            <h3>{{ tour.title || '未知旅行团名称' }}</h3>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无收藏"></el-empty>

      <div class="pagination-container" v-if="totalElements > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="totalElements"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>
</template>  

<script setup>
import { onMounted, ref ,reactive} from 'vue'
import { ElCard, ElEmpty, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { authAxios,publicAxios } from '@/utils/request';

const router = useRouter();
const collectedTours = ref([]);

const currentPage = ref(1);
const pageSize = ref(8); // 每页显示图片数量
const totalElements = ref(0);


//获得单个旅行团详情
const fetchTravelPackageDetail = async (id) => {
  try {
    const response = await publicAxios.get(`/public/travel-packages/${id}`);
    if (response.data.code === 200 && response.data.data) {
      return response.data.data;
    } else {
      const errorMessage = response.data.message || '';
      if (response.data.code === 400 && errorMessage.includes('找不到ID为') && errorMessage.includes('尚未发布')) {
        console.warn(`旅行团ID ${id} 已失效或未发布，前端将显示失效状态。`);
        return { isInvalid: true, message: '该旅行团已失效' }; // 返回一个特殊标记
      }
      console.warn(`获取旅行团ID ${id} 详情失败:`, errorMessage);
      return null;
    }
  } catch (error) {
    const actualErrorMessage = error.response?.data?.message;
    //console.log('后端错误消息 (catch block):', actualErrorMessage); 
    if (error.response && error.response.status === 400 && actualErrorMessage &&
        actualErrorMessage.includes('找不到ID为') && actualErrorMessage.includes('尚未发布')) { 
      console.warn(`旅行团ID ${id} 已失效或未发布 (通过错误响应捕获)，前端将显示失效状态。`);
      return { isInvalid: true, message: '该旅行团已失效' }; // 返回一个特殊标记
    }

    // 如果不是我们期望的特定400错误，则继续打印原始错误
    console.error(`获取旅行团ID ${id} 详情时发生错误:`, error);
    return null;
  }
};

//获取用户收藏列表
const fetchCollectedTours = async () => {
  try {
    const response = await authAxios.get('/user/favorites', {
     params: {
      page: currentPage.value,
      size: pageSize.value,
    }
    });

    if (response.data.code === 200 && response.data.data) {
      const basicCollectedItems = response.data.data.content;
      totalElements.value = response.data.data.totalElements;

      const detailedItemsPromises = basicCollectedItems.map(async (item) => {
        if (!item || !item.itemid || !item.itemType) {
          console.warn('发现无效的收藏项，跳过处理:', item);
          return {
            itemid: `invalid-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`, 
            itemType: item?.itemType || 'UNKNOWN',
            title: '无效收藏项',
            coverImageUrls: [],

          };
        }
        if (item.itemType === 'PACKAGE') {
          const packageDetail = await fetchTravelPackageDetail(item.itemid);
          if (packageDetail && packageDetail.isInvalid) {
            return {
              ...item,
              title: packageDetail.message, // 显示“该旅行团已失效”
              coverImageUrls: [], // 清空封面图片，或者可以使用一个默认的失效图片
              isInvalidPackage: true, // 添加一个标记，方便前端UI判断渲染样式
              location: '未知', // 或其他合适的默认值
              price: 0,
              durationInDays: 0,
            }
          }else if (packageDetail) {
            return {
              ...item, 
              title: packageDetail.title,
              coverImageUrls: packageDetail.coverImageUrls,
            };
            } else {
            return {
              ...item,
              title: `[${item.itemType}] ${item.itemid} (详情加载失败)`,
              location: '详情获取失败',
              coverImageUrls: [],
              price: 0,
              durationInDays: 0,
            };
          }
        } else {
          return {
            ...item,
            title: `[${item.itemType}] ${item.itemid} (非旅行团)`,
            location: '非旅行团类型',
            coverImageUrls: [],
            price: 0,
            durationInDays: 0,
          };
        }
      });
      collectedTours.value = await Promise.all(detailedItemsPromises);
    } else {
      ElMessage.error(response.data.message || '获取收藏列表失败。');
    }
  } catch (error) {
    console.error('获取收藏列表或详情时发生错误:', error);
    ElMessage.error('获取收藏列表时发生网络或服务器错误。');
  }
};

//跳转旅行团详情
const goToTourDetail = (id) => {
  router.push({ name: 'TravelGroupDetail', params: { id } });
};

const handlePageChange = (newPage) => {
  currentPage.value = newPage;
  fetchCollectedTours(); 
};

onMounted(() => {
  fetchCollectedTours();
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

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style>