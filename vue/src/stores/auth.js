import { defineStore } from 'pinia'
import { ref } from 'vue'
import { jwtDecode } from 'jwt-decode';

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const token = ref('')
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
  function login(authToken,Role) {
    token.value = authToken
    role.value = Role   
    userId.value = parseUserIdFromToken(authToken); 
    isLoggedIn.value = true
    console.log('userID',userId.value)

    // 将 token 和 role 存储到 localStorage
    localStorage.setItem('token', authToken)
    localStorage.setItem('role', Role ) 
    localStorage.setItem('userId', userId )
  }

  // 登出
  function logout() {
    token.value = ''
    role.value = null 
    isLoggedIn.value = false

    // 删除 localStorage 中的 token 和 role
    localStorage.removeItem('token')
    localStorage.removeItem('role') 
  }

  // checkAuth 函数仅用于初始化时从 localStorage 恢复状态
  function checkAuth() {
    const storedToken = localStorage.getItem('token')
    const storedRole = localStorage.getItem('role')

    if (storedToken && storedRole) { 
      token.value = storedToken
      role.value = storedRole 
      isLoggedIn.value = true
    } else {
      token.value = ''
      role.value = null 
      isLoggedIn.value = false
    }
  }

  return {
    isLoggedIn,
    token,
    role,
    login,
    logout,
    checkAuth,
    userId
  }
}, {
  persist: {
    paths: ['isLoggedIn', 'token', 'role','userId'] 
  }
})
