<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="nav">
      <div class="logo">BreezeTribe</div>
      <nav class="nav-center">
        <router-link to="/">首页</router-link>
        <router-link to="/square">游记广场</router-link>
        <router-link to="/login">登录</router-link>
        <router-link to="/register">注册</router-link>
      </nav>
      <div class="search-box">
        <input type="text" placeholder="请输入团名或景点...">
        <el-icon><Search /></el-icon>
      </div>
    </header>

      <!-- 视频文字 -->
    <div class="relative h-full w-full">
      <VideoText src="https://cdn.magicui.design/ocean-small.webm"> BREEZETRIBE </VideoText>
    </div>

    <!-- 推荐团列表 -->
    <section class="tour-list">
      <TourCard v-for="(tour, i) in tours" :key="i" :tour="tour" />
    </section>

    <!-- 热门主题推荐 -->
    <section class="topics">
      <h2>热门主题推荐</h2>
      <div class="buttons">
        <button>国内游</button>
        <button>海岛游</button>
        <button>学生团</button>
        <button>自驾团</button>
      </div>
    </section>

    <!-- 游记精选 -->
    <section class="notes">
      <h2 class="notes-header">游记精选</h2>
      <div class="notes-content">
        <NoteCard v-for="(note, i) in notes" :key="i" :note="note" />
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
  { title: '桂林阳朔5日游', image: 'img1.jpg', price: 2999, location: '广西' },
  { title: '广东巽寮湾4日游', image: 'img2.jpg', price: 2899, location: '广东' }
]


const notes = [
  {
    title: '在稻城亚丁遇见蓝色星球的眼泪',
    description: '海子山下，那一片纯净蓝色，真的好像是童话。',
    image: '',
    author: '阿木'
  },
  {
    title: '云南腾冲，泡温泉的冬日故事',
    description: '烟雾缭绕中，每一口呼吸都好像治愈了城市疲惫。',
    image: '/images/note2.jpg',
    author: '橙子'
  },
  {
    title: '和风微光里的镰仓小路',
    description: '在镰仓的海边慢慢走，像是走进了一部夏目友人帐。',
    image: '/images/note3.jpg',
    author: 'Luna'
  },
]

</script>

<style scoped>
.home-page {
  font-family: 'Quicksand', 'Poppins', sans-serif;
  color: #444;
  
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
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
  border: 1px solid #ccc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  font-size: 16px;
  transition: all 0.3s ease;
}
.search-box input:focus {
  outline: none;
  border-color: #4fc3f7;
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

.tour-list{
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  padding: 30px;
  justify-content: center;
}

.topics {
  padding: 20px;
  text-align: center;
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
