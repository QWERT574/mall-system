<template>
  <div class="security-container">
    <div class="page-header">
      <button class="back-btn" @click="router.back()">
        <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
          <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"/>
        </svg>
      </button>
      <h2 class="page-title">账户安全</h2>
    </div>

    <div class="security-card">
      <!-- 修改密码 -->
      <div class="section">
        <div class="section-header">
          <span class="section-icon">🔐</span>
          <h3>修改密码</h3>
        </div>
        <p class="section-desc">定期更换密码可以有效保护您的账户安全</p>

        <form @submit.prevent="handleChangePassword" class="form-body">
          <div class="field-group">
            <label>当前密码</label>
            <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入当前密码" required />
          </div>
          <div class="field-group">
            <label>新密码</label>
            <input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" required minlength="6" maxlength="20" />
          </div>
          <div class="field-group">
            <label>确认新密码</label>
            <input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" required minlength="6" maxlength="20" />
          </div>

          <div class="strength-bar">
            <div class="strength-track">
              <div class="strength-fill" :style="{ width: passwordStrength + '%' }" :class="'level-' + strengthLevel"></div>
            </div>
            <span class="strength-text">{{ strengthLabel }}</span>
          </div>

          <button type="submit" class="submit-btn" :disabled="pwdLoading">
            <span v-if="!pwdLoading">确认修改</span>
            <span v-else>提交中...</span>
          </button>
        </form>
      </div>

      <!-- 账户信息 -->
      <div class="section">
        <div class="section-header">
          <span class="section-icon">📋</span>
          <h3>账户信息</h3>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ user.username || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">手机号</span>
            <span class="info-value">{{ maskedPhone }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ user.nickname || '未设置' }}</span>
          </div>
        </div>
      </div>

      <!-- 注销账号 -->
      <div class="section danger-section">
        <div class="section-header">
          <span class="section-icon">⚠️</span>
          <h3>注销账号</h3>
        </div>
        <p class="section-desc">注销后账号将被永久删除，此操作不可恢复</p>
        <div class="deactivate-info">
          <div class="deactivate-warning">
            <p>注销账号后，以下数据将被永久删除且无法恢复：</p>
            <ul>
              <li>个人资料和账户信息</li>
              <li>所有订单记录</li>
              <li>收货地址信息</li>
              <li>优惠券和积分</li>
              <li>聊天记录</li>
            </ul>
          </div>
        </div>
        <button class="deactivate-btn" @click="handleDeactivate" :disabled="deactivating">
          <span v-if="!deactivating">申请注销账号</span>
          <span v-else>处理中...</span>
        </button>
      </div>

      <!-- 安全提示 -->
      <div class="tips-section">
        <h4>🛡️ 安全建议</h4>
        <ul>
          <li>密码长度不少于8位，包含字母和数字</li>
          <li>不要使用与其它平台相同的密码</li>
          <li>定期更换密码，避免使用生日、手机号等易猜信息</li>
          <li>不要将账号密码告知他人</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import api from '@/utils/api';
import { ElMessage } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

const user = computed(() => userStore.user);
const pwdLoading = ref(false);
const deactivating = ref(false);

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const maskedPhone = computed(() => {
  const phone = user.value.phone;
  if (!phone || phone.length < 11) return phone || '未绑定';
  return phone.substring(0, 3) + '****' + phone.substring(7);
});

const passwordStrength = computed(() => {
  const p = pwdForm.value.newPassword;
  if (!p) return 0;
  let score = 0;
  if (p.length >= 6) score += 25;
  if (p.length >= 10) score += 15;
  if (/[A-Z]/.test(p)) score += 15;
  if (/[a-z]/.test(p)) score += 10;
  if (/[0-9]/.test(p)) score += 20;
  if (/[^A-Za-z0-9]/.test(p)) score += 15;
  return Math.min(score, 100);
});

const strengthLevel = computed(() => {
  const s = passwordStrength.value;
  return s < 30 ? 1 : s < 60 ? 2 : s < 80 ? 3 : 4;
});

const strengthLabel = computed(() => {
  const labels = ['', '弱', '一般', '较强', '强'];
  return labels[strengthLevel.value];
});

const handleChangePassword = async () => {
  if (!pwdForm.value.oldPassword) {
    ElMessage.warning('请输入当前密码');
    return;
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致');
    return;
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位');
    return;
  }

  pwdLoading.value = true;
  try {
    const res = await api.updateUserInfo({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    });
    if (res.code === 0) {
      ElMessage.success('密码修改成功，请重新登录');
      setTimeout(() => {
        userStore.logout();
        router.push('/login');
      }, 1500);
    } else {
      ElMessage.error(res.message || '密码修改失败');
    }
  } catch (err) {
    ElMessage.error(err.message || '网络错误，请稍后重试');
  } finally {
    pwdLoading.value = false;
  }
};

const handleDeactivate = async () => {
  const confirmed = confirm('确定要注销账号吗？此操作不可恢复！');
  if (!confirmed) return;

  const doubleConfirm = confirm('再次确认：注销后所有数据将永久删除，无法恢复。确定继续？');
  if (!doubleConfirm) return;

  deactivating.value = true;
  try {
    const res = await api.post('/user/deactivate');
    if (res.code === 0) {
      ElMessage.success('账号已注销，即将退出登录');
      setTimeout(() => {
        userStore.logout();
        router.push('/login');
      }, 1500);
    } else {
      ElMessage.error(res.message || '注销失败');
    }
  } catch (err) {
    ElMessage.error(err.message || '网络错误，请稍后重试');
  } finally {
    deactivating.value = false;
  }
};
</script>

<style scoped>
.security-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid var(--border-light); background: var(--card-bg);
  color: var(--text-secondary); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.back-btn:hover { border-color: var(--accent); color: var(--accent); background: rgba(212,165,116,0.06); }

.page-title {
  font-family: var(--font-display); font-size: 1.75rem; font-weight: 700;
  color: var(--text-primary); margin: 0;
}

.security-card {
  background: var(--card-bg); border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm); border: 1px solid var(--border-light);
}

.section {
  padding: 28px 32px;
  border-bottom: 1px solid var(--border-light);
}
.section:last-child { border-bottom: none; }

.section-header {
  display: flex; align-items: center; gap: 10px; margin-bottom: 8px;
}
.section-icon { font-size: 22px; }
.section-header h3 { font-size: 17px; font-weight: 600; color: var(--text-primary); margin: 0; }

.section-desc { font-size: 13px; color: var(--text-tertiary); margin: 4px 0 20px; }

.form-body { display: flex; flex-direction: column; gap: 16px; }

.field-group label {
  display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary);
  margin-bottom: 6px;
}
.field-group input {
  width: 100%; padding: 12px 16px; border: 1px solid var(--border-light);
  border-radius: var(--radius-md); font-size: 14px; color: var(--text-primary);
  outline: none; transition: border-color 0.2s; box-sizing: border-box;
  background: var(--bg-1);
}
.field-group input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(212,165,116,0.08); }
.field-group input::placeholder { color: var(--text-tertiary); opacity: 0.5; }

.strength-bar {
  display: flex; align-items: center; gap: 12px; margin-top: -8px;
}
.strength-track {
  flex: 1; height: 4px; background: var(--border-light); border-radius: 2px; overflow: hidden;
}
.strength-fill {
  height: 100%; border-radius: 2px; transition: width 0.3s;
}
.level-1 { background: #ef4444; }
.level-2 { background: #f97316; }
.level-3 { background: #eab308; }
.level-4 { background: #22c55e; }
.strength-text { font-size: 12px; color: var(--text-tertiary); min-width: 36px; text-align: right; }

.submit-btn {
  width: 100%; padding: 13px; border: none; border-radius: var(--radius-md);
  background: linear-gradient(135deg, #8B5E3C, #6B3A1F); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer; letter-spacing: 2px;
  transition: all 0.3s; margin-top: 4px;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(139,94,60,0.25); }
.submit-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.info-list { display: flex; flex-direction: column; gap: 0; }
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0; border-bottom: 1px dashed var(--border-light);
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 14px; color: var(--text-secondary); }
.info-value { font-size: 14px; font-weight: 500; color: var(--text-primary); }

.tips-section {
  padding: 24px 32px; background: linear-gradient(135deg, rgba(212,165,116,0.04), rgba(139,94,60,0.02));
}
.tips-section h4 { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 12px; }
.tips-section ul { margin: 0; padding-left: 18px; }
.tips-section li { font-size: 13px; color: var(--text-secondary); line-height: 2; }

.danger-section {
  border-left: 3px solid #ef4444;
}

.deactivate-info {
  margin-bottom: 16px;
}

.deactivate-warning {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  padding: 16px;
}

.deactivate-warning p {
  font-size: 14px;
  font-weight: 600;
  color: #991b1b;
  margin: 0 0 10px;
}

.deactivate-warning ul {
  margin: 0;
  padding-left: 20px;
}

.deactivate-warning li {
  font-size: 13px;
  color: #b91c1c;
  line-height: 1.8;
}

.deactivate-btn {
  width: 100%;
  padding: 13px;
  border: 2px solid #ef4444;
  border-radius: var(--radius-md);
  background: transparent;
  color: #ef4444;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 2px;
  transition: all 0.3s;
}

.deactivate-btn:hover:not(:disabled) {
  background: #ef4444;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.25);
}

.deactivate-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .security-container { padding: 16px 12px 32px; }
  .section { padding: 20px 18px; }
}
</style>