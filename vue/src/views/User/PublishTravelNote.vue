<template>
  <div class="publish-note-page">
    <el-card class="publish-card">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="goBack" class="back-button"></el-button>
          <span>{{pageTitle}}</span>
          <el-button type="primary" :disabled="!canPublish" @click="publishNote" class="publish-button">{{headerButtonText}}</el-button>
        </div>
      </template>

            <div class="tag-button-wrapper">
            <el-button type = text @click="toggleTagSelector">{{ showSelector ? '完成添加' : '快来为你的游记添加标签吧！' }}</el-button>
            </div>

            <div v-if="showSelector" class="tag-selector">
              <div class="search-and-category-row"> <el-input v-model="searchName" placeholder="搜索标签" @input="fetchTags" clearable class="search-input">
                <template #prefix> <el-icon><Search /></el-icon>
                </template>
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
            <el-divider v-if="selectedTags.length > 0"></el-divider>

      <div class="content-editor">
        <el-input
          v-model="noteContent"
          type="textarea"
          placeholder="分享你的旅行见闻...（必填）"
          maxlength="500"
          show-word-limit
          class="note-textarea"
          rows="1"
          ref="textareaRef"
        />

        <el-divider class="clickable-divider" @click="showSmartAssistant = !showSmartAssistant">
          <span class="divider-text">
            <el-icon><MagicStick /></el-icon>
            {{ showSmartAssistant ? '收起智能助手' : '或点击此处使用智能助手生成' }}
          </span>
        </el-divider>

        <SmartContentGenerator v-if="showSmartAssistant" />

        <div class="media-upload-area">
          <div v-for="(file, index) in mediaFiles" :key="index" class="media-preview-item">
            <img :src="file.url" class="media-thumbnail" />
            <el-icon class="delete-media-icon" @click="removeMedia(index)"><CircleCloseFilled /></el-icon>
          </div>

          <el-upload
            class="media-uploader"
            action="#"
            :show-file-list="false"
            :on-change="handleMediaChange"
            :before-upload="beforeMediaUpload"
            :http-request="uploadMediaPlaceholder"
            multiple
            accept="image/jpeg,image/png"
            :limit="maxMediaFiles"
          >
            <el-icon><Plus /></el-icon>
            <div class="el-upload__text">添加照片（必填）</div>
          </el-upload>
        </div>

        <div class="title-input-container">
          <el-input
            v-model="noteTitle"
            placeholder="为你的游记添加一个标题（必填）"
            maxlength="50"
            show-word-limit
          />
        </div>

        <div class="location-picker" @click="pickLocation">
          <el-icon><Location /></el-icon>
          <span v-if="selectedLocation">{{ selectedLocation }}</span>
          <span v-else class="placeholder-text">你在哪里？（必填）</span>
          <el-button type="text">选择位置</el-button>
        </div>

        <div class="tips">
          <el-icon><InfoFilled /></el-icon>
          <span>请分享真实美好的旅行瞬间，文明发言哦！</span>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="locationDialogVisible"
      title="选择位置"
      width="400px"
    >
      <el-autocomplete
        v-model="searchLocation"
        :fetch-suggestions="querySearchSpots"
        placeholder="搜索地点"
        clearable
        @select="handleLocationSelect"
        value-key="name" class="location-autocomplete"
      >
        <template #default="{ item }">
          <div class="autocomplete-item">
            <div class="name">{{ item.name }}</div>
            <span class="address">{{ item.address }}</span>
          </div>
        </template>
      </el-autocomplete>

      <template #footer>
        <el-button @click="cancelLocationSelection">取消</el-button>
        <el-button type="primary" @click="confirmLocation" :disabled="!selectedSpotData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch} from 'vue';
import { useRouter,useRoute } from 'vue-router';
import {
  ElCard,
  ElButton,
  ElInput,
  ElUpload,
  ElIcon,
  ElMessage,
  ElDialog,
  ElAutocomplete, 
  ElMessageBox,
  ElTag,       
  ElDivider
} from 'element-plus';
import { ArrowLeft, Plus, Location, InfoFilled, CircleCloseFilled, Search, Cpu } from '@element-plus/icons-vue';
import { publicAxios,authAxios } from '@/utils/request';
import SmartContentGenerator from '@/components/AI/SmartContentGenerator.vue';

const router = useRouter();
const route = useRoute();

const noteContent = ref('');
const noteTitle = ref('');
const maxMediaFiles = 9;

// 智能助手默认不显示
const showSmartAssistant = ref(false);

const existingMediaFiles = ref([]); // 存储从后端加载的已有图片 {id}
const newlyUploadedMediaFiles = ref([]); // 存储用户本次会话新上传的图片 {id, url, type, rawFile}

const selectedLocation = ref(''); // 用于显示已选择的位置文本
const locationDialogVisible = ref(false);
const searchLocation = ref(''); // 用于 autocomplete 输入框
const selectedSpotData = ref(null); // 用于存储 autocomplete 选中的完整地点数据

const editingNoteId = ref(null);
// 计算属性：判断当前是编辑模式还是发布模式
const isEditMode = computed(() => !!editingNoteId.value);
// 计算属性：根据模式更改按钮文本
const headerButtonText = computed(() => isEditMode.value ? '更新' : '发布');
const pageTitle = computed(() => isEditMode.value ? '编辑游记' : '发布游记');
// mediaFiles 是所有图片的计算属性，用于展示
const mediaFiles = computed(() => {
  return [...existingMediaFiles.value, ...newlyUploadedMediaFiles.value];
});

const canPublish = computed(() => {
  return noteContent.value.trim() !== '' && mediaFiles.value.length > 0 && noteTitle.value.trim() !== '';
});

const textareaRef = ref(null);
const minTextareaHeight = 200;

const adjustTextareaHeight = () => {
  nextTick(() => {
    if (textareaRef.value && textareaRef.value.$el) {
      const textarea = textareaRef.value.$el.querySelector('textarea');
      if (textarea) {
        textarea.style.height = 'auto';
        const scrollHeight = textarea.scrollHeight;
        textarea.style.height = `${Math.max(scrollHeight, minTextareaHeight)}px`;
        textarea.style.overflowY = 'hidden';
      }
    }
  });
};

watch(noteContent, () => {
  adjustTextareaHeight();
});

onMounted(() => {
  adjustTextareaHeight();

  // 检查路由参数是否有 ID
  if (route.params.id) {
    editingNoteId.value = route.params.id;
    fetchNoteForEditing(editingNoteId.value); // 如果有 ID，就去获取游记详情并预填充
  }
});

// 获取游记详情用于预编辑
const fetchNoteForEditing = async (id) => {
  ElMessage.info('正在加载游记详情...');
  try {
    const response = await authAxios.get(`/user/posts/${id}`); // 获取游记详情

    if (response.data && response.data.code === 200 && response.data.data) {
      const note = response.data.data;
      console.log('notes:',note);
      noteContent.value = note.content;
      noteTitle.value = note.title === '无标题游记' ? '' : note.title; // 如果是默认标题则清空
      selectedLocation.value = note.spot? note.spot.name:null; // 预填充地点名称
      selectedSpotData.value = note.spot? { uid: note.spotId, name: note.spotName } : null; // 预填充地点数据

      existingMediaFiles.value = []; 
      if (note.imageIdAndUrls) {
        for (const imageId in note.imageIdAndUrls) {
          if (Object.prototype.hasOwnProperty.call(note.imageIdAndUrls, imageId)) {
            const imageUrl = note.imageIdAndUrls[imageId];
            existingMediaFiles.value.push({
              id: imageId,
              url: imageUrl
            });
          }
        }
      }
      newlyUploadedMediaFiles.value = [];

      selectedTags.value = []; 
      if (note.tags && Array.isArray(note.tags) && note.tags.length > 0) {
        selectedTags.value = note.tags.map(tag => ({
          id: tag.id,
          name: tag.name,
          category: tag.category
        }));
      }

      ElMessage.success('游记详情加载成功，可开始编辑！');
      adjustTextareaHeight(); // 确保内容填充后高度正确
    } else {
      ElMessage.error(response.data.message || '获取游记详情失败，请检查ID或稍后再试。');
      router.replace({ name: '用户发布游记' }); // 重定向到发布页面
    }
  } catch (error) {
    console.error('获取游记详情失败:', error);
    ElMessage.error('获取游记详情失败，请检查网络或稍后再试。');
    router.replace({ name: '用户发布游记' }); // 重定向到发布页面
  }
};

const goBack = () => {
  ElMessageBox.confirm('您确定要放弃当前编辑吗？未保存的内容将会丢失。', '提示', {
    confirmButtonText: '确定放弃',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    router.push({ name: '用户个人主页', query: { tab: 'notes' } })
  }).catch(() => {
    // 用户点击取消
  });
};

const handleMediaChange = (file) => {
  if (mediaFiles.value.length >= maxMediaFiles) {
    ElMessage.warning(`最多只能上传 ${maxMediaFiles} 个文件`);
    return false; 
  }
  const isJPGPNG = file.raw.type === 'image/jpeg' || file.raw.type === 'image/png';
  const isLt8000K = file.raw.size / 1024 < 8000; 

  if (!isJPGPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式！');
    return false;
  }
  if (!isLt8000K) {
    ElMessage.error('图片大小不能超过 8MB！');
    return false;
  }
  return true; 
};


const beforeMediaUpload = (rawFile) => {
  return true;
};

const uploadMediaPlaceholder = async (options) => {
  const file = options.file; 
  const checkResult = handleMediaChange({ raw: file });
  if (!checkResult) {
      options.onError(new Error('文件不符合要求')); 
      return;
  }

  ElMessage.info(`正在上传文件: ${file.name}...`);
  const formData = new FormData();
  formData.append('file', file); 

  try {
    const res = await authAxios.post('/user/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        options.onProgress({ percent: percentCompleted }); 
      }
    });

    if (res.data.code ===200&& res.data.data && res.data.data.fileId) {
      const newFileId = res.data.data.fileId; 
      newlyUploadedMediaFiles.value.push({
        id: newFileId, 
        url: URL.createObjectURL(file), 
        type: file.type,
        rawFile: file 
      });
      ElMessage.success(`${file.name} 上传成功！`);
      options.onSuccess(res.data, file); 
    } else {
      ElMessage.error(`${file.name} 上传失败: ${res.data.message || '服务器返回错误'}`);
      options.onError(new Error(res.data.message || '上传失败')); 
    }
  } catch (err) {
    console.error('文件上传失败:', err);
    ElMessage.error(`${file.name} 上传失败: ${err.response?.data?.message || '网络或服务器错误'}`);
    options.onError(err); 
  }
};


const removeMedia = async (index) => {
  const fileToRemove = mediaFiles.value[index]; // 获取要删除的文件对象

  if (!fileToRemove) {
    ElMessage.error('无法找到要删除的文件。');
    return;
  }

  console.log('fileToRemove',fileToRemove)
  // 检查文件是否存在于 existingMediaFiles 中
  const existingFileIndex = existingMediaFiles.value.findIndex(f => f.id === fileToRemove.id);
  // 检查文件是否存在于 newlyUploadedMediaFiles 中
  const newlyUploadedFileIndex = newlyUploadedMediaFiles.value.findIndex(f => f.id === fileToRemove.id);

  // 询问用户是否确认删除 (统一弹窗)
  let confirmMessage = '确定要删除此文件吗？';
  if (isEditMode.value && existingFileIndex !== -1) {
    confirmMessage = '确定要从游记中移除此图片吗？修改将在更新游记后保存。';
  } else if (newlyUploadedFileIndex !== -1) {
    confirmMessage = '确定要删除此上传文件吗？';
  } else {
    // 这是一个没有ID的临时文件，或者其他异常情况
    confirmMessage = '确定要移除此本地文件吗？';
  }


  ElMessageBox.confirm(confirmMessage, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    // 处理文件删除逻辑
    if (existingFileIndex !== -1) { // 如果是已存在的文件
      if (isEditMode.value) {
        // 编辑模式下，仅从前端 existingMediaFiles 数组中移除
        existingMediaFiles.value.splice(existingFileIndex, 1);
        ElMessage.success('图片已从游记中移除，将在更新后生效！');
        console.log('existingMediaFiles.value:',existingMediaFiles.value)
      }
    } else if (newlyUploadedFileIndex !== -1) { // 如果是新上传的文件
      if (fileToRemove.id) { // 如果有文件ID，说明已上传到服务器
        ElMessage.info('正在删除上传文件...');
        try {
          const res = await authAxios.delete(`/user/media/${fileToRemove.id}`);
          if (res.data.code === 200) {
            if (fileToRemove.url && fileToRemove.url.startsWith('blob:')) {
              URL.revokeObjectURL(fileToRemove.url); // 释放本地 blob URL
            }
            newlyUploadedMediaFiles.value.splice(newlyUploadedFileIndex, 1);
            ElMessage.success('新上传的媒体文件已成功删除！');
          } else {
            ElMessage.error(`文件删除失败: ${res.data.message || '服务器返回错误'}`);
          }
        } catch (err) {
          console.error('删除新上传媒体文件失败:', err);
          ElMessage.error(`删除新上传媒体文件失败: ${err.response?.data?.message || '网络错误或服务器异常'}`);
        }
      } else { // 没有文件ID，说明是临时的本地文件 (例如上传前被移除)
        if (fileToRemove.url && fileToRemove.url.startsWith('blob:')) {
          URL.revokeObjectURL(fileToRemove.url); 
        }
        newlyUploadedMediaFiles.value.splice(newlyUploadedFileIndex, 1);
        ElMessage.info('本地未上传文件已移除。');
      }

    } else {
      console.warn("尝试删除一个未识别的文件:", fileToRemove);
      ElMessage.warning('未能识别并移除文件。');
    }
  }).catch(() => {
    ElMessage.info('取消删除');
  });
};

const pickLocation = () => {
  searchLocation.value = selectedLocation.value;
  selectedSpotData.value = null; 
  locationDialogVisible.value = true;
};

const querySearchSpots = async (queryString, cb) => {
  if (!queryString) {
    return cb([]);
  }
  try {
    const res = await publicAxios.get('/public/spots/suggestions', {
      params: {
        keyword: queryString,
        region: '全球' 
      }
    });
    const results = res.data?.data || [];
    cb(results.map(item => ({ ...item, value: item.name })));
  } catch (err) {
    console.error('获取目的地建议失败', err);
    cb([]);
  }
};

const handleLocationSelect = (item) => {
  searchLocation.value = item.name; // 将选中的名称显示在输入框
  selectedSpotData.value = item; // 存储完整的地点数据，可能包含 ID, 地址等
  ElMessage.success(`已选择：${item.name}`);
  console.log('已选择地点',searchLocation.value,selectedSpotData.value)
};

const confirmLocation = () => {
  if (selectedSpotData.value) {
    selectedLocation.value = selectedSpotData.value.name; // 确认后更新显示文本
  } else if (searchLocation.value) {
    selectedLocation.value = searchLocation.value;
  } else {
    selectedLocation.value = ''; // 如果没有输入也没有选择，则清空
  }
  locationDialogVisible.value = false;
};

const cancelLocationSelection = () => {
  locationDialogVisible.value = false;
  searchLocation.value = '';
  selectedSpotData.value = null;
  ElMessage.info('取消选择位置');
};


const publishNote = async () => {
  if (!canPublish.value) {
    ElMessage.warning('请输入内容或上传图片/视频才能发布哦！');
    return;
  }

  ElMessage.info('正在发布游记...');

  // 合并现有图片ID和新上传图片ID，作为最终提交的图片列表
  const allImageIds = [
    ...existingMediaFiles.value.map(item => item.id).filter(id => id), // 现有图片 ID
    ...newlyUploadedMediaFiles.value.map(item => item.id).filter(id => id) // 新上传图片 ID
  ];

  // 从 selectedTags 中提取标签ID
  const selectedTagIds = selectedTags.value.map(tag => tag.id);

  if (noteContent.value.trim() === '' && imageIds.length === 0) {
      ElMessage.warning('请输入游记内容或上传图片！');
      return;
  }

  const requestBody = {
    title: noteTitle.value.trim() === '' ? '无标题游记' : noteTitle.value,
    content: noteContent.value,
    spotId: selectedSpotData.value ? selectedSpotData.value.uid : null,
    imageIds: allImageIds, 
    tagIds: selectedTagIds
  };

  try {
    let res; // 定义一个变量来存储 API 响应

    if (isEditMode.value) {
      ElMessage.info('正在更新游记...');
      console.log('正在提交更新游记信息...', requestBody);
      res = await authAxios.put(`/user/posts/${editingNoteId.value}`, requestBody);
      // --- 额外更新标签 ---
      ElMessage.info('正在更新游记标签...');
      console.log('正在提交更新游记标签...', selectedTagIds);
      try {
        // 调用独立的标签更新接口
        const tagUpdateRes = await authAxios.put(
            `/user/posts/${editingNoteId.value}/tags`, 
            selectedTagIds 
          );
        if (tagUpdateRes.data.code === 200) {
          ElMessage.success('游记标签更新成功！');
        } else {
          ElMessage.error(`游记标签更新失败: ${tagUpdateRes.data.message || '未知错误'}`);
        }
        } catch (tagError) {
          console.error('更新游记标签时发生错误:', tagError);
          ElMessage.error(`更新游记标签失败: ${tagError.response?.data?.message || '网络错误或服务器异常'}`);
        }
    } else {
      ElMessage.info('正在发布游记...');
      console.log('正在提交新游记信息...', requestBody);
      res = await authAxios.post('/user/posts', requestBody);
    }

    if (res.data.code === 200) {
      ElMessage.success(`${isEditMode.value ? '游记更新' : '游记发布'}成功！`);
      console.log(`${isEditMode.value ? '更新' : '发布'}成功`, res.data.data);
      
      noteContent.value = '';
      noteTitle.value = '';
      newlyUploadedMediaFiles.value.forEach(item => { // 释放新上传图片的本地 URL
        if (item.url && item.url.startsWith('blob:')) {
          URL.revokeObjectURL(item.url);
        }
      });
      newlyUploadedMediaFiles.value = []; // 清空新上传图片数组
      selectedLocation.value = '';
      selectedSpotData.value = null;
      searchLocation.value = '';
      editingNoteId.value = null; // 重置编辑ID，回到发布状态
      selectedTags.value = [];
      adjustTextareaHeight();
      // 根据模式跳转到不同的页面
      if (isEditMode.value) {
        router.push({ name: 'EditNote', params: { id: res.data.data.id } });
      } else {
        // 发布成功后，跳转到用户个人主页
        router.push('/user/me');
      }
    } else {
      ElMessage.error(`发布失败: ${postRes.data.message || '未知错误'}`);
    }

  } catch (error) {
    console.error('发布游记时发生错误:', error);
    ElMessage.error(`发布游记失败: ${error.response?.data?.message || '网络错误或服务器异常'}`);
  }
};

//标签相关
const showSelector = ref(false)
const searchName = ref('')
const category = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const tagList = ref([])
const selectedTags = ref([])
// AI 推荐标签相关状态
const aiLoading = ref(false); // AI 推荐加载状态
const aiSuggestedTags = ref([]); // AI 推荐的标签列表
const aiHasSearched = ref(false); // 是否尝试过 AI 推荐


const toggleTagSelector = () => {
  showSelector.value = !showSelector.value
   if (showSelector.value) {
    fetchTags(); 
    aiSuggestedTags.value = []; 
    aiHasSearched.value = false; 
  }
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

  const res = await publicAxios.get('/public/tags', { params })
  if (res.data.code === 200) {
    tagList.value = res.data.data.content
    total.value = res.data.data.totalElements
    console.log('标签数据：', res.data)
  }
}

const handlePageChange = (val) => {
  page.value = val
  fetchTags();
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

//  AI 推荐标签方法
const getAiSuggestedTags = async () => {
  const title = noteTitle.value.trim();
  const content = noteContent.value.trim();

  if (!title && !content) {
    ElMessage.warning('请输入游记标题或内容，才能获取 AI 推荐标签！');
    return;
  }

  aiLoading.value = true;
  aiHasSearched.value = true;
  aiSuggestedTags.value = []; // 清空之前的 AI 推荐结果

  try {
    const response = await authAxios.post('/chat/suggest-tags', {
      title: title,
      content: content,
    });

    if (response.data.code === 200) {
      if (response.data.data && response.data.data.length > 0) {
        aiSuggestedTags.value = response.data.data;
        ElMessage.success('AI 标签推荐成功！');
      } else {
        ElMessage.info('AI 未能推荐任何标签，请尝试修改内容。');
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

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

.publish-note-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 30px 20px;
  box-sizing: border-box;
  background-color: #f0f2f5;
  font-family: 'Noto Sans SC', sans-serif;
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.publish-card {
  width: 100%;
  max-width: 600px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  background-color: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.card-header span {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.back-button {
  color: #606266;
  border-color: transparent;
  background-color: transparent;
}
.back-button:hover {
  background-color: #f0f0f0;
}

.content-editor {
  padding: 20px 0;
}

.note-textarea {
  font-size: 1rem;
  line-height: 1.6;
  margin-bottom: 20px;
  min-height: 280px;
}
.note-textarea :deep(.el-textarea__inner) {
  box-shadow: none !important;
  border: none !important;
  resize: none !important;
  font-size: 1rem;
  line-height: 1.6;
  padding: 0;
  min-height: 200px !important;
  overflow-y: hidden !important;
}
.note-textarea :deep(.el-input__count) {
  bottom: -15px;
  right: 0;
  color: #999;
}


/* 媒体上传区域 */
.media-upload-area {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  margin-bottom: 20px;
}

.media-preview-item {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e0e0e0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
}

.media-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.delete-media-icon {
  position: absolute;
  top: 5px;
  right: 5px;
  color: #f56c6c;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  cursor: pointer;
  font-size: 1.2em;
  transition: transform 0.2s;
}
.delete-media-icon:hover {
  transform: scale(1.1);
}

.media-uploader {
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: border-color 0.3s ease;
  background-color: #fafafa;
}

.media-uploader:hover {
  border-color: #409eff;
}

.media-uploader .el-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 5px;
}

.media-uploader .el-upload__text {
  font-size: 12px;
  color: #8c939d;
}

.title-input-container {
  margin-bottom: 20px;
}
.title-input-container :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 1px solid #eee;
  border-radius: 0;
  padding-left: 0;
}
.title-input-container :deep(.el-input__inner) {
  font-size: 1rem;
  padding: 0;
}
.title-input-container :deep(.el-input__count) {
  bottom: 0px;
  right: 0;
  color: #999;
}


/* 位置选择 */
.location-picker {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
  cursor: pointer;
}
.location-picker .el-icon {
  font-size: 1.2em;
  color: #4bb6b0;
  margin-right: 8px;
}
.location-picker span {
  flex-grow: 1;
  color: #333;
}
.location-picker .placeholder-text {
  color: #999;
}

.tips {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 0.9em;
}
.tips .el-icon {
  margin-right: 8px;
  color: #909399;
}

/* 位置选择对话框内的样式 */
.location-autocomplete {
  width: 100%; /* 让输入框填充对话框宽度 */
}

.autocomplete-item {
  line-height: normal;
  padding: 7px 0;
}
.autocomplete-item .name {
  text-overflow: ellipsis;
  overflow: hidden;
  font-size: 14px;
  color: #333;
}
.autocomplete-item .address {
  font-size: 12px;
  color: #999;
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