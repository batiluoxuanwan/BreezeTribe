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
    </div>
</template>  

<script setup>
import { onMounted, ref ,reactive} from 'vue'
import { ElCard, ElEmpty, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { authAxios,publicAxios } from '@/utils/request';

const router = useRouter();
const collectedTours = ref([]);

const pagination = reactive({
  pageNumber: 0,
  pageSize: 10,
  totalElements: 0,
  totalPages: 0,
});

const searchParams = reactive({
  keyword: '',
  page: 1,
  size: 10,
  sortBy: 'createdTime',
  sortDirection: 'DESC',
});


//获得单个旅行团详情
const fetchTravelPackageDetail = async (id) => {
  try {
    const response = await publicAxios.get(`/public/travel-packages/${id}`);
    if (response.data.code === 200 && response.data.data) {
      return response.data.data;
    } else {
      console.warn(`获取旅行团ID ${id} 详情失败:`, response.data.message);
      return null;
    }
  } catch (error) {
    console.error(`获取旅行团ID ${id} 详情时发生错误:`, error);
    return null;
  }
};

//获取用户收藏列表
const fetchCollectedTours = async () => {
  try {
    const response = await authAxios.get('/user/favorites', {
      params: {
        page: searchParams.page,
        size: searchParams.size,
      },
    });

    if (response.data.code === 200 && response.data.data) {
      const basicCollectedItems = response.data.data.content;
      pagination.pageNumber = response.data.data.pageNumber;
      pagination.pageSize = response.data.data.pageSize;
      pagination.totalElements = response.data.data.totalElements;
      pagination.totalPages = response.data.data.totalPages;

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
          if (packageDetail) {
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

</style>