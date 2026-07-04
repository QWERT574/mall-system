<template>
  <div class="login-page ml-page-enter">
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="floating-emoji">
        <span class="emoji-item emoji-1">🌾</span>
        <span class="emoji-item emoji-2">🍃</span>
        <span class="emoji-item emoji-3">🌿</span>
        <span class="emoji-item emoji-4">🍂</span>
      </div>
    </div>

    <div class="login-layout">
      <div class="brand-panel">
        <div class="brand-inner">
          <div class="brand-header fade-slide-up delay-100">
            <div class="logo-wrapper">
              <div class="logo-mark">
                <span class="logo-emoji">🌾</span>
              </div>
              <div class="logo-text">
                <h1 class="logo-title">乡村振兴</h1>
                <p class="logo-subtitle">FARM TO TABLE</p>
              </div>
            </div>
            <p class="brand-desc">连接田间与餐桌，让每一份农产品都承载着土地的真诚与农人的匠心。</p>
          </div>

          <div class="feature-list fade-slide-up delay-300">
            <div class="feature-card">
              <div class="feature-icon-wrap">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="22" height="22">
                  <path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                </svg>
              </div>
              <div class="feature-info">
                <h4>品质保证</h4>
                <p>严格筛选优质农产品，源头品控</p>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-icon-wrap">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="22" height="22">
                  <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
              </div>
              <div class="feature-info">
                <h4>新鲜直达</h4>
                <p>产地直供，48小时冷链配送</p>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-icon-wrap">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="22" height="22">
                  <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                </svg>
              </div>
              <div class="feature-info">
                <h4>助农惠农</h4>
                <p>支持农户增收，共创美好乡村</p>
              </div>
            </div>
          </div>

          <div class="brand-footer fade-slide-up delay-500">
            <div class="testimonial">
              <p class="testimonial-text">"加入乡村振兴，开启您的品质农产品之旅。已有 <strong>10,000+</strong> 用户选择我们。"</p>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="form-card form-mount-anim">
          <div class="form-card-inner">
            <div class="form-header">
              <h2 class="form-title">欢迎回来</h2>
              <p class="form-subtitle">登录您的乡村振兴账号</p>
            </div>

            <form @submit.prevent="handleLogin" class="login-form">
              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  用户名
                </label>
                <div class="input-group">
                  <span class="input-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H3z"/>
                    </svg>
                  </span>
                  <input v-model="username" type="text" placeholder="请输入用户名" required class="form-input" />
                </div>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  密码
                </label>
                <div class="input-group">
                  <span class="input-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"/>
                    </svg>
                  </span>
                  <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" required class="form-input" />
                  <button type="button" class="toggle-vis" @click="showPassword = !showPassword">
                    <svg v-if="showPassword" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
                      <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                      <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z"/>
                    </svg>
                    <svg v-else viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
                      <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z"/>
                      <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z"/>
                    </svg>
                  </button>
                </div>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  验证码
                </label>
                <div class="captcha-group">
                  <div class="input-group captcha-input-group">
                    <span class="input-icon">
                      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                        <path fill-rule="evenodd" d="M10 1a4 4 0 00-4 4v3H5a2 2 0 00-2 2v8a2 2 0 002 2h10a2 2 0 002-2v-8a2 2 0 00-2-2h-1V5a4 4 0 00-4-4zm2 7V5a2 2 0 10-4 0v3h4z"/>
                      </svg>
                    </span>
                    <input
                      v-model="captchaCode"
                      type="text"
                      placeholder="请输入验证码"
                      maxlength="6"
                      class="form-input captcha-input"
                      @keyup.enter="handleLogin"
                    />
                  </div>
                  <div class="captcha-image-wrap" :class="{ refreshing: captchaLoading }" @click="loadCaptcha" :title="'点击刷新'">
                    <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" draggable="false" />
                    <div v-else class="captcha-loading">加载中</div>
                    <span v-if="captchaLoading" class="captcha-refresh-icon">
                      <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                        <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.1a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.277z"/>
                      </svg>
                    </span>
                  </div>
                </div>
              </div>

              <div class="remember-row">
                <label class="toggle-wrap">
                  <input type="checkbox" v-model="rememberMe" class="toggle-input" />
                  <span class="toggle-track">
                    <span class="toggle-thumb"></span>
                  </span>
                  <span class="toggle-label">记住我</span>
                </label>
              </div>

              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="!loading" class="btn-label">
                  登 录
                  <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="btn-arrow">
                    <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z"/>
                  </svg>
                </span>
                <span v-else class="btn-loading">
                  <span class="spinner"></span>
                  登录中...
                </span>
              </button>
            </form>

            <div class="form-footer">
              <span class="footer-text">还没有账号？</span>
              <router-link to="/register" class="footer-link">
                立即注册
                <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"/>
                </svg>
              </router-link>
            </div>

            <div class="merchant-link-row">
              <span class="merchant-link-text">商家后台登录？</span>
              <a :href="sellerLoginUrl" class="merchant-link">
                🏪 商家登录
                <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"/>
                </svg>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const username = ref('');
const password = ref('');
const loading = ref(false);
const showPassword = ref(false);
const rememberMe = ref(false);
// 验证码
const captchaKey = ref('');
const captchaCode = ref('');
const captchaImage = ref('');
const captchaLoading = ref(false);

const sellerLoginUrl = 'http://localhost:5173/login';

// 加载/刷新验证码
const loadCaptcha = async () => {
  if (captchaLoading.value) return;
  captchaLoading.value = true;
  try {
    const res = await api.getCaptcha(captchaKey.value || undefined);
    if (res.code === 0 && res.data) {
      captchaKey.value = res.data.key;
      captchaImage.value = res.data.image;
      captchaCode.value = '';
    } else {
      ElMessage.warning(res.message || '验证码加载失败');
    }
  } catch (e) {
    ElMessage.warning('验证码加载失败，请稍后再试');
  } finally {
    captchaLoading.value = false;
  }
};

const handleLogin = async () => {
  // 前端预校验
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  if (!captchaCode.value) {
    ElMessage.warning('请输入验证码');
    return;
  }
  loading.value = true;
  try {
    const response = await api.login({
      username: username.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value
    });
    if (response.code === 0) {
      const data = response.data;
      if (data.token && data.user) {
        userStore.setToken(data.token, data.user);
      }
      ElMessage.success('登录成功');
      setTimeout(() => { router.push(route.query.redirect || '/'); }, 800);
    } else {
      // 验证码错误或已过期时，自动刷新
      if (response.message && /验证码/.test(response.message)) {
        loadCaptcha();
      }
      ElMessage.error(response.message || '登录失败');
    }
  } catch (error) {
    // 后端 4xx 时通常也会带 message
    if (error?.response?.data?.message && /验证码/.test(error.response.data.message)) {
      loadCaptcha();
    }
    ElMessage.error(error?.response?.data?.message || error.message || '登录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadCaptcha();
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  font-family: var(--font-body);
}

.bg-layer { position: absolute; inset: 0; pointer-events: none; }

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(250, 248, 245, 0.98) 0%, rgba(245, 240, 232, 0.98) 50%, rgba(235, 228, 216, 0.98) 100%);
}

.floating-emoji { position: absolute; inset: 0; }

.emoji-item {
  position: absolute;
  font-size: 48px;
  opacity: 0.15;
  animation: floatEmoji 6s ease-in-out infinite;
}

.emoji-1 { top: 10%; left: 10%; animation-delay: 0s; }
.emoji-2 { top: 20%; right: 15%; animation-delay: 1.5s; }
.emoji-3 { bottom: 30%; left: 5%; animation-delay: 3s; }
.emoji-4 { bottom: 15%; right: 10%; animation-delay: 4.5s; }

@keyframes floatEmoji {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.login-layout {
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 32px;
  gap: 60px;
  position: relative;
  z-index: 1;
}

.brand-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.brand-inner { max-width: 420px; }
.brand-header { margin-bottom: 56px; }

.logo-wrapper { display: flex; align-items: center; gap: 18px; margin-bottom: 24px; }
.logo-mark { width: 52px; height: 52px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.logo-emoji { font-size: 40px; }

.logo-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary-800), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 4px;
  margin: 0;
  line-height: 1.2;
}

.logo-subtitle { font-family: var(--font-body); font-size: 11px; font-weight: 500; color: rgba(var(--color-primary-900-rgb), 0.6); letter-spacing: 3px; margin: 4px 0 0 0; }
.brand-desc { font-size: 15px; color: rgba(var(--color-primary-900-rgb), 0.6); line-height: 1.8; margin: 0; }

.feature-list { display: flex; flex-direction: column; gap: 20px; margin-bottom: 48px; }

.feature-card {
  display: flex; align-items: center; gap: 18px; padding: 18px 20px; border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.8); border: 1px solid rgba(var(--color-primary-500-rgb), 0.1);
  box-shadow: var(--shadow-sm); transition: all 0.4s;
}

.feature-card:hover { background: rgba(255, 255, 255, 0.95); border-color: var(--accent); box-shadow: 0 4px 16px rgba(var(--color-primary-500-rgb), 0.2); transform: translateX(6px); }

.feature-icon-wrap {
  width: 44px; height: 44px; border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(var(--color-primary-500-rgb), 0.15), rgba(var(--color-primary-900-rgb), 0.1));
  color: var(--color-primary-800); display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.feature-info h4 { font-size: 15px; font-weight: 600; color: var(--earth-brown-dark); margin: 0 0 2px 0; }
.feature-info p { font-size: 13px; color: rgba(var(--color-primary-900-rgb), 0.55); margin: 0; }

.testimonial { padding: 18px 20px; border-radius: var(--radius-lg); background: linear-gradient(135deg, rgba(var(--color-primary-500-rgb), 0.08), rgba(var(--color-primary-900-rgb), 0.04)); border-left: 3px solid var(--accent); }
.testimonial-text { font-size: 14px; color: rgba(var(--color-primary-900-rgb), 0.65); line-height: 1.7; margin: 0; font-style: italic; }
.testimonial-text strong { color: var(--color-primary-800); font-weight: 600; }

.form-panel { flex: 0 0 460px; display: flex; align-items: center; justify-content: center; }

.form-card {
  width: 100%; border-radius: 20px; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--color-primary-500-rgb), 0.1);
  box-shadow: 0 8px 48px rgba(0, 0, 0, 0.08), 0 0 60px rgba(var(--color-primary-500-rgb), 0.06);
  position: relative; overflow: hidden;
}

.form-card::before {
  content: ''; position: absolute; top: -1px; left: 20%; right: 20%; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(var(--color-primary-500-rgb), 0.6), transparent); border-radius: 2px;
}

.form-mount-anim {
  animation: formMount 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes formMount {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.form-card-inner { padding: 44px 40px 36px; }
.form-header { text-align: center; margin-bottom: 28px; }
.form-title { font-size: 26px; font-weight: 700; color: var(--earth-brown-dark); margin: 0 0 8px 0; letter-spacing: 2px; }
.form-subtitle { font-size: 14px; color: rgba(var(--color-primary-900-rgb), 0.5); margin: 0; font-family: var(--font-body); }

.field-label { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: rgba(var(--color-primary-900-rgb), 0.7); margin-bottom: 8px; letter-spacing: 1px; }

.login-form { display: flex; flex-direction: column; gap: 22px; }
.label-decorator { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 8px rgba(var(--color-primary-500-rgb), 0.4); }

.input-group { position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: rgba(var(--color-primary-900-rgb), 0.3); display: flex; z-index: 1; pointer-events: none; }

.form-input {
  width: 100%; padding: 14px 16px 14px 42px; background: rgba(250, 248, 245, 0.5);
  border: 1px solid rgba(var(--color-primary-900-rgb), 0.12); border-radius: var(--radius-lg); font-size: 14px;
  color: var(--earth-brown-dark); outline: none; transition: all 0.3s; font-family: var(--font-body);
}

.form-input::placeholder { color: rgba(var(--color-primary-900-rgb), 0.2); }
.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-500-rgb), 0.15), 0 0 20px rgba(var(--color-primary-500-rgb), 0.08);
  background: rgba(255, 255, 255, 0.8);
}

.toggle-vis { position: absolute; right: 12px; background: none; border: none; color: rgba(var(--color-primary-900-rgb), 0.35); cursor: pointer; padding: 4px; display: flex; align-items: center; transition: color 0.2s; }
.toggle-vis:hover { color: rgba(var(--color-primary-900-rgb), 0.7); }

.remember-row { display: flex; justify-content: space-between; align-items: center; }
.toggle-wrap { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.toggle-input { display: none; }

/* 验证码 */
.captcha-group {
  display: flex;
  align-items: stretch;
  gap: 10px;
}
.captcha-input-group { flex: 1; }
.captcha-input { letter-spacing: 2px; font-weight: 600; }
.captcha-image-wrap {
  position: relative;
  flex: 0 0 130px;
  height: 48px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(var(--color-primary-900-rgb), 0.12);
  background: rgba(250, 248, 245, 0.5);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-image-wrap:hover {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-500-rgb), 0.15);
}
.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
}
.captcha-loading {
  font-size: 12px;
  color: rgba(var(--color-primary-900-rgb), 0.4);
  letter-spacing: 1px;
}
.captcha-refresh-icon {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(250, 248, 245, 0.7);
  color: var(--color-primary-800);
  animation: spin 0.8s linear infinite;
}
.captcha-image-wrap.refreshing { pointer-events: none; }
.toggle-track {
  width: 40px; height: 22px; border-radius: 11px; background: var(--color-gray-300);
  position: relative; transition: background var(--transition-fast);
}
.toggle-input:checked + .toggle-track { background: var(--accent); }
.toggle-thumb {
  position: absolute; top: 2px; left: 2px; width: 18px; height: 18px;
  border-radius: 50%; background: var(--color-text-inverse);
  transition: transform var(--transition-fast);
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.toggle-input:checked + .toggle-track .toggle-thumb { transform: translateX(18px); }
.toggle-label { font-size: 13px; color: var(--text-secondary); }

.submit-btn {
  width: 100%; padding: 16px; border: none; border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary-800) 0%, var(--color-primary-900) 100%); color: var(--color-text-inverse);
  font-size: 16px; font-weight: 700; cursor: pointer; transition: all 0.4s;
  position: relative; overflow: hidden; letter-spacing: 6px; font-family: var(--font-display);
  box-shadow: 0 4px 24px rgba(var(--color-primary-900-rgb), 0.25);
}

.submit-btn::before { content: ''; position: absolute; inset: 0; background: linear-gradient(135deg, var(--accent) 0%, var(--color-primary-800) 100%); opacity: 0; transition: opacity 0.4s; }
.submit-btn:hover:not(:disabled)::before { opacity: 1; }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 36px rgba(var(--color-primary-900-rgb), 0.4); }
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-label, .btn-loading { position: relative; z-index: 1; display: inline-flex; align-items: center; gap: 10px; }
.btn-arrow { transition: transform 0.3s; }
.submit-btn:hover .btn-arrow { transform: translateX(4px); }

.spinner { width: 18px; height: 18px; border: 2px solid rgba(255, 255, 255, 0.3); border-top-color: var(--color-text-inverse); border-radius: 50%; animation: spin 0.7s linear infinite; }

@keyframes spin { to { transform: rotate(360deg); } }

.form-footer { text-align: center; margin-top: 28px; padding-top: 24px; border-top: 1px solid rgba(var(--color-primary-900-rgb), 0.08); }
.footer-text { font-size: 14px; color: rgba(var(--color-primary-900-rgb), 0.4); }
.footer-link { display: inline-flex; align-items: center; gap: 4px; color: var(--color-primary-800); font-weight: 600; font-size: 14px; text-decoration: none; margin-left: 6px; transition: color 0.2s; }
.footer-link:hover { color: var(--accent); }
.footer-link svg { transition: transform 0.3s; }
.footer-link:hover svg { transform: translateX(4px); }

.merchant-link-row { text-align: center; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-900-rgb), 0.06); }
.merchant-link-text { font-size: 13px; color: rgba(var(--color-primary-900-rgb), 0.4); margin-right: 6px; }

.merchant-link {
  display: inline-flex; align-items: center; gap: 4px; color: var(--accent); font-weight: 600;
  font-size: 13px; text-decoration: none; transition: all 0.3s; padding: 6px 14px;
  border-radius: var(--radius-md); border: 1px solid rgba(var(--color-primary-500-rgb), 0.2); background: rgba(var(--color-primary-500-rgb), 0.05);
}

.merchant-link:hover { background: rgba(var(--color-primary-500-rgb), 0.12); border-color: rgba(var(--color-primary-500-rgb), 0.4); color: var(--color-primary-800); }
.merchant-link svg { transition: transform 0.3s; }
.merchant-link:hover svg { transform: translateX(4px); }

@keyframes fadeInUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

.fade-slide-up { animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both; }
.delay-100 { animation-delay: 0.1s; }
.delay-200 { animation-delay: 0.2s; }
.delay-300 { animation-delay: 0.3s; }
.delay-500 { animation-delay: 0.5s; }

@media (max-width: 860px) { .brand-panel { display: none; } .login-layout { justify-content: center; } .form-panel { flex: none; width: 100%; max-width: 440px; } }
@media (max-width: 480px) { .login-layout { padding: 16px 12px; } .form-card-inner { padding: 32px 20px 28px; } .form-title { font-size: 22px; } .floating-emoji { display: none; } .submit-btn { letter-spacing: 4px; } }
</style>

