<template>
  <div class="newgroup">
    <div class="create-tour-package-page">
      <div class="header-section">
        <h2 class="page-title">
          <el-icon><Plus /></el-icon> {{ isEditMode ? '编辑旅行团' : '发布新的旅行团' }}
        </h2>
        <p class="page-subtitle">
          {{ isEditMode ? '在这里更新您的旅行团行程，让信息保持最新！' : '在这里创建您独一无二的旅行团行程，让更多人发现精彩！' }}
        </p>
        <el-button type="info" :icon="ArrowLeft" class="back-to-profile-btn" @click="goBack">
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
            <div class="tag-button-wrapper">
              <el-button type="text" @click="toggleTagSelector">{{ showSelector ? '完成添加' : '快来为你的旅行团添加标签吧！' }}</el-button>
            </div>

            <div v-if="showSelector" class="tag-selector">
              <div class="search-and-category-row">
                <el-input v-model="searchName" placeholder="搜索标签" @input="fetchTags" clearable class="search-input">
                  <template #prefix> <el-icon><Search /></el-icon> </template>
                </el-input>
                <el-select v-model="category" placeholder="选择分类" @change="fetchTags" clearable class="category-select">
                  <el-option label="主题" value="THEME" />
                  <el-option label="受众" value="TARGET_AUDIENCE" />
                  <el-option label="目的地" value="DESTINATION" />
                  <el-option label="特色" value="FEATURE" />
                </el-select>
              </div>

              <div class="tag-list">
                <el-tag
                  v-for="tag in tagList"
                  :key="tag.id"
                  :type="isSelected(tag) ? '' : 'info'"
                  @click="toggleTag(tag)"
                  class="tag-item"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
              
              <div class="pagination-wrapper">
                <el-pagination
                  layout="prev, pager, next"
                  :total="total"
                  :page-size="size"
                  :current-page="page"
                  @current-change="handlePageChange"
                  small
                />
              </div>
              <div class="ai-recommend-section">
                <el-button type="" :icon="Cpu" @click="getAiSuggestedTags" :loading="aiLoading">
                  获取 AI 智能推荐标签
                </el-button>
                <span v-if="aiLoading" class="loading-text">AI 正在思考中...</span>
                <div v-if="aiSuggestedTags.length > 0" class="ai-suggested-preview">
                  AI 推荐:
                  <el-tag
                    v-for="tag in aiSuggestedTags"
                    :key="tag.id"
                    size="small"
                    type="success"
                    effect="plain"
                    class="ai-tag-item"
                  >
                    {{ tag.name }}
                  </el-tag>
                  <el-button type="primary" size="small" link @click="applyAiTags">采纳推荐</el-button>
                </div>
                <span v-else-if="aiHasSearched && !aiLoading" class="no-ai-tags-tip">暂无 AI 推荐标签。</span>
              </div>
            </div>

            <div v-if="selectedTags.length > 0" class="selected-tags">
              <div class="selected-tags-content"> <span class="selected-tags-label">已选标签：</span> 
                <el-tag
                  v-for="tag in selectedTags"
                  :key="tag.id"
                  closable
                  @close="removeTag(tag)"
                  class="selected-tag-item" >
                  {{ tag.name }}
                </el-tag>
              </div>
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
                @change="onTravelDaysChange"
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

            <el-form-item>
                <el-divider class="clickable-divider" @click="showSmartAssistant = !showSmartAssistant">
                  <span class="divider-text">
                    <el-icon><MagicStick /></el-icon>
                    {{ showSmartAssistant ? '收起智能文案助手' : '或使用智能文案助手生成' }}
                  </span>
                </el-divider>
            </el-form-item>
            <SmartContentGenerator v-if="showSmartAssistant" />

            <el-form-item label="🖼️ 团主图">
              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :on-change="handleTourImageChange"
                :on-remove="handleTourImageRemove"
                :file-list="imageFileList"
                :limit="5"
                accept="image/jpeg,image/png"
                :multiple="true"
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
                v-if="day.spots.length === 0"
                description="点击右上方按钮，为这一天添加精彩的地点或活动！"
                :image-size="60"
                class="empty-spot-list"
              ></el-empty>
              <el-card
                v-for="(spot, i) in day.spots"
                :key="spot.uid"
                class="spot-card"
                shadow="hover"
              >
                <div class="spot-card-content">
                  <div class="spot-main-info">
                    <span class="spot-index">{{ i + 1 }}.</span>
                    <div class="spot-title-text">{{ spot.name }}</div>
                    <el-button type="danger" :icon="Delete" circle size="small" class="delete-spot-btn" @click="removeSpot(index, i)"></el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-card>

        <div class="submit-section" v-if="dailySchedules.length > 0">
          <el-button type="success" size="large" :icon="Check" class="submit-button" @click="submitTourPackage">
            {{ isEditMode ? '更新旅行团' : '发布旅行团' }}
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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue';
import { Search, Plus, InfoFilled, Calendar, List, Location, Check, Delete, ArrowLeft,Cpu } from '@element-plus/icons-vue';
import { publicAxios, authAxios } from '@/utils/request';
import { ElMessage, ElMessageBox ,ElTag, ElDivider } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import SmartContentGenerator from '@/components/AI/SmartContentGenerator.vue';

const router = useRouter();
const route = useRoute();

// --- 响应式数据 ---
const tourId = ref(null); // 如果有值，表示编辑模式
const title = ref('');
const travelDays = ref(1);
const dailySchedules = ref([]); // 存储每日行程，每个元素包含 { routeName, routeDescription, spots: [] }
const detailDescription = ref('');

// 图片管理
const imageFileList = reactive([]); // Element Plus el-upload 绑定的文件列表
const existingTourImageFileIds = reactive([]); // 存储从后端加载的图片ID
const newlyUploadedTourImageFileIds = reactive([]); // 存储用户本次会话新上传的图片ID

// 标签相关
const showSelector = ref(false);
const searchName = ref('');
const category = ref('');
const page = ref(1);
const size = 10;
const total = ref(0);
const tagList = ref([]);
const selectedTags = ref([]);
// AI 推荐标签相关状态
const aiLoading = ref(false); // AI 推荐加载状态
const aiSuggestedTags = ref([]); // AI 推荐的标签列表
const aiHasSearched = ref(false); // 是否尝试过 AI 推荐

// 景点搜索弹窗相关
const spotDialogVisible = ref(false);
const spotKeyword = ref('');
const spotResults = ref([]);
const loading = ref(false);
const activeDayIndex = ref(null); // 记录当前操作是哪一天

// --- 计算属性 ---
const isEditMode = computed(() => !!tourId.value); // 判断是否为编辑模式

const showSmartAssistant = ref(false); // 控制智能文案生成组件的显示/隐藏

// --- 生命周期钩子 ---
onMounted(() => {
  if (route.query.tourId) {
    const tourId = route.query.tourId;
    console.log("正在加载要编辑的旅行团 ID:", tourId);
    fetchTourForEditing(tourId);
  } else {
    // 新建旅行团模式
    generateDays();
  }
});

// --- 监听器 ---
watch(travelDays, (newVal, oldVal) => {
  // 只有在天数发生有效变化时才重新生成行程框架
  if (newVal > 0 && newVal !== oldVal) {
    generateDays();
  }
});

// 返回按钮逻辑
const goBack = () => {
  ElMessageBox.confirm('您确定要放弃当前编辑/发布吗？未保存的内容将会丢失。', '提示', {
    confirmButtonText: '确定放弃',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    router.push({ path: '/merchant/me', query: { activeTab: 'tourManagement' } }); 
  }).catch(() => {
    // 用户点击取消
  });
};

const fetchTourForEditing = async (id) => {
  if (!id) {
    ElMessage.error('缺少旅行团ID，无法加载详情。');
    // 确保至少生成1天的行程框架
    generateDays(); 
    return;
  }

  try {
    const response = await authAxios.get(`/dealer/travel-packages/travel-packages/${id}`); 
    if (response.data.code === 200 && response.data.data) {
      const tourToEdit = response.data.data;

      console.log('获取旅行团详情成功',tourToEdit)
      tourId.value = tourToEdit.id;
      title.value = tourToEdit.title;
      travelDays.value = tourToEdit.durationInDays;
      detailDescription.value = tourToEdit.detailedDescription;

      // 填充标签
      selectedTags.value = []; 
      if (tourToEdit.tags && Array.isArray(tourToEdit.tags)) {
        selectedTags.value.push(...tourToEdit.tags); 
      }

      // 填充图片信息
      //imageFileList.splice(0); // 清空文件列表
      //existingTourImageFileIds.splice(0); // 清空现有图片ID
      //newlyUploadedTourImageFileIds.splice(0); // 清空新上传图片ID

      imageFileList.splice(0); // 清空 Element Plus Upload 组件显示的文件列表
      existingTourImageFileIds.splice(0); // 清空现有图片ID
      newlyUploadedTourImageFileIds.splice(0); // 清空新上传图片ID

      if (tourToEdit.imageIdAndUrls) {
        for (const imageId in tourToEdit.imageIdAndUrls) {
          if (Object.prototype.hasOwnProperty.call(tourToEdit.imageIdAndUrls, imageId)) {
            const imageUrl = tourToEdit.imageIdAndUrls[imageId];
            imageFileList.push({
              name: `existing-image-${imageId}`, 
              url: imageUrl, 
              status: 'success', 
              uid: imageId 
            });
            existingTourImageFileIds.push(imageId);
          }
        }
      }

      // 填充每日行程
      dailySchedules.value.splice(0); // 清空现有行程
      if (tourToEdit.routes && Array.isArray(tourToEdit.routes)) {
        tourToEdit.routes.forEach(day => {
          dailySchedules.value.push({
            routeName: day.name || '',
            routeDescription: day.description || '',
            spots: day.spots && Array.isArray(day.spots) ? day.spots.map(spot => ({
              uid: spot.mapProviderUid,
              latitude: spot.latitude,
              longitude: spot.longititude,
              name: spot.name,
            })) : []
          });
        });
      } else {
        // 如果后端没有返回行程数据，但有天数，则生成空框架
        generateDays();
      }

      ElMessage.success('旅行团详情加载成功，可开始编辑！');
    } else {
      ElMessage.error(response.data.message || '获取旅行团详情失败！');
      generateDays(); // 失败也生成空框架
    }
  } catch (error) {
    console.error('获取旅行团详情时发生错误:', error);
    ElMessage.error('网络请求失败，无法加载旅行团详情。');
    generateDays(); // 失败也生成空框架
  }
};



// --- 标签相关方法 ---
const toggleTagSelector = () => {
  showSelector.value = !showSelector.value;
   if (showSelector.value) {
    fetchTags(); // 每次打开标签选择器都刷新标签列表
    aiSuggestedTags.value = []; // 清空 AI 推荐结果
    aiHasSearched.value = false; // 重置 AI 搜索状态
  }
};

const fetchTags = async () => {
  const params = {
    name: searchName.value,
    category: category.value,
    page: page.value,
    size,
    sortBy: 'createdTime',
    sortDirection: 'DESC'
  };
  try {
    const res = await publicAxios.get('/public/tags', { params }); 
    if (res.data.code === 200) {
      tagList.value = res.data.data.content;
      total.value = res.data.data.totalElements;
    }
  } catch (error) {
    console.error('获取标签失败:', error);
    ElMessage.error('获取标签失败，请重试。');
  }
};

const handlePageChange = (val) => {
  page.value = val;
  fetchTags();
};

const toggleTag = (tag) => {
  const index = selectedTags.value.findIndex(t => t.id === tag.id);
  if (index >= 0) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tag);
  }
};

const removeTag = (tag) => {
  selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id);
};

const isSelected = (tag) => {
  return selectedTags.value.some(t => t.id === tag.id);
};

// AI 推荐标签方法 
const getAiSuggestedTags = async () => {
  const tourTitle = title.value.trim();
  const tourContent = detailDescription.value.trim();

  if (!tourTitle && !tourContent) {
    ElMessage.warning('请输入旅行团标题或详细描述，才能获取 AI 推荐标签！');
    return;
  }

  aiLoading.value = true;
  aiHasSearched.value = true;
  aiSuggestedTags.value = []; // 清空之前的 AI 推荐结果

  try {
    const response = await authAxios.post('/chat/suggest-tags', {
      title: tourTitle,
      content: tourContent,
    });

    if (response.data.code === 200) {
      if (response.data.data && response.data.data.length > 0) {
        // 过滤掉 AI 推荐的标签中，已经存在于已选标签列表中的标签
        const newSuggestedTags = response.data.data.filter(aiTag => 
          !selectedTags.value.some(selectedTag => selectedTag.id === aiTag.id)
        );
        aiSuggestedTags.value = newSuggestedTags;
        if (newSuggestedTags.length > 0) {
          ElMessage.success('AI 标签推荐成功！');
        } else {
          ElMessage.info('AI 未能推荐新的标签，可能与现有标签重合。');
        }
      } else {
        ElMessage.info('AI 未能推荐任何标签，请尝试修改标题或描述。');
      }
    } else {
      ElMessage.error(response.data.message || '获取 AI 推荐标签失败');
    }
  } catch (error) {
    console.error('获取 AI 推荐标签失败:', error);
    ElMessage.error(`获取 AI 推荐标签失败: ${error.response?.data?.message || '网络或服务器错误'}`);
  } finally {
    aiLoading.value = false;
  }
};

// 采纳 AI 推荐的标签 
const applyAiTags = () => {
  if (aiSuggestedTags.value.length === 0) {
    ElMessage.warning('当前没有 AI 推荐的标签可以采纳。');
    return;
  }

  aiSuggestedTags.value.forEach(aiTag => {
    // 如果该 AI 标签不在已选标签中，则添加
    if (!selectedTags.value.some(sTag => sTag.id === aiTag.id)) {
      selectedTags.value.push(aiTag);
    }
  });
  ElMessage.success('已采纳 AI 推荐的标签！');
  aiSuggestedTags.value = []; // 采纳后清空 AI 推荐列表
  aiHasSearched.value = false; // 重置 AI 搜索状态
};

// --- 图片上传处理 ---
const handleTourImageChange = async (file, fileList) => {
  console.log('fileList:',fileList,imageFileList)
  // 检查是否超出最大限制
  if (fileList.length > 5) {
    ElMessage.warning(`最多只能上传 ${5} 张图片`);
    fileList.splice(fileList.indexOf(file), 1);
    imageFileList.splice(fileList.indexOf(file), 1); // 移除当前添加的文件
    return false;
  }

  const isJPGPNG = file.raw.type === 'image/jpeg' || file.raw.type === 'image/png';
  const isLt1000K = file.raw.size / 1024 < 1000; 

  if (!isJPGPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式！');
    fileList.splice(fileList.indexOf(file), 1);
    imageFileList.splice(fileList.indexOf(file), 1); // 移除不符合要求的文件
    return false;
  }
  if (!isLt1000K) {
    ElMessage.error('图片大小不能超过 1MB！');
    fileList.splice(fileList.indexOf(file), 1);
    imageFileList.splice(fileList.indexOf(file), 1); // 移除不符合要求的文件
    return false;
  }


  ElMessage.info(`正在上传文件: ${file.name}...`);
  const formData = new FormData();
  formData.append('file', file.raw);

  try {
    const res = await authAxios.post('/user/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        file.percentage = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        file.status = 'uploading';
      }
    });

    if (res.data.code === 200 && res.data.data && res.data.data.fileId) {
      const newFileId = res.data.data.fileId;
      console.log('上传了图片:',res)
      newlyUploadedTourImageFileIds.push(newFileId); // 记录新上传的图片ID
      file.status = 'success'; // 更新文件状态
      file.response = res.data; // 存储后端响应
      file.uid = res.data.data.fileId
      ElMessage.success(`${file.name} 上传成功！`);
    } else {
      file.status = 'fail'; // 更新文件状态
      ElMessage.error(`${file.name} 上传失败: ${res.data.message || '服务器返回错误'}`);
      // 从 Element Plus 文件列表中移除失败的文件
      const indexToRemove = imageFileList.findIndex(item => item.uid === file.uid);
      if (indexToRemove !== -1) {
        imageFileList.splice(indexToRemove, 1);
      }
    }
  } catch (err) {
    file.status = 'fail'; // 更新文件状态
    console.error('文件上传失败:', err);
    ElMessage.error(`${file.name} 上传失败: ${err.response?.data?.message || '网络或服务器错误'}`);
    // 从 Element Plus 文件列表中移除失败的文件
    const indexToRemove = imageFileList.findIndex(item => item.uid === file.uid);
    if (indexToRemove !== -1) {
      imageFileList.splice(indexToRemove, 1);
    }
  }
};

const handleTourImageRemove = async (file) => {
  console.log('尝试移除的文件:', file);
  const fileId = file.uid; 

  // 判断图片是已存在（从后端加载的）还是新上传的
  const isExistingImage = existingTourImageFileIds.includes(fileId);
  const isNewlyUploadedImage = newlyUploadedTourImageFileIds.includes(fileId);
  console.log('新上传的图片',newlyUploadedTourImageFileIds)

  if (!isExistingImage && !isNewlyUploadedImage) {
    console.warn("尝试移除一个未识别的图片文件，直接从UI移除:", file);
    ElMessage.warning('未能识别该图片，已从显示中移除。');
    const idxInDisplayList = imageFileList.findIndex(item => item.uid === fileId);
    if (idxInDisplayList !== -1) {
      imageFileList.splice(idxInDisplayList, 1);
    }
    return true; 
  }
  if (isExistingImage) {
    const index = existingTourImageFileIds.findIndex(id => id === fileId);
    if (index !== -1) {
      existingTourImageFileIds.splice(index, 1);
      ElMessage.success('图片已标记为移除，将在更新旅行团后生效。');
    } else {
      // 理论上不会发生，但以防万一
      console.warn(`尝试移除的现有图片ID ${fileId} 未在列表中找到。`);
    }
  } else if (isNewlyUploadedImage) {
    ElMessage.info('正在删除新上传的文件...');
    try {
      const response = await authAxios.delete(`/user/media/${fileId}`); 
      if (response.data.code === 200) {
        const index = newlyUploadedTourImageFileIds.findIndex(id => id === fileId);
        if (index !== -1) {
          newlyUploadedTourImageFileIds.splice(index, 1);
        }
        if (file.url && file.url.startsWith('blob:')) {
          URL.revokeObjectURL(file.url);
        }
        ElMessage.success('新上传的图片已从服务器移除！');
      } else {
        ElMessage.error(response.data.message || '从服务器删除图片失败。');
        return false; 
      }
    } catch (error) {
      console.error('调用删除接口失败:', error);
      ElMessage.error(`删除失败: ${error.response?.data?.message || '网络错误或服务器问题。'}`);
      return false; 
    }
  }

  return true;
};


// --- 行程生成 ---
const generateDays = () => {
  if (!travelDays.value || travelDays.value <= 0) {
    ElMessage.warning('请输入有效的旅行天数。');
    return;
  }
  // 根据 travelDays.value 创建相应数量的空行程日对象
  const newSchedules = Array.from({ length: travelDays.value }, (_, index) => {
    // 尽量保留已有的数据
    if (dailySchedules.value[index]) {
      return dailySchedules.value[index];
    } else {
      return {
        routeName: '',
        routeDescription: '',
        spots: []
      };
    }
  });
  dailySchedules.value = newSchedules;
  ElMessage.success(`已为您生成 ${travelDays.value} 天的行程框架！现在可以开始添加内容了。`);
};

// --- 景点搜索与管理 ---
const openSpotDialog = (dayIndex) => {
  activeDayIndex.value = dayIndex; // 记录当前操作是哪一天
  spotKeyword.value = ''; // 清空搜索关键词
  spotResults.value = []; // 清空上次搜索结果
  spotDialogVisible.value = true;
};

const querySearchSpots = async (queryString, cb) => {
  if (!queryString) return cb([]);
  try {
    const res = await publicAxios.get('/public/spots/suggestions', {
      params: {
        keyword: queryString,
        region: '全球' // 或者可以根据需要设置特定区域
      }
    });
    const results = res.data?.data || [];
    cb(results.map(item => ({ ...item, value: item.name }))); // value 属性用于 Element Plus Autocomplete 显示
  } catch (err) {
    console.error('获取目的地建议失败', err);
    cb([]);
  }
};

const handleSpotSelect = (item) => {
  if (activeDayIndex.value === null || activeDayIndex.value < 0 || activeDayIndex.value >= dailySchedules.value.length) {
    ElMessage.error('内部错误：无法确定要添加地点的日期。');
    return;
  }

  // 检查是否已添加重复景点 (通过uid判断唯一性)
  const currentDaySpots = dailySchedules.value[activeDayIndex.value].spots;
  const existingSpot = currentDaySpots.find(s => s.uid === item.uid);
  if (existingSpot) {
    ElMessage.warning(`"${item.name}" 已经添加到 DAY ${activeDayIndex.value + 1} 了，请勿重复添加。`);
    spotDialogVisible.value = false;
    spotKeyword.value = '';
    spotResults.value = [];
    return;
  }

  currentDaySpots.push({
    name: item.name,
    uid: item.uid,
    // 如果需要，可以在这里添加更多景点信息，如 location, description 等
  });
  
  ElMessage.success(`已将 "${item.name}" 添加到 DAY ${activeDayIndex.value + 1}。`);
  spotDialogVisible.value = false; // 关闭搜索弹窗
  spotKeyword.value = ''; 
  spotResults.value = []; 
};

const removeSpot = (dayIndex, spotIndex) => {
  const spotName = dailySchedules.value[dayIndex].spots[spotIndex].name;
  dailySchedules.value[dayIndex].spots.splice(spotIndex, 1);
  ElMessage.info(`已将 "${spotName}" 从行程中移除。`);
};


// --- 最终提交 ---
const submitTourPackage = async () => {
  // 数据校验
  if (!title.value) { ElMessage.error('请输入旅行团标题。'); return; }
  if (!travelDays.value || travelDays.value <= 0) { ElMessage.error('请输入有效的旅行天数。'); return; }
  if (!detailDescription.value) { ElMessage.error('请写入详细描述。'); return; }
  if (dailySchedules.value.length === 0) { ElMessage.error('请生成行程框架并添加行程。'); return; }

  // 合并所有图片ID
  const allImageIds = [
    ...existingTourImageFileIds,
    ...newlyUploadedTourImageFileIds
  ];

  if (allImageIds.length === 0) { ElMessage.error('请上传至少一张团主图。'); return; }

  // 获取选中的标签ID列表
  const selectedTagIds = selectedTags.value.map(tag => tag.id);

  // 构建要提交的 dailySchedules 数组
  const formattedDailySchedules = dailySchedules.value.map((day, index) => ({
    dayNumber: index + 1,
    routeName: day.routeName || '',
    routeDescription: day.routeDescription || '',
    spotUids: day.spots.map(spot => spot.uid) 
  }));

  // 构建提交数据对象
  const tourPackageData = {
    title: title.value,
    durationInDays: travelDays.value,
    dailySchedules: formattedDailySchedules,
    imgIds: allImageIds, 
    tagIds: selectedTagIds,
    detailedDescription: detailDescription.value, 
  };

  console.log('即将提交的旅行团数据:', JSON.stringify(tourPackageData, null, 2));
  console.log('dailySchedules',dailySchedules)

  try {
    let response;
    if (isEditMode.value) {
      ElMessage.info('正在更新旅行团...');
      response = await authAxios.put(`/dealer/travel-packages/${tourId.value}`, tourPackageData);
    } else {
      ElMessage.info('正在发布旅行团...');
      response = await authAxios.post('/dealer/travel-packages', tourPackageData);
    }

    if (response.data.code === 200) {
      ElMessage.success(`${isEditMode.value ? '旅行团更新' : '旅行团发布'}成功！🎉`);
      // 清空所有表单数据
      title.value = '';
      travelDays.value = 1;
      detailDescription.value = '';
      dailySchedules.value = [];
      imageFileList.splice(0);
      existingTourImageFileIds.splice(0);
      newlyUploadedTourImageFileIds.splice(0);
      selectedTags.value.splice(0);
      tourId.value = null; // 重置 tourId 为 null，回到发布模式
      
      router.push('/merchant/me'); // 跳转到商家个人中心
    } else {
      ElMessage.error(response.data.message || `${isEditMode.value ? '旅行团更新' : '旅行团发布'}失败，请重试。`);
    }
  } catch (error) {
    console.error('操作旅行团时发生错误:', error);
    ElMessage.error(`操作失败: ${error.response?.data?.message || '网络错误或服务器异常'}`);
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
/*发布新的旅行团*/
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
/* 在这里创建您独一无二的旅行团行程，让更多人发现精彩！*/
.page-subtitle {
  font-size: 1.1rem;
  color: #666;
  font-weight: 400;
}
/* 返回按钮 */
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

/* --添加标签相关-- */
.tag-button-wrapper {
  text-align: center; /* 使内部行内元素居中 */
  margin-bottom: 20px; /* 给按钮下方留点空间 */
}
.tag-selector {
  margin-top: 20px; /* 调整与上方按钮的间距 */
  margin-bottom: 20px; /* 调整与下方标签列表的间距 */
}
.search-and-category-row {
  display: flex; 
  gap: 20px; /* 搜索框与分类之间的间距 */
  margin-bottom: 15px; /* 与下方标签列表的间距 */
}
.search-input {
  flex-grow: 1; /* 让搜索框占据剩余的所有可用空间 */
}
.category-select {
  width: 150px; /* 设置选择分类的固定宽度 */
}
.pagination-wrapper { 
  display: flex;
  justify-content: center; /* 水平居中 */
  margin-top: 15px; /* 分页组件顶部间距 */
}
.selected-tags-label{
  font-size: 14px;      
  color: #606266;       
  font-weight: bold;    
  white-space: nowrap; 
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
  margin-top: 10px;/* 与上方标签选择器间距 */
  margin-bottom: 30px;/* 已选标签区域下方间距** */
}
.selected-tags-content{
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.selected-tags-label {
  font-weight: 500; 
  color: #555;     
  font-size: 14px;  
  white-space: nowrap; /* 防止文本换行 */
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
  background-color: #41b394;
  border-color: #5fbba5;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.submit-button .el-icon {
  margin-right: 10px;
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
  border-color: #53bcaa;
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

/* AI 推荐标签样式 */
.ai-recommend-section {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px dashed #eee;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.ai-recommend-section .loading-text, .ai-recommend-section .no-ai-tags-tip {
  color: #909399;
  font-size: 13px;
}

.ai-suggested-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  width: 100%; /* 确保它占据整行 */
}

.ai-tag-item {
  cursor: default; /* AI推荐的标签不应该直接点击选择 */
}


</style>