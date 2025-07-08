<template>
  <div class="admin-rag-management">
    <h2>RAG 知识库管理</h2>
    <p class="description">
      在这里，您可以管理智能问答系统的知识库内容，确保其数据最新且可供导出。
    </p>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>知识库同步</span>
        </div>
      </template>
      <div class="card-content">
        <p>手动触发RAG知识库的数据同步。此操作会将最新的旅行团数据更新到智能问答系统中。</p>
        <el-button
          type="primary"
          @click="syncRagKnowledgeBase"
          :loading="syncing"
          :disabled="syncing"
          class="action-button"
        >
          <el-icon><Refresh /></el-icon>
          {{ syncing ? '正在同步...' : '立即同步知识库' }}
        </el-button>
        <p v-if="lastSyncTime" class="sync-status">上次同步时间: {{ lastSyncTime }}</p>
      </div>
    </el-card>

    <el-card class="box-card mt-4">
      <template #header>
        <div class="card-header">
          <span>知识库导出</span>
        </div>
      </template>
      <div class="card-content">
        <p>将当前的RAG知识库内容导出为Excel (XLSX) 文件，并获取下载链接。</p>
        <el-button
          type="success"
          @click="exportRagKnowledgeBase"
          :loading="exporting"
          :disabled="exporting"
          class="action-button"
        >
          <el-icon><Download /></el-icon>
          {{ exporting ? '正在生成...' : '导出知识库为XLSX' }}
        </el-button>
        <div v-if="downloadUrl" class="download-link-container">
          <el-alert
            title="知识库已成功导出！"
            type="success"
            show-icon
            :closable="false"
            class="mt-2"
          >
            <p>点击下方链接下载：</p>
            <a :href="downloadUrl" target="_blank" class="download-link" @click="clearDownloadUrl">
              下载知识库文件 (有效期有限)
            </a>
            <p class="text-xs text-gray-500 mt-1">请尽快下载，链接可能在短时间内失效。</p>
          </el-alert>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh, Download } from '@element-plus/icons-vue';
// 直接从你的路径导入 authAxios
import { authAxios } from '@/utils/request'; // 请确保这个路径是正确的

// 状态变量
const syncing = ref(false);
const exporting = ref(false);
const downloadUrl = ref('');
// 从localStorage获取上次同步时间，如果没有则为空字符串
const lastSyncTime = ref(localStorage.getItem('lastRagSyncTime') || '');

// API基础路径，如果 authAxios 已经配置了baseURL，这里可能就不需要了，
// 但为了明确，我们保留这个变量，并在请求时直接使用相对路径。
const API_BASE_URL = '/admin/rag';

// --- 同步知识库 ---
const syncRagKnowledgeBase = async () => {
  ElMessageBox.confirm('确定要手动触发RAG知识库同步吗？这可能需要一些时间。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      syncing.value = true;
      try {
        const response = await authAxios.post(`${API_BASE_URL}/sync`);

        if (response.data.code === 200) {
          ElMessage.success('RAG知识库同步成功！');
          const now = new Date();
          lastSyncTime.value = now.toLocaleString(); // 更新显示的时间
          localStorage.setItem('lastRagSyncTime', lastSyncTime.value); // 存储到localStorage
        } else {
          ElMessage.error(`同步失败: ${response.data.message || '未知错误'}`);
        }
      } catch (error) {
        console.error('同步RAG知识库时发生错误:', error);
        ElMessage.error('同步RAG知识库失败，请检查网络或联系管理员。');
      } finally {
        syncing.value = false;
      }
    })
    .catch(() => {
      ElMessage.info('同步操作已取消。');
    });
};

// --- 导出知识库 ---
const exportRagKnowledgeBase = async () => {
  exporting.value = true;
  downloadUrl.value = ''; // 清除之前的下载链接
  try {
    const response = await authAxios.get(`${API_BASE_URL}/export-xlsx-url`);

    if (response.data.code === 200 && response.data.data && response.data.data.downloadUrl) {
      downloadUrl.value = response.data.data.downloadUrl;
      ElMessage.success('知识库导出链接已生成，请点击下载！');
    } else {
      ElMessage.error(`导出失败: ${response.data.message || '未能获取下载链接'}`);
    }
  } catch (error) {
    console.error('导出RAG知识库时发生错误:', error);
    ElMessage.error('导出RAG知识库失败，请检查网络或联系管理员。');
  } finally {
    exporting.value = false;
  }
};

const clearDownloadUrl = () => {
  downloadUrl.value = ''; // 点击链接后清除，避免重复显示或误解
};
</script>

<style scoped>
.admin-rag-management {
  padding: 20px;
  border-radius: 8px;
}

h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 24px;
}

.description {
  color: #666;
  margin-bottom: 25px;
  line-height: 1.6;
}

.box-card {
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #409eff; 
}

.card-content p {
  color: #555;
  margin-bottom: 15px;
  line-height: 1.5;
}

.action-button {
  margin-top: 10px;
  width: 200px; /* 统一按钮宽度 */
}

.action-button .el-icon {
  margin-right: 5px;
}

.sync-status {
  margin-top: 15px;
  font-size: 14px;
  color: #777;
}

.download-link-container {
  margin-top: 20px;
  border-left: 4px solid #67c23a; /* Element Plus success color */
  padding-left: 10px;
}

.download-link {
  color: #409eff;
  text-decoration: underline;
  cursor: pointer;
  font-weight: bold;
}

.download-link:hover {
  color: #66b1ff;
}

.text-xs {
  font-size: 12px;
}
.text-gray-500 {
  color: #909399;
}
.mt-4 {
  margin-top: 1.5rem; /* ~24px */
}
.mt-2 {
  margin-top: 0.5rem; /* ~8px */
}
</style>