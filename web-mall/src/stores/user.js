import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

const loadUserFromStorage = () => {
  try {
    const raw = localStorage.getItem('userInfo');
    if (!raw || raw === 'undefined' || raw === 'null' || raw === '{}') return null;
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed : null;
  } catch (e) {
    console.warn('解析用户信息失败,已重置:', e);
    localStorage.removeItem('userInfo');
    return null;
  }
};

export const useUserStore = defineStore('user', () => {
  // 状态（容错读取 localStorage）
  const token = ref(localStorage.getItem('token') || null);
  const userInfo = ref(loadUserFromStorage());

  // 计算属性：是否登录
  const isLoggedIn = computed(() => !!token.value);

  // 计算属性：用户信息
  const user = computed(() => userInfo.value || {});

  // Actions
  const setToken = (newToken, newUser) => {
    token.value = newToken;
    userInfo.value = newUser;
    if (newToken) {
      localStorage.setItem('token', newToken);
    } else {
      localStorage.removeItem('token');
    }
    if (newUser) {
      try {
        localStorage.setItem('userInfo', JSON.stringify(newUser));
      } catch (e) {
        console.warn('保存用户信息失败:', e);
      }
    } else {
      localStorage.removeItem('userInfo');
    }
  };

  const logout = () => {
    token.value = null;
    userInfo.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
  };

  const updateUserInfo = (newUser) => {
    userInfo.value = newUser;
    try {
      localStorage.setItem('userInfo', JSON.stringify(newUser));
    } catch (e) {
      console.warn('保存用户信息失败:', e);
    }
  };

  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    user,
    // Actions
    setToken,
    logout,
    updateUserInfo
  };
});
