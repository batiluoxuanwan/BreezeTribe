<template>
  <div class="square">
    <div class="travel-plaza-container">
      <!-- 页眉 -->
      <div class="header-area">
        <el-button type="primary"  class="back-home-button" @click="goHome"><el-icon><ArrowLeftBold /></el-icon>返回主页</el-button>
        <h1 class="plaza-title">旅行广场</h1>
        <div class="title-slogan">探索世界，分享精彩</div>
      </div>

      <!-- 搜索排序栏 -->
      <el-card class="filter-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="目的地">
            <el-input v-model="searchForm.destination" placeholder="输入目的地" clearable></el-input>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="searchForm.keyword" placeholder="搜索标题/内容" clearable></el-input>
          </el-form-item>
          <el-form-item label="排序">
            <el-select v-model="searchForm.sortBy" placeholder="请选择">
              <el-option label="最新发布" value="latest"></el-option>
              <el-option label="最受欢迎" value="popular"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="applyFilters">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-tabs v-model="activeTab" class="plaza-tabs">
        <el-tab-pane label="旅行团广场" name="groups">
          <div class="card-grid">
            <TravelGroupCard v-for="group in travelGroups" :key="group.id" :group="group" />
            <el-empty v-if="travelGroups.length === 0 && !loading" description="暂无旅行团数据"></el-empty>
          </div>
          <el-pagination
            v-if="groupTotal > 0 || loading"
            background
            layout="prev, pager, next"
            :total="groupTotal"
            :page-size="groupPageSize"
            :current-page="groupCurrentPage"
            @current-change="handleGroupPageChange"
            class="pagination"
          />
        </el-tab-pane>

        <el-tab-pane label="游记广场" name="notes">
          <div class="card-grid">
            <TravelNoteCard v-for="note in travelNotes" :key="note.id" :note="note" />
            <el-empty v-if="travelNotes.length === 0 && !loading" description="暂无游记数据"></el-empty>
          </div>
          <el-pagination
            v-if="noteTotal > 0 || loading"
            background
            layout="prev, pager, next"
            :total="noteTotal"
            :page-size="notePageSize"
            :current-page="noteCurrentPage"
            @current-change="handleNotePageChange"
            class="pagination"
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="loading" class="loading-overlay">
        <el-spinner size="large"></el-spinner>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ElTabs, ElTabPane, ElCard, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton, ElPagination, ElEmpty, ElMessage } from 'element-plus'; 
import TravelGroupCard from '@/components/TravelGroupCard.vue';
import TravelNoteCard from '@/components/TravelNoteCard.vue';
import { useRouter, useRoute } from 'vue-router';
import { publicAxios } from '@/utils/request'

const router = useRouter(); 
const route = useRoute(); // 当前路由信息
const searchKeyword = ref('')  // 需要初始化，从路由 query 里读取
const isSearchMode = ref(false) // 标识当前是否是搜索场景


// --- 返回主页 ---
const goHome = () => {
  router.push('/');
};

// --- 激活的Tab ---
const activeTab = ref('groups');

// --- 监听路由查询参数，用于激活对应的Tab ---
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab) {
      activeTab.value = newTab;
    }
  },
  { immediate: true } 
);


// --- 搜索表单 ---
const searchForm = ref({
  destination: '',
  keyword: '',
  sortBy: 'latest',
});

// --- 数据和分页 ---
const travelGroups = ref([]);
const groupTotal = ref(0);
const groupPageSize = 9; // 每页显示9个旅行团
const groupCurrentPage = ref(1);

const travelNotes = ref([]);
const noteTotal = ref(0);
const notePageSize = 9; // 每页显示9个游记
const noteCurrentPage = ref(1);

const loading = ref(false); // 全局加载状态

// --- 获取旅行团数据 ---
const fetchTravelGroups = async () => {
  loading.value = true; // 开始加载
  try {
    let response = null;
    if (isSearchMode.value && searchKeyword.value.trim()) {
      // 走搜索接口
      response = await publicAxios.get('/public/travel-packages/search', {
        params: {
          keyword: searchKeyword.value,
          page: groupCurrentPage.value,
          size: groupPageSize,
          // sortBy: 'createdTime',
          // sortDirection: 'DESC',
        },
      })
     
    const resultList = response.data.data?.content || [];
      // 如果搜索无结果，尝试普通接口
      if (resultList.length === 0) {
        ElMessage.warning('未找到匹配的旅行团，为您推荐其他旅行团。');
        isSearchMode.value = false; // 取消搜索模式
        response = await publicAxios.get('/public/travel-packages', {
          params: {
            page: groupCurrentPage.value,
            size: groupPageSize,
          },
        });
      }
    }
    else {
      // 走普通获取广场列表接口
      response = await publicAxios.get('/public/travel-packages', {
        params: {
          page: groupCurrentPage.value,
          size: groupPageSize,
          // destination: searchForm.value.destination,
          // keyword: searchForm.value.keyword,
          // sortBy: searchForm.value.sortBy,
        },
      })
    }

    if (response.data.code === 200) {
      travelGroups.value = response.data.data.content; // 更新旅行团数据
    } else {
      ElMessage.error(response.data.message || '获取旅行团数据失败！'); 
    }
  } catch (error) {
    console.error('获取旅行团失败:', error);
    ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
  } finally {
    loading.value = false; // 结束加载
  }
};

// --- 获取游记数据 ---
const fetchTravelNotes = async () => {
  loading.value = true;
  try {
    let response = null
    if (isSearchMode.value && searchKeyword.value.trim()) {
      // 走搜索接口
      response = await publicAxios.get('/public/travel-packages/search', {
        params: {
          keyword: searchKeyword.value,
          page: noteCurrentPage.value,    // 当前页码
          size: notePageSize,             // 每页数量
          // sortBy: 'createdTime',
          // sortDirection: 'DESC',
        },
      })
    } 
    else{
      response = await publicAxios.get('/public/posts', {
      params: {
        page: noteCurrentPage.value,    // 当前页码
        size: notePageSize,             // 每页数量
        // sortBy: 'createdTime',          
        // sortDirection: 'DESC',
      },
    });
  }
    if (response.data.code === 200) {
      travelNotes.value = response.data.data.content;  // 更新游记列表数据
      noteTotal.value = response.data.data.totalElements || 0; // 总条数，用于分页
    } else {
      ElMessage.error(response.data.message || '获取游记数据失败！');
      travelNotes.value = [];
      noteTotal.value = 0;
    }
  } catch (error) {
    console.error('获取游记失败:', error);
    ElMessage.error('网络请求失败，请检查您的网络或稍后再试！');
    travelNotes.value = [];
    noteTotal.value = 0;
  } finally {
    loading.value = false; // 结束加载
  }
};

// --- 分页变更 ---
const handleGroupPageChange = (page) => {
  groupCurrentPage.value = page;
  fetchTravelGroups();
};

const handleNotePageChange = (page) => {
  noteCurrentPage.value = page;
  fetchTravelNotes();
};

// --- 应用筛选/搜索 ---
const applyFilters = () => {
  // 搜索时重置当前Tab的页码
  if (activeTab.value === 'groups') {
    groupCurrentPage.value = 1;
    fetchTravelGroups();
  } else {
    noteCurrentPage.value = 1;
    fetchTravelNotes();
  }
};

// --- 重置筛选 ---
const resetFilters = () => {
  searchForm.value = {
    destination: '',
    keyword: '',
    sortBy: 'latest',
  };
  applyFilters(); // 重置后重新加载数据
};

// watch(
//   [activeTab, searchKeyword], // 监听 activeTab 和 keyword
//   ([newTab, newKeyword], [oldTab, oldKeyword]) => {
//     // 只有当 activeTab 改变或者 keyword 改变时才触发刷新
//     // 避免不必要的重复加载，可以根据需求更精细地控制

//     // 如果是 activeTab 改变，根据新的 tab 加载数据
//     if (newTab !== oldTab) {
//       if (newTab === 'groups') {
//         console.log('Tab switched to groups, fetching travel groups...');
//         fetchTravelGroups();
//       } else if (newTab === 'notes') {
//         console.log('Tab switched to notes, fetching travel notes...');
//         fetchTravelNotes();
//       }
//     } 
    
//     // 如果是 keyword 改变，并且当前 tab 匹配，也加载数据
//     // 注意：这里需要确保 newTab 是你想要加载数据的 tab
//     if (newKeyword !== oldKeyword && newKeyword.trim() !== '') { // 确保 keyword 不为空
//         if (newTab === 'groups') {
//             console.log('Keyword changed in groups tab, fetching travel groups...');
//             fetchTravelGroups();
//         } else if (newTab === 'notes') {
//             console.log('Keyword changed in notes tab, fetching travel notes...');
//             fetchTravelNotes();
//         }
//     }
//   },
//   { immediate: true } 
// );

// --- 组件挂载时默认加载旅行团数据 ---
onMounted(() => {
  searchKeyword.value = route.query.keyword || ''
  isSearchMode.value = !!searchKeyword.value.trim()
  fetchTravelGroups();
  fetchTravelNotes();
});
</script>

<style scoped>
.square{
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.travel-plaza-container {
  padding: 20px 40px;
  max-width: 1200px; /* 限制内容最大宽度，使页面更居中美观 */
  margin: 0 auto; /* 居中显示 */
}

.header-area {
  display: flex;
  flex-direction: column; /* 标题和口号垂直排列 */
  align-items: center; /* 水平居中 */
  margin-bottom: 30px;
  position: relative; /* 用于放置返回按钮 */
  padding-top: 20px; /* 顶部留白 */
}

.back-home-button {
  position: absolute;
  left: 0px; /* 距离左侧边框10px */
  top: 20px; /* 距离顶部10px */
  z-index: 10; /* 确保在最上层 */
}

/* 标题 */
.plaza-title {
  font-size: 3.2em; 
  color: #2c3e50; 
  text-align: center;
  margin-bottom: 10px; 
  position: relative;
  font-weight: 700; 
  letter-spacing: 2px; 
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1); 
}
/* 标题下划线 */
.plaza-title::after {
  content: '';
  position: absolute;
  bottom: -10px; 
  left: 50%;
  transform: translateX(-50%);
  width: 250px; 
  height: 5px; 
  background: linear-gradient(45deg, rgb(77,182,172), rgb(178,223,219));
  border-radius: 3px;
}

.title-slogan {
  font-size: 1.3em;
  color: #606266;
  margin-top: 15px; /* 与标题的间距 */
  text-align: center;
}


.filter-card {
  margin-bottom: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.search-form {
  display: flex;
  flex-wrap: wrap; /* 允许换行 */
  gap: 15px; /* 表单项之间的间距 */
  align-items: center;
  justify-content: center; /* 居中表单项 */
}
.search-form .el-form-item {
  margin-bottom: 0; /* 移除默认的底部间距 */
}

.plaza-tabs {
  margin-top: 20px;
}

/* Tab 标题样式 */
.plaza-tabs :deep(.el-tabs__item) {
  font-size: 1.1em;
  font-weight: bold;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); /* 响应式网格 */
  gap: 20px; /* 卡片间距 */
  margin-top: 20px;
  min-height: 400px; /* 确保在无数据时页面高度不塌陷 */
  align-content: start; /* 顶部对齐内容 */
}

.pagination {
  margin-top: 30px;
  justify-content: center;
  display: flex; /* Flexbox 居中 */
}

/* 加载覆盖层 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}
.loading-overlay p {
  margin-top: 10px;
  color: #606266;
}
</style>