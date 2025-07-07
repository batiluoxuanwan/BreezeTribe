<template>
  <div class="admin-report-handling-container">
    <el-card class="box-card" v-loading="loading">
      <el-tabs v-model="filterStatus" @tab-change="handleTabChange" class="report-tabs">
        <el-tab-pane label="待处理" name="PENDING"></el-tab-pane>
        <el-tab-pane label="已受理" name="ACCEPTED"></el-tab-pane>
        <el-tab-pane label="已驳回" name="REJECTED"></el-tab-pane>
        <el-tab-pane label="所有举报" name=""></el-tab-pane>
      </el-tabs>

      <el-table :data="reports" style="width: 100%" border stripe class="report-table">
        <el-table-column label="举报人" width="120">
          <template #default="{ row }">
            <el-link v-if="row.reporter && row.reporter.id" type="primary" @click="openUserProfile(row.reporter.id)">
              {{ row.reporter.username || '匿名用户' }}
            </el-link>
            <span v-else>匿名用户</span>
          </template>
        </el-table-column>
        <el-table-column label="被举报内容" min-width="280">
          <template #default="{ row }">
            <div class="reported-content-snippet">
              <strong>类型:</strong> {{ formatItemType(row.itemType) }}
              <br />
              <strong>内容:</strong> {{ row.summary || '无内容摘要' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" min-width="90">
          <template #default="{ row }">
            {{ formatReason(row.reason) }}
          </template>
        </el-table-column>
        <el-table-column label="附加信息" prop="additionalInfo" min-width="150"></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="140">
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button
                type="success"
                size="small"
                @click="confirmAction(row,'accept')"
              >
                接受
              </el-button>
              <el-button
                type="info"
                size="small"
                @click="confirmAction(row,'reject')"
              >
                驳回
              </el-button>
            </template>
            <template v-else>
              <el-button type="info" size="small" disabled>
                已处理
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalReports"
        background
        class="pagination"
      >
      </el-pagination>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="30%"
      center
    >
      <span>{{ dialogContent }}</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="executeConfirmedAction">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { authAxios } from '@/utils/request'; 
import { useAuthStore } from '@/stores/auth'; 

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.userId);

const reports = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const totalReports = ref(0);
const filterStatus = ref('PENDING'); // 默认显示待处理

// 弹窗相关 
const dialogVisible = ref(false);
const dialogTitle = ref('');
const dialogContent = ref('');
const selectedReport = ref(null); 
const dialogConfirmAction = ref(null);

// 用户主页弹窗相关
const showUserProfilePopup = ref(false);
const selectedUserProfileId = ref(null);
const isViewingUserFriend = ref(false); 

// 聊天弹窗相关
const showChatDialog = ref(false);
const selectedUserId = ref(null);


// 格式化举报项类型
const formatItemType = (type) => {
  switch (type) {
    case 'TRAVEL_PACKAGE':
      return '旅行团';
    case 'TRAVEL_POST':
      return '游记';
    case 'PACKAGE_COMMENT':
      return '旅行团评论';
    case 'POST_COMMENT':
      return '游记评论';
    default:
      return type;
  }
};

// 格式化举报原因
const formatReason = (reason) => {
  switch (reason) {
    case 'SPAM_AD':
      return '垃圾广告';
    case 'PORNOGRAPHIC':
      return '色情内容';
    case 'ILLEGAL_CONTENT':
      return '违法内容';
    case 'PERSONAL_ATTACK':
      return '人身攻击';
    case 'OTHER':
      return '其他';
    default:
      return reason;
  }
};

// 格式化举报状态
const formatStatus = (status) => {
  switch (status) {
    case 'PENDING':
      return '待处理';
    case 'ACCEPTED':
      return '已受理';
    case 'REJECTED':
      return '已驳回';
    default:
      return status;
  }
};

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'PENDING':
      return 'warning';
    case 'ACCEPTED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    default:
      return 'info';
  }
};

// 格式化时间戳
const formatTime = (timeString) => {
  if (!timeString) return '';
  const date = new Date(timeString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};


// 获取举报列表
const fetchReports = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'createdTime',
      sortDirection: 'DESC'
    };
    if (filterStatus.value !== 'ALL') {
      params.status = filterStatus.value;
    }

    console.log('params,',params)

    const response = await authAxios.get('/admin/content', { params });

    if (response.data.code === 200 && response.data.data) {
      console.log('获取举报内容:',response.data.data)
      reports.value = response.data.data.content;
      totalReports.value = response.data.data.totalElements;
      //ElMessage.success('举报列表加载成功！');
    } else {
      ElMessage.error(response.data.message || '获取举报列表失败。');
    }
  } catch (error) {
    console.error('获取举报列表请求失败:', error);
    ElMessage.error('获取举报列表网络错误或权限不足！');
  } finally {
    loading.value = false;
  }
};

// 处理分页大小改变
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1; // 改变页面大小时回到第一页
  fetchReports();
};

// 处理当前页码改变
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
  fetchReports();
};

// 处理 Tab 切换
const handleTabChange = () => {
  currentPage.value = 1; // 切换 Tab 时回到第一页
  fetchReports();
};

// 确认操作的弹窗
const confirmAction = (report, action) => {
  selectedReport.value = report;
  switch (action) {
    case 'accept':
      dialogTitle.value = '受理举报';
      dialogContent.value = `确定要受理此举报吗？（举报原因: ${formatReason(report.reason)}）。受理将删除相关违规内容。`;
      dialogConfirmAction.value = () => acceptReport(report.id);
      break;
    case 'reject':
      dialogTitle.value = '驳回举报';
      dialogContent.value = `确定要驳回此举报吗？（举报原因: ${formatReason(report.reason)}）`;
      dialogConfirmAction.value = () => rejectReport(report.id);
      break;
    default:
      return;
  }
  dialogVisible.value = true;
};

// 执行确认后的操作
const executeConfirmedAction = async () => {
  if (dialogConfirmAction.value) {
    await dialogConfirmAction.value();
  }
  dialogVisible.value = false;
  selectedReport.value = null;
  dialogConfirmAction.value = null;
};

// 受理举报
const acceptReport = async (reportId) => {
  try {
    const response = await authAxios.post(`/admin/content/reports/${reportId}/accept`);
    if (response.data.code === 200) {
      ElMessage.success('举报已成功受理，违规内容已处理！');
      fetchReports(); // 刷新列表
    } else {
      ElMessage.error(response.data.message || '受理举报失败。');
    }
  } catch (error) {
    console.error('受理举报请求失败:', error);
    ElMessage.error('受理举报网络错误或权限不足！');
  }
};

// 驳回举报 
const rejectReport = async (reportId) => {
  try {
    const response = await authAxios.post(`/admin/content/reports/${reportId}/reject`);
    if (response.data.code === 200) {
      ElMessage.success('举报已驳回！');
      fetchReports(); // 刷新列表
    } else {
      ElMessage.error(response.data.message || '驳回举报失败。');
    }
  } catch (error) {
    console.error('驳回举报请求失败:', error);
    ElMessage.error('驳回举报网络错误或权限不足！');
  }
};


// 组件挂载时获取数据
onMounted(() => {
  fetchReports();
});

// 监听分页参数或过滤状态变化，重新获取数据
watch([currentPage, pageSize, filterStatus], () => {
  fetchReports();
}, { immediate: true });

</script>

<style scoped>
.box-card {
  max-width: 1120px;
  margin: 0 auto;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.2em;
  font-weight: bold;
}

.report-tabs {
  margin-bottom: 20px;
}

.report-table {
  margin-top: 20px;
}

.reported-content-snippet {
  font-size: 0.9em;
  color: #606266;
  line-height: 1.5;
}

.reported-content-snippet strong {
  color: #303133;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}
</style>