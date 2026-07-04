<template>
  <div class="seller-layout">
    <div class="sidebar-overlay" :class="{ active: isMobileSidebarOpen }" @click="closeMobileSidebar"></div>
    <aside class="sidebar" :class="{ 'mobile-open': isMobileSidebarOpen }">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 3L4 7v6c0 4.4 3.6 8 8 8s8-3.6 8-8V7l-8-4z"/>
            <path d="M9 12l2 2 4-4"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">乡村振兴</span>
          <span class="brand-sub">商家中心</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <span class="nav-label">主菜单</span>
          <router-link to="/dashboard" class="nav-item" :class="{ active: isActive('/dashboard') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>
            </span>
            <span class="nav-text">仪表盘</span>
            <span v-if="pendingCount > 0" class="nav-badge">{{ pendingCount }}</span>
          </router-link>
          <router-link to="/products" class="nav-item" :class="{ active: isActive('/products') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
            </span>
            <span class="nav-text">商品管理</span>
          </router-link>
          <router-link to="/orders" class="nav-item" :class="{ active: isActive('/orders') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
            </span>
            <span class="nav-text">订单管理</span>
          </router-link>
        </div>

        <div class="nav-section">
          <span class="nav-label">营销工具</span>
          <router-link to="/coupons" class="nav-item" :class="{ active: isActive('/coupons') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 12V8H4v4"/><path d="M4 8l8-4 8 4"/><path d="M6 12v6a2 2 0 002 2h8a2 2 0 002-2v-6"/><circle cx="12" cy="15" r="1"/></svg>
            </span>
            <span class="nav-text">优惠券管理</span>
          </router-link>
          <router-link to="/discounts" class="nav-item" :class="{ active: isActive('/discounts') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            </span>
            <span class="nav-text">打折活动</span>
          </router-link>
        </div>

        <div class="nav-section">
          <span class="nav-label">客户服务</span>
          <router-link to="/customer-service" class="nav-item" :class="{ active: isActive('/customer-service') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            </span>
            <span class="nav-text">客服聊天</span>
          </router-link>
          <router-link to="/reviews" class="nav-item" :class="{ active: isActive('/reviews') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            </span>
            <span class="nav-text">评价管理</span>
          </router-link>
          <router-link to="/aftersale" class="nav-item" :class="{ active: isActive('/aftersale') }">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </span>
            <span class="nav-text">售后管理</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/profile" class="nav-item">
          <span class="nav-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </span>
          <span class="nav-text">商家信息</span>
        </router-link>
        <button class="logout-btn" @click="handleLogout">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <main class="main-area">
      <header class="top-header">
        <div class="header-left">
          <button class="hamburger-btn" @click="toggleMobileSidebar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="3" y1="6" x2="21" y2="6"/>
              <line x1="3" y1="12" x2="21" y2="12"/>
              <line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
          <div class="breadcrumb">
            <span class="breadcrumb-item">{{ pageTitle || '商家后台' }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-time">{{ todayDate }}</div>
          <div class="user-dropdown" @click="showUserMenu = !showUserMenu" v-click-outside="() => showUserMenu = false">
            <div class="user-avatar">{{ sellerStore.user?.username?.charAt(0) || '商' }}</div>
            <span class="user-name">{{ sellerStore.user?.username || '商家' }}</span>
            <svg class="chevron" :class="{ open: showUserMenu }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            <div class="user-menu" v-if="showUserMenu">
              <div class="user-menu-item" @click="router.push('/profile'); showUserMenu = false">个人信息</div>
              <div class="user-menu-divider"></div>
              <div class="user-menu-item danger" @click="handleLogout; showUserMenu = false">退出登录</div>
            </div>
          </div>
        </div>
      </header>

      <div class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
        <div class="layout-footer">
          &copy; 2026 济南大学毕业设计. All rights reserved.
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useSellerStore } from '@/stores/seller';
import { ElMessageBox, ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const route = useRoute();
const sellerStore = useSellerStore()
const showUserMenu = ref(false)
const pendingCount = ref(0)
const sidebarCollapsed = ref(false)
const isMobileSidebarOpen = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function toggleMobileSidebar() {
  isMobileSidebarOpen.value = !isMobileSidebarOpen.value
}

function closeMobileSidebar() {
  isMobileSidebarOpen.value = false
}

const pageTitle = computed(() => route.meta.title || '');
const todayDate = computed(() => {
  const d = new Date();
  const days = ['日','一','二','三','四','五','六'];
  return `${d.getFullYear()}年${d.getMonth()+1}月${d.getDate()}日 星期${days[d.getDay()]}`;
});

const isActive = (path) => route.path.startsWith(path);

const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value(); };
    document.addEventListener('click', el._clickOutside);
  },
  unmounted(el) { document.removeEventListener('click', el._clickOutside); }
};

const loadPendingCount = async () => {
  try {
    const user = sellerStore.user;
    if (!user?.id) return;
    const res = await api.getSellerDashboard(user.id);
    if (res) {
      pendingCount.value = res.pendingOrders || 0;
    }
  } catch (e) {}
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '<div style="text-align:center;padding:24px 0"><div style="font-size:44px;margin-bottom:12px">🏪</div><h3 style="font-size:17px;font-weight:600;color:#1f2937;margin-bottom:6px">确认退出登录</h3><p style="font-size:13px;color:#6b7280">退出后将返回商家登录页面</p></div>',
      '', {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        showClose: false,
        dangerouslyUseHTMLString: true,
        customClass: 'seller-logout-dialog'
      }
    );
    sellerStore.logout();
    ElMessage.success('已安全退出');
    router.push('/login');
  } catch (e) {}
};

onMounted(() => loadPendingCount());
</script>

<style scoped>
.seller-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
}

.sidebar {
  width: 232px;
  display: flex;
  flex-direction: column;
  background: var(--bg-sidebar);
  border-right: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.brand-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--color-primary-600));
  border-radius: var(--radius-sm);
  color: #fff;
  flex-shrink: 0;
}
.brand-icon svg { width: 20px; height: 20px; }

.brand-text {
  display: flex;
  flex-direction: column;
}
.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #fafaf9;
  letter-spacing: 0.02em;
}
.brand-sub {
  font-size: 11px;
  color: var(--text-sidebar);
  margin-top: 1px;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 12px;
}

.nav-section {
  margin-bottom: 8px;
}
.nav-section + .nav-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.nav-label {
  display: block;
  padding: 6px 12px 10px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255,255,255,0.25);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  margin: 1px 0;
  border-radius: 8px;
  text-decoration: none;
  color: var(--text-sidebar);
  font-size: 13.5px;
  font-weight: 500;
  transition: all var(--transition);
  position: relative;
}

.nav-item:hover {
  background: var(--bg-sidebar-hover);
  color: #e7e5e4;
}

.nav-item.active {
  background: var(--bg-sidebar-active);
  color: var(--text-sidebar-active);
  font-weight: 600;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--primary);
  border-radius: 0 3px 3px 0;
}

.nav-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.7;
}
.nav-item.active .nav-icon, .nav-item:hover .nav-icon { opacity: 1; }
.nav-icon svg { width: 17px; height: 17px; }

.nav-text { flex: 1; }

.nav-badge {
  background: var(--color-error);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-error);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
  width: 100%;
}
.logout-btn:hover {
  background: rgba(255, 77, 79, 0.1);
}
.logout-btn svg {
  width: 17px;
  height: 17px;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 56px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.breadcrumb-item {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-time {
  font-size: 12.5px;
  color: var(--text-muted);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 24px;
  cursor: pointer;
  transition: all var(--transition);
  position: relative;
}
.user-dropdown:hover {
  background: var(--border-light);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--color-warning));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.chevron {
  width: 14px;
  height: 14px;
  color: var(--text-muted);
  transition: transform var(--transition);
}
.chevron.open { transform: rotate(180deg); }

.user-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border);
  min-width: 160px;
  padding: 4px;
  z-index: 100;
  animation: scaleIn 0.15s ease-out;
}

.user-menu-item {
  padding: 8px 14px;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--transition);
}
.user-menu-item:hover { background: var(--color-gray-50); }
.user-menu-item.danger { color: var(--color-error); }
.user-menu-item.danger:hover { background: var(--color-error-light); }

.user-menu-divider {
  height: 1px;
  background: var(--border-light);
  margin: 3px 0;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
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

/* 汉堡菜单按钮 */
.hamburger-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-right: 4px;
  flex-shrink: 0;
}

.hamburger-btn svg {
  width: 20px;
  height: 20px;
  color: var(--color-text-secondary);
}

.hamburger-btn:hover {
  background: var(--color-gray-100);
}

/* 侧边栏遮罩层 */
.sidebar-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 998;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.sidebar-overlay.active {
  opacity: 1;
  pointer-events: auto;
}

/* 折叠侧边栏 */
.sidebar.collapsed {
  width: 64px;
}

.sidebar.collapsed .brand-name,
.sidebar.collapsed .brand-sub,
.sidebar.collapsed .nav-text,
.sidebar.collapsed .nav-label,
.sidebar.collapsed .nav-badge,
.sidebar.collapsed .logout-btn span {
  display: none;
}

.sidebar.collapsed .brand-text {
  display: none;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px 0;
}

.sidebar.collapsed .nav-icon {
  margin-right: 0;
}

.sidebar.collapsed .sidebar-footer .logout-btn {
  justify-content: center;
}

/* 响应式：移动端侧边栏抽屉模式 */
@media (max-width: 768px) {
  .hamburger-btn {
    display: flex;
  }

  .sidebar-overlay {
    display: block;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 999;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar.mobile-open {
    transform: translateX(0);
  }

  .content-area {
    padding: 16px;
  }
}

/* 底部版权条 */
.layout-footer {
  text-align: center;
  padding: 24px 0 16px;
  color: #94a3b8;
  font-size: 13px;
  border-top: 1px solid #e2e8f0;
  margin-top: 24px;
}
</style>

