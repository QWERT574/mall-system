<template>
  <div class="seller-login-page">
    <div class="login-left">
      <div class="brand-content">
        <div class="brand-icon">
          <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="8" y="24" width="48" height="32" rx="4" stroke="currentColor" stroke-width="2.5"/>
            <path d="M8 24L32 8L56 24" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <rect x="24" y="36" width="16" height="20" rx="2" stroke="currentColor" stroke-width="2"/>
            <circle cx="32" cy="20" r="3" fill="currentColor" opacity="0.6"/>
          </svg>
        </div>
        <h1 class="brand-title">乡村振兴 · 商家中心</h1>
        <p class="brand-subtitle">高效管理 · 智慧经营 · 增长无忧</p>
        <div class="brand-features">
          <div class="feature-item" v-for="(feat, i) in features" :key="i" :style="{ animationDelay: `${0.6 + i * 0.15}s` }">
            <span class="feature-dot"></span>
            <span class="feature-text">{{ feat }}</span>
          </div>
        </div>
      </div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>
    </div>

    <div class="login-right">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-desc">登录您的商家账号</p>
        </div>

        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" @submit.prevent="handleLogin" class="login-form">
          <el-form-item prop="username">
            <div class="input-group">
              <label class="input-label">账号</label>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名或手机号"
                :prefix-icon="User"
                size="large"
                clearable
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-group">
              <label class="input-label">密码</label>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </div>
          </el-form-item>

          <el-form-item prop="captchaCode">
            <div class="input-group">
              <label class="input-label">验证码</label>
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.captchaCode"
                  placeholder="请输入图形验证码"
                  :maxlength="6"
                  size="large"
                  class="captcha-input"
                  @keyup.enter="handleLogin"
                />
                <div
                  class="captcha-image"
                  :class="{ refreshing: captchaLoading }"
                  :title="'点击刷新'"
                  @click="loadCaptcha"
                >
                  <img v-if="captchaImage" :src="captchaImage" alt="captcha" draggable="false" />
                  <span v-else class="captcha-loading">加载中</span>
                  <span v-if="captchaLoading" class="captcha-spinner"></span>
                </div>
              </div>
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
            <a class="forgot-link" href="javascript:void(0)">忘记密码？</a>
          </div>

          <el-form-item>
            <button
              type="button"
              class="submit-btn"
              :class="{ loading: loading }"
              :disabled="loading"
              @click="handleLogin"
            >
              <span class="btn-text" v-if="!loading">登 录</span>
              <span class="btn-loading" v-else>
                <span class="spinner"></span>
                登录中...
              </span>
            </button>
          </el-form-item>
        </el-form>

        <div class="form-divider">
          <span class="divider-line"></span>
          <span class="divider-text">或</span>
          <span class="divider-line"></span>
        </div>

        <button class="register-btn" @click="$router.push('/register')">
          创建新账号
        </button>

        <p class="form-footer">登录即表示同意 <a href="javascript:void(0)">服务条款</a> 和 <a href="javascript:void(0)">隐私政策</a></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSellerStore } from '@/stores/seller'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../../utils/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const sellerStore = useSellerStore()
const loginFormRef = ref(null)

const features = [
  '商品管理 · 轻松上架与编辑',
  '订单处理 · 实时跟踪与发货',
  '数据洞察 · 经营决策有据可依',
  '客户服务 · 即时沟通响应'
]

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
  rememberMe: false
})

const loading = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')
const captchaLoading = ref(false)

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
])

const passwordRuleChecks = computed(() => {
  const pwd = loginForm.password
  const u = loginForm.username
  const results = []
  const lengthOk = pwd && pwd.length >= 8 && pwd.length <= 32
  results.push({ key: 'LENGTH', label: '长度 8-32 位', pass: lengthOk })
  const mixOk = pwd && /[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd)
  results.push({ key: 'MIX', label: '同时包含字母和数字', pass: mixOk })
  const weakOk = pwd && !WEAK_PASSWORDS.has(pwd.toLowerCase())
  results.push({ key: 'NOT_WEAK', label: '不能是常见弱密码', pass: weakOk })
  let sameAsAccount = false
  if (pwd && u && pwd.toLowerCase() === u.toLowerCase()) sameAsAccount = true
  results.push({ key: 'NOT_SAME', label: '不能与用户名相同', pass: !sameAsAccount })
  return results
})

const passwordAllPassed = computed(() => passwordRuleChecks.value.every(r => r.pass))

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const savedUsername = localStorage.getItem('savedUsername')
if (savedUsername) {
  loginForm.username = savedUsername
  loginForm.rememberMe = true
}

// 加载/刷新图形验证码
const loadCaptcha = async () => {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await api.getCaptcha(captchaKey.value || undefined)
    if (res && res.key) {
      captchaKey.value = res.key
      captchaImage.value = res.image
      loginForm.captchaCode = ''
    }
  } catch (e) {
    // ignore
  } finally {
    captchaLoading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  if (!loginForm.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  // 注意:密码复杂度校验仅在注册时强制,登录时不卡(老账号可能是早期弱密码)
  // 由后端负责最终的身份验证

  loading.value = true
  try {
    const result = await api.login({
      username: loginForm.username,
      password: loginForm.password,
      captchaKey: captchaKey.value,
      captchaCode: loginForm.captchaCode
    })

    if (result.token) {
      const user = result.user
      const token = result.token

      if (user.userType !== 1) {
        ElMessage.error('该账号不是商家账号，请使用商家账号登录')
        return
      }

      if (user.isVerified === 0) {
        ElMessage.error('您的商家账号正在审核中，请耐心等待审核通过后再登录')
        return
      }

      if (user.status !== 1) {
        ElMessage.error('您的商家账号已被禁用，请联系客服')
        return
      }

      sellerStore.setAuth(token, user)

      const savedToken = localStorage.getItem('seller_token')
      if (!savedToken) {
        localStorage.setItem('seller_token', token)
        localStorage.setItem('seller_user', JSON.stringify(user))
      }

      if (loginForm.rememberMe) {
        localStorage.setItem('savedUsername', loginForm.username)
      } else {
        localStorage.removeItem('savedUsername')
      }

      setTimeout(() => router.push('/dashboard'), 100)
    } else {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
  } catch (error) {
    if (error.message && /验证码/.test(error.message)) {
      loadCaptcha()
    }
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@600;700&family=DM+Sans:wght@400;500;600&display=swap');

.seller-login-page {
  min-height: 100vh;
  display: flex;
  background: #f8f6f3;
  font-family: 'DM Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

.login-left {
  flex: 0 0 45%;
  background: linear-gradient(160deg, #0a1628 0%, #132043 40%, #1a2a5e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 60px;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 400px;
}

.brand-icon {
  width: 64px;
  height: 64px;
  color: #d4a574;
  margin-bottom: 32px;
  animation: fadeSlideUp 0.6s ease both;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 12px 0;
  letter-spacing: 2px;
  animation: fadeSlideUp 0.6s ease 0.15s both;
}

.brand-subtitle {
  font-size: 15px;
  color: rgba(212, 165, 116, 0.85);
  margin: 0 0 48px 0;
  letter-spacing: 4px;
  animation: fadeSlideUp 0.6s ease 0.3s both;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  animation: fadeSlideUp 0.5s ease both;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d4a574;
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(212, 165, 116, 0.4);
}

.feature-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.5px;
}

.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border: 1.5px solid rgba(var(--color-primary-500-rgb), 0.15);
  border-radius: 4px;
  animation: floatShape 8s ease-in-out infinite;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 12%;
  right: 15%;
  transform: rotate(45deg);
  animation-delay: 0s;
}

.shape-2 {
  width: 50px;
  height: 50px;
  bottom: 20%;
  right: 25%;
  transform: rotate(30deg);
  border-radius: 50%;
  animation-delay: -2s;
  animation-duration: 10s;
}

.shape-3 {
  width: 35px;
  height: 35px;
  top: 35%;
  right: 8%;
  transform: rotate(60deg);
  animation-delay: -4s;
  animation-duration: 7s;
}

.shape-4 {
  width: 60px;
  height: 60px;
  bottom: 10%;
  left: 15%;
  transform: rotate(15deg);
  border-radius: 50%;
  animation-delay: -1s;
  animation-duration: 9s;
}

.shape-5 {
  width: 25px;
  height: 25px;
  top: 60%;
  left: 8%;
  transform: rotate(45deg);
  animation-delay: -3s;
  animation-duration: 11s;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

.login-right::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(212, 165, 116, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(10, 22, 40, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
  animation: fadeSlideUp 0.5s ease both;
}

.form-header {
  margin-bottom: 40px;
}

.form-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: #0a1628;
  margin: 0 0 8px 0;
}

.form-desc {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.input-group {
  width: 100%;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0ddd8 inset;
  padding: 4px 12px;
  background: #fff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c9c3b8 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #0a1628 inset;
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #1a1a1a;
}

:deep(.el-input__inner::placeholder) {
  color: #b8b3aa;
}

:deep(.el-input__prefix .el-icon) {
  color: #8c8c8c;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

:deep(.el-checkbox__label) {
  font-size: 13px;
  color: #666;
}

.forgot-link {
  font-size: 13px;
  color: #0a1628;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #d4a574;
}

/* 验证码 */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
}
.captcha-input {
  flex: 1;
}
.captcha-image {
  flex: 0 0 120px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #e0ddd8;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}
.captcha-image:hover {
  border-color: #0a1628;
}
.captcha-image.refreshing {
  pointer-events: none;
  opacity: 0.8;
}
.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  display: block;
}
.captcha-loading {
  font-size: 12px;
  color: #b8b3aa;
}
.captcha-spinner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.7);
}
.captcha-spinner::after {
  content: '';
  width: 18px;
  height: 18px;
  border: 2px solid #e0ddd8;
  border-top-color: #0a1628;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--color-primary-600) 0%, var(--color-primary-500) 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--color-primary-600) 0%, var(--color-primary-500) 100%);
  opacity: 0;
  transition: opacity 0.4s;
}

.submit-btn:hover:not(:disabled)::before {
  opacity: 1;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(10, 22, 40, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-text,
.btn-loading {
  position: relative;
  z-index: 1;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  vertical-align: middle;
  margin-right: 8px;
}

.form-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 28px 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: #e0ddd8;
}

.divider-text {
  font-size: 12px;
  color: #b8b3aa;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.register-btn {
  width: 100%;
  height: 48px;
  border: 1.5px solid #0a1628;
  border-radius: 10px;
  background: transparent;
  color: #0a1628;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.register-btn:hover {
  background: #0a1628;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(10, 22, 40, 0.15);
}

.form-footer {
  text-align: center;
  font-size: 12px;
  color: #b8b3aa;
  margin-top: 32px;
  line-height: 1.8;
}

.form-footer a {
  color: #0a1628;
  text-decoration: none;
  font-weight: 500;
}

.form-footer a:hover {
  color: #d4a574;
}

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatShape {
  0%, 100% {
    transform: translateY(0) rotate(var(--base-rotate, 45deg));
    opacity: 0.6;
  }
  50% {
    transform: translateY(-20px) rotate(calc(var(--base-rotate, 45deg) + 10deg));
    opacity: 1;
  }
}

.shape-1 { --base-rotate: 45deg; }
.shape-2 { --base-rotate: 30deg; }
.shape-3 { --base-rotate: 60deg; }
.shape-4 { --base-rotate: 15deg; }
.shape-5 { --base-rotate: 45deg; }

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .seller-login-page {
    flex-direction: column;
  }

  .login-left {
    flex: none;
    padding: 40px 30px;
    min-height: auto;
  }

  .brand-features {
    display: none;
  }

  .brand-title {
    font-size: 28px;
  }

  .login-right {
    padding: 30px 20px;
  }
}
</style>
