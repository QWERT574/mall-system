<template>
  <div class="payment-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">{{ order && order.status >= 1 ? '订单详情' : '订单支付' }}</h1>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p class="loading-text">正在加载订单信息...</p>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="error-container">
        <div class="error-icon">⚠️</div>
        <p class="error-text">{{ error }}</p>
        <button @click="goBack" class="btn-secondary">返回上一页</button>
      </div>

      <!-- 支付内容 -->
      <div v-else-if="order" class="payment-content">
        <!-- 订单信息 -->
        <div class="order-summary">
          <h3 class="summary-title">订单信息</h3>
          <div class="summary-row">
            <span class="summary-label">订单编号</span>
            <span class="summary-value">{{ order.orderSn || order.id }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">订单状态</span>
            <span :class="['status-text', getStatusClass(order.status)]">
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="summary-row">
            <span class="summary-label">下单时间</span>
            <span class="summary-value">{{ formatTime(order.createTime) }}</span>
          </div>
        </div>

        <!-- 商品列表 -->
        <div class="order-items">
          <h3 class="items-title">商品清单</h3>
          <div v-for="item in order.items" :key="item.id" class="item-row">
            <div class="item-info">
              <img :src="item.productImage || '/placeholder.png'" alt="商品" class="item-image" />
              <div class="item-detail">
                <p class="item-name">{{ item.productName }}</p>
                <p v-if="item.specName" class="item-spec">{{ item.specName }}</p>
              </div>
            </div>
            <div class="item-right">
              <div class="item-price">¥{{ item.price?.toFixed(2) || '0.00' }}</div>
              <div class="item-quantity">x{{ item.quantity }}</div>
            </div>
          </div>
        </div>

        <!-- 收货地址 -->
        <div v-if="order.consignee" class="shipping-info">
          <h3 class="items-title">收货信息</h3>
          <div class="address-card">
            <div class="address-row">
              <span class="consignee">{{ order.consignee }}</span>
              <span class="phone">{{ order.phone }}</span>
            </div>
            <div class="address-detail">
              {{ order.province }}{{ order.city }}{{ order.district }}{{ order.detail }}
            </div>
          </div>
        </div>

        <!-- 支付金额 -->
        <div class="payment-amount">
          <div class="amount-row">
            <span class="amount-label">商品总额：</span>
            <span class="amount-value">¥{{ order.totalPrice?.toFixed(2) || '0.00' }}</span>
          </div>
          <div class="amount-row discount-row" v-if="order.discountAmount > 0">
            <span class="amount-label">优惠券抵扣：</span>
            <span class="amount-value discount-value">-¥{{ (order.discountAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="amount-row">
            <span class="amount-label">运费：</span>
            <span class="amount-value">¥0.00</span>
          </div>
          <div class="amount-row total-row">
            <span class="amount-label">应付金额：</span>
            <span class="amount-value total-price">¥{{ (order.payAmount || order.totalPrice)?.toFixed(2) || '0.00' }}</span>
          </div>
        </div>

        <!-- 支付方式 - 仅未付款时显示 -->
        <div v-if="order.status === 0" class="payment-methods">
          <h3 class="methods-title">选择支付方式</h3>
          <div class="method-list">
            <div 
              v-for="method in paymentMethods" 
              :key="method.id"
              :class="['method-item', { active: selectedMethod === method.id }]"
              @click="selectMethod(method.id)"
            >
              <div class="method-icon">{{ method.icon }}</div>
              <div class="method-info">
                <div class="method-name">{{ method.name }}</div>
                <div class="method-desc">{{ method.desc }}</div>
              </div>
              <div class="method-check" v-if="selectedMethod === method.id">✓</div>
            </div>
          </div>
          <div class="payment-tip">
            <span class="tip-icon">ℹ️</span>
            <span class="tip-text">模拟支付环境，任何支付都不会真实扣款</span>
          </div>
        </div>

        <!-- 已付款时显示支付信息 -->
        <div v-if="order.status >= 1 && order.payment" class="payment-info">
          <h3 class="methods-title">支付信息</h3>
          <div class="payment-info-content">
            <div class="payment-info-row">
              <span class="payment-info-label">支付方式</span>
              <span class="payment-info-value">{{ getPaymentMethodName(order.payment.paymentMethod) }}</span>
            </div>
            <div class="payment-info-row">
              <span class="payment-info-label">支付金额</span>
              <span class="payment-info-value price">¥{{ (order.payment.amount || order.payAmount || order.totalPrice)?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="payment-info-row" v-if="order.payment.payTime">
              <span class="payment-info-label">支付时间</span>
              <span class="payment-info-value">{{ formatTime(order.payment.payTime) }}</span>
            </div>
            <div class="payment-info-row" v-if="order.payment.paymentNo">
              <span class="payment-info-label">交易流水号</span>
              <span class="payment-info-value mono">{{ order.payment.paymentNo }}</span>
            </div>
          </div>
        </div>

        <!-- 支付按钮 -->
        <div class="payment-actions">
          <button 
            class="btn-primary pay-btn" 
            @click="handlePay"
            :disabled="!selectedMethod || order.status !== 0"
          >
            {{ order.status === 0 ? '立即支付' : '订单已支付' }}
          </button>
          <button v-if="order.status === 0" @click="cancelOrder" class="btn-secondary cancel-btn">
            取消订单
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/utils/api';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref(null);
const order = ref(null);
const selectedMethod = ref('wechat');
const paymentMethods = [
  { id: 'wechat', name: '微信支付', icon: '💳', desc: '推荐使用' },
  { id: 'alipay', name: '支付宝', icon: '💰', desc: '快捷支付' },
  { id: 'bank', name: '银行卡', icon: '🏦', desc: '储蓄卡/信用卡' }
];

const formatTime = (time) => {
  if (!time) return '';
  
  // 如果是数组格式 [2026,4,9,0,25,28]
  if (Array.isArray(time)) {
    const [year, month, day, hour, minute, second] = time;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }
  
  // 如果是字符串格式
  const date = new Date(time);
  if (isNaN(date.getTime())) return '';
  
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getStatusText = (status) => {
  const statusMap = {
    0: '待付款',
    1: '已付款',
    2: '已发货',
    3: '已完成',
    4: '已取消'
  };
  return statusMap[status] || '未知状态';
};

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-pending',
    1: 'status-paid',
    2: 'status-shipped',
    3: 'status-completed',
    4: 'status-canceled'
  };
  return classMap[status] || '';
};

const loadOrder = async () => {
  try {
    const orderId = route.params.id;
    if (!orderId) {
      error.value = '订单 ID 不能为空';
      loading.value = false;
      return;
    }

    const res = await api.get(`/order/detail/${orderId}`);
    if (res.code === 0) {
      order.value = res.data;
    } else {
      error.value = res.message || '加载订单失败';
    }
  } catch (err) {
    console.error('加载订单失败:', err);
    error.value = '加载订单失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const selectMethod = (methodId) => {
  selectedMethod.value = methodId;
};

const handlePay = async () => {
  if (!selectedMethod.value) {
    ElMessage.warning('请选择支付方式');
    return;
  }

  if (!order.value) {
    ElMessage.error('订单信息加载失败');
    return;
  }
  
  if (order.value.status !== 0) {
    ElMessage.warning(`订单状态为${getStatusText(order.value.status)}，无需支付`);
    return;
  }

  try {
    // 显示模拟支付确认对话框
    await ElMessageBox.confirm(
      `确定要支付订单吗？\n\n商品总额：¥${order.value.totalPrice?.toFixed(2) || '0.00'}${order.value.discountAmount > 0 ? '\n优惠券抵扣：-¥' + order.value.discountAmount.toFixed(2) : ''}\n\n应付金额：¥${(order.value.payAmount || order.value.totalPrice)?.toFixed(2) || '0.00'}\n支付方式：${getSelectedMethodName()}\n\n（模拟支付，点击确定即可完成支付）`,
      '支付确认',
      {
        confirmButtonText: '确定支付',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--primary'
      }
    );

    // 显示支付处理中的提示
    const loadingMsg = ElMessage({
      message: '正在处理支付，请稍候...',
      type: 'info',
      duration: 0
    });

    const paymentInfo = {
      paymentMethod: selectedMethod.value,
      transactionId: 'SIMULATED_' + Date.now(),
      remark: `模拟支付 - ${getSelectedMethodName()}`
    };

    const res = await api.post(`/order/pay/${order.value.id}`, paymentInfo);
    
    // 关闭加载提示
    loadingMsg.close();
    
    if (res.code === 0) {
      ElMessage.success({
        message: '✅ 支付成功！正在跳转...',
        duration: 2000
      });
      setTimeout(() => {
        router.push('/order-list');
      }, 1500);
    } else {
      ElMessage.error({
        message: res.message || '支付失败',
        duration: 3000
      });
    }
  } catch (err) {
    if (err === 'cancel') {
      ElMessage.info('已取消支付');
    } else {
      console.error('支付失败:', err);
      ElMessage.error({
        message: err.response?.data?.message || '支付失败，请稍后重试',
        duration: 3000
      });
    }
  }
};

const getSelectedMethodName = () => {
  const method = paymentMethods.find(m => m.id === selectedMethod.value);
  return method ? method.name : '微信';
};

const getPaymentMethodName = (methodCode) => {
  const map = { 0: '其他', 1: '微信支付', 2: '支付宝', 3: '银行卡' };
  return map[methodCode] || '未知';
};

const cancelOrder = async () => {
  if (!order.value) {
    ElMessage.error('订单信息加载失败');
    return;
  }
  
  if (order.value.status !== 0) {
    ElMessage.warning('订单状态不允许取消');
    return;
  }

  try {
    const result = await ElMessageBox.confirm('确定要取消订单吗？取消后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    });

    if (result) {
      const res = await api.post(`/order/cancel/${order.value.id}`);
      if (res.code === 0) {
        ElMessage.success({
          message: '订单已取消',
          duration: 2000
        });
        loadOrder();
      } else {
        ElMessage.error({
          message: res.message || '取消订单失败',
          duration: 3000
        });
      }
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('取消订单失败:', err);
      ElMessage.error({
        message: err.response?.data?.message || '取消订单失败，请稍后重试',
        duration: 3000
      });
    }
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadOrder();
});
</script>

<style scoped>
.payment-page {
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

.loading-container,
.error-container {
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

.error-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.error-text {
  font-size: 16px;
  color: #999;
  margin-bottom: 20px;
}

.payment-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-summary,
.order-items,
.shipping-info,
.payment-amount,
.payment-methods,
.payment-info {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.summary-title,
.items-title,
.methods-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 14px;
  color: #666;
}

.summary-value {
  font-size: 14px;
  color: #333;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
}

.status-pending { color: #ff6b6b; }
.status-paid { color: #4ecdc4; }
.status-shipped { color: #45b7d1; }
.status-completed { color: #96c93d; }
.status-canceled { color: #999; }

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.item-row:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.item-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.item-spec {
  font-size: 12px;
  color: #999;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.item-price {
  font-size: 14px;
  color: #ff6b6b;
  font-weight: 600;
}

.item-quantity {
  font-size: 12px;
  color: #999;
}

.address-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.address-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.consignee {
  font-size: 14px;
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

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.amount-label {
  font-size: 14px;
  color: #666;
}

.amount-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.total-row {
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.total-row .amount-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.method-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.method-item:hover {
  border-color: var(--color-primary-500);
}

.method-item.active {
  border-color: var(--color-primary-500);
  background: #f5f5ff;
}

.method-icon {
  font-size: 32px;
  margin-right: 16px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.method-desc {
  font-size: 12px;
  color: #999;
}

.method-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary-500);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.payment-tip {
  margin-top: 16px;
  padding: 12px 16px;
  background: #e3f2fd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #1976d2;
}

.tip-icon {
  font-size: 16px;
}

.payment-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 0;
}

.pay-btn {
  width: 100%;
  padding: 16px;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.pay-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.pay-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  background: #fff;
  color: #666;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.btn-secondary {
  padding: 12px 24px;
  background: var(--color-primary-500);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #5568d3;
}

.payment-info-content {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.payment-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.payment-info-row:last-child {
  border-bottom: none;
}

.payment-info-label {
  font-size: 14px;
  color: #666;
}

.payment-info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.payment-info-value.price {
  color: #ff6b6b;
  font-weight: 700;
  font-size: 16px;
}

.payment-info-value.mono {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #999;
  word-break: break-all;
}

@media (max-width: 768px) {
  .payment-page { padding: 12px; }
  .method-list { flex-direction: column; }
  .method-item { flex-direction: column; text-align: center; gap: 8px; }
  .method-icon { margin-right: 0; font-size: 28px; }
  .method-check { align-self: center; }
  .item-row { flex-direction: column; align-items: flex-start; gap: 8px; }
  .item-right { flex-direction: row; align-items: center; width: 100%; justify-content: space-between; }
  .summary-row { flex-direction: column; align-items: flex-start; gap: 4px; }
  .payment-info-row { flex-direction: column; align-items: flex-start; gap: 4px; }
}

@media (max-width: 480px) {
  .payment-page { padding: 8px; }
  .page-title { font-size: 20px; }
  .order-summary, .order-items, .shipping-info, .payment-amount, .payment-methods, .payment-info { padding: 14px; }
  .summary-title, .items-title, .methods-title { font-size: 16px; }
  .total-price { font-size: 20px; }
  .pay-btn { padding: 14px; font-size: 16px; }
  .cancel-btn { padding: 12px; font-size: 14px; }
}
</style>
