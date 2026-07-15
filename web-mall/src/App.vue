<template>
  <div id="app">
    <div :class="['loading-bar', { 'is-loading': loadingBarState === 'loading', 'is-done': loadingBarState === 'done' }]">
      <div class="loading-bar-inner"></div>
    </div>

    <div v-if="message.show" :class="['message-box', message.type]" @click="closeMessage">
      <span class="message-icon">{{ message.type === 'success' ? '✓' : message.type === 'error' ? '✗' : 'ℹ' }}</span>
      <span class="message-text">{{ message.text }}</span>
    </div>

    <header v-if="showNav" class="top-nav glass">
      <div class="top-nav-content">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon-wrapper">
            <span class="logo-icon">🌾</span>
          </div>
          <div class="logo-text-group">
            <span class="logo-text">乡村振兴</span>
            <span class="logo-slogan">新鲜直达</span>
          </div>
        </div>

        <div class="search-container">
          <input
            type="text"
            placeholder="搜索新鲜好物..."
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
            class="search-input"
          />
          <button @click="handleSearch" class="search-btn">
            <span class="search-icon">🔍</span>
          </button>
        </div>

        <div class="nav-right">
          <div class="nav-item" @click="router.push('/ai')">
            <div class="nav-icon-wrapper">
              <span class="nav-icon">🤖</span>
            </div>
            <span class="nav-text">AI助手</span>
          </div>
          <div class="nav-item" @click="$router.push('/cart')">
            <div class="nav-icon-wrapper">
              <span class="nav-icon">🛒</span>
              <span v-if="cartCount > 0" class="cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
            </div>
            <span class="nav-text">购物车</span>
          </div>
          <div class="nav-item" @click="goToProfile">
            <div class="nav-icon-wrapper">
              <span class="nav-icon">👤</span>
            </div>
            <span class="nav-text">{{ isLoggedIn ? user.username : '登录' }}</span>
          </div>
        </div>

        <button class="hamburger-btn" @click="toggleMobileMenu">
          <span :class="['hamburger-icon', { 'is-open': mobileMenuOpen }]">
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
      </div>

      <transition name="mobile-menu">
        <div v-if="mobileMenuOpen" class="mobile-menu">
          <div class="mobile-menu-item" @click="mobileNavClick('/ai')">
            <span class="nav-icon">🤖</span>
            <span>AI助手</span>
          </div>
          <div class="mobile-menu-item" @click="mobileNavClick('/cart')">
            <span class="nav-icon">🛒</span>
            <span>购物车</span>
          </div>
          <div class="mobile-menu-item" @click="mobileNavClick(isLoggedIn ? '/profile' : '/login')">
            <span class="nav-icon">👤</span>
            <span>{{ isLoggedIn ? user.username : '登录' }}</span>
          </div>
        </div>
      </transition>
    </header>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <footer v-if="showNav" class="footer">
      <div class="footer-content">
        <div class="footer-main">
          <div class="footer-brand">
            <div class="footer-logo">
              <span class="footer-logo-icon">🌾</span>
              <span class="footer-logo-text">乡村振兴</span>
            </div>
            <p class="footer-desc">优质农产品，新鲜直达您的餐桌</p>
          </div>
          <div class="footer-links">
            <div class="footer-column">
              <h4 class="footer-title">关于我们</h4>
              <span class="footer-link-static">品牌故事</span>
              <span class="footer-link-static">联系我们</span>
              <span class="footer-link-static">加入我们</span>
            </div>
            <div class="footer-column">
              <h4 class="footer-title">帮助中心</h4>
              <router-link to="/product/list" class="footer-link">购物指南</router-link>
              <router-link to="/order" class="footer-link">配送说明</router-link>
              <router-link to="/aftersale" class="footer-link">售后服务</router-link>
            </div>
            <div class="footer-column">
              <h4 class="footer-title">关注我们</h4>
              <span class="footer-link-static">微信公众号</span>
              <span class="footer-link-static">微博</span>
              <span class="footer-link-static">抖音</span>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2026 济南大学毕业设计. All rights reserved.</p>
          <p>用心甄选，只为更好的你</p>
        </div>
      </div>
    </footer>

    <transition name="back-top-fade">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
        ↑
      </button>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useCartStore } from '@/stores/cart';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const cartStore = useCartStore();

const message = ref({ show: false, text: '', type: 'info' });
const searchKeyword = ref('');
const showBackToTop = ref(false);
const mobileMenuOpen = ref(false);
const loadingBarState = ref('hidden');

const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const cartCount = computed(() => cartStore.cartCount);

const showNav = computed(() => {
  return !['/login', '/register', '/reset-password'].includes(route.path);
});

const showMessage = (text, type = 'info') => {
  message.value = { show: true, text, type };
  setTimeout(() => {
    message.value.show = false;
  }, 3000);
};

const closeMessage = () => {
  message.value.show = false;
};

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value;
    router.push({ path: '/product/list', query: { search: keyword } });
    searchKeyword.value = '';
  }
};

const goToProfile = () => {
  if (isLoggedIn.value) {
    router.push('/profile');
  } else {
    router.push('/login');
  }
};

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value;
};

const mobileNavClick = (path) => {
  router.push(path);
  mobileMenuOpen.value = false;
};

const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300;
};

const handleKeydown = (e) => {
  if (e.key === 'Escape' && mobileMenuOpen.value) {
    mobileMenuOpen.value = false;
  }
};

let wasOnProductList = false;

watch(() => route.path, (newPath) => {
  if (wasOnProductList && !newPath.startsWith('/product/list')) {
    searchKeyword.value = '';
  }
  wasOnProductList = newPath.startsWith('/product/list');
}, { immediate: true });

router.beforeEach(() => {
  loadingBarState.value = 'loading';
});

router.afterEach(() => {
  loadingBarState.value = 'done';
  mobileMenuOpen.value = false;
  setTimeout(() => {
    loadingBarState.value = 'hidden';
  }, 500);
});

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-body);
  background: var(--bg-0);
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 10000;
  pointer-events: none;
}

.loading-bar-inner {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, var(--accent), var(--warm-orange));
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(212, 165, 116, 0.6);
}

.loading-bar.is-loading .loading-bar-inner {
  animation: loadingProgress 1.5s ease-out forwards;
}

.loading-bar.is-done .loading-bar-inner {
  width: 100%;
  transition: width 0.2s ease;
}

.loading-bar.is-done {
  animation: loadingFadeOut 0.3s ease 0.2s forwards;
}

@keyframes loadingProgress {
  0% { width: 0; }
  30% { width: 40%; }
  60% { width: 60%; }
  100% { width: 75%; }
}

@keyframes loadingFadeOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

.message-box {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 14px 28px;
  border-radius: var(--radius-full);
  color: white;
  font-size: 15px;
  font-weight: 600;
  z-index: 9999;
  cursor: pointer;
  box-shadow: var(--shadow-lg);
  display: flex;
  align-items: center;
  gap: 10px;
  animation: slideDown 0.3s ease-out;
  backdrop-filter: blur(10px);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.message-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.message-box.success {
  background: linear-gradient(135deg, var(--success), #5a8a5e);
}

.message-box.error {
  background: linear-gradient(135deg, var(--error), #a04040);
}

.message-box.info {
  background: linear-gradient(135deg, var(--info), #5a7a8f);
}

.top-nav {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid var(--border-light);
}

.top-nav-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(212, 165, 116, 0.3);
  transition: all var(--transition-base);
}

.logo:hover .logo-icon-wrapper {
  transform: rotate(-5deg);
  box-shadow: 0 6px 16px rgba(212, 165, 116, 0.4);
}

.logo-icon {
  font-size: 28px;
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-dark), var(--warm-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.logo-slogan {
  font-size: 11px;
  color: var(--text-tertiary);
  letter-spacing: 0.05em;
}

.search-container {
  flex: 1;
  position: relative;
  display: flex;
  max-width: 600px;
}

.search-input {
  flex: 1;
  padding: 12px 20px;
  border: 2px solid var(--border);
  border-radius: var(--radius-full) 0 0 var(--radius-full);
  font-size: 15px;
  outline: none;
  transition: all var(--transition-base);
  background: var(--card-bg);
}

.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(212, 165, 116, 0.1);
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: white;
  border: none;
  border-radius: 0 var(--radius-full) var(--radius-full) 0;
  cursor: pointer;
  font-size: 16px;
  transition: all var(--transition-base);
  box-shadow: 0 2px 8px rgba(212, 165, 116, 0.3);
}

.search-btn:hover {
  background: linear-gradient(135deg, var(--accent-dark), var(--accent));
  box-shadow: 0 4px 12px rgba(212, 165, 116, 0.4);
}

.search-icon {
  display: block;
}

.nav-right {
  display: flex;
  gap: 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
  position: relative;
}

.nav-item:hover {
  background: var(--bg-1);
  transform: translateY(-2px);
}

.nav-icon-wrapper {
  position: relative;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon {
  font-size: 22px;
}

.cart-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: linear-gradient(135deg, var(--warm-orange), var(--warm-orange-dark));
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: var(--radius-full);
  min-width: 18px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(224, 123, 57, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

.nav-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.hamburger-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  z-index: 110;
}

.hamburger-icon {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 24px;
}

.hamburger-icon span {
  display: block;
  height: 2px;
  background: var(--text-primary);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.hamburger-icon.is-open span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}

.hamburger-icon.is-open span:nth-child(2) {
  opacity: 0;
}

.hamburger-icon.is-open span:nth-child(3) {
  transform: rotate(-45deg) translate(5px, -5px);
}

.mobile-menu {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--border-light);
  padding: 8px 0;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  cursor: pointer;
  transition: background var(--transition-base);
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.mobile-menu-item:hover {
  background: var(--bg-1);
}

.mobile-menu-item .nav-icon {
  font-size: 20px;
}

.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: all 0.3s ease;
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.main-content {
  flex: 1;
  padding: 24px 0;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.back-to-top {
  position: fixed;
  bottom: 100px;
  right: 24px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: white;
  border: none;
  cursor: pointer;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(212, 165, 116, 0.4);
  z-index: 90;
  transition: all var(--transition-base);
}

.back-to-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(212, 165, 116, 0.5);
}

.back-top-fade-enter-active,
.back-top-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.back-top-fade-enter-from,
.back-top-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.footer {
  background: linear-gradient(135deg, var(--earth-brown-dark), var(--earth-brown));
  color: white;
  margin-top: auto;
  position: relative;
  overflow: hidden;
}

.footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), var(--nature-green), var(--warm-orange));
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 48px 24px 24px;
}

.footer-main {
  display: flex;
  justify-content: space-between;
  gap: 60px;
  margin-bottom: 40px;
}

.footer-brand {
  flex: 1;
  max-width: 300px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.footer-logo-icon {
  font-size: 32px;
}

.footer-logo-text {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.footer-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.footer-links {
  display: flex;
  gap: 60px;
}

.footer-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: white;
}

.footer-link {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  transition: all var(--transition-base);
  position: relative;
}

.footer-link::after {
  display: none;
}

.footer-link:hover {
  color: var(--accent-light);
  transform: translateX(4px);
}

.footer-link-static {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: default;
}

.footer-bottom {
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@media (max-width: 992px) {
  .top-nav-content {
    flex-wrap: wrap;
    gap: 16px;
  }

  .search-container {
    order: 3;
    width: 100%;
    max-width: none;
  }

  .footer-main {
    flex-direction: column;
    gap: 40px;
  }

  .footer-brand {
    max-width: none;
  }

  .footer-links {
    flex-wrap: wrap;
    gap: 40px;
  }
}

@media (max-width: 768px) {
  .top-nav-content {
    padding: 12px 16px;
  }

  .logo-text-group {
    display: none;
  }

  .nav-right {
    display: none;
  }

  .hamburger-btn {
    display: block;
  }

  .footer-links {
    gap: 30px;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }

  .back-to-top {
    bottom: 80px;
    right: 16px;
    width: 42px;
    height: 42px;
    font-size: 18px;
  }
}
</style>

