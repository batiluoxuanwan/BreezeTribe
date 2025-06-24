<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="nav">
      <div class="logo">BreezeTribe</div>
      <nav>
        <router-link to="/">首页</router-link>
        <router-link to="/square">游记广场</router-link>
        <router-link to="/login">登录</router-link>
        <router-link to="/register">注册</router-link>
      </nav>
      <input type="text" placeholder="搜索目的地 / 团名..." />
    </header>

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
      <h2>游记精选</h2>
      <NoteCard v-for="(note, i) in notes" :key="i" :note="note" />
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <a href="#">关于我们</a>
      <a href="#">联系我们</a>
      <a href="#">商户入驻</a>
      <a href="#">App 下载</a>
    </footer>
  </div>
</template>

<script setup>
import TourCard from '@/components/Tour.vue'
import NoteCard from '@/components/NoteCard.vue'

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
  background-color: #fafaf9;
  color: #444;
}

.nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 30px;
  background: #ffffff87;
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
}

.search-box {
  padding: 8px 14px;
  border: 1px solid #ddd;
  border-radius: 999px;
  background: #f8f8f8;
  transition: all 0.3s;
}

.search-box:focus {
  outline: none;
  border-color: #a3d3c0;
  background: #fff;
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

.tour-list,
.notes {
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

.topics h2 {
  margin-bottom: 1rem;
  font-size: 1.4rem;
  color: #6db193;
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

.footer {
  text-align: center;
  padding: 30px;
  background: #f2f2f2;
  color: #777;
  font-size: 0.9rem;
  border-top: 1px solid #e0e0e0;
  margin-top: 40px;
}

.footer a {
  margin: 0 12px;
  color: #777;
  text-decoration: none;
}

</style>
