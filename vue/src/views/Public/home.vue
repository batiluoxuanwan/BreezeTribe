<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="nav">
      <div class="logo">BreezeTribe</div>
      <nav class="nav-center">
        <router-link to="/">首页</router-link>
        <router-link to="/square">旅行广场</router-link>
        <router-link to="/login">登录</router-link>
        <router-link to="/register">注册</router-link>
      </nav>
      <div class="search-box">
        <input type="text" placeholder="请输入团名或景点...">
        <el-icon><Search /></el-icon>
      </div>
    </header>

    <VideoText src="/src/assets/Timeline 1.mp4"> BREEZETRIBE </VideoText>

    <section class="section-container tour-section">
      <h2 class="section-header">
        <span class="header-icon"><el-icon><Camera /></el-icon></span>
        热门旅行团
        <span class="header-subtitle">精选路线，即刻出发</span>
      </h2>
      <div class="card-grid">
        <TourCard v-for="tour in tours" :key="tour.id" :tour="tour" />
      </div>
      <div class="more-link-wrapper">
        <router-link to="/square?tab=groups" class="more-link">
          查看更多旅行团 <el-icon><ArrowRightBold /></el-icon>
        </router-link>
      </div>
    </section>

    <section class="section-container notes-section">
      <h2 class="section-header">
        <span class="header-icon"><el-icon><Notebook /></el-icon></span>
        游记精选
        <span class="header-subtitle">探索世界，分享你的故事</span>
      </h2>
      <div class="card-grid">
        <NoteCard v-for="note in notes" :key="note.id" :note="note" />
      </div>
      <div class="more-link-wrapper">
        <router-link to="/square?tab=notes" class="more-link">
          查看更多游记 <el-icon><ArrowRightBold /></el-icon>
        </router-link>
      </div>
    </section>

    <!-- 页脚 -->
    <el-footer class="footer">
      <div class="footer-content">
        <!-- 版权信息 -->
        <div class="copyright">
          Copyright © 2025 BREEZETRIBE
        </div>
      </div>
    </el-footer>
  </div>
</template>

<script setup>
import TourCard from '@/components/Tour.vue'
import NoteCard from '@/components/NoteCard.vue'
import VideoText from '@/components/ui/video-text/VideoText.vue'

const tours = [
  {
    id: 1,
    title: '云南丽江大理6日精品游', // 可以稍微长一点测试标题截断
    location: '丽江, 大理',
    image: 'https://via.placeholder.com/300x200?text=LijiangTour', // 示例图片
    price: 3200,
    rating: 4.8, // 新增：评分
    isHot: true, // 新增：是否热门
    duration: 6, // 新增：天数
    startDate: '2025-07-10', // 新增：出发日期
  },
  {
    id: 2,
    title: '川西秘境稻城亚丁7日深度探索之旅',
    location: '稻城亚丁',
    image: 'https://via.placeholder.com/300x200?text=DaochengTour',
    price: 5800,
    rating: 4.9,
    isHot: false,
    duration: 7,
    startDate: '2025-08-01',
  },
  {
    id: 3,
    title: '海南三亚自由行5天4晚，海岛风情',
    location: '三亚',
    image: 'https://via.placeholder.com/300x200?text=SanyaTour',
    price: 2800,
    rating: 4.7,
    isHot: true,
    duration: 5,
    startDate: '2025-09-05',
  },
]


const notes = [
  {
    id: 101,
    title: '夏日青海湖，一场关于色彩的旅行体验', // 标题可以稍微长一点测试截断
    description: '青海湖的夏天，是碧蓝与金黄的交织，是心灵的洗涤。这里有你向往的一切，包括壮丽的油菜花海和神秘的湖畔风光，每一次呼吸都充满纯净。', // 描述可以稍微长一点测试截断
    author: '小马快跑',
    avatar: 'https://via.placeholder.com/50/FFD700/FFFFFF?text=MP', // 示例头像
    image: 'https://via.placeholder.com/400x250?text=QinghaiLake', // 示例封面图
    views: 12345, // 新增：浏览量
    likes: 2890, // 新增：点赞数
    category: '风光摄影', // 新增：分类标签
    publishDate: '2025-06-20' // 新增：发布日期
  },
  {
    id: 102,
    title: '一个人也能玩转成都，吃喝玩乐全指南与心得分享',
    description: '从火锅串串到熊猫基地，一个人的成都也能精彩纷呈，带你吃遍大街小巷，体验地道川味美食和悠闲茶馆文化，感受这座城市的慢生活节奏。',
    author: '吃货小C',
    avatar: 'https://via.placeholder.com/50/87CEEB/FFFFFF?text=CC',
    image: 'https://via.placeholder.com/400x250?text=ChengduFood',
    views: 9876,
    likes: 1567,
    category: '美食攻略',
    publishDate: '2025-06-18'
  }
];

</script>

<style scoped>
.home-page {
  font-family: 'Quicksand', 'Poppins', sans-serif;
  color: #444;
  
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  background-attachment: fixed;
}

.nav {
  position: fixed;    /* 悬浮，脱离文档流 */
  top: 0;
  left: 0;
  width: 100%;       /* 宽度铺满 */
  z-index: 1000;     /* 保证在最上层 */
  
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 30px;
  background:rgba(128,203,196,0.55) ;
  border-bottom: 1px solid #eee;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.03);
}


.nav .logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #6da0b1;
}

.nav nav a {
  margin: 0 24px;
  color: #555;
  text-decoration: none;
  font-weight: 500;
  font-size: 1.2rem;
}

.nav-center {
  text-align:center;
  max-width: 600px;
}

.search-box {
  align-items: center;
  display: flex;
  position: relative;
  margin-left:20px;
}
.search-box input {
  width: 100%;
  padding: 12px 20px;
  border-radius: 999px;
  border: 1px solid #51e6d2;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #ffffff;
}
.search-box input:focus {
  outline: none;
  border-color: #01bda4;
  box-shadow: 0 0 0 3px rgba(79, 195, 247, 0.3);
}
.search-box .el-icon {
  position: absolute;
  right: 10px; /* Adjust as needed, ensure it's within padding */
  top: 50%;
  transform: translateY(-50%);
  color: #6da0b1;
  cursor: pointer;
}



.banner {
  text-align: center;
  padding: 60px 20px 30px;
  background: linear-gradient(to right, #e0f7f4, #fff8f0);
  border-radius: 1.5rem;
  margin: 20px auto;
  max-width: 90%;
  box-shadow: 0 4px 16px rgba(180, 220, 210, 0.3);
}

.banner h1 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  color: #6a9b8d;
}

.banner p {
  font-size: 1.1rem;
  color: #888;
}

.home-page > .VideoText {
  padding-top: 80px; 
}

.section-container {
  padding: 60px 40px; 
  max-width: 1200px;
  margin: 40px auto; 
  border-radius: 15px; 
  background-color: rgba(255, 255, 255, 0.65); 
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1); 
}

/* 区域标题通用样式 */
.section-header {
  text-align: center;
  font-size: 2.2rem; /* 增大标题字号 */
  color: #333; /* 标题颜色 */
  font-weight: 700;
  margin-bottom: 40px; /* 标题与内容间距 */
  position: relative;
  padding-bottom: 15px; /* 为下划线留出空间 */
}

.section-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 250px; /* 下划线宽度 */
  height: 5px; /* 下划线粗细 */
  background: linear-gradient(45deg, rgb(77,182,172), rgb(178,223,219)); /* 渐变色下划线 */
  border-radius: 3px;
}

.section-header .header-icon {
  color: #25a574; 
  font-size: 0.9em; 
  margin-right: 10px;
}

.section-header .header-subtitle {
  display: block; 
  color: #666;
  font-weight: 400;
  font-size: 0.6em; 
  margin-top: 10px;
}

/* 卡片网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); /* 响应式网格，适应卡片大小 */
  gap: 25px; /* 卡片间距 */
  justify-content: center; /* 卡片居中 */
  margin-bottom: 40px; /* 与“查看更多”链接的间距 */
}

/* 查看更多链接 */
.more-link-wrapper {
  text-align: center;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background-color: rgb(102, 162, 156);
  color: #fff;
  padding: 12px 25px;
  border-radius: 30px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}

.more-link:hover {
  background-color: rgb(153, 213, 207);
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(64, 158, 255, 0.4);
}

h2 {
  margin-bottom: 1rem;
  font-size: 1.4rem;
  color: #6db193;
}

.notes {
  padding: 30px;
}

.notes-header {
  text-align: center;
  font-size: 1.4rem;
  margin-bottom: 1rem;
  color: #6db193;
}

.notes-content {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.buttons button {
  margin: 5px;
  padding: 8px 16px;
  background-color: #e9f6f3;
  border: none;
  border-radius: 999px;
  color: #4b9d83;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.buttons button:hover {
  background-color: #d3ece7;
}

/* 页脚 */
.footer {
  flex-shrink: 0;
  padding: 40px 0 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(7, 7, 7, 0.8);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.copyright {
  font-size: 16px;
  opacity: 0.8;
}

</style>
