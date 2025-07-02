<script>
export default {
  name: "FriendList"
}
</script>
<template>
  <div class="friend-list p-4 bg-white rounded-xl shadow-md max-w-3xl mx-auto mt-10
">
  <!-- 顶部标题栏 -->
  <div class="flex justify-between items-center mb-10">
    <h2 class="text-2xl font-bold text-gray-800">我的好友</h2>
    <div class="flex space-x-5">
      <button @click="goToAddFriend" class="text-blue-600 hover:text-blue-800 text-2xl">
        ➕
      </button>
      <button @click="goToFriendRequests" class="text-green-600 hover:text-green-800 text-2xl">
        👥
      </button>
    </div>
  </div>

  <!-- 好友列表 -->
    <ul class="mt-12 flex flex-col gap-5">
    <li
        v-for="friend in friends"
        :key="friend.id"
        class="flex items-center justify-between p-3 bg-gray-100 rounded-lg shadow-sm hover:bg-gray-200 cursor-pointer transition"
        @click="openChat(getFriendId(friend))"
    >
      <div class="flex items-center">
        <img
            :src="friendAvatars[getFriendId(friend)] || defaultAvatar"
            alt="avatar"
            class="w-12 h-12 rounded-full mr-4 object-cover"
        />
        <span class="text-lg font-medium text-gray-800">{{ getFriend(friend).username }}</span>
      </div>
    </li>
  </ul>
  </div>
</template>


<script setup>
import {ref, onMounted, computed, reactive} from 'vue';
import { authAxios } from '@/utils/request.js';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import defaultAvatar from '@/assets/NotFoundsonailong.jpg';


const authStore = useAuthStore();
const friends = ref([]);
const token = authStore.token;
const userId = computed(() => authStore.userId);
const friend = reactive({
  id: '',
  username: '',
  avatarUrl: '',
  role: '',
  active: true,
});
const friendAvatars = ref({}); // userId -> avatarUrl



const getFriend = (friend) => {
  return friend.account1.id === userId.value ? friend.account2 : friend.account1;
};

const getFriendId = (friend) => {
  return getFriend(friend).id;
};

const fetchFriendInfo = async (friendId) => {
  try {
    //@Operation(summary = "获取指定用户发的基础信息")
    //@GetMapping("/users/{userId}/info")
    const res = await authAxios.get(`public/users/${friendId}/info`,{
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      Object.assign(friend, res.data.data);
      console.log('✅✅✅✅✅✅✅✅✅✅✅✅好友信息:', friend);
      console.log('聊天对象头像:',friend.avatarUrl);
      const avatar = res.data.data.avatarUrl;
      friendAvatars.value[friendId] = avatar;
      console.log('头像：', avatar);
    } else {
      ElMessage.error(res.data.message || '获取好友信息失败');
    }
  } catch (error) {
    ElMessage.error('获取好友信息失败');
    console.error(error);
  }
};
const fetchFriends = async () => {
  try {
    const params = {
      page: 1,
      size: 10000,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };
    const res = await authAxios.post('/friend/friends', params, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data.code === 200) {
      friends.value = res.data.data.content;  // 取分页结果中的记录数组
      console.log("OOOOOOOOOOOOOOKKKKKKKKKKKKKKK");
      //并发请求头像
      await Promise.all(
          friends.value.map(friend => fetchFriendInfo(getFriendId(friend)))
      );
    } else {
      ElMessage.error(res.data.message || '获取好友列表失败');
    }
  } catch (error) {
    ElMessage.error('获取好友列表失败');
    console.error(error);
  }
};

const openChat = (friendId) => {
  // 这里跳转到聊天页面，假设路由是 /chat/:friendId
  window.location.href = `/chat/${friendId}`;
};

const goToAddFriend = () => {
  
};

const goToFriendRequests = () => {

};

onMounted(() => {
  fetchFriends();
});
</script>
