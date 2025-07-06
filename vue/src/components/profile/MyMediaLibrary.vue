<template>
  <div class="my-media-library">
    <h2 class="section-title">我的素材库</h2>
    <div v-if="loading" class="loading-container">
      <el-spinner size="large" />
      <p>加载中...</p>
    </div>
    <div v-else-if="images.length === 0" class="empty-media-library">
      <el-empty description="您的素材库空空如也" />
      <p>快去上传一些图片吧！</p>
    </div>
    <div v-else>
      <el-row :gutter="20">
        <el-col :span="6" v-for="image in images" :key="image.id" class="image-card-col">
          <el-card shadow="hover" class="image-card">
            <div class="image-wrapper">
              <img :src="image.url" class="media-image" :alt="image.filename" />
              <div class="overlay">
                <el-button type="danger" :icon="Delete" circle @click="confirmDelete(image)" class="delete-button" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <div class="pagination-container" v-if="totalElements > pageSize">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request'; // 假设你的认证请求实例

const images = ref([]);
const loading = ref(true);
const currentPage = ref(1);
const pageSize = ref(8); // 每页显示图片数量
const totalElements = ref(0);

// --- 获取媒体库图片 ---
const fetchMediaFiles = async () => {
  loading.value = true;
  try {
    const response = await authAxios.get('/user/media', {
      params: {
        page: currentPage.value, 
        size: pageSize.value,
        sortBy: 'createdTime', 
        sortDirection: 'DESC' 
      }
    });
    if (response.data.code === 200 && response.data.data) {
        console.log('获取媒体库数据:',response.data)
      images.value = response.data.data.content;
      totalElements.value = response.data.data.totalElements; // 更新总数
      console.log('获取媒体库数据:',response.data,totalElements.value,pageSize.value)
    } else {
      ElMessage.error(response.data.message || '获取素材库文件失败。');
      images.value = [];
      totalElements.value = 0;
    }
  } catch (error) {
    console.error('获取素材库文件时发生错误:', error);
    ElMessage.error('加载素材库失败，请检查网络。');
    images.value = [];
    totalElements.value = 0;
  } finally {
    loading.value = false;
  }
};


// --- 分页改变事件 ---
const handlePageChange = (newPage) => {
  currentPage.value = newPage;
  fetchMediaFiles();
};

// --- 确认删除图片 ---
const confirmDelete = (image) => {
  ElMessageBox.confirm(`确定要删除该图片吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  .then(() => {
    deleteImage(image.id);
  })
  .catch(() => {
    ElMessage.info('已取消删除。');
  });
};

// --- 删除图片 ---
const deleteImage = async (imageId) => {
  try {
    const response = await authAxios.delete(`/user/media/${imageId}`);
    if (response.data.code === 200) {
      ElMessage.success('图片删除成功！');
      if (images.value.length === 1 && currentPage.value > 1) {
        currentPage.value--;
      }
      fetchMediaFiles(); // 重新加载数据
    } else {
      ElMessage.error(response.data.message || '删除图片失败。');
    }
  } catch (error) {
    console.error('删除图片时发生错误:', error);
    ElMessage.error('删除图片失败，请检查网络。');
  }
};

// --- 格式化日期 ---
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
};

onMounted(() => {
  fetchMediaFiles();
});
</script>

<style scoped>
.my-media-library {
  padding: 20px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  min-height: 500px; /* 保证内容区域高度 */
}

.section-title {
  font-size: 2em;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
  position: relative;
  padding-bottom: 10px;
}

.section-title::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background-color: #4fc0b5;
  border-radius: 2px;
}

.loading-container, .empty-media-library {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 400px; /* 保证加载和空状态有足够高度 */
  color: #999;
}

.empty-media-library p {
  margin-top: 20px;
  font-size: 1.1em;
}

.image-card-col {
  margin-bottom: 20px;
}

.image-card {
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  height: 250px; /* 固定卡片高度，保持一致性 */
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 180px; /* 图片预览高度 */
  overflow: hidden;
}

.media-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 裁剪图片以填充容器 */
  transition: transform 0.3s ease;
}

.image-card:hover .media-image {
  transform: scale(1.05);
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-card:hover .overlay {
  opacity: 1;
}

.delete-button {
  transform: scale(1.2);
  opacity: 0.9;
}

.delete-button:hover {
  transform: scale(1.3);
  opacity: 1;
}

.image-info {
  padding: 10px;
  text-align: center;
}

.image-filename {
  font-size: 1em;
  color: #333;
  margin-bottom: 5px;
  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 溢出隐藏 */
  text-overflow: ellipsis; /* 显示省略号 */
}

.image-upload-time {
  font-size: 0.85em;
  color: #888;
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style>