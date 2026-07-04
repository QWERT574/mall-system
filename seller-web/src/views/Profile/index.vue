<template>
  <div class="profile-container">
    <h2 class="page-header">商家信息</h2>
    
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">⟳</div>
      <p>加载信息中...</p>
    </div>
    
    <div v-else class="profile-content">
      <div class="profile-card">
        <div class="profile-header">
          <div class="avatar">👤</div>
          <h3 class="seller-name">{{ sellerInfo.companyName || sellerInfo.nickname || '商家' }}</h3>
          <span class="verification-badge" :class="getVerifyClass(sellerInfo.isVerified)">
            {{ getVerifyText(sellerInfo.isVerified) }}
          </span>
        </div>
        
        <form class="profile-form" @submit.prevent="handleSubmit">
          <!-- 基本信息 -->
          <div class="form-section">
            <h4 class="section-title">基本信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">商家名称 <span class="required">*</span></label>
                <input 
                  type="text" 
                  v-model="form.companyName" 
                  class="form-input"
                  placeholder="请输入商家名称"
                  required
                >
              </div>
              
              <div class="form-group">
                <label class="form-label">联系人 <span class="required">*</span></label>
                <input 
                  type="text" 
                  v-model="form.contactName" 
                  class="form-input"
                  placeholder="请输入联系人姓名"
                  required
                >
              </div>
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">联系电话 <span class="required">*</span></label>
                <input 
                  type="tel" 
                  v-model="form.phone" 
                  class="form-input"
                  placeholder="请输入联系电话"
                  required
                >
              </div>
              
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input 
                  type="email" 
                  v-model="form.email" 
                  class="form-input"
                  placeholder="请输入邮箱地址"
                >
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">商家地址 <span class="required">*</span></label>
              <input 
                type="text" 
                v-model="form.companyAddress" 
                class="form-input"
                placeholder="请输入详细地址"
                required
              >
            </div>
          </div>

          <!-- 资质信息 -->
          <div class="form-section">
            <h4 class="section-title">资质信息（用于审核）</h4>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">营业执照</label>
                <div class="upload-area" @click="triggerUpload('businessLicense')">
                  <div v-if="form.qualifications.businessLicense" class="upload-preview">
                    <img :src="form.qualifications.businessLicense" alt="营业执照">
                    <span class="upload-change">点击更换</span>
                  </div>
                  <div v-else class="upload-placeholder">
                    <span class="upload-icon">📄</span>
                    <span>点击上传营业执照</span>
                  </div>
                </div>
              </div>
              
              <div class="form-group">
                <label class="form-label">法人身份证（正面）</label>
                <div class="upload-area" @click="triggerUpload('idCardFront')">
                  <div v-if="form.qualifications.idCardFront" class="upload-preview">
                    <img :src="form.qualifications.idCardFront" alt="身份证正面">
                    <span class="upload-change">点击更换</span>
                  </div>
                  <div v-else class="upload-placeholder">
                    <span class="upload-icon">🆔</span>
                    <span>点击上传身份证正面</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">法人身份证（反面）</label>
                <div class="upload-area" @click="triggerUpload('idCardBack')">
                  <div v-if="form.qualifications.idCardBack" class="upload-preview">
                    <img :src="form.qualifications.idCardBack" alt="身份证反面">
                    <span class="upload-change">点击更换</span>
                  </div>
                  <div v-else class="upload-placeholder">
                    <span class="upload-icon">🆔</span>
                    <span>点击上传身份证反面</span>
                  </div>
                </div>
              </div>
              
              <div class="form-group">
                <label class="form-label">经营许可证</label>
                <div class="upload-area" @click="triggerUpload('permitLicense')">
                  <div v-if="form.qualifications.permitLicense" class="upload-preview">
                    <img :src="form.qualifications.permitLicense" alt="经营许可证">
                    <span class="upload-change">点击更换</span>
                  </div>
                  <div v-else class="upload-placeholder">
                    <span class="upload-icon">📋</span>
                    <span>点击上传经营许可证</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 审核状态 -->
          <div class="form-section">
            <h4 class="section-title">审核状态</h4>
            <div class="status-info" :class="getVerifyClass(sellerInfo.isVerified)">
              <span class="status-icon">{{ getVerifyIcon(sellerInfo.isVerified) }}</span>
              <div class="status-detail">
                <span class="status-text">{{ getVerifyDetailText(sellerInfo.isVerified) }}</span>
                <span v-if="sellerInfo.verificationInfo" class="status-message">{{ sellerInfo.verificationInfo }}</span>
              </div>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="submit" class="btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
            <button 
              v-if="sellerInfo.isVerified === 0 || sellerInfo.isVerified === 2" 
              type="button" 
              class="btn-secondary"
              @click="handleSubmitAudit"
              :disabled="saving"
            >
              {{ saving ? '提交中...' : '提交审核' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 隐藏的文件上传input -->
    <input 
      type="file" 
      ref="fileInput" 
      style="display: none" 
      accept="image/*"
      @change="handleFileChange"
    >
  </div>
</template>

<script>
import api from '@/utils/api';
import { ElMessage } from 'element-plus';

export default {
  data() {
    return {
      sellerInfo: {
        id: null,
        companyName: '',
        contactName: '',
        phone: '',
        email: '',
        companyAddress: '',
        isVerified: 0,
        verificationInfo: '',
        qualifications: {}
      },
      form: {
        id: null,
        companyName: '',
        contactName: '',
        phone: '',
        email: '',
        companyAddress: '',
        qualifications: {
          businessLicense: '',
          idCardFront: '',
          idCardBack: '',
          permitLicense: ''
        }
      },
      loading: true,
      saving: false,
      currentUploadType: ''
    };
  },
  mounted() {
    this.loadSellerInfo();
  },
  methods: {
    async loadSellerInfo() {
      this.loading = true;
      try {
        const userInfo = JSON.parse(localStorage.getItem('seller_user') || '{}');
        const sellerId = userInfo.id;
        
        if (!sellerId) {
          ElMessage.error('请先登录');
          return;
        }
        
        const response = await api.get(`/seller/info?sellerId=${sellerId}`);
        this.sellerInfo = { ...this.sellerInfo, ...(response || {}) };
        this.form = {
          id: this.sellerInfo.id,
          companyName: this.sellerInfo.companyName || '',
          contactName: this.sellerInfo.contactName || '',
          phone: this.sellerInfo.phone || '',
          email: this.sellerInfo.email || '',
          companyAddress: this.sellerInfo.companyAddress || '',
          qualifications: {
            businessLicense: '',
            idCardFront: '',
            idCardBack: '',
            permitLicense: ''
          }
        };
      } catch (error) {
        console.error('加载商家信息失败:', error);
        ElMessage.error('加载商家信息失败');
      } finally {
        this.loading = false;
      }
    },
    
    async handleSubmit() {
      if (!this.form.companyName) {
        ElMessage.error('请输入商家名称');
        return;
      }
      
      if (!this.form.id) {
        ElMessage.error('商家ID不存在，请重新登录');
        return;
      }
      
      this.saving = true;
      
      try {
        const data = {
          id: this.form.id,
          name: this.form.companyName,
          contact: this.form.contactName,
          phone: this.form.phone,
          email: this.form.email,
          address: this.form.companyAddress
        };
        
        const response = await api.post('/seller/update', data);
        ElMessage.success('商家信息更新成功');
        this.loadSellerInfo();
      } catch (error) {
        console.error('保存失败:', error);
        ElMessage.error(error.message || '保存失败');
      } finally {
        this.saving = false;
      }
    },
    
    async handleSubmitAudit() {
      if (!this.form.companyName || !this.form.contactName || !this.form.phone || !this.form.companyAddress) {
        ElMessage.error('请完善基本信息后再提交审核');
        return;
      }
      
      this.saving = true;
      
      try {
        // 先保存信息
        await this.handleSubmit();
        
        // 提交审核（将审核状态重置为待审核）
        const response = await api.post(`/seller/submit-audit/${this.form.id}`);
        this.$emit('show-success', '审核提交成功，请耐心等待');
        this.loadSellerInfo();
      } catch (error) {
        console.error('提交审核失败:', error);
        this.$emit('show-error', error.message || '提交审核失败');
      } finally {
        this.saving = false;
      }
    },
    
    triggerUpload(type) {
      this.currentUploadType = type;
      this.$refs.fileInput.click();
    },
    
    handleFileChange(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      // 这里简化处理，实际应该上传到服务器
      const reader = new FileReader();
      reader.onload = (e) => {
        this.form.qualifications[this.currentUploadType] = e.target.result;
      };
      reader.readAsDataURL(file);
      
      // 清空input，允许重复选择同一文件
      event.target.value = '';
    },
    
    getVerifyClass(status) {
      const classMap = {
        0: 'pending',
        1: 'verified',
        2: 'rejected'
      };
      return classMap[status] || 'pending';
    },
    
    getVerifyText(status) {
      const textMap = {
        0: '⏳ 审核中',
        1: '✓ 已认证',
        2: '✗ 审核未通过'
      };
      return textMap[status] || '⏳ 审核中';
    },
    
    getVerifyIcon(status) {
      const iconMap = {
        0: '⏳',
        1: '✅',
        2: '❌'
      };
      return iconMap[status] || '⏳';
    },
    
    getVerifyDetailText(status) {
      const textMap = {
        0: '正在审核中，请耐心等待管理员审核',
        1: '已通过审核，可以正常经营',
        2: '审核未通过，请根据反馈修改后重新提交'
      };
      return textMap[status] || '正在审核中';
    }
  }
};
</script>

<style scoped>
.profile-container {
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-header {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.loading-spinner {
  font-size: 48px;
  animation: spin 1s linear infinite;
  color: var(--color-primary-500);
}

.loading-state p {
  margin-top: 16px;
  color: #666;
}

.profile-content {
  max-width: 900px;
  margin: 0 auto;
}

.profile-card {
  padding: 20px;
}

.profile-header {
  text-align: center;
  margin-bottom: 32px;
}

.avatar {
  font-size: 64px;
  margin-bottom: 16px;
}

.seller-name {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.verification-badge {
  display: inline-block;
  padding: 6px 16px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.verification-badge.verified {
  background: #d4edda;
  color: #155724;
}

.verification-badge.rejected {
  background: #f8d7da;
  color: #721c24;
}

.form-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e9ecef;
}

.form-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid var(--color-primary-500);
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.required {
  color: #f56c6c;
}

.form-input {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover {
  border-color: var(--color-primary-500);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
}

.upload-icon {
  font-size: 32px;
}

.upload-preview {
  position: relative;
  width: 100%;
}

.upload-preview img {
  width: 100%;
  max-height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.upload-change {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 8px;
  font-size: 12px;
  border-radius: 0 0 4px 4px;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #e6a23c;
}

.status-info.verified {
  background: #d4edda;
  border-left-color: #67c23a;
}

.status-info.rejected {
  background: #f8d7da;
  border-left-color: #f56c6c;
}

.status-icon {
  font-size: 32px;
}

.status-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.status-message {
  font-size: 13px;
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e9ecef;
}

.btn-primary {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-secondary {
  padding: 12px 32px;
  background: white;
  color: var(--color-primary-500);
  border: 2px solid var(--color-primary-500);
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-primary-500);
  color: white;
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
}
</style>
