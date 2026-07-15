<template>
  <div class="profile-container ml-page-enter">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <p class="page-subtitle">管理您的账户信息</p>
    </div>

    <div class="profile-card">
      <div class="profile-header">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="avatar-wrapper">
            <div class="avatar">
              <img v-if="user.avatar" :src="user.avatar" class="avatar-img" />
              <span v-else class="avatar-icon">👤</span>
            </div>
          </div>
          <div class="user-info">
            <h3>{{ user.username || '用户' }}</h3>
            <p>{{ user.phone || '未绑定手机号' }}</p>
          </div>
        </div>

        <div class="order-stats">
          <div class="stat-item" @click="router.push({ path: '/order-list', query: { status: '0' } })">
            <span class="stat-count">{{ orderStats.pending }}</span>
            <span class="stat-label">待付款</span>
          </div>
          <div class="stat-item" @click="router.push({ path: '/order-list', query: { status: '1' } })">
            <span class="stat-count">{{ orderStats.shipping }}</span>
            <span class="stat-label">待发货</span>
          </div>
          <div class="stat-item" @click="router.push({ path: '/order-list', query: { status: '2' } })">
            <span class="stat-count">{{ orderStats.receiving }}</span>
            <span class="stat-label">待收货</span>
          </div>
          <div class="stat-item" @click="router.push({ path: '/order-list', query: { status: '3' } })">
            <span class="stat-count">{{ orderStats.completed }}</span>
            <span class="stat-label">已完成</span>
          </div>
        </div>

        <div class="header-decoration">
          <span class="deco-item deco-1">🌾</span>
          <span class="deco-item deco-2">🍃</span>
          <span class="deco-item deco-3">🌿</span>
        </div>
      </div>

      <div class="profile-menu">
        <div class="menu-item" @click="router.push('/order')">
          <span class="menu-icon">📦</span>
          <span class="menu-text">我的订单</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/address/list')">
          <span class="menu-icon">📍</span>
          <span class="menu-text">收货地址</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/aftersale')">
          <span class="menu-icon">🔧</span>
          <span class="menu-text">售后服务</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/coupons')">
          <span class="menu-icon">🎫</span>
          <span class="menu-text">领券中心</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/service')">
          <span class="menu-icon">💬</span>
          <span class="menu-text">在线客服</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/security')">
          <span class="menu-icon">🔒</span>
          <span class="menu-text">账户安全</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item deactivate" @click="handleDeactivate">
          <span class="menu-icon">⚠️</span>
          <span class="menu-text">注销账号</span>
          <span class="menu-arrow">→</span>
        </div>
        <div class="menu-item logout" @click="handleLogout">
          <span class="menu-icon">🚪</span>
          <span class="menu-text">退出登录</span>
          <span class="menu-arrow">→</span>
        </div>
      </div>

      <div class="profile-footer">
        <p>最后登录：{{ lastLoginTime }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { ElMessageBox, ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const userStore = useUserStore();

const user = computed(() => userStore.user);
const lastLoginTime = ref('');

const orderStats = reactive({
  pending: 0,
  shipping: 0,
  receiving: 0,
  completed: 0
});

const loadOrderStats = async () => {
  try {
    const res = await api.get('/order/list', { params: { page: 1, size: 999 } });
    if (res.code === 0 && res.data && res.data.list) {
      const list = res.data.list;
      orderStats.pending = list.filter(o => o.status === 0).length;
      orderStats.shipping = list.filter(o => o.status === 1).length;
      orderStats.receiving = list.filter(o => o.status === 2).length;
      orderStats.completed = list.filter(o => o.status === 3).length;
    }
  } catch (e) {}
};

onMounted(() => {
  const now = new Date();
  const options = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  };
  lastLoginTime.value = now.toLocaleDateString('zh-CN', options);
  loadOrderStats();
});

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<div style="font-size: 48px; margin-bottom: 16px;">👋</div>' +
      '<h3 style="font-size: 18px; font-weight: 600; color: #1f2937; margin-bottom: 8px;">确认退出登录</h3>' +
      '<p style="font-size: 14px; color: #6b7280;">退出后将返回首页，请确认您已保存所有操作</p>' +
      '</div>',
      '',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        confirmButtonClass: 'buyer-logout-confirm',
        cancelButtonClass: 'buyer-logout-cancel',
        customClass: 'buyer-logout-dialog',
        showClose: false,
        dangerouslyUseHTMLString: true
      }
    );
    userStore.logout();
    ElMessage.success('已安全退出');
    router.push('/login');
  } catch (error) {
    ElMessage.info('已取消退出');
  }
};

const handleDeactivate = async () => {
  try {
    await ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<div style="font-size: 48px; margin-bottom: 16px;">⚠️</div>' +
      '<h3 style="font-size: 18px; font-weight: 600; color: #dc2626; margin-bottom: 8px;">注销账号</h3>' +
      '<p style="font-size: 14px; color: #6b7280;">注销后账号数据将无法恢复，请谨慎操作</p>' +
      '</div>',
      '',
      {
        confirmButtonText: '继续注销',
        cancelButtonText: '取消',
        confirmButtonClass: 'buyer-deactivate-confirm',
        cancelButtonClass: 'buyer-deactivate-cancel',
        customClass: 'buyer-deactivate-dialog',
        showClose: false,
        dangerouslyUseHTMLString: true
      }
    );

    await ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<div style="font-size: 48px; margin-bottom: 16px;">🚨</div>' +
      '<h3 style="font-size: 18px; font-weight: 600; color: #dc2626; margin-bottom: 8px;">最终确认</h3>' +
      '<p style="font-size: 14px; color: #6b7280;">此操作不可逆，确定要注销账号吗？</p>' +
      '</div>',
      '',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '再想想',
        confirmButtonClass: 'buyer-deactivate-final-confirm',
        cancelButtonClass: 'buyer-deactivate-cancel',
        customClass: 'buyer-deactivate-dialog',
        showClose: false,
        dangerouslyUseHTMLString: true
      }
    );

    const res = await api.post('/user/deactivate');
    if (res.code === 0) {
      ElMessage.success('账号已注销');
      userStore.logout();
      router.push('/login');
    } else {
      ElMessage.error(res.message || '注销失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.info('已取消注销');
    }
  }
};
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

.profile-card {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  border: 1px solid var(--border-light);
  position: relative;
}

.profile-header {
  position: relative;
  padding: 48px 30px 36px;
  background: linear-gradient(135deg,
    var(--accent) 0%,
    var(--nature-green) 50%,
    var(--warm-orange) 100%
  );
  color: white;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 5c-5 10-15 15-25 15 5 10 15 20 25 35 10-15 20-25 25-35-10 0-20-5-25-15z' fill='%23ffffff' fill-opacity='0.05'/%3E%3C/svg%3E");
  opacity: 0.6;
}

.header-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-wrapper {
  position: relative;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all var(--transition-base);
  overflow: hidden;
}

.avatar:hover {
  transform: scale(1.05);
  border-color: white;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-icon {
  font-size: 40px;
}

.user-info h3 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-info p {
  font-size: 16px;
  opacity: 0.95;
  margin: 0;
}

.order-stats {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-around;
  margin-top: 24px;
  padding: 16px 0;
  background: rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(8px);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}

.stat-count {
  font-size: 22px;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}

.header-decoration {
  position: absolute;
  top: 16px;
  right: 24px;
  display: flex;
  gap: 12px;
  opacity: 0.3;
}

.deco-item {
  font-size: 24px;
  animation: float 4s ease-in-out infinite;
}

.deco-1 { animation-delay: 0s; }
.deco-2 { animation-delay: 1s; }
.deco-3 { animation-delay: 2s; }

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-8px) rotate(10deg);
  }
}

.profile-menu {
  padding: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 30px;
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  border-left: 3px solid transparent;
}

.menu-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background: linear-gradient(90deg, var(--accent), transparent);
  transition: width var(--transition-base);
}

.menu-item:hover {
  background: linear-gradient(90deg, var(--bg-1), transparent);
  border-left-color: var(--accent);
}

.menu-item:hover::before {
  width: 100%;
}

.menu-item:hover .menu-arrow {
  transform: translateX(4px);
  color: var(--accent-dark);
}

.menu-icon {
  font-size: 22px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--bg-1), var(--bg-2));
  transition: all var(--transition-base);
}

.menu-item:hover .menu-icon {
  background: linear-gradient(135deg, var(--accent-light), var(--accent));
  transform: scale(1.05);
}

.menu-text {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.menu-arrow {
  font-size: 18px;
  color: var(--text-tertiary);
  transition: all var(--transition-base);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item.logout .menu-text {
  color: var(--error);
}

.menu-item.logout:hover {
  background: linear-gradient(90deg, rgba(199, 80, 80, 0.08), transparent);
  border-left-color: var(--error);
}

.menu-item.logout:hover .menu-icon {
  background: linear-gradient(135deg, rgba(199, 80, 80, 0.15), rgba(199, 80, 80, 0.08));
}

.menu-item.deactivate .menu-text {
  color: #dc2626;
}

.menu-item.deactivate:hover {
  background: linear-gradient(90deg, rgba(220, 38, 38, 0.08), transparent);
  border-left-color: #dc2626;
}

.menu-item.deactivate:hover .menu-icon {
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.15), rgba(220, 38, 38, 0.08));
}

.profile-footer {
  padding: 20px 30px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-1);
}

.profile-footer p {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
  text-align: center;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 24px 16px;
  }

  .page-title {
    font-size: 2rem;
  }

  .profile-header {
    padding: 36px 20px 28px;
  }

  .header-content {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .user-info h3 {
    font-size: 24px;
  }

  .order-stats {
    margin-top: 16px;
    padding: 12px 0;
  }

  .stat-count {
    font-size: 18px;
  }

  .header-decoration {
    top: 12px;
    right: 16px;
  }

  .deco-item {
    font-size: 20px;
  }

  .menu-item {
    padding: 16px 20px;
  }

  .profile-footer {
    padding: 16px 20px;
  }
}

:deep(.buyer-logout-dialog) {
  .el-message-box {
    width: 360px;
    border-radius: 20px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
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
    border-top: 1px solid #f0f0f0;
  }
}

:deep(.buyer-logout-confirm) {
  flex: 1;
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  border: none;
  border-radius: 12px;
  height: 44px;
  font-weight: 600;
  color: #fff;

  &:hover {
    background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
    box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
    transform: translateY(-1px);
  }
}

:deep(.buyer-logout-cancel) {
  flex: 1;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  height: 44px;
  font-weight: 600;
  color: #666;

  &:hover {
    background: #e8e8e8;
    border-color: #d0d0d0;
  }
}

:deep(.buyer-deactivate-dialog) {
  .el-message-box {
    width: 360px;
    border-radius: 20px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
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
    border-top: 1px solid #f0f0f0;
  }
}

:deep(.buyer-deactivate-confirm) {
  flex: 1;
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
  border: none;
  border-radius: 12px;
  height: 44px;
  font-weight: 600;
  color: #fff;

  &:hover {
    background: linear-gradient(135deg, #b91c1c 0%, #dc2626 100%);
    box-shadow: 0 4px 16px rgba(220, 38, 38, 0.3);
    transform: translateY(-1px);
  }
}

:deep(.buyer-deactivate-final-confirm) {
  flex: 1;
  background: linear-gradient(135deg, #991b1b 0%, #dc2626 100%);
  border: none;
  border-radius: 12px;
  height: 44px;
  font-weight: 600;
  color: #fff;

  &:hover {
    background: linear-gradient(135deg, #7f1d1d 0%, #991b1b 100%);
    box-shadow: 0 4px 16px rgba(153, 27, 27, 0.3);
    transform: translateY(-1px);
  }
}

:deep(.buyer-deactivate-cancel) {
  flex: 1;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  height: 44px;
  font-weight: 600;
  color: #666;

  &:hover {
    background: #e8e8e8;
    border-color: #d0d0d0;
  }
}
</style>
