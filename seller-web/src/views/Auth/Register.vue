<template>
  <div class="seller-register-page">
    <div class="register-left">
      <div class="brand-content">
        <div class="brand-icon">
          <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="8" y="24" width="48" height="32" rx="4" stroke="currentColor" stroke-width="2.5"/>
            <path d="M8 24L32 8L56 24" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <rect x="24" y="36" width="16" height="20" rx="2" stroke="currentColor" stroke-width="2"/>
            <circle cx="32" cy="20" r="3" fill="currentColor" opacity="0.6"/>
          </svg>
        </div>
        <h1 class="brand-title">乡村振兴 · 商家入驻</h1>
        <p class="brand-subtitle">创建账号 · 开启经营之旅</p>
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

    <div class="register-right">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">创建商家账号</h2>
          <p class="form-desc">填写以下信息完成商家入驻</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister" class="register-form">
          <el-form-item prop="username">
            <div class="input-group">
              <label class="input-label">用户名</label>
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable />
            </div>
          </el-form-item>

          <el-form-item prop="phone">
            <div class="input-group">
              <label class="input-label">手机号</label>
              <el-input v-model="form.phone" placeholder="请输入手机号" size="large" clearable />
            </div>
          </el-form-item>

          <el-form-item prop="code">
            <div class="input-group">
              <label class="input-label">验证码</label>
              <div class="code-row">
                <el-input v-model="form.code" placeholder="请输入验证码" :maxlength="6" size="large" class="code-input" />
                <button
                  type="button"
                  class="send-code-btn"
                  :class="{ counting: countdown > 0 }"
                  :disabled="sendingCode || countdown > 0"
                  @click="sendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : (sendingCode ? '发送中' : '获取验证码') }}
                </button>
              </div>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-group">
              <label class="input-label">密码</label>
              <el-input v-model="form.password" type="password" placeholder="请设置密码（6-20 位）" size="large" show-password />
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <div class="input-group">
              <label class="input-label">确认密码</label>
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
            </div>
          </el-form-item>

          <div class="review-tip">
            <span class="tip-dot"></span>
            <span>商家注册后需等待管理员审核通过方可登录</span>
          </div>

          <el-form-item>
            <button
              type="button"
              class="submit-btn"
              :class="{ loading: loading }"
              :disabled="loading || (form.password && !passwordAllPassed)"
              @click="handleRegister"
            >
              <span class="btn-text" v-if="!loading">提 交 入 驻</span>
              <span class="btn-loading" v-else>
                <span class="spinner"></span>
                注册中...
              </span>
            </button>
          </el-form-item>
        </el-form>

        <div class="form-divider">
          <span class="divider-line"></span>
          <span class="divider-text">已有账号</span>
          <span class="divider-line"></span>
        </div>

        <button class="login-btn" @click="$router.push('/login')">
          返回登录
        </button>

        <p class="form-footer">注册即表示同意 <a href="javascript:void(0)">服务条款</a> 和 <a href="javascript:void(0)">隐私政策</a></p>

        <div class="user-link-row">
          <span class="user-link-text">普通用户购物？</span>
          <a :href="mallRegisterUrl" class="user-link-btn">
            👤 去商城注册
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../../utils/api';
import { ElMessage } from 'element-plus';

export default {
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };
    return {
      form: {
        username: '',
        phone: '',
        code: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      },
      features: [
        '商品管理 · 轻松上架与编辑',
        '订单处理 · 实时跟踪与发货',
        '数据洞察 · 经营决策有据可依',
        '客户服务 · 即时沟通响应'
      ],
      mallRegisterUrl: 'http://localhost:5176/register',
      sendingCode: false,
      countdown: 0,
      loading: false
    };
  },
  // ===== 密码复杂度校验(与后端 PasswordValidator 完全一致) =====
  computed: {
    WEAK_PASSWORDS() {
      return new Set([
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
    },
    passwordRuleChecks() {
      const pwd = this.form.password;
      const u = this.form.username;
      const ph = this.form.phone;
      const results = [];
      const lengthOk = pwd && pwd.length >= 8 && pwd.length <= 32;
      results.push({ key: 'LENGTH', label: '长度 8-32 位', pass: lengthOk });
      const mixOk = pwd && /[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd);
      results.push({ key: 'MIX', label: '同时包含字母和数字', pass: mixOk });
      const weakOk = pwd && !this.WEAK_PASSWORDS.has(pwd.toLowerCase());
      results.push({ key: 'NOT_WEAK', label: '不能是常见弱密码', pass: weakOk });
      let sameAsAccount = false;
      if (pwd) {
        if (u && pwd.toLowerCase() === u.toLowerCase()) sameAsAccount = true;
        if (ph && pwd === ph) sameAsAccount = true;
        if (ph && ph.length >= 6 && pwd === ph.slice(-6)) sameAsAccount = true;
      }
      results.push({ key: 'NOT_SAME', label: '不能与用户名/手机号相同', pass: !sameAsAccount });
      return results;
    },
    passwordAllPassed() {
      return this.passwordRuleChecks.every(r => r.pass);
    }
  },
  methods: {
    async sendCode() {
      try {
        await this.$refs.formRef.validateField('phone');
      } catch {
        return;
      }
      this.sendingCode = true;
      try {
        const res = await api.sendCode(this.form.phone);
        const devCode = res && res.devCode;
        ElMessage.success(devCode ? `验证码已发送 (dev: ${devCode})` : '验证码已发送');
        this.countdown = 60;
        const timer = setInterval(() => {
          this.countdown--;
          if (this.countdown <= 0) clearInterval(timer);
        }, 1000);
      } catch (error) {
        ElMessage.error(error.message || '发送验证码失败');
      } finally {
        this.sendingCode = false;
      }
    },

    async handleRegister() {
      try {
        await this.$refs.formRef.validate();
      } catch {
        return;
      }
      if (!this.passwordAllPassed) {
        const failed = this.passwordRuleChecks.find(r => !r.pass);
        ElMessage.error('密码不满足要求：' + failed.label);
        return;
      }
      this.loading = true;
      try {
        await api.register({
          username: this.form.username,
          phone: this.form.phone,
          code: this.form.code,
          password: this.form.password,
          userType: 1
        });
        ElMessage.success('注册成功，即将跳转登录');
        setTimeout(() => {
          this.$router.push('/login');
        }, 1200);
      } catch (error) {
        ElMessage.error(error.message || '注册失败');
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.seller-register-page {
  min-height: 100vh;
  display: flex;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.register-left {
  flex: 0 0 45%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 60px;
  background: linear-gradient(160deg, #0a1628 0%, #132043 40%, #1a2a5e 100%);
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 400px;
}

.brand-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 32px;
  color: #d4a574;
  animation: fadeSlideUp 0.6s ease both;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-family: 'PingFang SC', 'Microsoft YaHei', serif;
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
  flex-shrink: 0;
  background: #d4a574;
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
  border: 1.5px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  animation: floatShape 8s ease-in-out infinite;
}

.shape-1 { width: 80px; height: 80px; top: 12%; right: 15%; transform: rotate(45deg); animation-delay: 0s; }
.shape-2 { width: 50px; height: 50px; bottom: 20%; right: 25%; transform: rotate(30deg); border-radius: 50%; animation-delay: -2s; animation-duration: 10s; }
.shape-3 { width: 35px; height: 35px; top: 35%; right: 8%; transform: rotate(60deg); animation-delay: -4s; animation-duration: 7s; }
.shape-4 { width: 60px; height: 60px; bottom: 10%; left: 15%; transform: rotate(15deg); border-radius: 50%; animation-delay: -1s; animation-duration: 9s; }
.shape-5 { width: 25px; height: 25px; top: 60%; left: 8%; transform: rotate(45deg); animation-delay: -3s; animation-duration: 11s; }

.register-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  background: #f8f6f3;
}

.register-right::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(212, 165, 116, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(10, 22, 40, 0.03) 0%, transparent 50%);
}

.form-wrapper {
  width: 100%;
  max-width: 440px;
  animation: fadeSlideUp 0.5s ease both;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-family: 'PingFang SC', 'Microsoft YaHei', serif;
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
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0ddd8 inset;
  background: #fff;
  padding: 4px 12px;
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

.code-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  padding: 0 20px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s ease;
  background: transparent;
  border: 1.5px solid #0a1628;
  color: #0a1628;
}

.send-code-btn:hover:not(:disabled) {
  background: #0a1628;
  color: #fff;
}

.send-code-btn:disabled,
.send-code-btn.counting {
  border-color: #e0ddd8;
  color: #b8b3aa;
}

/* 密码规则清单 */
.pw-rules {
  list-style: none;
  padding: 10px 4px 0;
  margin: 10px 0 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 14px;
  font-size: 12px;
}
.pw-rule {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.4;
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
.rule-fail { color: rgba(220, 38, 38, 0.7); }
.rule-fail .rule-icon { background: rgba(220, 38, 38, 0.1); color: #dc2626; }
.rule-pass { color: #16a34a; font-weight: 500; }
.rule-pass .rule-icon { background: rgba(22, 163, 74, 0.18); color: #16a34a; }

@media (max-width: 480px) { .pw-rules { grid-template-columns: 1fr; } }

.review-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 8px;
  background: linear-gradient(135deg, rgba(212, 165, 116, 0.12), rgba(212, 165, 116, 0.05));
  border: 1px solid rgba(212, 165, 116, 0.2);
  color: #8c6d3f;
}

.tip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #d4a574;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #1a2a5e 0%, #0a1628 100%);
  color: #fff;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #132043 0%, #0a1628 100%);
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
  margin: 24px 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: #e0ddd8;
}

.divider-text {
  font-size: 12px;
  color: #b8b3aa;
  letter-spacing: 2px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
  border: 1.5px solid #0a1628;
  color: #0a1628;
}

.login-btn:hover {
  background: #0a1628;
  color: #fff;
  box-shadow: 0 4px 16px rgba(10, 22, 40, 0.15);
  transform: translateY(-1px);
}

.form-footer {
  text-align: center;
  font-size: 12px;
  color: #b8b3aa;
  margin-top: 28px;
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

.user-link-row {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e0ddd8;
}

.user-link-text {
  font-size: 13px;
  color: #b8b3aa;
  margin-right: 6px;
}

.user-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #d4a574;
  font-weight: 600;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.3s;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(212, 165, 116, 0.2);
  background: rgba(212, 165, 116, 0.05);
}

.user-link-btn:hover {
  background: rgba(212, 165, 116, 0.12);
  border-color: rgba(212, 165, 116, 0.4);
  color: #8c6d3f;
}

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
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
  .seller-register-page {
    flex-direction: column;
  }

  .register-left {
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

  .register-right {
    padding: 30px 20px;
  }
}
</style>