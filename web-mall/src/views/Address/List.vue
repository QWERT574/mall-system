<template>
  <div class="address-list-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">收货地址</h1>
        <button @click="goToAdd" class="btn-add">
          <el-icon><Plus /></el-icon>
          新增地址
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p class="loading-text">正在加载地址列表...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="addresses.length === 0" class="empty-container">
        <el-empty description="暂无收货地址" :image-size="100" />
        <button @click="goToAdd" class="btn-add-address">添加第一个地址</button>
      </div>

      <!-- 地址列表 -->
      <div v-else class="address-list">
        <div 
          v-for="address in addresses" 
          :key="address.id"
          :class="['address-card', { default: address.isDefault }]"
          @click="selectAddress(address)"
        >
          <!-- 默认标签 -->
          <div v-if="address.isDefault" class="default-tag">默认</div>
          
          <!-- 操作按钮 -->
          <div class="card-actions">
            <button @click.stop="editAddress(address)" class="btn-action edit">
              <el-icon><Edit /></el-icon>
            </button>
            <button @click.stop="deleteAddress(address.id)" class="btn-action delete">
              <el-icon><Delete /></el-icon>
            </button>
          </div>

          <!-- 地址信息 -->
          <div class="address-info">
            <div class="address-row">
              <span class="consignee">{{ address.consignee }}</span>
              <span class="phone">{{ address.phone }}</span>
            </div>
            <div class="address-detail">
              {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import api from '@/utils/api';

const router = useRouter();
const userStore = useUserStore();
const user = computed(() => userStore.user);

const loading = ref(true);
const addresses = ref([]);

const loadAddresses = async () => {
  try {
    loading.value = true;
    if (!user.value || !user.value.id) {
      ElMessage.warning('请先登录');
      router.push('/login');
      return;
    }

    const res = await api.get(`/user/address/list?userId=${user.value.id}`);
    if (res.code === 0) {
      addresses.value = res.data || [];
    } else {
      ElMessage.error(res.message || '加载地址列表失败');
    }
  } catch (error) {
    console.error('加载地址列表失败:', error);
    ElMessage.error('加载地址列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const goToAdd = () => {
  router.push('/address/add');
};

const editAddress = (address) => {
  router.push(`/address/edit/${address.id}`);
};

const deleteAddress = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res = await api.post(`/user/address/delete/${id}`);
    if (res.code === 0) {
      ElMessage.success('地址删除成功');
      loadAddresses();
    } else {
      ElMessage.error(res.message || '删除地址失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除地址失败:', error);
      ElMessage.error('删除地址失败，请稍后重试');
    }
  }
};

const selectAddress = (address) => {
  // 如果是从订单页面跳转过来的，选择地址后返回
  const fromOrder = router.currentRoute.value.query.from;
  if (fromOrder === 'order') {
    router.push({
      path: '/order/confirm',
      query: { addressId: address.id }
    });
  }
};

onMounted(() => {
  loadAddresses();
});
</script>

<style scoped>
.address-list-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
}

.container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.btn-add {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-add:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.loading-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid var(--color-primary-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: #999;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999;
  margin-bottom: 20px;
}

.btn-add-address {
  padding: 12px 24px;
  background: var(--color-primary-500);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-add-address:hover {
  background: #5568d3;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.address-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.address-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.address-card.default {
  border-color: var(--color-primary-500);
  background: linear-gradient(135deg, #f5f7ff 0%, #fff 100%);
}

.default-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
}

.btn-action {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-action.edit {
  background: #f0f0f0;
  color: #666;
}

.btn-action.edit:hover {
  background: var(--color-primary-500);
  color: white;
}

.btn-action.delete {
  background: #f0f0f0;
  color: #666;
}

.btn-action.delete:hover {
  background: #ff6b6b;
  color: white;
}

.address-info {
  margin-top: 20px;
}

.address-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.consignee {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.phone {
  font-size: 14px;
  color: #666;
}

.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .address-list-page { padding: 12px; }
  .address-card { padding: 16px; }
  .address-info { margin-top: 16px; }
  .btn-action { width: 28px; height: 28px; }
  .btn-add { padding: 8px 14px; font-size: 13px; }
}

@media (max-width: 480px) {
  .address-list-page { padding: 8px; }
  .page-title { font-size: 20px; }
  .address-card { padding: 12px; }
  .address-info { margin-top: 12px; }
  .consignee { font-size: 14px; }
  .phone { font-size: 13px; }
  .address-detail { font-size: 13px; }
  .btn-add { padding: 6px 10px; font-size: 12px; }
  .btn-action { width: 26px; height: 26px; }
}
</style>
