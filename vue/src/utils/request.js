// utils/request.js
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

// 创建不带 token 的 Axios 实例：用于登录、注册等公共接口
export const publicAxios = axios.create({
  baseURL: 'https://121.43.136.251:8080/api',
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
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：从 Pinia 获取 token 添加到请求头
authAxios.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    console.log(authStore.token)
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }
    console.log('最终请求 headers:', config.headers)
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理 401 错误
authAxios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
