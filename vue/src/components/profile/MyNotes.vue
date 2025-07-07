<template>
  <div class="notes-header">
    <el-button type="primary" :icon="Plus" @click="goToPublishTravelNote">发布新游记</el-button>
    <el-button type="primary" :icon="MapLocation" @click="showFootprintMap" class="footprint-btn">我的足迹</el-button>
  </div>
    <div v-loading="noteLoading">
      <div v-if="notes.length > 0" class="card-grid">
        <el-card
          v-for="note in notes"
          :key="note.id"
          class="note-card hover-card"
          @click.stop="goToDetail(note.id)"
        >
          <img
            v-if="note.coverImageUrl"
            :src="note.coverImageUrl"
            class="note-img"
          />
          <div class="card-info">
            <h3>{{ note.title }}</h3>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无游记"></el-empty>
    </div>

    <div class="load-more-container">
      <el-button
        v-if="hasMoreNotes"
        type="text"
        :loading="noteLoading"
        @click="fetchNotes(false)"
        class="load-more-btn"
      >
        {{ noteLoading ? "加载中..." : "加载更多游记" }}
      </el-button>
      <p v-else-if="notes.length > 0 && noMoreNotes" class="no-more-text">
        已加载全部游记
      </p>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { authAxios } from "@/utils/request";
import { ElButton, ElCard, ElEmpty, ElMessage } from "element-plus";
import { Plus,MapLocation } from "@element-plus/icons-vue";

const router = useRouter();
const notes = ref([]);
const currentPage = ref(1);
const pageSize = ref(6); // 每页记录数
const totalNotes = ref(0); // 总游记数量
const noteLoading = ref(false); // 加载状态
const noMoreNotes = ref(false); // 是否没有更多游记了

// 判断是否还有更多游记
const hasMoreNotes = computed(() => {
  return notes.value.length < totalNotes.value && !noMoreNotes.value;
});

const fetchNotes = async (reset = false) => {
  if (noteLoading.value) return; // 如果正在加载中，则跳过
  if (noMoreNotes.value && !reset) {
    // 如果已经没有更多，且不是重置操作，则跳过
    ElMessage.info("没有更多游记了。");
    return;
  }

  noteLoading.value = true;
  if (reset) {
    currentPage.value = 0; // 重置页码
    notes.value = []; // 清空现有游记
    noMoreNotes.value = false; // 重置没有更多的状态
  }

  const nextPage = currentPage.value + 1; // 下一页的页码

  try {
    const response = await authAxios.get("/user/posts", {
      params: {
        page: nextPage, // 请求下一页的数据
        size: pageSize.value,
        sortBy: "createdTime",
        sortDirection: "DESC",
      },
    });

    if (response.data.code === 200 && response.data.data) {
      const newNotes = response.data.data.content;
      totalNotes.value = response.data.data.totalElements;

      if (reset) {
        notes.value = newNotes; // 重置时直接替换
      } else {
        notes.value = [...notes.value, ...newNotes]; // 追加新数据
      }

      currentPage.value = nextPage; // 更新当前页码

      // 判断是否加载完所有数据
      if (notes.value.length >= totalNotes.value) {
        noMoreNotes.value = true;
        ElMessage.info("所有游记已加载完毕。");
      } else {
        // ElMessage.success(`成功加载第 ${nextPage} 页游记！`); // 加载成功提示
      }
    } else {
      ElMessage.warning("未能获取游记数据，请稍后再试。");
      noMoreNotes.value = true;
    }
  } catch (error) {
    console.error("获取游记列表失败:", error);
    ElMessage.error("获取游记列表失败，请检查网络或稍后再试。");
    noMoreNotes.value = true;
  } finally {
    noteLoading.value = false;
  }
};

// 跳转详情页并传递游记id
const goToDetail = (id) => {
  console.log("查看游记详情:", id);
  router.push({ name: "EditNote", params: { id: id } });
};

// 跳转到发布游记页面
const goToPublishTravelNote = () => {
  router.push("/user/publish-travel-note");
};

const showFootprintMap = () => {
  router.push("/mymap");
}

// 在这里调用 onMounted，只获取游记数据
onMounted(() => {
  fetchNotes(true);
});
</script>

<style scoped>
.card-info {
  flex-grow: 1;
  text-align: left;
}

.card-info h3 {
  margin: 0;
  font-size: 17px;
  color: #333;
  font-weight: 600;
}

.card-info p {
  margin: 5px 0 0;
  color: #777;
  font-size: 13px;
  line-height: 1.4;
}

.note-img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
  margin-right: 20px;
  flex-shrink: 0;
}

/* 我的游记部分的标题和按钮容器 */
.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.notes-section-title {
  font-size: 1.5rem; 
  font-weight: 600;
  color: #333;
  margin: 0; 
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 10px; 
}

/* 卡片 */
.note-card {
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

.no-more-text {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 15px;
}

.load-more-container {
  text-align: center;
  padding: 20px;
  margin-top: 30px; 
}

</style>
