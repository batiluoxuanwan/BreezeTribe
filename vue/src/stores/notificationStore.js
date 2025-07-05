// src/stores/notificationStore.js
import { defineStore } from 'pinia';
import { authAxios } from '@/utils/request';

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCounts: {
      all: 0,
      likes: 0,
      comments: 0,
      system: 0,
    },
  }),

  actions: {
    async fetchUnreadCounts() {
      try {
        const res = await authAxios.get('/notifications/unread-counts');
        if (res.data.code === 200) {
          this.unreadCounts.all = res.data.data.totalUnread;
          this.unreadCounts.likes = res.data.data.countsByType.LIKES_AND_FAVORITES;
          this.unreadCounts.comments = res.data.data.countsByType.COMMENTS_AND_REPLIES;
          this.unreadCounts.system = res.data.data.countsByType.SYSTEM_AND_ORDERS;
        }
      } catch (error) {
        console.error('获取通知数量失败', error);
      }
    },
  },
});
