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

// --- 模拟数据 (Mock Data) ---
const ALL_TRAVEL_GROUPS = [
  { id: 1, title: '云南丽江大理6日游', destination: '丽江, 大理', imageUrl: 'https://via.placeholder.com/300x200?text=LijiangGroup', price: 3200, startDate: '2025-07-10', duration: 6, members: 12, rating: 4.8, description: '探索丽江古城，漫步大理洱海，感受云南的浪漫与风情。', views: 1200 ,isHot:true },
  { id: 2, title: '川西秘境稻城亚丁7日深度游', destination: '稻城亚丁', imageUrl: 'https://via.placeholder.com/300x200?text=DaochengGroup', price: 5800, startDate: '2025-08-01', duration: 7, members: 8, rating: 4.9, description: '走进蓝色星球上最后一片净土，领略雪山、湖泊、森林的绝美风光。', views: 1500 },
  { id: 3, title: '海南三亚自由行5天4晚', destination: '三亚', imageUrl: 'https://via.placeholder.com/300x200?text=SanyaGroup', price: 2800, startDate: '2025-09-05', duration: 5, members: 15, rating: 4.7, description: '阳光沙滩，碧海蓝天，尽享热带海岛风情。', views: 1000 },
  { id: 4, title: '桂林山水甲天下4日经典游', destination: '桂林', imageUrl: 'https://via.placeholder.com/300x200?text=GuilinGroup', price: 2500, startDate: '2025-07-20', duration: 4, members: 10, rating: 4.6, description: '乘船游漓江，探访阳朔西街，沉醉于水墨画般的山水之间。', views: 1100,isNew:true },
  { id: 5, title: '青海湖敦煌大环线8日游', destination: '青海湖, 敦煌', imageUrl: 'https://via.placeholder.com/300x200?text=QinghaiDunhuangGroup', price: 6500, startDate: '2025-08-15', duration: 8, members: 7, rating: 4.9, description: '穿越西北戈壁，感受青海湖的浩瀚，领略敦煌莫高窟的艺术魅力。', views: 1800 },
  { id: 6, title: '张家界凤凰古城双飞6日游', destination: '张家界, 凤凰', imageUrl: 'https://via.placeholder.com/300x200?text=ZhangjiajieGroup', price: 3500, startDate: '2025-09-10', duration: 6, members: 10, rating: 4.7, description: '探秘阿凡达仙境，漫步烟雨凤凰，感受湘西的神秘与浪漫。', views: 1300 },
  { id: 7, title: '福建厦门鼓浪屿3日休闲游', destination: '厦门', imageUrl: 'https://via.placeholder.com/300x200?text=XiamenGroup', price: 1800, startDate: '2025-07-25', duration: 3, members: 18, rating: 4.5, description: '漫步鼓浪屿，品尝海鲜小吃，感受厦门的文艺气息。', views: 900 },
  { id: 8, title: '北京故宫长城经典5日游', destination: '北京', imageUrl: 'https://via.placeholder.com/300x200?text=BeijingGroup', price: 2900, startDate: '2025-08-20', duration: 5, members: 12, rating: 4.8, description: '深度体验首都文化，领略故宫的庄严与长城的雄伟。', views: 1400 },
  { id: 9, title: '四川成都九寨沟5日精华游', destination: '成都, 九寨沟', imageUrl: 'https://via.placeholder.com/300x200?text=ChengduGroup', price: 4500, startDate: '2025-09-01', duration: 5, members: 9, rating: 4.9, description: '品尝成都美食，探访九寨沟的童话世界。', views: 1600 },
  { id: 10, title: '新疆伊犁杏花沟喀纳斯10日深度', destination: '新疆', imageUrl: 'https://via.placeholder.com/300x200?text=XinjiangGroup', price: 8000, startDate: '2025-06-28', duration: 10, members: 6, rating: 5.0, description: '探索大美新疆，感受异域风情和壮丽自然。', views: 2000 },
  { id: 11, title: '西藏拉萨布达拉宫7日净化心灵之旅', destination: '拉萨', imageUrl: 'https://via.placeholder.com/300x200?text=TibetGroup', price: 7000, startDate: '2025-07-05', duration: 7, members: 9, rating: 4.9, description: '高原圣地，感受信仰的力量，挑战身体与心灵的极限。', views: 1900 },
  { id: 12, title: '长白山天池森林探险5日游', destination: '长白山', imageUrl: 'https://via.placeholder.com/300x200?text=ChangbaiGroup', price: 3800, startDate: '2025-08-10', duration: 5, members: 10, rating: 4.7, description: '探秘神秘天池，漫步原始森林，呼吸纯净空气。', views: 1150 },
];

const ALL_TRAVEL_NOTES = [
  { id: 101, title: '【游记】夏日青海湖，一场关于色彩的旅行', author: '小马快跑', publishDate: '2025-06-20', views: 1234, likes: 289, coverImage: 'https://via.placeholder.com/300x200?text=QinghaiNote', description: '青海湖的夏天，是碧蓝与金黄的交织，是心灵的洗涤。', tags: ['青海', '自驾', '摄影'] },
  { id: 102, title: '【攻略】一个人也能玩转成都，吃喝玩乐全指南', author: '吃货小C', publishDate: '2025-06-18', views: 987, likes: 156, coverImage: 'https://via.placeholder.com/300x200?text=ChengduNote', description: '从火锅串串到熊猫基地，一个人的成都也能精彩纷呈。', tags: ['成都', '美食', '攻略'] },
  { id: 103, title: '【体验】我在丽江古城当“义工”的日子', author: '背包客小王', publishDate: '2025-06-15', views: 1500, likes: 320, coverImage: 'https://via.placeholder.com/300x200?text=LijiangNote', description: '在丽江古城深度体验当地生活，感受不一样的旅行。', tags: ['丽江', '古城', '义工'] },
  { id: 104, title: '【摄影】稻城亚丁：眼睛在天堂，身体在地狱', author: '光影捕手', publishDate: '2025-06-10', views: 2000, likes: 450, coverImage: 'https://via.placeholder.com/300x200?text=DaochengNote', description: '用镜头记录稻城亚丁的四季，每一帧都是壁纸。', tags: ['稻城亚丁', '摄影', '徒步'] },
  { id: 105, title: '【亲子游】带着娃去三亚，海岛度假攻略', author: '辣妈咪', publishDate: '2025-06-05', views: 850, likes: 100, coverImage: 'https://via.placeholder.com/300x200?text=SanyaNote', description: '三亚亲子游的N种玩法，让孩子爱上海边！', tags: ['三亚', '亲子', '海岛'] },
  { id: 106, title: '【自驾】穿越川藏线，G318上的诗和远方', author: '老司机阿Z', publishDate: '2025-05-30', views: 2500, likes: 500, coverImage: 'https://via.placeholder.com/300x200?text=ChuanzangNote', description: '一次史诗般的自驾之旅，挑战与美景并存。', tags: ['川藏线', '自驾', '探险'] },
  { id: 107, title: '【美食】广东潮汕，碳水天堂的N种打开方式', author: '美食探店', publishDate: '2025-05-25', views: 1100, likes: 200, coverImage: 'https://via.placeholder.com/300x200?text=ChaoshanNote', description: '从早茶到夜宵，潮汕美食让人流连忘返。', tags: ['潮汕', '美食', '小吃'] },
  { id: 108, title: '【小众】寻访江南古镇，乌镇之外的惊喜', author: '悠悠漫步', publishDate: '2025-05-20', views: 700, likes: 80, coverImage: 'https://via.placeholder.com/300x200?text=JiangnanNote', description: '避开人潮，探索那些鲜为人知的江南水乡。', tags: ['江南', '古镇', '文化'] },
  { id: 109, title: '【徒步】武功山反穿，虐并快乐着', author: '户外达人', publishDate: '2025-05-15', views: 1800, likes: 380, coverImage: 'https://via.placeholder.com/300x200?text=WugongshanNote', description: '挑战武功山，云海日出，美不胜收。', tags: ['武功山', '徒步', '户外'] },
  { id: 110, title: '【避坑】去新疆旅行，这些事情你必须知道！', author: '旅游达人', publishDate: '2025-05-10', views: 1600, likes: 350, coverImage: 'https://via.placeholder.com/300x200?text=XinjiangNote', description: '关于新疆旅行的实用建议，让你少走弯路。', tags: ['新疆', '避坑', '攻略'] },
  { id: 111, title: '【感受】第一次去西藏，我被震撼了', author: '心灵旅者', publishDate: '2025-05-05', views: 1900, likes: 400, coverImage: 'https://via.placeholder.com/300x200?text=TibetNote', description: '高原反应与绝美风光并存，一次难忘的西藏之旅。', tags: ['西藏', '心灵', '感悟'] },
  { id: 112, title: '【秘境】长白山深处的人间仙境', author: '探索家', publishDate: '2025-05-01', views: 1000, likes: 120, coverImage: 'https://via.placeholder.com/300x200?text=ChangbaiNote', description: '除了天池，长白山还有许多值得探访的秘密。', tags: ['长白山', '秘境', '自然'] },
];

// --- 激活的Tab ---
const activeTab = ref('groups');

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

watch(
  [activeTab, searchKeyword], // 监听 activeTab 和 keyword
  ([newTab, newKeyword], [oldTab, oldKeyword]) => {
    // 只有当 activeTab 改变或者 keyword 改变时才触发刷新
    // 避免不必要的重复加载，可以根据需求更精细地控制

    // 如果是 activeTab 改变，根据新的 tab 加载数据
    if (newTab !== oldTab) {
      if (newTab === 'groups') {
        console.log('Tab switched to groups, fetching travel groups...');
        fetchTravelGroups();
      } else if (newTab === 'notes') {
        console.log('Tab switched to notes, fetching travel notes...');
        fetchTravelNotes();
      }
    } 
    
    // 如果是 keyword 改变，并且当前 tab 匹配，也加载数据
    // 注意：这里需要确保 newTab 是你想要加载数据的 tab
    if (newKeyword !== oldKeyword && newKeyword.trim() !== '') { // 确保 keyword 不为空
        if (newTab === 'groups') {
            console.log('Keyword changed in groups tab, fetching travel groups...');
            fetchTravelGroups();
        } else if (newTab === 'notes') {
            console.log('Keyword changed in notes tab, fetching travel notes...');
            fetchTravelNotes();
        }
    }
  },
  { immediate: true } 
);

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