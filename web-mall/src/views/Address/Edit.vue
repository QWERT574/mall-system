<template>
  <div class="address-edit-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">{{ isEdit ? '编辑地址' : '新增地址' }}</h1>
      </div>

      <!-- 表单 -->
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="收货人" prop="consignee">
            <el-input v-model="form.consignee" placeholder="请输入收货人姓名" />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号码" maxlength="11" />
          </el-form-item>

          <el-form-item label="所在地区" prop="region">
            <el-cascader
              v-model="form.region"
              :options="regionOptions"
              placeholder="请选择省/市/区"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="详细地址" prop="detail">
            <el-input
              v-model="form.detail"
              type="textarea"
              :rows="3"
              placeholder="请输入详细地址，如街道、门牌号等"
            />
          </el-form-item>

          <el-form-item label="默认地址">
            <el-switch v-model="form.isDefault" :active-value="true" :inactive-value="false" />
            <span class="switch-tip">设为默认收货地址</span>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <button @click="submitForm" class="btn-submit">保存地址</button>
              <button @click="goBack" class="btn-cancel">取消</button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import api from '@/utils/api';
import regionData from '@/data/regionData.js';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const user = computed(() => userStore.user);
const formRef = ref(null);

const isEdit = computed(() => !!route.params.id);

const form = reactive({
  consignee: '',
  phone: '',
  region: [],
  detail: '',
  isDefault: false
});

const rules = {
  consignee: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  region: [
    { required: true, message: '请选择所在地区', trigger: 'change' }
  ],
  detail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { min: 5, message: '详细地址至少 5 个字符', trigger: 'blur' }
  ]
};

const regionOptions = ref(regionData);

const loadAddress = async () => {
  if (!isEdit.value) return;

  try {
    const addressId = route.params.id;
    const res = await api.get(`/user/address/detail/${addressId}`);
    if (res.code === 0 && res.data) {
      const address = res.data;
      form.consignee = address.consignee;
      form.phone = address.phone;
      // 解析地区
      form.region = [address.province, address.city, address.district];
      form.detail = address.detail;
      form.isDefault = address.isDefault;
    }
  } catch (error) {
    console.error('加载地址详情失败:', error);
    ElMessage.error('加载地址详情失败');
  }
};

const submitForm = async () => {
  try {
    await formRef.value.validate();

    if (!user.value || !user.value.id) {
      ElMessage.warning('请先登录');
      router.push('/login');
      return;
    }

    const addressData = {
      userId: user.value.id,
      consignee: form.consignee,
      phone: form.phone,
      province: form.region[0],
      city: form.region[1],
      district: form.region[2],
      detail: form.detail,
      isDefault: form.isDefault
    };

    let res;
    if (isEdit.value) {
      addressData.id = route.params.id;
      res = await api.post('/user/address/update', addressData);
    } else {
      res = await api.post('/user/address/add', addressData);
    }

    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '地址更新成功' : '地址添加成功');
      router.push('/address/list');
    } else {
      ElMessage.error(res.message || (isEdit.value ? '更新地址失败' : '添加地址失败'));
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存地址失败:', error);
      ElMessage.error(isEdit.value ? '更新地址失败' : '添加地址失败');
    }
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadAddress();
});
</script>

<style scoped>
.address-edit-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
}

.container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.form-container {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  width: 100%;
}

.btn-submit {
  padding: 12px 40px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-cancel {
  padding: 12px 40px;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.switch-tip {
  margin-left: 10px;
  font-size: 14px;
  color: #666;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #333;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

:deep(.el-textarea__inner) {
  border-radius: 6px;
}

@media (max-width: 768px) {
  .address-edit-page { padding: 12px; }
  .form-container { padding: 20px; }
  :deep(.el-form-item__label) { width: auto !important; text-align: left; padding-bottom: 4px; }
  :deep(.el-form-item) { flex-direction: column; align-items: stretch; }
  :deep(.el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-cascader) { width: 100%; }
}

@media (max-width: 480px) {
  .address-edit-page { padding: 8px; }
  .form-container { padding: 14px; }
  .page-title { font-size: 20px; }
  .btn-submit { padding: 10px 30px; font-size: 14px; }
  .btn-cancel { padding: 10px 30px; font-size: 14px; }
}
</style>
