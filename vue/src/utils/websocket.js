import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

let stompClient = null;
let connected = false;

/**
 * 建立 WebSocket 连接，传入收到消息后的回调函数
 * @param {function} onMessageCallback - 收到消息时调用
 */
export function connectWebSocket(token, onMessageCallback) {
    //const socket = new SockJS('http://localhost:8081/ws');
    const socket = new SockJS(`http://localhost:8081/ws?token=${encodeURIComponent(token)}`);
    stompClient = Stomp.over(socket);

    stompClient.connect(
        { Authorization: `Bearer ${token}` },
        () => {
            connected = true;
            console.log('✅ WebSocket 已连接');

            stompClient.subscribe('/user/queue/messages', (message) => {
                const msgObj = JSON.parse(message.body);
                onMessageCallback(msgObj);
            });
        },
        (error) => {
            console.error('❌ WebSocket 连接失败：', error);
        }
    );
}

/**
 * 发送消息
 * @param {object} chatMessage - 消息对象，必须包含 from, to, content
 */
export function sendMessage(chatMessage) {
    if (connected && stompClient) {
        stompClient.send('/app/chat', {}, JSON.stringify(chatMessage));
    } else {
        console.warn('WebSocket 尚未连接，发送失败');
    }
}
