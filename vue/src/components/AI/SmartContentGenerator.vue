<template>
  <div class="smart-content-generator">
    <el-card class="box-card generator-card">
      <template #header>
        <div class="card-header">
          <span>智能文案生成</span>
        </div>
      </template>
      <div class="card-content">
        <p class="description">
          根据您的需求和角色，AI将为您生成精彩的游记标题、描述或旅行团营销文案。
        </p>

        <el-form :model="formData" label-position="top">
          <el-form-item label="您的核心信息与要求：">
            <el-input
              v-model="formData.text"
              :rows="5"
              type="textarea"
              placeholder="例如：
              （商家）目的地：张家界，特色：玻璃栈道，自然风光，3日游，目标客户：年轻人，希望突出刺激感和美景。
              （用户）主题：第一次去云南，感受：阳光，古城，美食，希望写得活泼有趣。"
              class="core-text-input"
              resize="vertical"
            ></el-input>
            <div class="tip-text">
              <el-icon><InfoFilled /></el-icon>
              请输入旅行团路线信息或您的游记感受，越详细，生成内容越精准。
            </div>
          </el-form-item>

          <el-form-item label="您的角色：">
            <el-radio-group v-model="formData.role">
              <el-radio-button label="MERCHANT">商家（发布旅行团）</el-radio-button>
              <el-radio-button label="USER">用户（发布游记）</el-radio-button>
            </el-radio-group>
            <div class="tip-text">
              <el-icon><InfoFilled /></el-icon>
              选择角色以获得更符合场景的AI文案。
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="generateContent"
              :loading="generating"
              :disabled="generating || !formData.text.trim()"
              class="generate-button"
            >
              <el-icon><Lightning /></el-icon>
              {{ generating ? 'AI正在创作...' : '智能生成文案' }}
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider v-if="generatedContent">生成结果</el-divider>

        <div v-if="generatedContent" class="generated-content-section">
          <el-input
            v-model="generatedContent"
            :rows="8"
            type="textarea"
            readonly
            class="generated-text-output"
            resize="vertical"
          ></el-input>
          <div class="action-buttons">
            <el-button type="success" @click="copyContent" class="copy-button">
              <el-icon><CopyDocument /></el-icon> 复制文案
            </el-button>
            <el-button @click="clearContent">
              <el-icon><Delete /></el-icon> 清空
            </el-button>
          </div>
        </div>

        <el-empty v-else-if="!generating && hasGenerated" description="未能生成内容，请尝试调整输入或稍后重试。"></el-empty>
        <el-empty v-else description="请输入您的核心信息和角色，让AI为您创作！"></el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { Lightning, InfoFilled, CopyDocument, Delete } from '@element-plus/icons-vue';
import { authAxios } from '@/utils/request';

const formData = reactive({
  text: '',
  role: 'MERCHANT' // 默认选择商家角色
});

const generatedContent = ref('');
const generating = ref(false);
const hasGenerated = ref(false); // 标记是否已经尝试过生成内容

const generateContent = async () => {
  if (!formData.text.trim()) {
    ElMessage.warning('请输入您的核心信息与要求！');
    return;
  }

  generating.value = true;
  hasGenerated.value = true;
  generatedContent.value = ''; // 清空上次生成的内容

  try {
    const response = await authAxios.post('/chat/generate-content', formData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (response.data.code === 200 && response.data.data && response.data.data.content) {
      generatedContent.value = response.data.data.content;
      ElMessage.success('文案已成功生成！');
    } else {
      ElMessage.error(`生成失败: ${response.data.message || '未能生成内容'}`);
    }
  } catch (error) {
    console.error('调用智能内容生成接口失败:', error);
    ElMessage.error('文案生成失败，请检查网络或稍后再试。');
  } finally {
    generating.value = false;
  }
};

const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(generatedContent.value);
    ElMessage.success('文案已复制到剪贴板！');
  } catch (err) {
    console.error('复制失败:', err);
    ElMessage.error('复制失败，请手动复制。');
  }
};

const clearContent = () => {
  generatedContent.value = '';
  hasGenerated.value = false; // 重置状态，显示默认Empty
};
</script>

<style scoped>
.smart-content-generator {
  padding: 20px;
  border-radius: 10px;
}

.card-header {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.description {
  color: #606266;
  margin-bottom: 25px;
  line-height: 1.6;
}

.core-text-input {
  margin-bottom: 10px;
}

.tip-text {
  font-size: 13px;
  color: #909399;
  margin-top: 5px;
  display: flex;
  align-items: center;
}

.tip-text .el-icon {
  margin-right: 5px;
}

.generate-button {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  margin-top: 15px; /* 调整按钮与上方内容的间距 */
}

.generate-button .el-icon {
  margin-right: 8px;
}

.generated-content-section {
  margin-top: 20px;
  border-radius: 4px;
  padding: 15px;
}

.generated-text-output {
  margin-bottom: 15px;
  font-size: 14px;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 10px; /* 按钮之间的间距 */
  justify-content: flex-end; /* 按钮靠右对齐 */
}

.action-buttons .el-button {
  font-size: 14px;
}

.el-divider {
  margin-top: 30px;
  margin-bottom: 30px;
}
</style>