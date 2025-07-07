// utils/request.js
import axios from 'axios'
import {statsBuffer as accessToken} from "motion-v";
import {useAuthStore} from "@/stores/auth.js";
import {connectWebSocket, onMessageCallback} from "@/utils/websocket.js";

// 创建不带 token 的 Axios 实例：用于登录、注册等公共接口
export const publicAxios = axios.create({
    baseURL: 'https://121.43.136.251:8080/api',
    // baseURL: 'http://localhost:8081/api',
    //baseURL: 'https://frp-dad.com:36680/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

publicAxios.interceptors.request.use(config => {
    console.log('Axios 发出请求 config：', config)
    return config
})

// 创建带 token 的 Axios 实例：用于需要认证的业务接口
export const authAxios = axios.create({
    baseURL: 'https://121.43.136.251:8080/api',
    // baseURL: 'http://localhost:8081/api',
    //baseURL: 'https://frp-dad.com:36680/api',
  headers: {
    'Content-Type': 'application/json'
  },
  validateStatus: function (status) {
    return true; // 始终返回 true，表示所有状态码都被认为是“成功的”响应
  }
})

// 请求拦截器：从 Pinia 获取 token 添加到请求头
authAxios.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    console.log(authStore.accessToken)
    if (authStore.accessToken) {
      config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
    }
    console.log('最终请求 headers:', config.headers)
    return config
  },
  (error) => Promise.reject(error)
)

let isRefreshing = false
let pendingQueue = []
// 响应拦截器：处理 401 错误
authAxios.interceptors.response.use(
  (response) => response,
  async (error) => {
      if (error.response && error.response.status === 401) {
          const authStore = useAuthStore()
          //authStore.logout()
          //window.location.href = '/login'
          const originalRequest = error.config
          // accessToken 失效（401） 且当前请求不是刷新请求自身
          if (error.response?.status === 401 && !originalRequest._retry) {
              originalRequest._retry = true

              // 避免并发多次刷新
              if (isRefreshing) {
                  return new Promise(resolve => {
                      pendingQueue.push(() => {
                          originalRequest.headers['Authorization'] = `Bearer ${authStore.accessToken}`
                          resolve(authAxios(originalRequest))
                      })
                  })
              }
              isRefreshing = true

              try {
                  const res = await publicAxios.post('/auth/refresh', {
                      refreshToken: authStore.refreshToken
                  })
                  console.log("#######   刷新token    ##########")
                  const newAccessToken = res.data.data.accessToken
                  authStore.updateAccessToken(newAccessToken)
                  await connectWebSocket(newAccessToken, onMessageCallback);
                  // 执行等待队列
                  pendingQueue.forEach(cb => cb())
                  pendingQueue = []
                  console.log("执行等待队列", pendingQueue.length)
                  // 重试原请求
                  originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`
                  return authAxios(originalRequest)
              } catch (e) {
                  // 刷新失败，跳转登录
                  //authStore.logout()
                  //window.location.href = '/login'
                  return Promise.reject(e)
              } finally {
                  isRefreshing = false
              }
          }
          return Promise.reject(error)
      }
  }
)
