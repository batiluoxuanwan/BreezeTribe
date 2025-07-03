// stores/chatStore.js
import { defineStore } from 'pinia'
import { reactive } from 'vue'
import { useAuthStore } from './auth'  // ⬅ 用于获取当前用户 ID

export const useChatStore = defineStore('chatStore', () => {
    const authStore = useAuthStore()

    // ✅ 所有消息，结构为：{ friendId: [msg1, msg2, ...] }
    const messages = reactive({})

    // ✅ 未读消息计数，结构为：{ friendId: count }
    const unread = reactive({})

    /**
     * 添加一条消息（无论是自己发出还是对方发来的）
     * 会根据是“谁和我对话”来确定 friendId（始终存“对方”的 ID）
     */
    function addMessage(msg) {
        const myId = authStore.userId
        const friendId = msg.from === myId ? msg.to : msg.from

        if (!messages[friendId]) {
            messages[friendId] = []
        }

        messages[friendId].push(msg)

        // ✅ 可选：根据时间戳排序，避免乱序
        messages[friendId].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    }

    /**
     * 设置某个好友的消息历史（如第一次进入聊天界面加载历史）
     */
    function setHistory(friendId, history) {
        messages[friendId] = [...history]  // ✅ 用新的数组替换，确保响应式
        messages[friendId].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    }

    /**
     * 增加某个好友的未读消息数 +1
     */
    function addUnread(friendId) {
        unread[friendId] = (unread[friendId] || 0) + 1
    }

    /**
     * 清空某个好友的未读消息数
     */
    function clearUnread(friendId) {
        unread[friendId] = 0
    }

    /**
     * 获取与某个好友的所有消息（getter）
     */
    function getMessages(friendId) {
        return messages[friendId] || []
    }

    return {
        messages,
        unread,
        addMessage,
        setHistory,
        addUnread,
        clearUnread,
        getMessages
    }
})
