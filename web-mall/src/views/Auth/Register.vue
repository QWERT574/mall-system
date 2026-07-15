<template>
  <div class="register-page ml-page-enter">
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="floating-emoji">
        <span class="emoji-item emoji-1">🌾</span>
        <span class="emoji-item emoji-2">🍃</span>
        <span class="emoji-item emoji-3">🌿</span>
        <span class="emoji-item emoji-4">🍂</span>
      </div>
    </div>

    <div class="register-layout">
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
              <h2 class="form-title">创建账号</h2>
              <p class="form-subtitle">填写以下信息，即刻开启品质生活</p>
            </div>

            <form @submit.prevent="handleRegister" class="register-form">
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
                  <input
                    v-model="username"
                    type="text"
                    placeholder="请输入用户名（3-20位）"
                    minlength="3"
                    maxlength="20"
                    required
                    class="form-input"
                  />
                </div>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  手机号码
                </label>
                <div class="input-group">
                  <span class="input-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z"/>
                    </svg>
                  </span>
                  <input
                    v-model="phone"
                    type="tel"
                    placeholder="请输入11位手机号"
                    maxlength="11"
                    required
                    class="form-input"
                    @input="validatePhone"
                  />
                </div>
                <span v-if="phoneError" class="field-error">{{ phoneError }}</span>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  验证码
                </label>
                <div class="verify-row">
                  <div class="input-group">
                    <span class="input-icon">
                      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                        <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"/>
                      </svg>
                    </span>
                    <input v-model="verifyCode" type="text" placeholder="请输入验证码" maxlength="6" required class="form-input" />
                  </div>
                  <button type="button" class="send-code-btn" @click="sendCode" :disabled="codeCountdown > 0">
                    {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                  </button>
                </div>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  设置密码
                </label>
                <div class="input-group">
                  <span class="input-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"/>
                    </svg>
                  </span>
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码（6-20位）"
                    minlength="6"
                    maxlength="20"
                    required
                    class="form-input"
                    @input="checkPasswordStrength"
                  />
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
                <div v-if="password" class="strength-meter">
                  <div class="strength-bars">
                    <span :class="['strength-segment', pwStrengthClass]"></span>
                    <span :class="['strength-segment', { 'strength-medium': pwStrength === 'medium' || pwStrength === 'strong', 'strength-weak': pwStrength === 'weak', 'strength-strong': pwStrength === 'strong' }]"></span>
                    <span :class="['strength-segment', { 'strength-strong': pwStrength === 'strong', 'strength-weak': pwStrength === 'weak' || pwStrength === 'medium' }]"></span>
                  </div>
                  <span class="strength-label" :class="pwStrengthClass">{{ pwStrengthText }}</span>
                </div>

                <!-- 密码规则清单:实时显示哪些满足,哪些不满足 -->
                <ul v-if="password" class="pw-rules" aria-label="密码规则">
                  <li
                    v-for="rule in passwordRuleChecks"
                    :key="rule.key"
                    :class="['pw-rule', rule.pass ? 'rule-pass' : 'rule-fail']"
                  >
                    <span class="rule-icon">{{ rule.pass ? '✓' : '✗' }}</span>
                    <span>{{ rule.label }}</span>
                  </li>
                </ul>
              </div>

              <div class="field-block">
                <label class="field-label">
                  <span class="label-decorator"></span>
                  确认密码
                </label>
                <div class="input-group">
                  <span class="input-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
                    </svg>
                  </span>
                  <input
                    v-model="confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    required
                    class="form-input"
                    :class="{ 'input-error': confirmPassword && confirmMismatch }"
                  />
                </div>
                <span v-if="confirmMismatch" class="field-error">两次输入的密码不一致</span>
              </div>

              <div class="agreement-row">
                <label class="checkbox-wrap">
                  <span class="checkbox-icon">
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
                    </svg>
                  </span>
                  <span class="checkbox-text">注册即表示同意 <a href="#" class="inline-link">服务条款</a> 和 <a href="#" class="inline-link">隐私政策</a></span>
                </label>
              </div>

              <button type="submit" class="submit-btn" :disabled="loading || (password && !passwordAllPassed)">
                <span v-if="!loading" class="btn-label">
                  立即注册
                  <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="btn-arrow">
                    <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z"/>
                  </svg>
                </span>
                <span v-else class="btn-loading">
                  <span class="spinner"></span>
                  注册中...
                </span>
              </button>
            </form>

            <div class="form-footer">
              <span class="footer-text">已有账号？</span>
              <router-link to="/login" class="footer-link">
                立即登录
                <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"/>
                </svg>
              </router-link>
            </div>

            <div class="merchant-link-row">
              <span class="merchant-link-text">想要开店入驻？</span>
              <a :href="sellerRegisterUrl" class="merchant-link">
                🏪 商家入驻
                <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"/>
                </svg>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <transition name="toast">
      <div v-if="toast.show" :class="['toast-message', 'toast-' + toast.type]">
        <span class="toast-icon">
          <svg v-if="toast.type === 'success'" viewBox="0 0 20 20" fill="currentColor" width="20" height="20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
          </svg>
          <svg v-else viewBox="0 0 20 20" fill="currentColor" width="20" height="20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"/>
          </svg>
        </span>
        <span>{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';

const router = useRouter();

const username = ref('');
const phone = ref('');
const password = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const showPassword = ref(false);
const phoneError = ref('');
const verifyCode = ref('');
const codeCountdown = ref(0);
const toast = ref({ show: false, message: '', type: 'success' });

const sellerRegisterUrl = 'http://localhost:5173/register';

let toastTimer = null;
let codeTimer = null;

const confirmMismatch = computed(() => {
  return confirmPassword.value && password.value !== confirmPassword.value;
});

const pwStrength = computed(() => {
  const pwd = password.value;
  if (!pwd) return 'none';
  if (pwd.length < 6) return 'weak';
  const hasUpper = /[A-Z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasSpecial = /[^A-Za-z0-9]/.test(pwd);
  const score = [hasUpper, hasNumber, hasSpecial].filter(Boolean).length;
  if (pwd.length >= 12 && score >= 2) return 'strong';
  if (pwd.length >= 8 && score >= 1) return 'medium';
  return 'weak';
});

const pwStrengthClass = computed(() => 'strength-' + pwStrength.value);
const pwStrengthText = computed(() => {
  const map = { weak: '弱', medium: '中等', strong: '强' };
  return map[pwStrength.value] || '';
});

// ===== 密码复杂度校验(与后端 PasswordValidator 完全一致) =====
const WEAK_PASSWORDS = new Set([
  '12345678','123456789','1234567890','00000000','11111111',
  '22222222','33333333','44444444','55555555','66666666',
  '77777777','88888888','99999999','01234567','98765432',
  'qwertyui','asdfghjk','zxcvbnm1','qazwsxedc',
  'password','password1','password123','p@ssw0rd','passw0rd',
  'iloveyou','welcome','welcome1','admin123','abc12345',
  'abcd1234','qwerty123','asdf1234','zxcv1234','1q2w3e4r',
  'qweasdzxc','qwertyuiop','asdfghjkl','12341234','11223344',
  'abcdefgh','abcdefg1','abcd1234','aa123456','a1234567',
  'letmein1','trustno1','dragon12','monkey12','master12',
  'shadow12','michael1','jennifer','jordan23','superman1',
  'batman12','14725836','147258369','159753123'
]);

// 密码规则定义(单一来源,后端 PasswordValidator 同步)
const PASSWORD_RULES = [
  { key: 'LENGTH', label: '长度 8-32 位' },
  { key: 'MIX_LETTER_NUMBER', label: '同时包含字母和数字' },
  { key: 'NOT_WEAK', label: '不能是常见弱密码' },
  { key: 'NOT_SAME_AS_ACCOUNT', label: '不能与用户名/手机号相同' }
];

const passwordRuleChecks = computed(() => {
  const pwd = password.value;
  const u = username.value;
  const ph = phone.value;
  const results = [];

  // 1. 长度
  const lengthOk = pwd && pwd.length >= 8 && pwd.length <= 32;
  results.push({ key: 'LENGTH', label: '长度 8-32 位', pass: lengthOk });

  // 2. 字母 + 数字
  const mixOk = pwd && /[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd);
  results.push({ key: 'MIX_LETTER_NUMBER', label: '同时包含字母和数字', pass: mixOk });

  // 3. 弱密码
  const weakOk = pwd && !WEAK_PASSWORDS.has(pwd.toLowerCase());
  results.push({ key: 'NOT_WEAK', label: '不能是常见弱密码', pass: weakOk });

  // 4. 与账号相同
  let sameAsAccount = false;
  if (pwd) {
    if (u && pwd.toLowerCase() === u.toLowerCase()) sameAsAccount = true;
    if (ph && pwd === ph) sameAsAccount = true;
    if (ph && ph.length >= 6 && pwd === ph.slice(-6)) sameAsAccount = true;
  }
  results.push({ key: 'NOT_SAME_AS_ACCOUNT', label: '不能与用户名/手机号相同', pass: !sameAsAccount });

  return results;
});

const passwordAllPassed = computed(() => passwordRuleChecks.value.every(r => r.pass));

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => {
    toast.value.show = false;
  }, 3000);
};

const validatePhone = () => {
  if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
    phoneError.value = '请输入正确的手机号码';
  } else {
    phoneError.value = '';
  }
};

const checkPasswordStrength = () => {};

const sendCode = async () => {
  if (codeCountdown.value > 0) return;
  if (!phone.value || phoneError.value) {
    showToast('请输入正确的手机号码', 'error');
    return;
  }
  try {
    const res = await api.sendSmsCode(phone.value);
    if (res.code === 0) {
      codeCountdown.value = 60;
      codeTimer = setInterval(() => {
        codeCountdown.value--;
        if (codeCountdown.value <= 0) {
          clearInterval(codeTimer);
        }
      }, 1000);
      // dev 模式:把验证码直接显示出来,方便测试
      if (res.data && res.data.devCode) {
        showToast(`验证码已发送 (dev: ${res.data.devCode})`, 'success');
      } else {
        showToast('验证码已发送', 'success');
      }
    } else {
      showToast(res.message || '发送失败', 'error');
    }
  } catch (e) {
    showToast('发送失败，请稍后重试', 'error');
  }
};

const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    showToast('两次输入的密码不一致', 'error');
    return;
  }
  if (!verifyCode.value) {
    showToast('请输入短信验证码', 'error');
    return;
  }
  if (!passwordAllPassed.value) {
    // 找到第一个不通过的规则,给出具体提示
    const failed = passwordRuleChecks.value.find(r => !r.pass);
    showToast('密码不满足要求：' + failed.label, 'error');
    return;
  }

  loading.value = true;
  try {
    const response = await api.register({
      username: username.value,
      phone: phone.value,
      password: password.value,
      code: verifyCode.value,
      userType: 0
    });

    if (response.code === 0) {
      showToast('注册成功！即将跳转到登录页...', 'success');
      setTimeout(() => {
        router.push('/login');
      }, 1500);
    } else {
      showToast(response.message || '注册失败', 'error');
    }
  } catch (error) {
    showToast(error.message || '注册失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  font-family: var(--font-body);
}

.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    rgba(250, 248, 245, 0.98) 0%,
    rgba(245, 240, 232, 0.98) 50%,
    rgba(235, 228, 216, 0.98) 100%);
}

.floating-emoji {
  position: absolute;
  inset: 0;
}

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

.register-layout {
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 32px;
  gap: 60px;
  position: relative;
  z-index: 1;
}

.brand-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.brand-inner {
  max-width: 420px;
}

.brand-header {
  margin-bottom: 56px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
}

.logo-mark {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-emoji {
  font-size: 40px;
}

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

.logo-subtitle {
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 500;
  color: rgba(var(--color-primary-900-rgb), 0.6);
  letter-spacing: 3px;
  margin: 4px 0 0 0;
}

.brand-desc {
  font-size: 15px;
  color: rgba(var(--color-primary-900-rgb), 0.6);
  line-height: 1.8;
  margin: 0;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 48px;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(var(--color-primary-500-rgb), 0.1);
  box-shadow: var(--shadow-sm);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--accent);
  box-shadow: 0 4px 16px rgba(var(--color-primary-500-rgb), 0.2);
  transform: translateX(6px);
}

.feature-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(var(--color-primary-500-rgb), 0.15), rgba(var(--color-primary-900-rgb), 0.1));
  color: var(--color-primary-800);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--earth-brown-dark);
  margin: 0 0 2px 0;
}

.feature-info p {
  font-size: 13px;
  color: rgba(var(--color-primary-900-rgb), 0.55);
  margin: 0;
}

.testimonial {
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(var(--color-primary-500-rgb), 0.08), rgba(var(--color-primary-900-rgb), 0.04));
  border-left: 3px solid var(--accent);
}

.testimonial-text {
  font-size: 14px;
  color: rgba(var(--color-primary-900-rgb), 0.65);
  line-height: 1.7;
  margin: 0;
  font-style: italic;
}

.testimonial-text strong {
  color: var(--color-primary-800);
  font-weight: 600;
}

.form-panel {
  flex: 0 0 460px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--color-primary-500-rgb), 0.1);
  box-shadow:
    0 8px 48px rgba(0, 0, 0, 0.08),
    0 0 60px rgba(var(--color-primary-500-rgb), 0.06);
  position: relative;
  overflow: hidden;
}

.form-card::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(var(--color-primary-500-rgb), 0.6), transparent);
  border-radius: 2px;
}

.form-mount-anim {
  animation: formMount 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes formMount {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.form-card-inner {
  padding: 44px 40px 36px;
}

.form-header {
  text-align: center;
  margin-bottom: 36px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--earth-brown-dark);
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.form-subtitle {
  font-size: 14px;
  color: rgba(var(--color-primary-900-rgb), 0.5);
  margin: 0;
  font-family: var(--font-body);
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(var(--color-primary-900-rgb), 0.7);
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.label-decorator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 8px rgba(var(--color-primary-500-rgb), 0.4);
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: rgba(var(--color-primary-900-rgb), 0.3);
  display: flex;
  z-index: 1;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 14px 16px 14px 42px;
  background: rgba(250, 248, 245, 0.5);
  border: 1px solid rgba(var(--color-primary-900-rgb), 0.12);
  border-radius: var(--radius-lg);
  font-size: 14px;
  color: var(--earth-brown-dark);
  outline: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: var(--font-body);
}

.form-input::placeholder {
  color: rgba(var(--color-primary-900-rgb), 0.2);
}

.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-500-rgb), 0.15), 0 0 20px rgba(var(--color-primary-500-rgb), 0.08);
  background: rgba(255, 255, 255, 0.8);
}

.form-input.input-error {
  border-color: rgba(var(--color-error-rgb), 0.4);
}

.toggle-vis {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: rgba(var(--color-primary-900-rgb), 0.35);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.toggle-vis:hover {
  color: rgba(var(--color-primary-900-rgb), 0.7);
}

.verify-row {
  display: flex;
  gap: 10px;
}

.verify-row .input-group {
  flex: 1;
}

.send-code-btn {
  flex-shrink: 0;
  padding: 14px 16px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
}

.send-code-btn:hover:not(:disabled) {
  background: var(--accent);
  color: var(--color-text-inverse);
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: var(--color-gray-300);
  color: var(--text-tertiary);
  background: transparent;
}

.field-error {
  display: block;
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--color-error);
  margin-top: 6px;
  animation: fadeInUp 0.3s ease-out;
}

.strength-meter {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.strength-bars {
  display: flex;
  gap: 5px;
  flex: 1;
}

.strength-segment {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: rgba(var(--color-primary-900-rgb), 0.08);
  transition: all 0.3s;
}

.strength-segment.strength-weak {
  background: var(--color-error);
  box-shadow: 0 0 8px rgba(var(--color-error-rgb), 0.4);
}

.strength-segment.strength-medium {
  background: var(--color-warning);
  box-shadow: 0 0 8px rgba(var(--color-warning), 0.4);
}

.strength-segment.strength-strong {
  background: var(--color-success);
  box-shadow: 0 0 8px rgba(var(--color-success), 0.4);
}

.strength-label {
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.strength-label.strength-weak { color: var(--color-error); }
.strength-label.strength-medium { color: var(--color-warning); }
.strength-label.strength-strong { color: var(--color-success); }

/* 密码规则清单 */
.pw-rules {
  list-style: none;
  padding: 10px 4px 0;
  margin: 10px 0 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 14px;
  font-size: 12px;
  font-family: var(--font-body);
}
.pw-rule {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.4;
  transition: color 0.2s;
}
.rule-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
  transition: all 0.25s;
}
.rule-fail {
  color: rgba(var(--color-error-rgb), 0.7);
}
.rule-fail .rule-icon {
  background: rgba(var(--color-error-rgb), 0.1);
  color: var(--color-error);
}
.rule-pass {
  color: var(--color-success);
  font-weight: 500;
}
.rule-pass .rule-icon {
  background: rgba(var(--color-success), 0.18);
  color: var(--color-success);
}

@media (max-width: 480px) {
  .pw-rules { grid-template-columns: 1fr; }
}

.agreement-row {
  padding: 8px 0;
}

.checkbox-wrap {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}

.checkbox-icon {
  color: rgba(var(--color-primary-900-rgb), 0.45);
  flex-shrink: 0;
  margin-top: 1px;
}

.checkbox-text {
  font-size: 13px;
  color: rgba(var(--color-primary-900-rgb), 0.5);
  line-height: 1.5;
}

.inline-link {
  color: var(--color-primary-800);
  text-decoration: none;
  transition: color 0.2s;
}

.inline-link:hover {
  color: var(--accent);
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary-800) 0%, var(--color-primary-900) 100%);
  color: var(--color-text-inverse);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  letter-spacing: 2px;
  box-shadow: 0 4px 24px rgba(var(--color-primary-900-rgb), 0.25);
  font-family: var(--font-display);
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--accent) 0%, var(--color-primary-800) 100%);
  opacity: 0;
  transition: opacity 0.4s;
}

.submit-btn:hover:not(:disabled)::before {
  opacity: 1;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(var(--color-primary-900-rgb), 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-label {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn-arrow {
  transition: transform 0.3s;
}

.submit-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.btn-loading {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--color-text-inverse);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-footer {
  text-align: center;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid rgba(var(--color-primary-900-rgb), 0.08);
}

.footer-text {
  font-size: 14px;
  color: rgba(var(--color-primary-900-rgb), 0.4);
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary-800);
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  margin-left: 6px;
  transition: color 0.2s;
}

.footer-link:hover {
  color: var(--accent);
}

.footer-link svg {
  transition: transform 0.3s;
}

.footer-link:hover svg {
  transform: translateX(4px);
}

.merchant-link-row {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--color-primary-900-rgb), 0.06);
}

.merchant-link-text {
  font-size: 13px;
  color: rgba(var(--color-primary-900-rgb), 0.4);
  margin-right: 6px;
}

.merchant-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--accent);
  font-weight: 600;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.3s;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(var(--color-primary-500-rgb), 0.2);
  background: rgba(var(--color-primary-500-rgb), 0.05);
}

.merchant-link:hover {
  background: rgba(var(--color-primary-500-rgb), 0.12);
  border-color: rgba(var(--color-primary-500-rgb), 0.4);
  color: var(--color-primary-800);
}

.merchant-link svg {
  transition: transform 0.3s;
}

.merchant-link:hover svg {
  transform: translateX(4px);
}

.toast-message {
  position: fixed;
  top: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: var(--z-toast);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 500;
  font-family: var(--font-body);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(16px);
}

.toast-success {
  background: rgba(var(--color-success), 0.15);
  border: 1px solid rgba(var(--color-success), 0.3);
  color: var(--nature-green-light);
}

.toast-error {
  background: rgba(var(--color-error-rgb), 0.15);
  border: 1px solid rgba(var(--color-error-rgb), 0.3);
  color: var(--color-error);
}

.toast-icon {
  display: flex;
  flex-shrink: 0;
}

.toast-enter-active {
  animation: toastIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-leave-active {
  animation: toastOut 0.3s ease-in;
}

@keyframes toastIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}

@keyframes toastOut {
  to {
    opacity: 0;
    transform: translateX(-50%) translateY(-12px) scale(0.95);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-slide-up {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.delay-100 { animation-delay: 0.1s; }
.delay-200 { animation-delay: 0.2s; }
.delay-300 { animation-delay: 0.3s; }
.delay-500 { animation-delay: 0.5s; }

@media (max-width: 860px) {
  .brand-panel {
    display: none;
  }

  .register-layout {
    justify-content: center;
  }

  .form-panel {
    flex: none;
    width: 100%;
    max-width: 440px;
  }
}

@media (max-width: 480px) {
  .register-layout {
    padding: 16px 12px;
  }

  .form-card-inner {
    padding: 32px 20px 28px;
  }

  .form-title {
    font-size: 22px;
  }

  .floating-emoji {
    display: none;
  }
}
</style>

