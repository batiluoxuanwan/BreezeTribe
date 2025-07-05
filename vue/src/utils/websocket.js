import SockJS from 'sockjs-client';

import { Client } from '@stomp/stompjs';
import {ElMessage, ElNotification} from "element-plus";
import {useChatStore} from "@/stores/chatStore.js";
import {useRoute} from "vue-router";
import {useAuthStore} from "@/stores/auth.js";
import {publicAxios} from "@/utils/request.js";

let stompClient = null;
//let connected = false;
let reconnecting = false;

const authStore= useAuthStore()
const chatStore = useChatStore()
const route = useRoute()

/**
 * 建立 WebSocket 连接，传入收到消息后的回调函数
 * @param {function} onMessageCallback - 收到消息时调用
 */
export async function connectWebSocket(token, onMessageCallback) {
    if (stompClient && stompClient.active) {
        return;
    }
    // 清理旧连接（防止重复激活）
    if (stompClient) {
        try {
            await stompClient.deactivate();
        } catch (e) {
            console.warn("清理旧连接失败", e);
        }
    }
    stompClient = new Client({
        webSocketFactory: () =>
            //new SockJS(`https://121.43.136.251:8080/ws?token=${encodeURIComponent(token)}`),
            new SockJS(`http://localhost:8081/ws?token=${encodeURIComponent(token)}`),

        connectHeaders: {
            Authorization: `Bearer ${token}`
        },

        debug: (str) => console.log(str),

        onConnect: () => {
            console.log("✅ WebSocket 已连接");
            reconnecting = false;

            stompClient.subscribe("/user/queue/messages", (message) => {
                const msgObj = JSON.parse(message.body);
                onMessageCallback(msgObj);
            });
        },

        onWebSocketClose: async () => {
            console.warn("⚠️ WebSocket 断开连接");
            if (reconnecting) return;

            reconnecting = true;
            try {
                const authStore = useAuthStore()
                const res = await publicAxios.post('/auth/refresh', {
                    refreshToken: authStore.refreshToken
                })
                const newAccessToken = res.data.data.accessToken
                authStore.updateAccessToken(newAccessToken)
                console.log("🔁 WebSocket尝试使用新token重连");
                await connectWebSocket(newAccessToken, onMessageCallback);
            } catch (e) {
                console.error("❌ Token刷新失败", e);
                ElMessage.error("登录状态已过期，请重新登录");
                localStorage.clear();
                window.location.href = "/login";
            }
        },

        onStompError: (frame) => {
            console.error("❌ STOMP 错误:", frame.headers["message"]);
        },

        onWebSocketError: (error) => {
            console.error("❌ WebSocket 错误:", error);
        }
    });

    stompClient.activate();
}



function isInChatPageWith(friendId) {
    return route.name === 'ChatRoom' && route.params.friendId === friendId
}

function showChatToast(msg) {
    ElNotification({
        title: '📩 新消息',
        message: msg.content,
        type: 'info',
        duration: 3000
    })
}

export function onMessageCallback(msg) {

    const myId = authStore.userId;
    const friendId = msg.from === myId ? msg.to : msg.from;

    // 3. 添加到聊天记录
    chatStore.addMessage(msg);

    // 4. 如果当前不在这个聊天页面，添加未读消息并展示提醒
    if (!isInChatPageWith(friendId)) {
        chatStore.addUnread(friendId);
        showChatToast(msg);
    }
}
/**
 * 发送消息
 * @param {object} chatMessage - 消息对象，必须包含 from, to, content
 */
export function sendMessage(chatMessage) {
    if (stompClient && stompClient.active) {
        stompClient.publish({
            destination: '/app/chat',
            body: JSON.stringify(chatMessage),
        });
    } else {
        console.warn('WebSocket 尚未连接，发送失败');
    }
}
