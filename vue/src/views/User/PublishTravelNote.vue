<template>
  <div class="publish-note-page">
    <el-card class="publish-card">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="goBack" class="back-button"></el-button>
          <span>发布游记</span>
          <el-button type="primary" :disabled="!canPublish" @click="publishNote" class="publish-button">发布</el-button>
        </div>
      </template>

      <div class="content-editor">
        <el-input
          v-model="noteContent"
          type="textarea"
          placeholder="分享你的旅行见闻..."
          maxlength="500"
          show-word-limit
          class="note-textarea"
          rows="1"
          ref="textareaRef"
        />

        <div class="media-upload-area">
          <div v-for="(file, index) in mediaFiles" :key="index" class="media-preview-item">
            <img v-if="file.type.startsWith('image')" :src="file.url" class="media-thumbnail" />
            <video v-else-if="file.type.startsWith('video')" :src="file.url" controls class="media-thumbnail"></video>
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
            <div class="el-upload__text">添加照片</div>
          </el-upload>
        </div>

        <div class="title-input-container">
          <el-input
            v-model="noteTitle"
            placeholder="为你的游记添加一个标题（可选）"
            maxlength="50"
            show-word-limit
          />
        </div>

        <div class="location-picker" @click="pickLocation">
          <el-icon><Location /></el-icon>
          <span v-if="selectedLocation">{{ selectedLocation }}</span>
          <span v-else class="placeholder-text">你在哪里？</span>
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
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  ElCard,
  ElButton,
  ElInput,
  ElUpload,
  ElIcon,
  ElMessage,
  ElDialog,
  // 移除了 ElRadioGroup, ElRadio
  ElAutocomplete, // 新增 ElAutocomplete
  ElMessageBox
} from 'element-plus';
import { ArrowLeft, Plus, Location, InfoFilled, CircleCloseFilled } from '@element-plus/icons-vue';
import { publicAxios,authAxios } from '@/utils/request';

const router = useRouter();

const noteContent = ref('');
const noteTitle = ref('');
const mediaFiles = ref([]);
const maxMediaFiles = 9;

const selectedLocation = ref(''); // 用于显示已选择的位置文本
const locationDialogVisible = ref(false);
const searchLocation = ref(''); // 用于 autocomplete 输入框
const selectedSpotData = ref(null); // 用于存储 autocomplete 选中的完整地点数据

const canPublish = computed(() => {
  return noteContent.value.trim() !== '' || mediaFiles.value.length > 0;
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
});


const goBack = () => {
  ElMessageBox.confirm('您确定要放弃当前编辑吗？未保存的内容将会丢失。', '提示', {
    confirmButtonText: '确定放弃',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    router.back();
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
  const isLt1000K = file.raw.size / 1024 < 1000; 

  if (!isJPGPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式！');
    return false;
  }
  if (!isLt1000K) {
    ElMessage.error('图片大小不能超过 1MB！');
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
      mediaFiles.value.push({
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
  const fileToRemove = mediaFiles.value[index];

  if (!fileToRemove || !fileToRemove.id) {
      if (fileToRemove && fileToRemove.url.startsWith('blob:')) {
          URL.revokeObjectURL(fileToRemove.url);
      }
      mediaFiles.value.splice(index, 1);
      ElMessage.info('本地文件已移除');
      return;
  }

  ElMessageBox.confirm('确定要从服务器删除此文件吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      ElMessage.info('正在删除文件...');
      const res = await authAxios.delete(`/user/media/${fileToRemove.id}`);

      if (res.data.code === 200) {
        if (fileToRemove.url.startsWith('blob:')) {
          URL.revokeObjectURL(fileToRemove.url); 
        }
        mediaFiles.value.splice(index, 1); 
        ElMessage.success('媒体文件已成功删除！');
      } else {
        ElMessage.error(`文件删除失败: ${res.data.message || '服务器返回错误'}`);
      }
    } catch (err) {
      console.error('删除媒体文件失败:', err);
      ElMessage.error(`删除媒体文件失败: ${err.response?.data?.message || '网络错误或服务器异常'}`);
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

  const imageIds = mediaFiles.value.map(item => item.id).filter(id => id); 

  if (noteContent.value.trim() === '' && imageIds.length === 0) {
      ElMessage.warning('请输入游记内容或上传图片！');
      return;
  }

  try {
    const requestBody = {
      title: noteTitle.value.trim() === '' ? '无标题游记' : noteTitle.value,
      content: noteContent.value,
      spotId: selectedSpotData.value ? selectedSpotData.value.id : null,
      imageIds: imageIds, 
    };

    ElMessage.info('正在提交游记信息...');
    const postRes = await authAxios.post('/user/posts', requestBody); 

    if (postRes.data.code === 200) {
      ElMessage.success('游记发布成功！');
      noteContent.value = '';
      noteTitle.value = '';
      mediaFiles.value.forEach(item => {
        if (item.url.startsWith('blob:')) {
          URL.revokeObjectURL(item.url); 
        }
      });
      mediaFiles.value = []; 
      selectedLocation.value = '';
      selectedSpotData.value = null;
      searchLocation.value = '';
      adjustTextareaHeight();
      router.push('/user/me'); 
    } else {
      ElMessage.error(`发布失败: ${postRes.data.message || '未知错误'}`);
    }

  } catch (error) {
    console.error('发布游记时发生错误:', error);
    ElMessage.error(`发布游记失败: ${error.response?.data?.message || '网络错误或服务器异常'}`);
  }
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
  border-bottom: 1px solid #f0f0f0;
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
</style>