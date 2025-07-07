<template>
  <div class="footprint-map-page">
    <el-card class="map-card">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="goBack" class="back-button"></el-button>
          <span>我的足迹</span>
        </div>
      </template>

      <div v-if="!mapLoaded" class="map-loading-overlay">
        <el-icon class="is-loading loading-icon"><Loading /></el-icon>
        <span>百度地图加载中...</span>
      </div>
      <div v-else-if="mapLoaded && posts.length === 0 && !loading" class="map-empty-overlay">
        <p>暂无足迹数据，请发布游记并包含地理位置信息。</p>
      </div>

      <div id="baidumap-container" :class="{'map-active': mapLoaded}" style="width: 100%; height: 600px;">
        </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted,nextTick} from 'vue';
import { useRouter } from 'vue-router'; // 引入 useRouter
import { ElMessage, ElCard, ElButton, ElIcon } from 'element-plus';
import { ArrowLeft, Loading } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request'; // 假设 authAxios 在这里

const router = useRouter(); // 初始化 router

// --- 地图相关变量 ---
let map = null; // 百度地图实例变量
const mapLoaded = ref(false); // 百度地图SDK是否加载完成
let currentMarkers = []; // 用于存储地图上的所有标记点，方便管理

// --- 游记数据相关变量 ---
const posts = ref([]); // 存储所有游记数据
const loading = ref(false); // 游记数据加载状态
const noMore = ref(false); // 是否没有更多游记 (这里我们一次性加载所有，所以这个变量可能不严格需要分页时才用到)
const page = ref(0); // API请求的页码，从0开始
const size = 100; // 假设一次性加载所有（或足够多的）游记，避免分页频繁交互地图

// --- 地图SDK加载函数 ---
const loadBMapGL = () => {
  return new Promise((resolve, reject) => {
    // 如果已经加载，直接返回
    if (window.BMapGL) {
      resolve(window.BMapGL);
      return;
    }

    // 清理可能存在的旧脚本，避免重复加载
    const existingScripts = document.querySelectorAll('script[src*="map.baidu.com"]');
    existingScripts.forEach(script => script.remove());

    // 创建新的脚本标签
    const script = document.createElement("script");
    script.type = "text/javascript";
    // 您的百度地图API Key
    const BAIDU_MAP_AK = 'OUxgi9tGCKkijW4VW8d5F8FxcFRNfDfz'; 
    script.src = `https://api.map.baidu.com/api?type=webgl&v=1.0&ak=${BAIDU_MAP_AK}&callback=initBMapGLCallback`;
    
    // 全局回调函数，百度地图SDK加载完成后会自动调用
    window.initBMapGLCallback = () => {
      if (window.BMapGL) {
        console.log("百度地图API已加载", window.BMapGL);
        resolve(window.BMapGL);
      } else {
        reject(new Error("BMapGL对象未定义，请检查API Key和网络连接"));
      }
      // 清理回调函数，避免全局污染
      delete window.initBMapGLCallback;
    };

    // 错误处理
    script.onerror = (error) => {
      console.error("百度地图SDK脚本加载失败:", error);
      delete window.initBMapGLCallback;
      reject(new Error("百度地图SDK脚本加载失败，请检查网络"));
    };

    // 添加到文档头部
    document.head.appendChild(script);
  });
};

// --- 初始化地图控件 ---
const initMapControls = (bmapInstance) => {
  if (!bmapInstance || !window.BMapGL) return;

  // 添加缩放控件
  const zoomControl = new BMapGL.NavigationControl({
    anchor: window.BMAP_ANCHOR_TOP_LEFT, // 控件在地图上的位置
    type: window.BMAP_NAVIGATION_CONTROL_ZOOM // 缩放控件
  });
  bmapInstance.addControl(zoomControl);
  
  // 启用键盘操作
  bmapInstance.enableKeyboard();
};

// --- 加载所有游记数据 (分批加载) ---
const loadPosts = async () => {
  if (loading.value) return;
  loading.value = true;
  posts.value = []; // 清空之前的游记数据

  let currentPage = 1; // 从第一页开始
  const pageSize = 100; // 每次请求最大记录数
  let hasMore = true;

  try {
    while (hasMore) {
      const response = await authAxios.get('/user/posts', {
        params: {
          page: currentPage,
          size: pageSize,
          sortBy: 'createdTime', 
          sortDirection: 'DESC'
        }
      });

      if (response.data.code === 200 && response.data.data) {
        const newContent = response.data.data.content || [];
        posts.value = [...posts.value, ...newContent]; 

        const totalPages = response.data.data.totalPages;
        // 如果当前页是最后一页，或者没有新数据，则停止加载
        if (currentPage >= totalPages || newContent.length === 0) {
          hasMore = false;
        } else {
          currentPage++; // 继续加载下一页
        }
      } else {
        ElMessage.error(response.data.message || '获取游记失败。');
        console.error('获取游记失败：', response.data.message);
        hasMore = false; // 出现错误时停止循环
      }
    }
    
    // 所有数据加载完成后，再添加标记
    if (map) {
      addMarkers();
    }

  } catch (err) {
    ElMessage.error('请求游记接口出错，请检查网络。');
    console.error('请求游记接口出错：', err);
  } finally {
    loading.value = false;
  }
};

// --- 加载地图标记 ---
const addMarkers = () => {
  const BMapGL = window.BMapGL;
  if (!map || !posts.value || !BMapGL) return;

  // 清除地图上的旧标记
  currentMarkers.forEach(marker => map.removeOverlay(marker));
  currentMarkers = [];

  const pointsToFitView = [];

  // 添加新标记
  posts.value.forEach(item => {
    // 确保游记数据包含有效的经纬度
    if (item.spot && item.spot.latitude !== undefined && item.spot.longitude !== undefined) {
      const point = new BMapGL.Point(item.spot.longitude, item.spot.latitude);
      pointsToFitView.push(point); // 收集点位用于调整视野

      const marker = new BMapGL.Marker(point);
      map.addOverlay(marker);
      currentMarkers.push(marker); // 添加到 marker 列表中

    // 信息窗口内容
    const content = `
    <div style="width:240px; border: none; border-radius: 10px; background: white; padding: 18px; position: relative; font-family: 'Segoe UI', Arial, sans-serif;display: flex;flex-direction: column;align-items: center; justify-content: center;">
        ${item.coverImageUrl ? `
        <img src="${item.coverImageUrl}" 
            alt="${item.spot.name || '游记地点'}封面" 
            style="width: 100%; height: 140px; object-fit: cover; border-radius: 8px; margin-bottom: 12px; display: block;"/>
        ` : `
        <div style="width: 100%; height: 100px; background-color: #f0f0f0; border-radius: 8px; margin-bottom: 12px; display: flex; align-items: center; justify-content: center; color: #999; font-size: 14px;">
            暂无图片
        </div>
        `}

        <div style="font-size: 14px; color: #555; line-height: 1.5; margin-bottom: 10px;">
        ${item.spot.city ? `<strong>城市:</strong> ${item.spot.city}<br>` : ''}
        ${item.spot.address ? `<strong>地址:</strong> ${item.spot.address}<br>` : ''}
        <strong>发布时间:</strong> ${item.createdTime ? new Date(item.createdTime).toLocaleDateString() : 'N/A'}
        </div>

        <a href="/my-note/${item.id}" target="_blank" 
        style="display: block; text-align: center; padding: 8px 15px; color: #44beae; border-radius: 5px; text-decoration: none; font-size: 14px; transition: background-color 0.3s ease;">
        查看游记详情 &gt;
        </a>
    </div>
    `;

      const infoWindow = new BMapGL.InfoWindow(content, {
        offset: new BMapGL.Size(0, -30) // 信息窗口偏移量
      });

      marker.addEventListener('click', () => {
        map.openInfoWindow(infoWindow, point);
      });
    }else {
      console.warn(`游记 ${item.id} (${item.title}) 缺少有效的地理位置信息，将不显示在地图上。`);
    }
  });

  // 调整地图视野以包含所有标记点
  if (pointsToFitView.length > 0) {
    const view = map.getViewport(pointsToFitView);
    map.setViewport(view);
  } else {
    // 如果没有点位，设置一个默认中心和缩放
    map.centerAndZoom(new BMapGL.Point(116.404, 39.915), 5); // 默认北京，适当缩放
  }
};

// --- 返回上一页 ---
const goBack = () => {
  router.push({ name: '用户个人主页', query: { tab: 'notes' } }); 
};

// --- 生命周期钩子 ---
onMounted(async () => {
  try {
    // 加载百度地图SDK
    const BMapGL = await loadBMapGL();
    mapLoaded.value = true;
    console.log("百度地图SDK已成功加载。");

    await nextTick(); 

    // 创建地图实例
    map = new BMapGL.Map("baidumap-container");
    console.log("地图实例已创建。");

    // 启用滚轮缩放
    map.enableScrollWheelZoom(true);

    // 设置中心点和缩放级别
    map.centerAndZoom(new BMapGL.Point(108.948024, 34.263161), 5);
    
    // 初始化地图控件
    initMapControls(map);
    console.log("地图控件已初始化。");

    // 加载游记数据并添加标记
    await loadPosts();
    console.log("游记数据已加载并尝试添加标记。",posts.value);

  } catch (error) {
    console.error("地图或数据加载初始化失败：", error);
    ElMessage.error(`地图加载失败: ${error.message || '未知错误'}`);
    mapLoaded.value = true; // 即使失败也要设置为true，避免一直显示加载中
  }
});

// 组件卸载时清理
onUnmounted(() => {
  if (map) {
    map.destroy(); // 销毁地图实例，释放资源
    map = null;
  }
  // 清理全局回调函数
  if (window.initBMapGLCallback) {
    delete window.initBMapGLCallback;
  }
});
</script>

<style scoped>
.map-card {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  padding: 30px 0px;
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

.back-button {
  color: #606266;
  border-color: #dcdfe6;
}

.back-button:hover {
  background-color: #f2f6fc;
  border-color: #c6e2ff;
  color: #409eff;
}

#baidumap-container {
  position: relative; /* 确保内部加载状态能够正确覆盖 */
}

/* 去除水印 */
::v-deep(.BMapGL_cpyCtrl) {
  display: none;
}

::v-deep(.anchorBL) {
  display: none !important;
}

/* 去除阴影 */
::deep .BMapGLInfoWindow {
  box-shadow: none !important; /* 移除任何可能的默认阴影 */
  border: none !important; /* 移除任何可能的默认边框 */
  width: auto !important;
  max-width: 300px !important;
}

::deep .BMapGL_bubble_content {
  box-shadow: none !important;
  border: none !important;
  background-color: transparent !important; 
}

::deep .BMapGL_bubble_pop {
  box-shadow: none !important;
  border: none !important;
  background-color: transparent !important;
  width: auto !important;
  max-width: 300px !important;
}

.map-loading, .map-empty {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.8);
  z-index: 10; /* 确保在地图上方 */
  font-size: 1.1rem;
  color: #666;
  border-radius: 8px; /* 匹配卡片圆角 */
}

.loading-icon {
  font-size: 2.5rem;
  color: #409eff;
  margin-bottom: 10px;
}
</style>