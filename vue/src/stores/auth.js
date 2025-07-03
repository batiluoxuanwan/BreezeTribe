import { defineStore } from 'pinia'
import { ref } from 'vue'
import { jwtDecode } from 'jwt-decode';
import {statsBuffer as accessToken} from "motion-dom";

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const accessToken = ref('')
  const refreshToken = ref('')
  const role = ref(null) 
  const userId = ref(null);

  // 辅助函数：从 token 中解析 userId
  const parseUserIdFromToken = (authToken) => {
    try {
      if (authToken) {
        const decoded = jwtDecode(authToken);
        return decoded.sub;
      }
    } catch (e) {
      console.error("Error decoding token:", e);
      return null;
    }
    return null;
  };

  // 登录，接受一个包含 token 和 role 的对象
  function login(tokens,Role) {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    role.value = Role   
    userId.value = parseUserIdFromToken(tokens.accessToken);
    isLoggedIn.value = true
    console.log('userID',userId.value)

    // 将 token 和 role 存储到 localStorage
    localStorage.setItem('accessToken', tokens.accessToken)
    localStorage.setItem('refreshToken', tokens.refreshToken)
    localStorage.setItem('role', Role )
    localStorage.setItem('userId', userId )
  }
  function updateAccessToken(newToken) {
    accessToken.value = newToken
    localStorage.setItem('accessToken', newToken)
  }
  // 登出
  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    role.value = null 
    isLoggedIn.value = false

    // 删除 localStorage 中的 token 和 role
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('role') 
  }

  // checkAuth 函数仅用于初始化时从 localStorage 恢复状态
  function checkAuth() {
    const storedAccessToken = localStorage.getItem('accessToken')
    const storedRefreshToken = localStorage.getItem('refreshToken')
    const storedRole = localStorage.getItem('role')

    if (storedAccessToken && storedRefreshToken && storedRole) {
      accessToken.value = storedAccessToken
      refreshToken.value = storedRefreshToken
      role.value = storedRole
      userId.value = parseUserIdFromToken(storedAccessToken)
      isLoggedIn.value = true
    } else {
      logout()
    }
  }

  return {
    isLoggedIn,
    accessToken,
    refreshToken,
    role,
    login,
    logout,
    checkAuth,
    updateAccessToken,
    userId
  }
}, {
  persist: {
    paths: ['isLoggedIn',  'accessToken', 'refreshToken', 'role','userId']
  }
})
