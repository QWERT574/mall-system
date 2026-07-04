import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

const loadCartFromStorage = () => {
  try {
    const raw = localStorage.getItem('cartItems');
    if (!raw || raw === 'undefined' || raw === 'null') return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    console.warn('解析购物车数据失败,已重置:', e);
    localStorage.removeItem('cartItems');
    return [];
  }
};

export const useCartStore = defineStore('cart', () => {
  // 状态（容错读取 localStorage,避免脏数据导致 store 初始化失败）
  const cartItems = ref(loadCartFromStorage());

  // 计算属性
  const cartCount = computed(() => {
    return cartItems.value.reduce((total, item) => total + (item.count || 0), 0);
  });

  const cartTotal = computed(() => {
    return cartItems.value.reduce((total, item) => total + (item.price || 0) * (item.count || 0), 0);
  });

  // Actions
  const saveToLocalStorage = () => {
    try {
      localStorage.setItem('cartItems', JSON.stringify(cartItems.value));
    } catch (e) {
      console.warn('保存购物车数据失败:', e);
    }
  };

  const addToCart = (product) => {
    const existingItem = cartItems.value.find(item => item.id === product.id);
    if (existingItem) {
      existingItem.count += 1;
    } else {
      cartItems.value.push({
        ...product,
        count: 1
      });
    }
    saveToLocalStorage();
  };

  const removeFromCart = (productId) => {
    cartItems.value = cartItems.value.filter(item => item.id !== productId);
    saveToLocalStorage();
  };

  const updateItemCount = (productId, count) => {
    const item = cartItems.value.find(item => item.id === productId);
    if (item) {
      item.count = Math.max(1, count);
      saveToLocalStorage();
    }
  };

  const clearCart = () => {
    cartItems.value = [];
    saveToLocalStorage();
  };

  return {
    // 状态
    cartItems,
    // 计算属性
    cartCount,
    cartTotal,
    // Actions
    addToCart,
    removeFromCart,
    updateItemCount,
    clearCart
  };
});
