<template>
  <div class="travel-note-detail-page">
    <el-button type="info" :icon="ArrowLeft" class="back-button" @click="goBack">
      返回
    </el-button>

    <el-card v-if="loading" v-loading="loading" class="detail-card is-loading">
      <div class="loading-placeholder">正在加载游记详情...</div>
    </el-card>

    <el-card v-else-if="note" class="detail-card">
      <div class="card-header">
        <div class="author-profile">
          <el-avatar :src="note.author.avatarUrl || '/default-avatar.png'" :size="50" class="author-avatar"></el-avatar>
          <div class="author-info-text">
            <span class="author-name">{{ note.author.username || '匿名用户' }}</span>
            <span class="publish-time">{{ formatTime(note.createdTime) }}</span>
          </div>
        </div>
        <h1 class="note-title">{{ note.title }}</h1>
      </div>

      <div class="note-content">
        <div class="note-text-content" v-html="note.content">
        </div>

        <div v-if="note.imageUrls && note.imageUrls.length > 0" class="image-gallery">
          <img
            v-for="(imageUrl, index) in note.imageUrls"
            :key="index"
            :src="imageUrl"
            alt="游记图片"
            class="gallery-image"
            @click="previewImage(imageUrl, note.imageUrls)"
          />
        </div>

        <div v-if="note.spot" class="note-spot">
          <el-icon><Location /></el-icon>
          <span class="spot-name">{{ note.spot.name }}</span>
          <span v-if="note.spot.address" class="spot-address">({{ note.spot.address }})</span>
        </div>

        <div v-if="note.tags && note.tags.length > 0" class="note-tags">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" effect="plain">{{ tag.name }}</el-tag>
        </div>

        <div class="note-stats">
          <span class="stat-item"><el-icon><View /></el-icon> {{ note.viewCount || 0 }}</span>
          <span class="stat-item" @click="toggleLike(note)">
          <el-icon :class="{ 'liked': note.isLiked }"><Pointer /></el-icon> {{ note.likeCount || 0 }}
          </span>
          <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ note.commentCount || 0 }}</span>
        </div>

        <div class="note-actions">
          </div>
      </div>

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
            maxlength="200"
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
          <div v-if="!isLoggedIn" class="login-prompt">
            <el-alert title="请登录后发表评论" type="info" show-icon :closable="false" center>
              <template #title>
                <span class="alert-title">请 <el-link type="primary" @click="goToLogin">登录</el-link> 后发表评论</span>
              </template>
            </el-alert>
          </div>
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
                  <span class="comment-date">{{ formatTime(comment.createdTime) }}</span>
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
                            <span class="reply-date">{{ formatTime(reply.createdTime) }}</span>
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

    <el-empty v-else description="游记不存在或已删除" class="empty-state"></el-empty>
  </div>

  </template>

<script setup>
import { ref, onMounted , computed, watch,reactive} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage,ElMessageBox } from 'element-plus';
import { ArrowLeft, Star, ChatDotRound ,Delete} from '@element-plus/icons-vue';
import { publicAxios,authAxios } from '@/utils/request'; 
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.userId);
const isLoggedIn = computed(()=> authStore.isLoggedIn);//判断是否登录

const route = useRoute(); // 获取当前路由信息
const router = useRouter(); // 获取路由器实例

const note = ref(null); // 存储游记详情数据
const loading = ref(true); // 加载状态
const postId = ref(null); // 存储从路由获取的 postId

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

// 获取互动状态
const fetchInteractionStatus = async (itemId) => {
    if (note.value) { 
        note.value.isLiked = false;
        note.value.isFavorited = false;
    }

    if (!authStore.isLoggedIn) { 
        console.log('用户未登录，跳过获取互动状态。');
        return;
    }

    if (!itemId) {
        console.warn('缺少项目ID，无法获取互动状态。');
        return;
    }
    const itemType = 'POST'; 
    const statusMapKey = `${itemType}_${itemId}`; 
    try {
        const response = await authAxios.post('/user/interactions/status', {
            items: [
                {
                    id: itemId,
                    type: 'POST' 
                }
            ]
        });

        if (response.data.code === 200 && response.data.data.statusMap) {
            const status = response.data.data.statusMap[statusMapKey];
            if (status) {
                note.value.isLiked = status.liked;
                note.value.isFavorited = status.favorited; 
            }
            console.log('status ',response.data.data)
        } else {
            console.warn('获取互动状态失败:', response.data.message);
        }
    } catch (error) {
        console.error('获取互动状态请求失败:', error);
        if (error.response && error.response.status === 401) {
            ElMessage.warning('您未登录，无法获取点赞和收藏状态。');
        } else {
           ElMessage.error('获取点赞/收藏状态网络错误！');
        }
    }
};

// --- 点赞功能 ---
const toggleLike = async (note) => {
  // 防止重复点击导致多次请求
  if (note.liking) { 
    return;
  }
  note.liking = true; 

  const itemId = note.id;
  const itemType = note.itemType || 'POST'; 

  try {
    let response;
    if (note.isLiked) {
      response = await authAxios.delete('/user/likes', {
        data: { 
          itemId: itemId,
          itemType: itemType
        }
      });
    } else {
      response = await authAxios.post('/user/likes', {
        itemId: itemId,
        itemType: itemType
      });
    }

    if (response.data.code === 200) {
      if (note.isLiked) {
        note.likeCount--; 
      } else {
        note.likeCount++; 
      }
      note.isLiked = !note.isLiked; 
      ElMessage.success(note.isLiked ? '点赞成功！' : '取消点赞成功！');
    } else {
      ElMessage.error(response.data.message || '操作失败，请稍后再试。');
      await fetchInteractionStatus(itemId);
    }
  } catch (error) {
    console.error('点赞/取消请求失败:', error);
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(error.response.data.message);
    } else if (error.response && error.response.status === 401) {
       ElMessage.error('请先登录才能点赞！'); 
       router.push('/login');
    } else if (error.response && error.response.status === 403) {
       ElMessage.error('您没有权限进行此操作！'); 
    } else {
      ElMessage.error('网络错误或操作失败，请稍后再试！');
    }
    await fetchInteractionStatus(itemId);
  } finally {
    note.liking = false; 
  }
};

// 计算属性：是否有更多主评论可以加载
const hasMoreComments = computed(() => {
  return comments.value.length < totalComments.value;
});

/**
 * 格式化时间戳为更友好的日期时间字符串
 * @param {string} timeString - ISO 格式的时间字符串
 * @returns {string} 格式化后的时间字符串
 */
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

/**
 * 获取游记详情的函数
 */
const fetchNoteDetail = async () => {
  loading.value = true;
  try {
    const response = await publicAxios.get(`/public/posts/${postId.value}`);
    
    if (response.data && response.data.code === 200 && response.data.data) {
      note.value = response.data.data;
      
      ElMessage.success('游记详情加载成功！');
      console.log('游记详情:', note.value);
    } else {
      note.value = null;
      ElMessage.error('未能获取游记详情，请检查游记ID或稍后再试。');
    }
  } catch (error) {
    console.error('获取游记详情失败:', error);
    note.value = null;
    ElMessage.error('获取游记详情失败，请检查网络或稍后再试。');
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.push({ name: '旅行广场', query: { tab: 'notes' } });
};

// 组件挂载时执行
onMounted(() => {
  postId.value = route.params.id; 
  if (postId.value) {
    fetchNoteDetail();
    fetchInteractionStatus(postId.value);
    fetchComments(true); // 首次加载主评论，从第一页开始
  } else {
    loading.value = false;
    ElMessage.error('缺少游记ID，无法加载详情。');
  }
  console.log('userId:',currentUserId)
  console.log('token.userId:',authStore.userId )
  console.log('authStore:',authStore )
});

// 获取主评论列表
const fetchComments = async (reset = false) => {
  if (!postId.value) return;
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
    const response = await publicAxios.get(`/public/posts/${postId.value}/comments`, {
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
  if (!isLoggedIn.value) { 
    ElMessage.warning('请先登录才能发表评论！');
    return;
  }
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
    
    const response = await authAxios.post(`/user/posts/comments/${postId.value}`, payload);

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
    if (error.response && error.response.status === 401) {
      ElMessage.error('请先登录才能发表评论！');
      // 可以引导用户去登录页: router.push('/login'); 
    } else {
      ElMessage.error('发布评论失败，请检查网络或稍后再试。');
    }
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
    
    const response = await authAxios.post(`/user/posts/comments/${postId.value}`, payload);

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

const goToLogin = () => {
  router.push('/login'); // 假设你的登录路由是 '/login'
};

// 监听路由参数变化，当 postId 改变时重新加载游记和评论
watch(
  () => route.params.id,
  (newId) => {
    if (newId && newId !== postId.value) {
      postId.value = newId;
      fetchNoteDetail();
      fetchComments(true); // 重新加载评论，从第一页开始
    }
  }
);

</script>

<style scoped>
/* 引入 Google Fonts - Noto Sans SC 用于更现代的中文显示 */
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

/* 页面整体容器 */
.travel-note-detail-page {
  min-height: 100vh;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center; /* 让卡片居中 */
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
}
.back-button:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

/* 游记详情卡片 */
.detail-card {
  max-width: 700px; 
  width: 100%; 
  margin: 20px auto; 
  border-radius: 10px; 
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); 
  background-color: #ffffff; 
  padding: 20px 25px; 
}

/* 加载状态时的卡片高度，避免抖动 */
.detail-card.is-loading {
  min-height: 300px; 
}

/* 加载占位符样式 */
.loading-placeholder {
  min-height: 150px; 
  display: flex;
  justify-content: center;
  align-items: center;
  color: #909399;
  font-size: 1.1em;
}

/* 卡片头部 - 模拟朋友圈的用户信息 */
.card-header {
  padding-bottom: 20px; /* 增加底部间距 */
  border-bottom: none; /* 移除底部分割线 */
}

.author-profile {
  display: flex;
  align-items: center;
  margin-bottom: 15px; /* 头像和标题之间的间距 */
}

.author-avatar {
  margin-right: 15px;
  border: 2px solid #eee; /* 浅色边框 */
}

.author-info-text {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 1.15em;
  font-weight: 600;
  color: #333;
  margin-bottom: 3px;
}

.publish-time {
  font-size: 0.85em;
  color: #909399; /* 浅灰色时间 */
}

/* 游记标题样式 */
.note-title {
  font-size: 1.6em; /* 标题稍小，更像动态 */
  color: #2c3e50; /* 深色文字 */
  margin: 0; /* 移除默认外边距 */
  line-height: 1.4;
  font-weight: 700;
  text-align: left; /* 标题左对齐 */
}

/* 游记内容区域 */
.note-content {
  padding: 10px 0; /* 调整内容内边距 */
}

/* 封面图片容器 */
.cover-image-container {
  margin-bottom: 25px; /* 封面图下方间距 */
}

/* 封面图片样式 */
.cover-image {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

/* 游记正文富文本内容样式，使用 :deep() 穿透 Element Plus 默认样式 */
.note-text-content {
  line-height: 1.8; /* 更舒适的行高 */
  font-size: 1.05em; /* 文本大小 */
  color: #303133;
  text-align: justify; /* 文本两端对齐 */
  margin-bottom: 20px;

  :deep(p) {
    margin-bottom: 0.8em; /* 段落间距 */
    word-break: break-word; /* 单词换行 */
  }
  :deep(img) {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 15px auto;
    border-radius: 6px;
    box-shadow: 0 1px 5px rgba(0, 0, 0, 0.1);
  }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin-top: 1.5em;
    margin-bottom: 0.8em;
    color: #34495e; /* 更柔和的标题颜色 */
    font-weight: 600;
  }
  :deep(ul), :deep(ol) {
    margin-left: 25px; /* 列表缩进 */
    margin-bottom: 1em;
  }
  :deep(li) {
    margin-bottom: 0.5em;
  }
  :deep(a) {
    color: #409EFF;
    text-decoration: none;
  }
  :deep(pre) { /* 代码块 */
    background-color: #f8f8f8;
    border-left: 3px solid #409EFF;
    padding: 10px 15px;
    margin-bottom: 1em;
    overflow-x: auto;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
    font-size: 0.9em;
  }
  :deep(blockquote) { /* 引用块 */
    border-left: 4px solid #ccc;
    padding-left: 15px;
    color: #666;
    margin-bottom: 1em;
    font-style: italic;
  }
}

/* 图片画廊（图片列表）样式 */
.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); /* 响应式列，至少180px宽 */
  gap: 10px; /* 图片之间的间距 */
  margin-top: 20px;
  margin-bottom: 25px;
  max-width: 100%; /* 确保不超过父容器 */
  justify-content: center; /* 图片少时居中显示 */
}

.gallery-image {
  width: 100%;
  height: 180px; /* 固定高度，保持图片大小一致 */
  object-fit: cover; /* 裁剪图片以填充容器 */
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: zoom-in; /* 鼠标悬停时显示放大镜图标 */
  transition: transform 0.2s ease-in-out;
}

.gallery-image:hover {
  transform: scale(1.02); /* 悬停放大效果 */
}

/* 地点信息样式 */
.note-spot {
  display: flex;
  align-items: center;
  margin-top: 15px;
  margin-bottom: 20px;
  color: #606266;
  font-size: 0.95em;
  padding: 8px 12px;
  background-color: #f2f6fc;
  border-left: 3px solid #44beae;
  border-radius: 4px;
}

.note-spot .el-icon {
  margin-right: 8px;
  font-size: 1.1em;
}

.spot-name {
  font-weight: 500;
  margin-right: 5px;
}

.spot-address {
  color: #909399;
}

/* 标签样式 */
.note-tags {
  margin-top: 25px; /* 标签顶部间距 */
  padding-top: 15px;
  border-top: 1px dashed #e4e7ed; /* 虚线分隔 */
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.el-tag {
  background-color: #ecf5ff;
  border-color: #d9ecff;
  font-weight: 500;
  padding: 0 10px; /* 调整标签内边距 */
  height: 28px; /* 调整标签高度 */
  line-height: 26px;
  border-radius: 14px; /* 更圆润的标签 */
}

/* 互动统计数据样式 */
.note-stats {
  display: flex;
  justify-content: space-around; /* 统计数据均匀分布 */
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee; /* 顶部细线 */
  color: #606266;
  font-size: 0.9em;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px; /* 图标和文字间距 */
}

.stat-item .el-icon {
  font-size: 1.1em;
  color: #909399;
}

.stat-item .el-icon.liked {
    color: var(--el-color-primary); 
}

.stat-item .el-icon.favorited {
    color: var(--el-color-primary); 
}

/* 互动按钮区 */
.note-actions {
  display: flex;
  justify-content: flex-end; /* 按钮靠右 */
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee; /* 顶部细线 */
  gap: 20px; /* 按钮间距 */
}

.note-actions .el-button.el-button--text {
  color: #606266; /* 文本按钮颜色 */
  font-size: 0.95em;
  padding: 0 5px;
}
.note-actions .el-button.el-button--text .el-icon {
  font-size: 1.1em;
  margin-right: 5px;
}

/* 空状态样式 */
.empty-state {
  margin-top: 50px;
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
  color: #50bfb2; /* 回复按钮悬停颜色 */
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