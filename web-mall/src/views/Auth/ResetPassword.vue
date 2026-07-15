<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2 class="auth-title">重置密码</h2>
      <form @submit.prevent="handleReset" class="auth-form">
        <div class="form-group">
          <label for="phone">手机号</label>
          <input
            id="phone"
            v-model="phone"
            type="tel"
            placeholder="请输入注册手机号"
            pattern="[1][3-9][0-9]{9}"
            required
            class="form-input"
          />
        </div>
        <div class="form-group">
          <label for="code">验证码</label>
          <div class="code-input-group">
            <input
              id="code"
              v-model="code"
              type="text"
              placeholder="请输入验证码"
              required
              class="form-input"
            />
            <button 
              type="button" 
              class="send-code-btn"
              :disabled="sending || countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label for="password">新密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入新密码"
            required
            class="form-input"
          />
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
        <div class="form-group">
          <label for="confirmPassword">确认新密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            required
            class="form-input"
          />
        </div>
        <button type="submit" class="submit-btn" :disabled="loading || (password && !passwordAllPassed)">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>
      </form>
      <div class="auth-footer">
        <span>记得密码？</span>
        <router-link to="/login" class="auth-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();

const phone = ref('');
const code = ref('');
const password = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const sending = ref(false);
const countdown = ref(0);

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

const passwordRuleChecks = computed(() => {
  const pwd = password.value;
  const ph = phone.value;
  const results = [];
  const lengthOk = pwd && pwd.length >= 8 && pwd.length <= 32;
  results.push({ key: 'LENGTH', label: '长度 8-32 位', pass: lengthOk });
  const mixOk = pwd && /[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd);
  results.push({ key: 'MIX_LETTER_NUMBER', label: '同时包含字母和数字', pass: mixOk });
  const weakOk = pwd && !WEAK_PASSWORDS.has(pwd.toLowerCase());
  results.push({ key: 'NOT_WEAK', label: '不能是常见弱密码', pass: weakOk });
  let sameAsAccount = false;
  if (pwd) {
    if (ph && pwd === ph) sameAsAccount = true;
    if (ph && ph.length >= 6 && pwd === ph.slice(-6)) sameAsAccount = true;
  }
  results.push({ key: 'NOT_SAME_AS_ACCOUNT', label: '不能与手机号相同', pass: !sameAsAccount });
  return results;
});

const passwordAllPassed = computed(() => passwordRuleChecks.value.every(r => r.pass));

const sendCode = async () => {
  if (!phone.value || !/^1[3-9]\d{9}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号');
    return;
  }
  sending.value = true;
  try {
    const res = await api.sendSmsCode(phone.value);
    if (res.code === 0) {
      countdown.value = 60;
      const timer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) clearInterval(timer);
      }, 1000);
      const devCode = res.data && res.data.devCode;
      ElMessage.success(devCode ? `验证码已发送 (dev: ${devCode})` : '验证码已发送');
    } else {
      ElMessage.error(res.message || '发送失败');
    }
  } catch (error) {
    console.error('发送验证码失败:', error);
    ElMessage.error('发送验证码失败，请稍后重试');
  } finally {
    sending.value = false;
  }
};

const handleReset = async () => {
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }
  if (!passwordAllPassed.value) {
    const failed = passwordRuleChecks.value.find(r => !r.pass);
    ElMessage.error('密码不满足要求：' + failed.label);
    return;
  }

  loading.value = true;
  try {
    const response = await api.resetPassword({
      phone: phone.value,
      code: code.value,
      password: password.value
    });

    if (response.code === 0) {
      ElMessage.success('密码重置成功！请登录');
      router.push('/login');
    } else {
      ElMessage.error(response.message || '重置失败');
    }
  } catch (error) {
    console.error('重置密码错误:', error);
    ElMessage.error(error.response?.data?.message || '重置失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-container {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  padding: 20px;
}

.auth-card {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 420px;
}

.auth-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin-bottom: 30px;
}

.auth-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary-500);
}

.code-input-group {
  display: flex;
  gap: 10px;
}

.code-input-group .form-input {
  flex: 1;
}

.send-code-btn {
  padding: 12px 20px;
  background: var(--color-primary-500);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.3s;
}

.send-code-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.submit-btn:hover {
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.auth-link {
  color: var(--color-primary-500);
  font-weight: 600;
  text-decoration: none;
  margin-left: 5px;
}

.auth-link:hover {
  text-decoration: underline;
}

/* 密码规则清单 */
.pw-rules {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 12px;
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
  width: 14px;
  height: 14px;
  border-radius: 50%;
  font-size: 9px;
  font-weight: 700;
  flex-shrink: 0;
}
.rule-fail { color: rgba(220, 38, 38, 0.7); }
.rule-fail .rule-icon { background: rgba(220, 38, 38, 0.1); color: #dc2626; }
.rule-pass { color: #16a34a; font-weight: 500; }
.rule-pass .rule-icon { background: rgba(22, 163, 74, 0.18); color: #16a34a; }

@media (max-width: 480px) { .pw-rules { grid-template-columns: 1fr; } }

@media (max-width: 480px) {
  .auth-container { padding: 12px; }
  .auth-card { padding: 24px 18px; border-radius: 12px; }
  .auth-title { font-size: 22px; margin-bottom: 20px; }
  .form-input { padding: 10px 12px; font-size: 15px; }
  .send-code-btn { padding: 10px 14px; font-size: 13px; }
  .submit-btn { padding: 12px; font-size: 15px; }
  .code-input-group { gap: 6px; }
}
</style>
