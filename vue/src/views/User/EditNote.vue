<!-- 游记详情 -->
 <template>
  <div class="my-travel-note-detail-container">
    <el-button type="info" class="back-button" @click="goBackToMyNotes">
      <el-icon><ArrowLeftBold /></el-icon>返回
    </el-button>

    <el-card v-if="loading" class="loading-card">
      <el-skeleton animated :rows="6"></el-skeleton>
      <div class="loading-text">正在加载游记详情...</div>
    </el-card>

    <el-card v-else-if="error" class="error-card">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetchNoteDetail(route.params.id)">重试</el-button>
        </template>
      </el-result>
    </el-card>

    <el-card v-else-if="!note" class="empty-card">
      <el-empty description="未找到该游记或游记不存在"></el-empty>
    </el-card>

    <el-card v-else class="note-detail-card">
      <div class="author-actions">
        <el-button type="primary" :icon="Edit" @click="editNote">编辑游记</el-button>
        <el-popconfirm
          title="确定要删除这篇游记吗？删除后将无法恢复！"
          confirm-button-text="确认删除"
          cancel-button-text="取消"
          @confirm="deleteNote"
        >
          <template #reference>
            <el-button type="danger" :icon="Delete">删除游记</el-button>
          </template>
        </el-popconfirm>
      </div>

      <h1 class="note-title">{{ note.title }}</h1>
      <div class="note-meta">
        <el-avatar :src="note.author.avatarUrl || '/default-avatar.png'" :size="40"></el-avatar>
        <span class="author-name">{{ note.author.username || '匿名用户' }}</span>
        <span class="publish-date">发布于：{{ formatDateTime(note.createdTime) }}</span>
        <span class="views">
          <el-icon><View /></el-icon>{{ note.viewCount || 0 }} 阅读
        </span>
        <span class="favorite">
          <el-icon><Star /></el-icon> {{ note.favoriteCount || 0 }}
        </span>
        <span class="likes">
          <el-icon><Pointer /></el-icon>{{ note.likeCount || 0 }} 喜欢
        </span>
        <span class="comments">
          <el-icon><ChatDotRound /></el-icon>{{ note.commentCount || 0 }} 评论
        </span>
      </div>

      <el-divider></el-divider>

      <div class="note-content" v-html="note.content">
        </div>

      <div v-if="note.imageIdAndUrls && Object.keys(note.imageIdAndUrls).length > 0" class="image-gallery">
        <img
          v-for="(imageUrl, imageId) in note.imageIdAndUrls"
          :key="imageId"
          :src="imageUrl"
          alt="游记图片"
          class="gallery-image"
          @click="previewImage(imageUrl, Object.values(note.imageIdAndUrls))"
        />
        <el-image-viewer
          v-if="showImageViewer"
          :url-list="imagePreviewList"
          :initial-index="imagePreviewIndex"
          @close="closeImageViewer"
        />
      </div>

      <div class="note-tags" v-if="note.tags && note.tags.length">
        <el-tag v-for="(tag, index) in note.tags" :key="index" size="large" class="note-tag">{{ tag }}</el-tag>
      </div>
      
      <el-divider></el-divider>

      <!-- 评论区 -->
      <div class="comment-section">
        <h2>评论 ({{ note.commentCount || 0 }})</h2>
        <!-- 评论游记输入框 -->
        <div class="comment-input-area">
          <el-input
            v-model="newCommentContent"
            type="textarea"
            :rows="3"
            placeholder="发表你的评论..."
            show-word-limit
          ></el-input>
          <el-button
            type="primary"
            :icon="ChatDotRound"
            class="publish-comment-btn"
            :loading="commentPosting"
            @click="postComment"
          >
            发表评论
          </el-button>
        </div>

        <el-divider></el-divider>

        <!-- 评论列表 -->
        <div v-loading="commentsLoading">
          <div v-if="comments.length > 0" class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :src="comment.author.avatarUrl || '/default-avatar.png'" :size="40" class="comment-avatar"></el-avatar>
              <div class="comment-content-wrapper">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.author.username || '匿名用户' }}</span>
                  <span class="comment-date">{{ formatDateTime(comment.createdTime) }}</span>
                  <el-button
                    v-if="currentUserId === comment.author.id"  type="danger"
                    :icon="Delete"
                    circle
                    size="small"
                    class="delete-comment-btn"
                    @click="deleteCommentOrReply(comment, null, 'comment')"
                    title="删除评论"
                  ></el-button>
                </div>
                <!-- 评论正文 -->
                <p class="comment-text">{{ comment.content }}</p>

                <!-- 回复预览 -->
                <div v-if="comment.repliesPreview && comment.repliesPreview.length > 0 && !expandedCommentId[comment.id]" class="replies-preview">
                  <div v-for="reply in comment.repliesPreview" :key="reply.id" class="reply-item">
                    <span class="reply-author">{{ reply.author.username || '匿名用户' }}</span>
                    <span v-if="reply.replyToUsername">回复 <span class="reply-to-user">@{{ reply.replyToUsername }}</span></span>
                    : {{ reply.content }}
                    <div class="reply-actions-inline">
                       <el-button type="text" :icon="ChatDotRound" @click="setReplyTarget(comment, reply)">回复</el-button>
                       <el-button
                         v-if="currentUserId === reply.author.id"
                         type="text"
                         :icon="Delete"
                         size="small"
                         class="delete-reply-inline-btn"
                         @click="deleteCommentOrReply(reply, comment, 'reply')"
                         title="删除回复"
                        ></el-button>
                    </div>
                  </div>
                  <div v-if="comment.totalReplies > comment.repliesPreview.length" class="view-more-replies" @click="toggleReplies(comment)">
                    查看全部回复 <el-icon><ArrowRight /></el-icon>
                  </div>
                </div>

                <!-- 回复该评论 -->
                <div class="comment-actions">
                  <el-button type="text" :icon="ChatDotRound" @click="toggleReplyInput(comment)">回复</el-button>
                </div>

                <!-- 展开该评论所有回复 -->
                <div v-if="expandedCommentId[comment.id]" class="full-replies-area">
                  <div v-loading="repliesLoadingForComment[comment.id]">
                    <div v-if="comment.fullReplies && comment.fullReplies.length > 0" class="full-reply-list">
                      <div v-for="reply in comment.fullReplies" :key="reply.id" class="full-reply-item">
                        <el-avatar :src="reply.author.avatarUrl || '/default-avatar.png'" :size="30" class="reply-avatar"></el-avatar>
                        <div class="full-reply-content-wrapper">
                          <div class="reply-header">
                            <span class="reply-author">{{ reply.author.username || '匿名用户' }}</span>
                            <span v-if="reply.replyToUsername" class="reply-to-info">回复 <span class="reply-to-user">@{{ reply.replyToUsername }}</span></span>
                            <span class="reply-date">{{ formatDateTime(reply.createdTime) }}</span>
                            <el-button
                              v-if="currentUserId === reply.author.id"
                              type="danger"
                              :icon="Delete"
                              circle
                              size="small"
                              class="delete-reply-btn"
                              @click="deleteCommentOrReply(reply, comment, 'reply')"
                              title="删除回复"
                            ></el-button>
                          </div>
                          <p class="reply-text">{{ reply.content }}</p>
                          <div class="reply-actions">
                            <el-button type="text" :icon="ChatDotRound" @click="setReplyTarget(comment, reply)">回复</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                    <el-empty v-else description="暂无回复" class="no-replies-empty"></el-empty>

                    <div class="load-more-replies-container">
                      <el-button
                        v-if="comment.fullReplies && comment.fullReplies.length < comment.totalReplies"
                        type="info"
                        plain
                        :loading="repliesLoadingForComment[comment.id]"
                        @click="fetchAllReplies(comment)"
                        class="load-more-btn"
                      >
                        {{ repliesLoadingForComment[comment.id] ? '加载中...' : '加载更多回复' }}
                      </el-button>
                      <p v-else-if="comment.fullReplies && comment.fullReplies.length > 0 && comment.fullReplies.length >= comment.totalReplies" class="no-more-text">
                        已加载全部回复
                      </p>
                    </div>
                  </div>
                  <div class="collapse-replies" @click="toggleReplies(comment)">
                    <el-icon><ArrowUp /></el-icon> 折叠
                  </div>
                </div>

                <div v-if="activeReplyInputId === comment.id" class="reply-input-area-inline">
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    :rows="3"
                    :placeholder="`回复 ${currentReplyTarget?.author.username || comment.author.username}:`"
                    maxlength="200"
                    show-word-limit
                  ></el-input>
                  <div class="reply-input-actions">
                    <el-button type="info" size="small" @click="activeReplyInputId = null; replyContent = ''; currentReplyTarget = null;">取消</el-button>
                    <el-button type="primary" size="small" :loading="commentPosting" @click="postReplyComment(comment)">
                      回复
                    </el-button>
                  </div>
                </div>

              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论" class="no-comments"></el-empty>

          <div class="load-more-comments-container">
            <el-button
              v-if="hasMoreComments"
              type="primary"
              plain
              :loading="commentsLoading"
              @click="fetchComments()"
              class="load-more-btn"
            >
              {{ commentsLoading ? '加载中...' : '加载更多评论' }}
            </el-button>
            <p v-else-if="comments.length > 0 && !hasMoreComments" class="no-more-text">
              已加载全部评论
            </p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch,reactive ,computed} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElCard, ElButton, ElMessage, ElMessageBox, ElSkeleton, ElResult, ElEmpty, ElAvatar, ElIcon, ElDivider, ElImage, ElTag, ElPopconfirm,ElImageViewer } from 'element-plus';
import { ArrowLeftBold, Edit, Delete, View, ChatDotRound, Picture } from '@element-plus/icons-vue';
import { authAxios,publicAxios } from '@/utils/request'; 
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.userId);
const isLoggedIn = computed(()=> authStore.isLoggedIn);//判断是否登录
const route = useRoute();
const router = useRouter();

const note = ref(null);
const loading = ref(true);
const error = ref(null);
let noteId =ref(null)

// --- 图片预览相关变量 ---
const showImageViewer = ref(false);
const imagePreviewList = ref([]);
const imagePreviewIndex = ref(0);

// --- 图片预览功能 ---
const previewImage = (currentImageUrl, allImageUrls) => {
  imagePreviewList.value = allImageUrls;
  imagePreviewIndex.value = allImageUrls.indexOf(currentImageUrl); // 查找当前点击图片在列表中的索引
  showImageViewer.value = true;
};

const closeImageViewer = () => {
  showImageViewer.value = false;
  imagePreviewList.value = []; // 清空列表，释放内存
  imagePreviewIndex.value = 0; // 重置索引
};

// --- 主评论区数据 ---
const comments = ref([]); // 存储主评论列表
const newCommentContent = ref(''); // 绑定主评论输入框的内容
const commentPosting = ref(false); // 评论发布状态，防止重复提交

const commentsCurrentPage = ref(0); // 主评论当前页码
const commentsPageSize = ref(5); // 每页主评论数，可以根据需要调整，例如10或11
const totalComments = ref(0); // 总主评论数
const commentsLoading = ref(false); // 主评论列表加载状态

// --- 就地展开的回复数据 (新加或调整的重点) ---
const expandedCommentId = reactive({}); // 用于控制哪个主评论的回复已展开
const repliesLoadingForComment = reactive({}); // 用于控制每个评论的回复加载状态
const repliesCurrentPageForComment = reactive({}); // 用于控制每个评论的回复当前页码

// --- 回复输入框数据和状态 ---
const activeReplyInputId = ref(null); // 当前显示回复输入框的评论ID (即点击了"回复"按钮的评论)
const replyContent = ref(''); // 回复输入框的内容
const currentReplyTarget = ref(null); // 当前回复的目标（可能是主评论，也可能是某个回复）


// 计算属性：是否有更多主评论可以加载
const hasMoreComments = computed(() => {
  return comments.value.length < totalComments.value;
});

// --- 格式化日期时间 ---
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '未知时间';
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};


// --- 获取游记详情数据 ---
const fetchNoteDetail = async (noteId) => {
  if (!noteId) {
    error.value = '游记ID无效，无法加载。';
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = null;
  note.value = null; // 清空旧数据

  try {
    const response = await authAxios.get(`/user/posts/${noteId}`); 
    if (response.data.code === 200) {
      note.value = response.data.data;
      ElMessage.success('游记详情加载成功！');
    } else {
      error.value = response.data.message || '获取游记详情失败。';
      ElMessage.error(error.value);
    }
  } catch (err) {
    console.error('Error fetching travel note detail:', err);
    error.value = '网络请求失败或服务器错误，请稍后再试。';
    ElMessage.error(error.value);
  } finally {
    loading.value = false;
  }
};

// --- 返回我的游记列表 ---
const goBackToMyNotes = () => {
  router.push({ name: '用户个人主页', query: { tab: 'notes' } }); 
};

// --- 编辑游记 ---
const editNote = () => {
  if (note.value && note.value.id) {
    // 跳转到编辑游记页面，并带上游记ID
    router.push({ name: '用户编辑游记', params: { id: note.value.id } }); 
  } else {
    ElMessage.warning('无法编辑，游记信息不完整。');
  }
};

// --- 删除游记 ---
const deleteNote = async () => {
  if (!note.value || !note.value.id) {
    ElMessage.warning('无法删除，游记ID无效！');
    return;
  }
  try {
    const response = await authAxios.delete(`/user/posts/${note.value.id}`); 
    if (response.data.code === 200) {
      ElMessage.success('游记删除成功！');
      router.push({ name: '用户个人主页', query: { tab: 'notes' } });
    } else {
      ElMessage.error(response.data.message || '删除游记失败！');
    }
  } catch (err) {
    console.error('Error deleting travel note:', err);
    ElMessage.error('网络请求失败，删除游记失败！');
  }
};

// 获取主评论列表
const fetchComments = async (reset = false) => {
  console.log('noteId',noteId)
  if (commentsLoading.value && !reset) return; // 防止重复加载，但允许重置加载

  commentsLoading.value = true;

  if (reset) {
    commentsCurrentPage.value = 1; // 从第一页开始
    comments.value = []; // 清空现有评论
    totalComments.value = 0; // 重置总数
    Object.keys(expandedCommentId).forEach(key => delete expandedCommentId[key]);
    Object.keys(repliesLoadingForComment).forEach(key => delete repliesLoadingForComment[key]);
    Object.keys(repliesCurrentPageForComment).forEach(key => delete repliesCurrentPageForComment[key]);

  } else {
    commentsCurrentPage.value++; // 加载下一页
  }

  try {
    const response = await publicAxios.get(`/public/posts/${noteId}/comments`, {
      params: {
        page: commentsCurrentPage.value,
        size: commentsPageSize.value,
        sortBy: 'createdTime',
        sortDirection: 'DESC' // 主评论通常倒序（最新在上面）
      }
    });

    if (response.data && response.data.code === 200 && response.data.data) {
      const newComments = response.data.data.content.map(comment => ({
        ...comment,
        fullReplies: [], 
        repliesLoadedPage: comment.repliesPreview && comment.repliesPreview.length > 0 ? 1 : 0 // 如果有预览，则认为第一页已加载
      }));

      console.log('已加载主评论',response.data.data)

      totalComments.value = response.data.data.totalElements; // 从后端获取总评论数

      if (reset) {
        comments.value = newComments;
      } else {
        comments.value = [...comments.value, ...newComments]; // 追加评论
      }
      // ElMessage.success(`评论加载成功，当前第 ${commentsCurrentPage.value} 页。`);
    } else {
      ElMessage.warning(response.data.message || '未能获取评论数据。');
      if (!reset) commentsCurrentPage.value--; // 加载失败，页码回退
    }
  } catch (error) {
    console.error('获取评论列表失败:', error);
    ElMessage.error('获取评论列表失败，请检查网络或稍后再试。');
    if (!reset) commentsCurrentPage.value--; // 加载失败，页码回退
  } finally {
    commentsLoading.value = false;
  }
};

/**
 * 发布主评论的函数
 */
const postComment = async () => {
  if (!newCommentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空！');
    return;
  }
  if (commentPosting.value) {
    return; // 防止重复提交
  }

  commentPosting.value = true;
  try {
    const payload = {
      content: newCommentContent.value.trim(),
      parentId: null // 直接评论游记
    };
    
    const response = await authAxios.post(`/user/posts/comments/${noteId}`, payload);

    if (response.data && response.data.code === 200) {
      ElMessage.success('评论发布成功！');
      // 成功后重新加载评论列表（从第一页开始）以显示新评论
      await fetchComments(true); // 重置并刷新评论列表
      newCommentContent.value = ''; // 清空评论输入框
      // 更新游记详情中的评论计数（如果需要实时更新）
      if (note.value) {
        note.value.commentCount = totalComments.value; // 使用评论总数更新
      }
    } else {
      ElMessage.error(response.data.message || '评论发布失败！');
    }
  } catch (error) {
    console.error('发布评论失败:', error);
    ElMessage.error('发布评论失败，请检查网络或稍后再试。');
  } finally {
    commentPosting.value = false;
  }
};

/**
 * 切换某个评论的回复展开/折叠状态 (就地展开功能)
 * @param {object} comment - 被操作的评论对象
 */
const toggleReplies = async (comment) => {
  const commentId = comment.id;
  if (expandedCommentId[commentId]) {
    // 如果已展开，则折叠
    expandedCommentId[commentId] = false;
  } else {
    // 如果未展开，则展开并加载回复
    expandedCommentId[commentId] = true;
    // 只有在没有加载过回复或者当前页码为0时才加载第一页
    if (!comment.fullReplies || comment.fullReplies.length === 0 || comment.repliesLoadedPage === 0) {
      repliesCurrentPageForComment[commentId] = 0; // 重置页码到0，以便 fetchAllReplies 时递增到1
      await fetchAllReplies(comment);
    }
  }
};

/**
 * 获取单条评论的所有回复列表 (就地加载，支持分页)
 * @param {object} comment - 要加载回复的评论对象
 * @param {boolean} reset - 是否重置该评论的回复列表并从第一页开始加载
 */
const fetchAllReplies = async (comment, reset = false) => {
  const commentId = comment.id;
  if (!commentId) return;
  if (repliesLoadingForComment[commentId] && !reset) return; // 正在加载中且不是重置，则跳过

  repliesLoadingForComment[commentId] = true;

  if (reset) {
    repliesCurrentPageForComment[commentId] = 1; // 重置到第一页
    comment.fullReplies = []; // 清空该评论的完整回复
  } else {
    // 获取当前页码，如果是第一次加载，则从 0 开始递增到 1
    repliesCurrentPageForComment[commentId] = (repliesCurrentPageForComment[commentId] || 0) + 1;
  }

  try {
    const response = await publicAxios.get(`/public/posts/comments/${commentId}/replies`, {
      params: {
        page: repliesCurrentPageForComment[commentId],
        size: 5, // 楼中楼回复每页数量，可调整
        sortBy: 'createdTime',
        sortDirection: 'ASC' // 楼中楼回复通常按时间正序
      }
    });

    if (response.data && response.data.code === 200 && response.data.data) {
      const newReplies = response.data.data.content;
      console.log('该条评论的回复有',response.data.data)
      
      if (reset) {
        comment.fullReplies = newReplies;
      } else {
        comment.fullReplies = [...comment.fullReplies, ...newReplies]; // 追加回复
      }
      comment.repliesLoadedPage = repliesCurrentPageForComment[commentId]; // 更新已加载页码
      comment.totalReplies = response.data.data.totalElements

      console.log('comment:',comment)
    } else {
      ElMessage.warning(response.data.message || '未能获取回复数据。');
      if (!reset) repliesCurrentPageForComment[commentId]--; // 加载失败，页码回退
    }
  } catch (error) {
    console.error('获取回复列表失败:', error);
    ElMessage.error('获取回复列表失败，请检查网络或稍后再试。');
    if (!reset) repliesCurrentPageForComment[commentId]--; // 加载失败，页码回退
  } finally {
    repliesLoadingForComment[commentId] = false;
  }
};

/**
 * 切换回复输入框的显示/隐藏 (就地显示)
 * @param {object} comment - 主评论对象
 */
const toggleReplyInput = (comment) => {
  if (activeReplyInputId.value === comment.id) {
    // 如果是同一个评论，则关闭输入框
    activeReplyInputId.value = null;
    replyContent.value = '';
    currentReplyTarget.value = null;
  } else {
    // 否则，打开该评论的输入框，并将回复目标设为该主评论
    activeReplyInputId.value = comment.id;
    replyContent.value = '';
    currentReplyTarget.value = comment; // 默认回复目标是主评论
  }
};

/**
 * 设置回复目标（用于点击回复列表中的具体回复进行楼中楼回复）
 * @param {object} parentComment - 主评论对象
 * @param {object} targetReply - 实际要回复的目标回复对象
 */
const setReplyTarget = (parentComment, targetReply) => {
  activeReplyInputId.value = parentComment.id; // 确保主评论的回复输入框显示
  replyContent.value = ''; // 清空内容
  currentReplyTarget.value = targetReply; // 设置为具体回复对象
};


/**
 * 发布回复评论的函数 (包括主评论下的回复和楼中楼回复)
 * @param {object} parentComment - 对应的父评论对象 (即点击“回复”按钮时所在的主评论)
 */
const postReplyComment = async (parentComment) => {
  if (!isLoggedIn.value) { 
    ElMessage.warning('请先登录才能发表回复！');
    return;
  }
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空！');
    return;
  }
  if (commentPosting.value) return;

  commentPosting.value = true;
  try {
    const payload = {
      content: replyContent.value.trim(),
      // 如果 currentReplyTarget 是该主评论本身，则parentId是主评论ID
      // 如果 currentReplyTarget 是该主评论下的某个回复，则parentId是该回复的ID
      parentId: currentReplyTarget.value.id 
    };

    console.log('准备回复评论',payload)
    
    const response = await authAxios.post(`/user/posts/comments/${noteId}`, payload);

    if (response.data && response.data.code === 200) {
      ElMessage.success('回复发布成功！');
      replyContent.value = ''; // 清空输入框
      activeReplyInputId.value = null; // 关闭回复输入框
      currentReplyTarget.value = null; // 重置回复目标

      await fetchComments(true); 

      // 如果当前父评论的回复列表是展开的，则也刷新其 fullReplies
      if (expandedCommentId[parentComment.id]) {
        await fetchAllReplies(parentComment, true);
      }
    } else {
      ElMessage.error(response.data.message || '回复发布失败！');
    }
  } catch (error) {
    console.error('发布回复评论失败:', error);
    if (error.response && error.response.status === 401) {
      ElMessage.error('请先登录才能发表回复！');
    } else {
      ElMessage.error('发布回复评论失败，请检查网络或稍后再试。');
    }
  } finally {
    commentPosting.value = false;
  }
};

/**
 * 删除评论或回复的函数
 * @param {object} item - 要删除的评论或回复对象
 * @param {object} parentComment - 如果是回复，则为对应的父评论对象
 * @param {string} type - 'comment' 或 'reply'
 */
const deleteCommentOrReply = async (item, parentComment = null, type = 'comment') => {
  try {
    await ElMessageBox.confirm(`确定要删除这条${type === 'comment' ? '评论' : '回复'}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    const commentIdToDelete = item.id;
    const isReply = type === 'reply';
    const requestUrl = `/user/posts/comments/${commentIdToDelete}`;

    const response = await authAxios.delete(requestUrl);

    if (response.data && response.data.code === 200) {
      ElMessage.success(`${type === 'comment' ? '评论' : '回复'}删除成功！`);

      // --- 关键优化部分开始 ---

      if (type === 'comment') {
        // 如果删除的是主评论
        const index = comments.value.findIndex(c => c.id === item.id);
        if (index !== -1) {
          comments.value.splice(index, 1); // 从 comments 数组中移除该评论
        }
        // 更新游记的评论总数
        if (note.value) {
          note.value.commentCount = Math.max(0, note.value.commentCount - 1);
        }
      } else if (type === 'reply' && parentComment) {
        // 如果删除的是回复
        // 从 parentComment.fullReplies 中移除回复
        const replyIndexInFull = parentComment.fullReplies.findIndex(r => r.id === item.id);
        if (replyIndexInFull !== -1) {
          parentComment.fullReplies.splice(replyIndexInFull, 1);
        }

        //从 parentComment.repliesPreview 中移除回复 (如果存在)
        const replyIndexInPreview = parentComment.repliesPreview.findIndex(r => r.id === item.id);
        if (replyIndexInPreview !== -1) {
          parentComment.repliesPreview.splice(replyIndexInPreview, 1);
        }

        // 更新父评论的回复总数
        parentComment.totalReplies = Math.max(0, parentComment.totalReplies - 1);

        // 更新游记的评论总数 (因为回复也算在游记的总评论数内)
        if (note.value) {
          note.value.commentCount = Math.max(0, note.value.commentCount - 1);
        }
      }
    } else {
      ElMessage.error(response.data.message || `${type === 'comment' ? '评论' : '回复'}删除失败！`);
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除操作。');
    } else {
      console.error(`删除${type === 'comment' ? '评论' : '回复'}失败:`, error);
      if (error.response && error.response.status === 401) {
        ElMessage.error('登录已过期或您没有权限删除此项，请重新登录！');
      } else if (error.response && error.response.status === 403) {
        ElMessage.error('您没有权限删除此项！');
      } else {
        ElMessage.error(`删除${type === 'comment' ? '评论' : '回复'}失败，请检查网络或稍后再试。`);
      }
    }
  }
};

// --- 生命周期钩子 ---
onMounted(() => {
  noteId = route.params.id;
  fetchNoteDetail(noteId);
  fetchComments(true);
});

watch(
  () => route.params.id, // 监听路由参数 id 的变化
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      console.log(`MyTravelNoteDetail: 路由 ID 变化，从 ${oldId} 变为 ${newId}。重新加载数据。`);
      fetchNoteDetail(newId);
    }
  }
);
</script>

<style scoped>
.my-travel-note-detail-container {
  min-height: 100vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center; 
  font-family: 'Noto Sans SC', sans-serif, 'Helvetica Neue', Helvetica, Arial, sans-serif;
  position: relative; 
  background-image: url('@/assets/bg1.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

/* 固定在左上角的返回按钮 */
.back-button {
  position: fixed; /* 固定定位 */
  top: 20px;
  left: 20px;
  z-index: 1000; /* 确保在最顶层 */
  background-color: rgba(0, 0, 0, 0.5); /* 半透明背景 */
  border: none;
  color: #fff;
  padding: 10px 15px;
  border-radius: 20px; /* 更圆润 */
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}
.back-button:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

.loading-card, .error-card, .empty-card {
  padding: 40px;
  text-align: center;
  border-radius: 8px;
}

.loading-text {
  margin-top: 20px;
  color: #606266;
}

.note-detail-card {
  max-width: 700px; 
  width: 100%; 
  margin: 20px auto; 
  border-radius: 10px; 
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); 
  background-color: #ffffff; 
  padding: 20px 25px; 
}

.author-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  gap: 10px; /* 按钮间距 */
}

.note-title {
  font-size: 2.5em;
  color: #333;
  text-align: center;
  margin-bottom: 20px;
  line-height: 1.3;
}

.note-meta {
  display: flex;
  align-items: center;
  justify-content: center; /* 居中显示元信息 */
  gap: 15px; /* 各个元信息项的间距 */
  color: #666;
  font-size: 0.95em;
  margin-bottom: 30px;
  flex-wrap: wrap; /* 允许在小屏幕上换行 */
}

.note-meta .el-avatar {
  flex-shrink: 0; /* 防止头像被压缩 */
}

.note-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.note-meta .el-icon {
  font-size: 1.1em;
  color: #909399;
}

.note-cover-wrapper {
  margin: 30px auto;
  max-width: 700px; /* 控制封面图最大宽度 */
  height: 400px; /* 固定高度，保持比例 */
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.note-cover {
  width: 100%;
  height: 100%;
  display: block; /* 移除图片底部空白 */
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 1.2em;
}

.note-content {
  line-height: 1.8;
  font-size: 1.1em;
  color: #333;
  min-height: 100px; 
  margin-top: 30px;
  padding: 0 20px; 
  word-wrap: break-word; 
}

/* 针对富文本内容的基础样式重置，防止外部CSS影响 */
.note-content :deep(p) {
  margin-bottom: 1em;
}
.note-content :deep(h1), .note-content :deep(h2), .note-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  color: #2c3e50;
}
.note-content :deep(img) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1em auto;
  border-radius: 4px;
}
.note-content :deep(pre), .note-content :deep(code) {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
}

/* 图片画廊（图片列表）样式 */
.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); /* 更灵活的列数，小图显示更多 */
  gap: 10px; /* 图片之间的间距 */
  margin-top: 20px;
  margin-bottom: 25px;
  max-width: 100%; /* 确保不超过父容器 */
  justify-content: center; /* 图片少时居中显示 */
}

.gallery-image {
  width: 100%;
  height: 150px; /* 固定高度，保持图片大小一致 */
  object-fit: cover; /* 裁剪图片以填充容器 */
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: zoom-in; /* 鼠标悬停时显示放大镜图标 */
  transition: transform 0.2s ease-in-out;
}

.gallery-image:hover {
  transform: scale(1.02); /* 悬停放大效果 */
}

.note-tags {
  margin-top: 30px;
  text-align: center;
  padding: 0 20px;
}

.note-tag {
  margin: 5px;
  font-size: 0.9em;
}

.comment-section {
  margin-top: 40px;
  padding: 0 20px;
}

.comment-section h2 {
  font-size: 1.8em;
  color: #333;
  margin-bottom: 20px;
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.comment-info {
  flex-grow: 1;
}

.comment-user {
  font-weight: bold;
  color: #555;
  margin-right: 10px;
}

.comment-date {
  font-size: 0.85em;
  color: #999;
}

.comment-text {
  margin-top: 5px;
  color: #333;
  line-height: 1.6;
}

/* 评论区整体样式 */
.comment-section {
  margin-top: 30px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
}

.comment-section h2 {
  font-size: 1.5em;
  color: #333;
  margin-bottom: 20px;
  font-weight: 600;
  border-left: 4px solid var(--el-color-primary, #409eff); /* 左侧强调线 */
  padding-left: 10px;
}

/* 评论输入区域 */
.comment-input-area {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
}

.publish-comment-btn {
  align-self: flex-end; /* 按钮靠右 */
  width: 120px; /* 固定按钮宽度 */
  border-radius: 20px; /* 圆角按钮 */
  font-weight: 500;
  letter-spacing: 0.5px;
  line-height: 1.5;
}

/* 登录提示样式 */
.login-prompt {
  margin-top: 15px;
  text-align: center;
  background-color: #ecfff8; 
  border-radius: 8px;
  padding: 10px 15px;
  border: 1px solid #ecfff8;
}

.login-prompt .el-alert {
  padding: 0; 
  background-color: transparent; 
  border: none;
  line-height: inherit; 
}

.login-prompt .alert-title {
  font-size: 0.95em;
  color: #606266;
}

.login-prompt .el-link {
  font-size: 1em;
  font-weight: bold;
  margin: 0 3px;
  margin-bottom: 4px;
  line-height: inherit;
}

/* 评论列表 */
.comment-list {
  margin-top: 20px;
}

.comment-item {
  display: flex;
  align-items: flex-start; 
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #eee; 
}

.comment-item:last-child {
  border-bottom: none; 
  margin-bottom: 0;
  padding-bottom: 0;
}

.comment-avatar {
  margin-right: 15px;
  flex-shrink: 0; 
}

.comment-content-wrapper {
  flex-grow: 1; 
}

.comment-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 5px;
}

.comment-author {
  font-weight: bold;
  color: #50bfb2; 
  margin-right: 10px;
  font-size: 1.05em;
}

.comment-date {
  font-size: 0.85em;
  color: #909399;
}

.comment-text {
  margin-top: 5px;
  color: #303133;
  line-height: 1.6;
  font-size: 0.95em;
  word-break: break-word; /* 评论内容自动换行 */
}


/* 回复预览区样式 */
.replies-preview {
  background-color: #f7f7f7;
  border-radius: 8px;
  padding: 10px 15px;
  margin-top: 10px; 
  border: 1px solid #eee;
  transition: max-height 0.3s ease-out, opacity 0.3s ease-out;
}

.reply-item {
  font-size: 0.88em;
  color: #555;
  line-height: 1.5;
  margin-bottom: 5px;
  word-break: break-word;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-author {
  font-weight: bold;
  color: #606266;
}

.reply-to-user {
  color: #50bfb2;
  font-weight: bold;
}

.reply-actions-inline {
  display: inline-block; /* 让按钮与文本在同一行 */
  margin-left: 10px; /* 与文本保持距离 */
}

.reply-actions-inline .el-button--text {
  font-size: 0.8em; /* 预览区按钮字体小一点 */
  padding: 0 5px;
  color: #888; /* 颜色浅一点 */
}

.reply-actions-inline .el-button--text:hover {
  color: #50bfb2;
}

/* 查看更多回复按钮 */
.view-more-replies {
  margin-top: 8px;
  font-size: 0.85em;
  color: #50bfb2;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 3px;
  font-weight: 500; /* 稍加粗 */
  transition: color 0.2s ease;
}
.view-more-replies:hover {
  color: #52ac98; /* 悬停变色 */
  text-decoration: underline;
}

.comment-actions {
  text-align: right;
  margin-top: 8px;
}

.comment-actions .el-button--text {
  color: #606266;
  font-size: 0.9em;
}
.comment-actions .el-button--text:hover {
  color: #50bfb2; 
}

/* 展开的完整回复区域样式 */
.full-replies-area {
  margin-top: 10px;
  padding: 15px;
  background-color: #fcfcfc; /* 浅色背景区分 */
  border-radius: 8px;
  border: 1px solid #e0e0e0; /* 更明显的边框 */
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05); /* 轻微内阴影 */
  /* 可以添加动画效果，如max-height, opacity */
  overflow: hidden; /* 确保内容不溢出 */
  transition: max-height 0.3s ease-in, opacity 0.3s ease-in; /* 平滑展开/折叠 */
}

/* 完整回复列表 */
.full-reply-list {
  padding-left: 0px; /* 移除默认内边距 */
}

.full-reply-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px; /* 楼中楼回复间距 */
  padding-bottom: 10px;
  border-bottom: 1px dotted #f0f0f0; /* 更细的点状分隔线 */
}

.full-reply-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.full-reply-item .reply-avatar {
  margin-right: 10px;
  flex-shrink: 0;
  width: 30px; /* 楼中楼头像略小 */
  height: 30px;
}

.full-reply-content-wrapper {
  flex-grow: 1;
}

.full-reply-content-wrapper .reply-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 3px;
}

.full-reply-content-wrapper .reply-author {
  font-weight: 500;
  color: #333;
  margin-right: 8px;
  font-size: 0.95em;
}

.full-reply-content-wrapper .reply-to-info {
  font-size: 0.85em;
  color: #888;
  margin-right: 8px;
}

.full-reply-content-wrapper .reply-to-user {
  color: #50bfb2;
  font-weight: 600;
}

.full-reply-content-wrapper .reply-date {
  font-size: 0.78em;
  color: #999;
}

.full-reply-content-wrapper .reply-text {
  margin-top: 3px;
  color: #444;
  line-height: 1.5;
  font-size: 0.9em;
  word-break: break-word;
}

.full-reply-content-wrapper .reply-actions {
  text-align: right;
  margin-top: 5px;
}
.full-reply-content-wrapper .reply-actions .el-button--text {
  font-size: 0.85em;
  color: #888;
}
.full-reply-content-wrapper .reply-actions .el-button--text:hover {
  color: #50bfb2;
}


/* 加载更多回复按钮容器 */
.load-more-replies-container {
  text-align: center;
  margin-top: 15px;
}

/* 楼中楼暂无回复 */
.no-replies-empty {
  padding: 20px 0;
  font-size: 0.9em;
  color: #999;
}

/* 折叠按钮 */
.collapse-replies {
  text-align: center;
  margin-top: 15px;
  font-size: 0.9em;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 5px 0;
  border-top: 1px dotted #e0e0e0; /* 分隔线 */
  transition: color 0.2s ease;
}
.collapse-replies:hover {
  color: #60b9b2;
}

/* 就地回复输入框 */
.reply-input-area-inline {
  margin-top: 15px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #eee;
  box-shadow: 0 1px 5px rgba(0,0,0,0.05); /* 轻微阴影 */
}

.reply-input-actions {
  margin-top: 10px;
  text-align: right;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.reply-input-actions .el-button {
  padding: 8px 15px;
}

/* 加载更多主评论按钮容器 */
.load-more-comments-container {
  text-align: center;
  margin-top: 20px;
  padding-top: 15px; /* 与上一条评论列表保持距离 */
  border-top: 1px dashed #eee; /* 与评论列表分隔 */
}

.load-more-btn {
  width: 180px;
  border-radius: 20px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.no-more-text {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  margin-top: 15px;
}

/* 暂无评论的样式 */
.no-comments {
  margin-top: 30px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .travel-note-detail-page {
    padding: 10px;
  }
  .detail-card {
    margin: 10px auto;
    padding: 15px;
  }
  .back-button {
    top: 10px;
    left: 10px;
    padding: 8px 12px;
  }
  .card-header {
    padding-bottom: 15px;
  }
  .note-title {
    font-size: 1.4em;
  }
  .note-text-content {
    font-size: 0.95em;
  }
  .author-avatar {
    width: 40px;
    height: 40px;
  }
  .author-name {
    font-size: 1.05em;
  }
  .publish-time {
    font-size: 0.8em;
  }

  /* 评论区响应式 */
  .comment-section h2 {
    font-size: 1.3em;
  }
  .publish-comment-btn,
  .load-more-btn {
    width: 100%;
  }
  .comment-item {
    flex-direction: column;
    align-items: flex-start;
  }
  .comment-avatar {
    margin-right: 0;
    margin-bottom: 10px;
  }
  .replies-preview {
    padding: 8px 12px;
  }
  /* 调整楼中楼回复的间距 */
  .full-reply-item {
    margin-bottom: 10px;
    padding-bottom: 8px;
  }
  .full-reply-item .reply-avatar {
    width: 25px;
    height: 25px;
  }
  .full-reply-content-wrapper .reply-author {
    font-size: 0.9em;
  }
  .full-reply-content-wrapper .reply-text {
    font-size: 0.85em;
  }
}

/* 主评论删除按钮样式 */
.comment-header .delete-comment-btn {
  margin-left: auto; /* 推到右侧 */
  margin-right: 5px; /* 与日期保持一点距离 */
  color: #f56c6c; /* Element UI danger color */
  border: none;
  background-color: transparent;
  transition: all 0.2s ease-in-out;
}

.comment-header .delete-comment-btn:hover {
  background-color: rgba(245, 108, 108, 0.1); 
  transform: scale(1.1);
}

/* 回复预览中的删除按钮 */
.reply-actions-inline .delete-reply-inline-btn {
  color: #f56c6c; /* danger color */
  font-size: 0.8em; /* 与其他内联回复按钮保持一致 */
  margin-left: 5px; /* 与回复按钮间距 */
}

.reply-actions-inline .delete-reply-inline-btn:hover {
  color: #ff4949;
  text-decoration: underline;
}

/* 完整回复中的删除按钮样式 */
.full-reply-content-wrapper .reply-header .delete-reply-btn {
  margin-left: auto; /* 推到右侧 */
  margin-right: 5px; /* 与日期保持距离 */
  color: #f56c6c;
  border: none;
  background-color: transparent;
  transition: all 0.2s ease-in-out;
}

.full-reply-content-wrapper .reply-header .delete-reply-btn:hover {
  background-color: rgba(245, 108, 108, 0.1);
  transform: scale(1.1);
}

</style>