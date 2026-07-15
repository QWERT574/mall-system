<template>
  <el-container class="layout-container">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobileSidebarOpen"
      class="mobile-overlay"
      @click="isMobileSidebarOpen = false"
    ></div>

    <el-aside
      :width="isCollapsed ? '64px' : '220px'"
      class="layout-aside"
      :class="{ 'mobile-open': isMobileSidebarOpen }"
    >
      <div class="aside-brand">
        <div class="brand-icon">🌾</div>
        <div v-show="!isCollapsed" class="brand-text">
          <span class="brand-name">乡村振兴</span>
          <span class="brand-sub">管理后台</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
        >
          <el-icon><component :is="route.meta.icon" /></el-icon>
          <span>{{ route.meta.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-button text class="logout-btn" @click="handleCommand('logout')">
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!isCollapsed">退出登录</span>
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn desktop-collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-icon class="hamburger-btn" @click="toggleMobileSidebar">
            <Expand />
          </el-icon>
          <span class="logo">商城管理</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              <el-icon><User /></el-icon>
              {{ userInfo?.nickname || '管理员' }}
            </span>
            <template #dropdown>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view />
        <div class="layout-footer">
          &copy; 2026 济南大学毕业设计. All rights reserved.
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { User, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapsed = ref(false)
const isMobileSidebarOpen = ref(false)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function toggleMobileSidebar() {
  isMobileSidebarOpen.value = !isMobileSidebarOpen.value
}

const userInfo = computed(() => userStore.userInfo)

const menuRoutes = computed(() => {
  return [
    { path: '/dashboard', meta: { title: '仪表盘', icon: 'Odometer' }, icon: 'Odometer' },
    { path: '/product', meta: { title: '商品管理', icon: 'Goods' }, icon: 'Goods' },
    { path: '/order', meta: { title: '订单管理', icon: 'ShoppingCart' }, icon: 'ShoppingCart' },
    { path: '/user', meta: { title: '用户管理', icon: 'User' }, icon: 'User' },
    { path: '/seller', meta: { title: '商家审核', icon: 'Shop' }, icon: 'Shop' },
    { path: '/activity', meta: { title: '活动管理', icon: 'Calendar' }, icon: 'Calendar' },
    { path: '/aftersale', meta: { title: '售后管理', icon: 'Service' }, icon: 'Service' },
    { path: '/knowledge', meta: { title: '知识库管理', icon: 'Collection' }, icon: 'Collection' },
    { path: '/system', meta: { title: '系统设置', icon: 'Setting' }, icon: 'Setting' }
  ]
})

const activeMenu = computed(() => route.path)

const handleMenuSelect = (index: string) => {
  router.push(index)
  isMobileSidebarOpen.value = false
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<div style="font-size: 48px; margin-bottom: 16px;">🚪</div>' +
      '<h3 style="font-size: 18px; font-weight: 600; color: #1f2937; margin-bottom: 8px;">确认退出登录</h3>' +
      '<p style="font-size: 14px; color: #6b7280;">退出后将返回登录页面，请确认您已保存所有操作</p>' +
      '</div>',
      '',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        confirmButtonClass: 'logout-confirm-btn',
        cancelButtonClass: 'logout-cancel-btn',
        customClass: 'logout-dialog',
        showClose: false,
        dangerouslyUseHTMLString: true
      }
    )
    await userStore.logoutAction()
    ElMessage.success('已安全退出')
    router.push('/login')
  } catch (error) {
    ElMessage.info('已取消退出')
  }
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background-color: var(--color-bg-page);
}

.layout-aside {
  background: linear-gradient(180deg, #1c1917 0%, #292524 100%);
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 侧边栏品牌区 */
.aside-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 1px;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.sidebar-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.65);
  margin: 2px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(212, 165, 116, 0.12);
  color: rgba(255, 255, 255, 0.9);
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(212, 165, 116, 0.3);
}

/* 侧边栏底部 */
.aside-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.logout-btn {
  width: 100%;
  justify-content: flex-start;
  color: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  color: var(--color-error);
  background: rgba(255, 77, 79, 0.1);
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, #ffffff, var(--color-primary-50));
  border-bottom: 2px solid var(--color-primary-100);
  padding: 0 24px;
  height: 60px;
  box-shadow: var(--shadow-xs);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: var(--color-text-secondary);
}

.collapse-btn:hover {
  background: var(--color-gray-100);
  color: var(--color-primary-500);
}

.header-left .logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary-700);
  font-family: var(--font-family-display);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
  transition: color 0.2s;
}

.user-name:hover {
  color: var(--color-primary-500);
}

.el-main {
  padding: 20px;
  overflow-y: auto;
  background-color: var(--color-bg-page);
}

/* 退出登录对话框样式 */
:deep(.logout-dialog) {
  .el-message-box {
    width: 380px;
    border-radius: 16px;
    box-shadow: var(--shadow-2xl);
    border: none;
    overflow: hidden;
  }
  
  .el-message-box__header {
    display: none;
  }
  
  .el-message-box__content {
    padding: 0;
    margin: 0;
  }
  
  .el-message-box__btns {
    display: flex;
    gap: 12px;
    padding: 20px 24px;
    border-top: 1px solid var(--color-border-light);
  }
}

:deep(.logout-confirm-btn) {
  flex: 1;
  background: linear-gradient(135deg, var(--color-error) 0%, var(--color-error-dark) 100%);
  border: none;
  border-radius: 10px;
  height: 40px;
  font-weight: 500;
  color: #fff;
  
  &:hover {
    background: linear-gradient(135deg, var(--color-error-dark) 0%, #b71c1c 100%);
    box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  }
}

:deep(.logout-cancel-btn) {
  flex: 1;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  height: 40px;
  font-weight: 500;
  color: var(--color-text-secondary);
  
  &:hover {
    background: var(--color-gray-100);
    border-color: var(--color-border-default);
  }
}

/* 响应式：小屏幕侧边栏变为抽屉模式 */
.hamburger-btn {
  display: none;
  font-size: 20px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: var(--color-text-secondary);
}

.hamburger-btn:hover {
  background: var(--color-gray-100);
  color: var(--color-primary-500);
}

.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  transition: opacity 0.3s ease;
}

@media (max-width: 768px) {
  .desktop-collapse-btn {
    display: none;
  }

  .hamburger-btn {
    display: inline-flex;
  }

  .layout-aside {
    position: fixed !important;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .layout-aside.mobile-open {
    transform: translateX(0);
  }

  .el-main {
    padding: 12px;
  }
}

/* 底部版权条 */
.layout-footer {
  text-align: center;
  padding: 24px 0 16px;
  color: #909399;
  font-size: 13px;
  border-top: 1px solid #ebeef5;
  margin-top: 24px;
}
</style>

