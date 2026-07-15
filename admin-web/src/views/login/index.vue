<template>
  <div class="admin-login-page">
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
      <div class="floating-shapes">
        <span class="shape shape-1"></span>
        <span class="shape shape-2"></span>
        <span class="shape shape-3"></span>
        <span class="shape shape-4"></span>
        <span class="shape shape-5"></span>
      </div>
    </div>

    <div class="login-layout">
      <div class="brand-panel">
        <div class="brand-inner">
          <div class="brand-header fade-slide-up delay-100">
            <div class="logo-section">
              <div class="logo-geo">
                <svg viewBox="0 0 56 56" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <polygon points="28,4 52,18 52,38 28,52 4,38 4,18" stroke="currentColor" stroke-width="1.8" fill="none"/>
                  <polygon points="28,8 48,20 48,36 28,48 8,36 8,20" stroke="currentColor" stroke-width="1" fill="none" opacity="0.4"/>
                  <circle cx="28" cy="28" r="8" stroke="currentColor" stroke-width="1.5" fill="none"/>
                  <circle cx="28" cy="28" r="3" fill="currentColor" opacity="0.6"/>
                  <line x1="28" y1="2" x2="28" y2="10" stroke="currentColor" stroke-width="1" opacity="0.3"/>
                  <line x1="28" y1="46" x2="28" y2="54" stroke="currentColor" stroke-width="1" opacity="0.3"/>
                </svg>
              </div>
              <div class="logo-text">
                <h1 class="logo-name">乡村振兴控制台</h1>
                <p class="logo-tagline">管理控制台</p>
              </div>
            </div>
            <p class="brand-desc">Administration Console — 统一管理平台，高效运营您的电商业务。</p>
          </div>

          <div class="system-status fade-slide-up delay-300">
            <div class="status-card">
              <div class="status-indicator online">
                <span class="status-dot"></span>
                <span class="status-label">系统运行中</span>
              </div>
              <span class="status-tag">SYS.ADMIN.v2.7</span>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                    <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"/>
                  </svg>
                </span>
                <span class="info-text">{{ currentTime }}</span>
              </div>
              <div class="info-item">
                <span class="info-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                    <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"/>
                  </svg>
                </span>
                <span class="info-text">NODE: CN-EAST-01</span>
              </div>
              <div class="info-item">
                <span class="info-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                    <path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
                  </svg>
                </span>
                <span class="info-text">TLS 1.3 加密传输</span>
              </div>
            </div>
          </div>

          <div class="brand-footer fade-slide-up delay-500">
            <div class="brand-quote">
              <p>"安全、高效、可靠的管理平台，为您的电商业务保驾护航。"</p>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="form-card fade-slide-up delay-200">
          <div class="form-card-inner">
            <div class="form-header">
              <h2 class="form-title">管理员登录</h2>
              <p class="form-subtitle">请输入您的管理员凭据以继续</p>
            </div>

            <el-form
              :model="loginForm"
              :rules="rules"
              ref="loginFormRef"
              @submit.prevent="handleLogin"
              class="login-form"
            >
              <el-form-item prop="username">
                <div class="field-group">
                  <label class="field-label">
                    <span class="label-deco"></span>
                    用户名
                  </label>
                  <el-input
                    v-model="loginForm.username"
                    placeholder="输入管理员账号"
                    size="large"
                    clearable
                  >
                    <template #prefix>
                      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="input-prefix-icon">
                        <path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H3z"/>
                      </svg>
                    </template>
                  </el-input>
                </div>
              </el-form-item>

              <el-form-item prop="password">
                <div class="field-group">
                  <label class="field-label">
                    <span class="label-deco"></span>
                    密码
                  </label>
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="输入密码"
                    size="large"
                    show-password
                    @keyup.enter="handleLogin"
                  >
                    <template #prefix>
                      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="input-prefix-icon">
                        <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"/>
                      </svg>
                    </template>
                  </el-input>
                </div>
              </el-form-item>

              <el-form-item prop="captchaCode">
                <div class="field-group">
                  <label class="field-label">
                    <span class="label-deco"></span>
                    验证码
                  </label>
                  <div class="captcha-row">
                    <el-input
                      v-model="loginForm.captchaCode"
                      placeholder="输入图形验证码"
                      size="large"
                      maxlength="6"
                      class="captcha-input"
                      @keyup.enter="handleLogin"
                    >
                      <template #prefix>
                        <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="input-prefix-icon">
                          <path fill-rule="evenodd" d="M3 4a2 2 0 012-2h10a2 2 0 012 2v12a2 2 0 01-2 2H5a2 2 0 01-2-2V4zm2 0v3h10V4H5zm0 5v2h6V9H5zm8 0v2h2V9h-2zm-8 4v2h4v-2H5zm6 0v2h4v-2h-4z"/>
                        </svg>
                      </template>
                    </el-input>
                    <div class="captcha-image-wrap" @click="loadCaptcha" title="点击刷新">
                      <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
                      <span v-else class="captcha-loading">加载中...</span>
                    </div>
                  </div>
                </div>
              </el-form-item>

              <el-form-item>
                <button
                  type="button"
                  class="submit-btn"
                  :disabled="loading"
                  @click="handleLogin"
                >
                  <span v-if="!loading" class="btn-content">
                    登 录
                    <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" class="btn-arrow">
                      <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z"/>
                    </svg>
                  </span>
                  <span v-else class="btn-loading">
                    <span class="pulse-dot"></span>
                    验证中...
                  </span>
                </button>
              </el-form-item>
            </el-form>

            <div class="form-footer">
              <div class="footer-info">
                <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"/>
                </svg>
                <span>仅限授权管理员访问 · 所有操作将被记录</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)
const currentTime = ref('')
const captchaImage = ref('')
const captchaKey = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入图形验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度不正确', trigger: 'blur' }
  ]
}

let timeInterval: ReturnType<typeof setInterval> | null = null

const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

// 加载图形验证码
const loadCaptcha = async () => {
  try {
    const res: any = await getCaptcha(captchaKey.value || undefined)
    captchaKey.value = res.key
    captchaImage.value = res.image
    // 输入框清空，让用户输入新验证码
    loginForm.captchaCode = ''
  } catch (err) {
    console.error('加载验证码失败:', err)
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  loadCaptcha()
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
})

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.loginAction(
          loginForm.username,
          loginForm.password,
          captchaKey.value,
          loginForm.captchaCode
        )
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error: any) {
        // 登录失败时刷新验证码，让用户重试
        loadCaptcha()
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* ============================================================
   管理员登录页样式（双栏布局：左 brand 介绍 + 右登录表单）
   ============================================================ */

.admin-login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 600px;
  overflow: hidden;
  background: #0f172a;
  color: #e2e8f0;
  font-family: var(--font-family-body, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif);
}

/* === 背景层 === */
.bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 800px at 0% 0%, rgba(99, 102, 241, 0.25), transparent 60%),
    radial-gradient(1000px 700px at 100% 100%, rgba(56, 189, 248, 0.18), transparent 60%),
    linear-gradient(160deg, #0f172a 0%, #1e293b 100%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}
.bg-glow-1 { width: 480px; height: 480px; top: -120px; left: -120px; background: #6366f1; }
.bg-glow-2 { width: 420px; height: 420px; bottom: -140px; right: -100px; background: #38bdf8; }

.floating-shapes .shape {
  position: absolute;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 4px;
  animation: floatMove 14s ease-in-out infinite;
}
.shape-1 { top: 12%; left: 18%; width: 28px; height: 28px; animation-delay: 0s; }
.shape-2 { top: 30%; left: 8%;  width: 16px; height: 16px; animation-delay: 2s; }
.shape-3 { top: 60%; left: 25%; width: 22px; height: 22px; animation-delay: 4s; }
.shape-4 { top: 75%; left: 12%; width: 18px; height: 18px; animation-delay: 6s; }
.shape-5 { top: 45%; left: 32%; width: 14px; height: 14px; animation-delay: 8s; }

@keyframes floatMove {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-20px) rotate(45deg); }
}

/* === 整体两栏布局 === */
.login-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 480px;
  width: 100%;
  height: 100vh;
}

/* === 左侧 brand 介绍区 === */
.brand-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 80px;
}

.brand-inner {
  width: 100%;
  max-width: 520px;
  color: #e2e8f0;
}

.brand-header { margin-bottom: 48px; }

.logo-section {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.logo-geo {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  color: #38bdf8;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-geo svg { width: 100%; height: 100%; }

.logo-text { line-height: 1.2; }

.logo-name {
  font-family: var(--font-family-display, "Georgia", serif);
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 1px;
  margin: 0;
  color: #f1f5f9;
}

.logo-tagline {
  font-size: 14px;
  color: #94a3b8;
  margin: 4px 0 0;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 16px;
  line-height: 1.7;
  color: #cbd5e1;
  margin: 0;
}

/* === 系统状态卡 === */
.system-status { margin-bottom: 48px; }

.status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  margin-bottom: 20px;
  backdrop-filter: blur(8px);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.18);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.5; }
}

.status-label { color: #cbd5e1; }
.status-tag {
  font-family: monospace;
  font-size: 12px;
  color: #38bdf8;
  padding: 4px 10px;
  background: rgba(56, 189, 248, 0.12);
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 8px;
  font-size: 13px;
  color: #cbd5e1;
}
.info-icon { color: #38bdf8; display: inline-flex; }
.info-text { font-family: monospace; }

/* === 底部格言 === */
.brand-quote {
  padding: 20px 24px;
  border-left: 3px solid #38bdf8;
  background: rgba(56, 189, 248, 0.05);
  border-radius: 0 8px 8px 0;
}
.brand-quote p {
  font-style: italic;
  font-size: 15px;
  line-height: 1.7;
  color: #cbd5e1;
  margin: 0;
}

/* === 右侧表单区 === */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(12px);
  border-left: 1px solid rgba(148, 163, 184, 0.1);
}

.form-card {
  width: 100%;
  max-width: 400px;
  background: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.4);
}

.form-card-inner { width: 100%; }

.form-header { margin-bottom: 32px; }

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #f1f5f9;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
}

.form-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.login-form { width: 100%; }

.field-group { margin-bottom: 8px; }

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #cbd5e1;
  margin-bottom: 8px;
  font-weight: 500;
}

.label-deco {
  display: inline-block;
  width: 3px;
  height: 12px;
  background: linear-gradient(180deg, #38bdf8, #6366f1);
  border-radius: 2px;
}

:deep(.login-form .el-input__wrapper) {
  background: rgba(15, 23, 42, 0.6) !important;
  border-radius: 8px !important;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.2) inset !important;
  padding: 4px 12px !important;
  transition: all 0.2s ease;
}
:deep(.login-form .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.5) inset !important;
}
:deep(.login-form .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #38bdf8 inset, 0 0 0 4px rgba(56, 189, 248, 0.15) !important;
}
:deep(.login-form .el-input__inner) {
  color: #f1f5f9 !important;
  height: 42px;
  font-size: 14px;
}
:deep(.login-form .el-input__inner::placeholder) {
  color: #64748b;
}
:deep(.input-prefix-icon) {
  color: #64748b;
  margin-right: 8px;
}

/* === 验证码行 === */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.captcha-input {
  flex: 1;
  min-width: 0;
}
.captcha-image-wrap {
  flex-shrink: 0;
  width: 130px;
  height: 42px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.6);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.2) inset;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.captcha-image-wrap:hover {
  box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.5) inset;
}
.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.captcha-loading {
  font-size: 12px;
  color: #64748b;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #38bdf8 0%, #6366f1 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  margin-top: 12px;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(56, 189, 248, 0.35);
}
.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.btn-arrow { transition: transform 0.2s; }
.submit-btn:hover .btn-arrow { transform: translateX(3px); }

.btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  animation: pulse 1s ease-in-out infinite;
}

.form-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed rgba(148, 163, 184, 0.15);
}
.footer-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  justify-content: center;
}
.footer-info svg { color: #f59e0b; }

/* === 入场动画 === */
.fade-slide-up {
  opacity: 0;
  transform: translateY(20px);
  animation: fadeSlideUp 0.6s ease forwards;
}
.delay-100 { animation-delay: 0.1s; }
.delay-200 { animation-delay: 0.2s; }
.delay-300 { animation-delay: 0.3s; }
.delay-500 { animation-delay: 0.5s; }

@keyframes fadeSlideUp {
  to { opacity: 1; transform: translateY(0); }
}

/* === 响应式：小屏折叠为单列 === */
@media (max-width: 960px) {
  .login-layout {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
  .form-panel {
    border-left: none;
    background: transparent;
  }
}
</style>
