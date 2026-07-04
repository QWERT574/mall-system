import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useSellerStore = defineStore('seller', () => {
  const token = ref(localStorage.getItem('seller_token') || '');
  const user = ref(JSON.parse(localStorage.getItem('seller_user') || '{}'));
  
  const isLoggedIn = computed(() => !!token.value);
  const sellerInfo = computed(() => user.value || {});
  
  function setAuth(authToken, userInfo) {
    token.value = authToken;
    user.value = userInfo;
    localStorage.setItem('seller_token', authToken);
    localStorage.setItem('seller_user', JSON.stringify(userInfo));
  }
  
  function logout() {
    token.value = '';
    user.value = {};
    localStorage.removeItem('seller_token');
    localStorage.removeItem('seller_user');
  }
  
  function updateUserInfo(info) {
    user.value = { ...user.value, ...info };
    localStorage.setItem('seller_user', JSON.stringify(user.value));
  }
  
  return {
    token,
    user,
    isLoggedIn,
    sellerInfo,
    setAuth,
    logout,
    updateUserInfo
  };
});
