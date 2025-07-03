<template>
  <div class="newgroup">
    <div class="create-tour-package-page">
      <div class="header-section">
        <h2 class="page-title">
          <el-icon><Plus /></el-icon> 发布新的旅行团
        </h2>
        <p class="page-subtitle">在这里创建您独一无二的旅行团行程，让更多人发现精彩！</p>
        <el-button type="info" :icon="ArrowLeft" class="back-to-profile-btn" @click="goToProfile">
          返回
        </el-button>
      </div>

      <div class="form-sections-container">
        <el-card class="form-card basic-info-card">
          <template #header>
            <div class="card-header">
              <h3>基本信息</h3>
              <el-icon><InfoFilled /></el-icon>
            </div>
          </template>
          <div>
          <el-button @click="toggleTagSelector">{{ showSelector ? '完成添加' : '添加标签' }}</el-button>

          <div v-if="showSelector" class="tag-selector">
            <el-input v-model="searchName" placeholder="搜索标签" @input="fetchTags" clearable />
            <el-select v-model="category" placeholder="选择分类" @change="fetchTags" clearable>
              <el-option label="主题" value="THEME" />
              <el-option label="受众" value="TARGET_AUDIENCE" />
              <el-option label="目的地" value="DESTINATION" />
              <el-option label="特色" value="FEATURE" />
            </el-select>

            <div class="tag-list">
              <el-tag
                v-for="tag in tagList"
                :key="tag.id"
                :type="isSelected(tag) ? 'success' : 'info'"
                @click="toggleTag(tag)"
                class="tag-item"
              >
                {{ tag.name }}
              </el-tag>
            </div>

            <el-pagination
              layout="prev, pager, next"
              :total="total"
              :page-size="size"
              :current-page="page"
              @current-change="handlePageChange"
              small
            />
          </div>

          <div v-if="selectedTags.length > 0" class="selected-tags">
            <h4>已选标签：</h4>
            <el-tag
              v-for="tag in selectedTags"
              :key="tag.id"
              closable
              @close="removeTag(tag)"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </div>
          <el-form label-width="100px" label-position="left">
            <el-form-item label="📝 标题">
              <el-input v-model="title" placeholder="请输入旅行团的吸引人的标题，例如：魔都寻宝：上海经典三日游"></el-input>
            </el-form-item>

            <el-form-item label="📅 旅行天数">
              <el-input-number
                v-model="travelDays"
                :min="1"
                :max="365"
                placeholder="输入旅行天数"
                style="width: 100%;"
              ></el-input-number>
            </el-form-item>

            <el-form-item label="📃 详细描述">
              <el-input
                v-model="detailDescription"
                type="textarea"
                :rows="5"
                placeholder="在这里详细描述旅行团的特色、包含服务、注意事项等"
              ></el-input>
            </el-form-item>

            <el-form-item label="🖼️ 团主图">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :on-change="handleTourImageUpload"
                :on-remove="handleTourImageRemove"
                :file-list="tourImageUrls.map(url => ({
                  url: url
                }))"
                :limit="5" accept="image/jpeg,image/png"
              >
                <el-icon><Plus /></el-icon>
                <template #tip>
                  <div class="el-upload__tip">
                    上传旅行团主封面图片，最多可上传5张 (JPG/PNG格式，单张不超过 1000KB)
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="generateDays" :disabled="!travelDays || travelDays <= 0" class="generate-btn">
                <el-icon><Calendar /></el-icon> 生成行程框架
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="form-card itinerary-card" v-if="dailySchedules.length > 0">
          <template #header>
            <div class="card-header">
              <h3>行程安排</h3>
              <el-icon><List /></el-icon>
            </div>
          </template>

          <div v-for="(day, index) in dailySchedules" :key="index" class="day-section">
            <div class="day-header">
              <h4>DAY {{ index + 1 }}</h4>
              <el-button type="primary" link @click="openSpotDialog(index)">
                <el-icon><Plus /></el-icon> 添加地点/活动
              </el-button>
            </div>

            <el-form label-width="90px" label-position="left" class="daily-route-form">
              <el-form-item label="路线名称">
                <el-input v-model="day.routeName" placeholder="例如：上海市区经典一日游"></el-input>
              </el-form-item>
              <el-form-item label="路线描述">
                <el-input
                  v-model="day.routeDescription"
                  type="textarea"
                  :rows="2"
                  placeholder="简述当天行程亮点或交通方式"
                ></el-input>
              </el-form-item>
            </el-form>

            <div class="spot-list">
              <el-empty
                v-if="day.length === 0"
                description="点击右上方按钮，为这一天添加精彩的地点或活动！"
                :image-size="60"
                class="empty-spot-list"
              ></el-empty>
              <el-card
                v-for="(spot, i) in day"
                :key="i"
                class="spot-card"
                shadow="hover"
              >
                <div class="spot-card-content">
                  <div class="spot-main-info">
                    <span class="spot-index">{{ i + 1 }}.</span>
                    <div class="spot-title-text">{{ spot.name }}</div>
                    <el-button type="danger" :icon="Delete" circle size="small" class="delete-spot-btn" @click="removeSpot(index, i)"></el-button>
                  </div>

                  <!-- <div class="note-preview" @click="openSpotDetailDialog(index, i)">
                    <el-icon><Edit /></el-icon>
                    <div class="note-content">
                      <div v-if="spot.timeRange && spot.timeRange.length === 2 && spot.timeRange[0] && spot.timeRange[1]" class="time-range-display">
                        <el-icon><Clock /></el-icon> {{ spot.timeRange[0] }} - {{ spot.timeRange[1] }}
                      </div>
                      <div v-if="spot.note" class="actual-note">{{ spot.note }}</div>
                      <el-image
                        v-if="spot.imageUrl"
                        :src="spot.imageUrl"
                        :preview-src-list="[spot.imageUrl]"
                        fit="cover"
                        class="spot-image-preview"
                      />
                      <div v-if="!spot.note && (!spot.timeRange || spot.timeRange.length === 0) && !spot.imageUrl" class="empty-note">
                        点击此处，为该地点添加时间、备注或图片
                      </div>
                    </div>
                  </div> -->
                </div>
              </el-card>
            </div>
          </div>
        </el-card>

        <div class="submit-section" v-if="dailySchedules.length > 0">
          <el-button type="success" size="large" :icon="Check" class="submit-button" @click="submitTourPackage">
            发布旅行团
          </el-button>
        </div>
      </div>

      <el-dialog v-model="spotDialogVisible" title="搜索并添加景点/活动" width="600px" class="spot-search-dialog">
        <el-autocomplete
          v-model="spotKeyword"
          :fetch-suggestions="querySearchSpots"
          placeholder="输入关键词搜索景点或活动，例如：外滩、上海迪士尼"
          clearable
          @select="handleSpotSelect"
          style="width: 100%"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #loading>
            <div class="loading-text">加载中...</div>
          </template>
          <template #default="{ item }">
            <div class="autocomplete-item">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-location">{{ item.province }} {{ item.city }} {{ item.district }}</div>
            </div>
          </template>
        </el-autocomplete>

        <div v-if="spotResults.length" class="search-results-list">
          <el-card
            v-for="(item, idx) in spotResults"
            :key="idx"
            class="result-card"
            shadow="hover"
            @click="handleSpotSelect(item)"
          >
            <strong>{{ item.name }}</strong>
            <div class="result-location">
              <el-icon><Location /></el-icon> {{ item.province }} {{ item.city }} {{ item.district }}
            </div>
          </el-card>
        </div>
        <div v-if="loading" class="loading-status">搜索中...</div>

        <template #footer>
          <el-button @click="spotDialogVisible = false">取消</el-button>
        </template>
      </el-dialog>

      <!-- <el-dialog v-model="spotDetailDialogVisible" :title="currentSpot.name || '编辑地点/活动详情'" width="500px" class="spot-detail-dialog">
        <el-form label-width="80px">
          <el-form-item label="备注">
            <el-input
              v-model="currentSpot.note"
              type="textarea"
              placeholder="输入此地点/活动的详细备注或介绍，例如：推荐午餐地点、最佳观景时间"
              rows="4"
            />
          </el-form-item>
          <el-form-item label="时间段">
            <el-time-picker
              is-range
              v-model="currentSpot.timeRange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="图片">
            <div class="image-upload-wrapper">
              <el-upload
                class="image-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="(file) => handleImageUpload(file)"
                accept="image/jpeg,image/png"
              >
                <img v-if="currentSpot.imageUrl" :src="currentSpot.imageUrl" class="uploaded-image" />
                <el-icon v-else class="uploader-icon"><Plus /></el-icon>
              </el-upload>
              <el-button
                v-if="currentSpot.imageUrl"
                type="danger"
                :icon="Delete"
                circle
                size="small"
                class="remove-image-btn"
                @click="currentSpot.imageUrl = ''"
                title="移除图片"
              ></el-button>
            </div>
            <div class="el-upload__tip">
              点击上传图片 (JPG/PNG格式，大小不超过 500KB)
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" @click="spotDetailDialogVisible = false">确定</el-button>
        </template>
      </el-dialog> -->
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, useId } from 'vue'
import { Search, Edit, Upload, Plus, InfoFilled, Calendar, List, Location, Check, Clock, Delete, ArrowLeft } from '@element-plus/icons-vue'
import { publicAxios,authAxios } from '@/utils/request'
import { ElMessage,ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const goToProfile = () => {
  router.push('/merchant/me')
}

// 旅行团整体信息
const title = ref('')
const travelDays = ref(1) 
const dailySchedules = ref([]) 
const detailDescription = ref(null) 
const activeDayIndex = ref(null) 
const tourImageUrls = reactive([]);
const uploadedBackendFileIds = reactive([]); 
// 景点搜索弹窗相关
const spotDialogVisible = ref(false)
const spotKeyword = ref('')
const spotResults = ref([])
const loading = ref(false)

// // 景点详情/备注弹窗相关
// const spotDetailDialogVisible = ref(false)
// const currentSpot = reactive({
//   name: '',
//   note: '',
//   timeRange: [],
//   imageUrl: '',
//   dayIndex: null, // 记录当前编辑的景点属于哪一天
//   spotIndex: null, // 记录当前编辑的景点是当天的第几个
// })

const showSelector = ref(false)
const searchName = ref('')
const category = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const tagList = ref([])
const selectedTags = ref([])

const toggleTagSelector = () => {
  showSelector.value = !showSelector.value
  if (showSelector.value) fetchTags()
}

const fetchTags = async () => {
  const params = {
    name: searchName.value,
    category: category.value,
    page: page.value,
    size,
    sortBy: 'createdTime',
    sortDirection: 'DESC'
  }

  const res = await publicAxios.get('/public', { params })
  if (res.data.code === 200) {
    tagList.value = res.data.data.content
    total.value = res.data.data.totalElements
    console.log('标签数据：', res.data)
  }
}

const handlePageChange = (val) => {
  page.value = val
  fetchTags()
}

const toggleTag = (tag) => {
  const index = selectedTags.value.findIndex(t => t.id === tag.id)
  if (index >= 0) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

const removeTag = (tag) => {
  selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id)
}

const isSelected = (tag) => {
  return selectedTags.value.some(t => t.id === tag.id)
}

// --- 图片上传处理 ---
const handleTourImageUpload = async (file) => {
  // 基本文件类型和大小校验
  const isJPGPNG = file.raw.type === 'image/jpeg' || file.raw.type === 'image/png';
  const isLt1000K = file.raw.size / 1024 < 1000; 

  if (!isJPGPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式！');
    return false;
  }
  if (!isLt1000K) {
    ElMessage.error('图片大小不能超过 1MB！');
    return false;
  }

  // 生成前端预览URL (异步操作)
  let previewUrl = '';
  try {
    previewUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.onerror = (error) => reject(error);
      reader.readAsDataURL(file.raw);
    });
  } catch (error) {
    console.error('读取文件进行预览失败:', error);
    ElMessage.error('无法读取图片文件进行预览。');
    return false;
  }

  // 准备 FormData
  const formData = new FormData();
  formData.append('file', file.raw);

  // 发送文件到后端
  try {
    const response = await authAxios.post('/user/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    // 处理后端响应
    if (response.data.code === 200) {
      const uploadedId = response.data.data.fileId; 
      tourImageUrls.push(previewUrl); 
      uploadedBackendFileIds.push(uploadedId);
      console.log(tourImageUrls)

      ElMessage.success('图片上传成功！');
      return true;
    } else {
      ElMessage.error(response.data.message || '图片上传失败，请重试。');
      return false;
    }
  } catch (error) {
    console.error('图片上传失败:', error);
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(`上传失败: ${error.response.data.message}`);
    } else {
      ElMessage.error('网络错误或服务器问题，图片上传失败。');
    }
    return false;
  }
};

// --- 图片删除处理 ---
const handleTourImageRemove = async (file) => {
  // 查找要删除的预览图片在 tourImageUrls 中的索引
  const index = tourImageUrls.findIndex(url => url === file.url);
  if (index === -1) {
    ElMessage.error('未找到要移除的图片。');
    return false;
  }

  // 根据索引获取对应的后端文件ID
  const fileIdToRemove = uploadedBackendFileIds[index]; 

  // 弹出确认框
  try {
    await ElMessageBox.confirm(
      `确定要删除此图片吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    // 用户点击确定，执行后端删除操作
    if (fileIdToRemove) { 
      try {
        const response = await authAxios.delete(`/user/media/${fileIdToRemove}`);
        if (response.data.code === 200) {
          ElMessage.success('图片已从服务器移除！');
          // 从前端预览列表和后端ID列表中移除
          tourImageUrls.splice(index, 1);
          uploadedBackendFileIds.splice(index, 1); // **同步移除对应的后端ID**
          return true;
        } else {
          ElMessage.error(response.data.message || '从服务器删除图片失败。');
          return false;
        }
      } catch (error) {
        console.error('调用删除接口失败:', error);
        if (error.response && error.response.data && error.response.data.message) {
            ElMessage.error(`删除失败: ${error.response.data.message}`);
        } else {
            ElMessage.error('网络错误或服务器问题，图片删除失败。');
        }
        return false;
      }
    } else {
      tourImageUrls.splice(index, 1); // 仅从前端移除
      ElMessage.info('图片已从预览移除，但未与后端文件关联。');
      return true;
    }
  } catch (cancel) {
    // 用户点击了取消，不执行任何操作
    ElMessage.info('已取消删除操作。');
    return false;
  }
};

// --- 行程生成 ---
const generateDays = () => {
  if (!travelDays.value || travelDays.value <= 0) {
    ElMessage.warning('请输入有效的旅行天数。');
    return;
  }
  dailySchedules.value = Array.from({ length: travelDays.value }, () => ([]));
  ElMessage.success(`已为您生成 ${travelDays} 天的行程框架！现在可以开始添加内容了。`);
};

// --- 景点搜索与管理 ---
const openSpotDialog = (dayIndex) => {
  activeDayIndex.value = dayIndex; // 记录当前操作是哪一天
  spotKeyword.value = ''; // 清空搜索关键词
  spotResults.value = []; // 清空上次搜索结果
  spotDialogVisible.value = true;
};

const querySearchSpots =async (queryString, cb) => {
  if (!queryString) return cb([])
  try {
    const res = await publicAxios.get('/public/spots/suggestions', {
      params: {
        keyword: queryString,
        region: '全球' 
      }
    })
    const results = res.data?.data || []
    cb(results.map(item => ({ ...item, value: item.name })))
  } catch (err) {
    console.error('获取目的地建议失败', err)
    cb([])
  }
};

const handleSpotSelect = (item) => {
  if (activeDayIndex.value === null) {
    ElMessage.error('请先点击某天的“添加地点/活动”按钮来选择日期。');
    return;
  }

  // 检查是否已添加重复景点
  const existingSpot = dailySchedules.value[activeDayIndex.value].find(s => s.name === item.name);
  if (existingSpot) {
    ElMessage.warning(`"${item.name}" 已经添加到 DAY ${activeDayIndex.value + 1} 了，请勿重复添加。`);
    spotDialogVisible.value = false;
    spotKeyword.value = '';
    spotResults.value = [];
    return;
  }

  dailySchedules.value[activeDayIndex.value].push({
    name: item.name,
    uid: item.uid
  });
  console.log(item)
  ElMessage.success(`已将 "${item.name}" 添加到 DAY ${activeDayIndex.value + 1}。`);
  spotDialogVisible.value = false; // 关闭搜索弹窗
  spotKeyword.value = ''; 
  spotResults.value = []; 
};

const removeSpot = (dayIndex, spotIndex) => {
  const spotName = dailySchedules.value[dayIndex][spotIndex].name;
  dailySchedules.value[dayIndex].splice(spotIndex, 1);
  ElMessage.info(`已将 "${spotName}" 从行程中移除。`);
};

// // --- 景点详情/备注弹窗 ---
// const openSpotDetailDialog = (dayIndex, spotIndex) => {
//   // 深拷贝景点数据，避免直接修改原始数据导致意外副作用
//   Object.assign(currentSpot, JSON.parse(JSON.stringify(dailySchedules.value[dayIndex][spotIndex])));
//   currentSpot.dayIndex = dayIndex;
//   currentSpot.spotIndex = spotIndex;
//   spotDetailDialogVisible.value = true;
// };

// // 监听弹窗关闭事件，将 currentSpot 的修改同步回原始数据
// watch(spotDetailDialogVisible, (newVal) => {
//   if (!newVal && currentSpot.dayIndex !== null && currentSpot.spotIndex !== null) {
//     const dayIdx = currentSpot.dayIndex;
//     const spotIdx = currentSpot.spotIndex;
//     // 将 currentSpot 的属性更新到原始数据中
//     Object.assign(dailySchedules.value[dayIdx][spotIdx], {
//       note: currentSpot.note,
//       timeRange: currentSpot.timeRange,
//       imageUrl: currentSpot.imageUrl,
//     });
//     // 重置 currentSpot 状态
//     Object.assign(currentSpot, { name: '', note: '', timeRange: [], imageUrl: '', dayIndex: null, spotIndex: null });
//   }
// });

// --- 最终提交 ---
const submitTourPackage = async () => {
  // 数据校验
  if (!title.value) { ElMessage.error('请输入旅行团标题。'); return; }
  if (!travelDays.value) { ElMessage.error('请输入旅行天数。'); return; }
  if (!detailDescription.value) { ElMessage.error('请写入详细描述。'); return; }
  if (dailySchedules.value.length === 0) { ElMessage.error('请生成行程框架并添加行程。'); return; }
  if (uploadedBackendFileIds.length === 0) { ElMessage.error('请上传至少一张团主图。'); return; }

  // 构建要提交的 dailySchedules 数组
  const formattedDailySchedules = dailySchedules.value.map((day, index) => ({
    dayNumber: index + 1,
    routeName: day.routeName || '',
    routeDescription: day.routeDescription || '',
    spotUids: day.map(spot => spot.uid) 
  }));

    // 构建提交数据对象
  const tourPackageData = {
    title: title.value,
    durationInDays: travelDays.value,
    dailySchedules: formattedDailySchedules,
    imgIds: uploadedBackendFileIds, 
  };

  console.log('即将提交的旅行团数据:', JSON.stringify(tourPackageData, null, 2));

  try {
    const response = await authAxios.post('/dealer/travel-packages', tourPackageData);

    if (response.data.code === 200) {
      ElMessage.success('恭喜！您的旅行团已成功发布！请耐心等待审核吧🎉');
      router.push('/merchant/me'); 
    } else {
      ElMessage.error(response.data.message || '旅行团发布失败，请重试。');
    }
  } catch (error) {
    console.error('发布旅行团时发生错误:', error);
    if (error.response && error.response.data && error.response.data.message) {
        ElMessage.error(`发布失败: ${error.response.data.message}`);
    } else {
        ElMessage.error('网络错误或服务器问题，请稍后再试。');
    }
  }
};

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');

.newgroup{
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  min-height: 100vh;
  align-items: center;
  padding: 40px 0;
}

.create-tour-package-page {
  padding: 40px;
  max-width: 1000px; 
  margin: auto;
  font-family: 'Poppins', sans-serif;
  background-color: #f0f2f5; 
  border-radius: 18px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08); 
}

.header-section {
  text-align: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0; /* 分隔线 */
}

.page-title {
  font-size: 2.2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-title .el-icon {
  font-size: 2.2rem;
  margin-right: 10px;
  color: #00796b; 
}

.page-subtitle {
  font-size: 1.1rem;
  color: #666;
  font-weight: 400;
}

.back-to-profile-btn{
  position:absolute;
  top:10px;
  left:10px;
  padding:8px 15px;
  font-size:0.9rem;
  border-radius:8px;
  background-color:#607d8b;
  border-color:#607d8b;
  color:#fff;
  transition:all 0.3s ease;
}

.form-sections-container {
  display: flex;
  flex-direction: column;
  gap: 30px; /* 卡片之间的间距 */
}

.form-card {
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  background-color: #ffffff;
  padding: 25px 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
}

.card-header .el-icon {
  font-size: 1.5rem;
  color: #909399; /* 标题图标颜色 */
}

/* 基本信息卡片 */
.basic-info-card .el-form-item {
  margin-bottom: 20px;
}

.basic-info-card .el-form-item__label {
  font-weight: 500;
  color: #555;
}

.generate-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 1rem;
  letter-spacing: 1px;
  border-radius: 8px;
  background-color: #4CAF50; /* 绿色 */
  border-color: #4CAF50;
  transition: all 0.3s ease;
}

.generate-btn:hover {
  background-color: #43A047;
  border-color: #43A047;
  transform: translateY(-2px);
}

.generate-btn .el-icon {
  margin-right: 8px;
}

/* 行程安排卡片 */
.itinerary-card {
  margin-top: 20px;
}

.day-section {
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 25px;
  background-color: #fdfdfd;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.day-section:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 1px dashed #f0f0f0; /* 虚线分隔 */
  padding-bottom: 10px;
}

.day-header h4 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
  color: #555;
  letter-spacing: 0.5px;
}

.day-header .el-button {
  font-size: 0.95rem;
  font-weight: 500;
  color: #00796b; /* 主题色 */
}

.day-header .el-button:hover {
  text-decoration: underline;
}

.spot-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); /* 响应式网格布局 */
  gap: 15px;
  margin-top: 15px;
}

.empty-spot-list {
  grid-column: 1 / -1; /* 横跨所有列 */
  margin: 20px 0;
  font-size: 0.95rem;
  color: #999;
}

.spot-card {
  border-radius: 10px;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease-in-out;
  display: flex;
  flex-direction: column;
}

.spot-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.spot-card-content {
  padding: 15px;
}

.spot-main-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.spot-index {
  font-weight: 700;
  color: #00796b; /* 主题色 */
  font-size: 1.1rem;
  margin-right: 8px;
}

.spot-title-text {
  flex-grow: 1;
  font-weight: 600;
  font-size: 1.1rem;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.delete-spot-btn {
  color: #f56c6c; /* 红色 */
  border-color: #f56c6c;
  opacity: 0.7;
}

.delete-spot-btn:hover {
  opacity: 1;
  background-color: #fef0f0;
}

/* .note-preview {
  display: flex;
  align-items: flex-start; 
  gap: 10px;
  background-color: #f9fbfb; 
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px dashed #e0e0e0;
  min-height: 70px; 
  transition: all 0.2s ease;
}

.note-preview:hover {
  background-color: #e0f2f1; 
  border-color: #b2dfdb;
}

.note-preview .el-icon {
  font-size: 1.2rem;
  color: #909399; 
  margin-top: 2px;
}

.note-content {
  flex: 1;
  font-size: 0.9rem;
  color: #666;
  line-height: 1.4;
  word-break: break-word; 
}

.time-range-display {
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
}

.time-range-display .el-icon {
  font-size: 0.9rem;
  margin-right: 5px;
  color: #606266;
}

.actual-note {
  margin-bottom: 5px;
}

.spot-image-preview {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  margin-top: 8px;
  object-fit: cover;
  border: 1px solid #eee;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
} */

/* 提交按钮区域 */
.submit-section {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.submit-button {
  padding: 15px 30px;
  font-size: 1.2rem;
  font-weight: 600;
  border-radius: 10px;
  background-color: #2196F3; /* 蓝色作为主要操作按钮 */
  border-color: #2196F3;
  transition: all 0.3s ease;
}

.submit-button:hover {
  background-color: #1976D2;
  border-color: #1976D2;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.submit-button .el-icon {
  margin-right: 10px;
}

/* 弹窗样式覆盖 */
.spot-search-dialog .el-dialog__header,
.spot-detail-dialog .el-dialog__header {
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.spot-search-dialog .el-dialog__title,
.spot-detail-dialog .el-dialog__title {
  font-weight: 600;
  color: #333;
}

.autocomplete-item {
  line-height: normal;
  padding: 7px;
}
.autocomplete-item .item-name {
  text-overflow: ellipsis;
  overflow: hidden;
  font-size: 14px;
  color: #333;
}
.autocomplete-item .item-location {
  font-size: 12px;
  color: #999;
}
.loading-text, .loading-status {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  padding: 10px 0;
}

.search-results-list {
  margin-top: 20px;
  max-height: 300px; /* 限制搜索结果列表的高度 */
  overflow-y: auto; /* 超出部分滚动 */
  padding-right: 10px; /* 为滚动条留出空间 */
}

.search-results-list .result-card {
  margin-bottom: 10px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  padding: 15px;
}

.search-results-list .result-card:hover {
  background-color: #e3f2fd; /* 浅蓝色背景 */
  border-color: #bbdefb;
  transform: translateX(5px);
}

.result-location {
  font-size: 0.85rem;
  color: #777;
  display: flex;
  align-items: center;
  margin-top: 5px;
}

.result-location .el-icon {
  margin-right: 5px;
  font-size: 0.9rem;
  color: #606266;
}

/* 图片上传组件样式 */
.image-upload-wrapper {
  position: relative;
  width: 120px; /* 统一尺寸 */
  height: 120px;
}

.image-uploader {
  width: 100%;
  height: 100%;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: border-color 00.3s ease;
}
.image-uploader:hover {
  border-color: #409eff;
}
.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.el-upload__tip {
  font-size: 0.8rem;
  color: #999;
  margin-top: 5px;
}
.remove-image-btn {
  position: absolute;
  top: -8px; /* 调整位置 */
  right: -8px; /* 调整位置 */
  z-index: 10;
  background-color: rgba(255, 0, 0, 0.7);
  border-color: rgba(255, 0, 0, 0.7);
  color: #fff;
  transition: all 0.2s ease;
}
.remove-image-btn:hover {
  background-color: red;
  border-color: red;
  transform: scale(1.1);
}

.tag-list {
  margin: 10px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
}

.selected-tags {
  margin-top: 10px;
}
</style>