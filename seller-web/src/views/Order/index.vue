<template>
  <div class="order-page">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="order-stats">
        <span class="stat-badge warning" v-if="pendingShipCount > 0">待发货 {{ pendingShipCount }}</span>
        <span class="stat-badge success">共 {{ orders.length }} 笔</span>
      </div>
    </div>
    <List 
      :orders="orders" 
      :loading="loading"
      @ship-order="handleShipOrder"
      @view-order="handleViewOrder"
    />
    
    <el-dialog v-model="dialogVisible" title="订单详情" :width="dialogWidth" :close-on-click-modal="false">
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="info-grid">
            <div class="info-item"><span class="info-label">订单号</span><span>#{{ currentOrder.id }}</span></div>
            <div class="info-item"><span class="info-label">订单编号</span><span>{{ currentOrder.orderSn || '-' }}</span></div>
            <div class="info-item"><span class="info-label">订单状态</span><el-tag :type="getStatusType(currentOrder.status)" size="small">{{ getStatusText(currentOrder.status) }}</el-tag></div>
            <div class="info-item"><span class="info-label">下单时间</span><span>{{ formatDateTime(currentOrder.createdAt || currentOrder.createTime) }}</span></div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>收货信息</h4>
          <div class="info-grid">
            <div class="info-item"><span class="info-label">收货人</span><span>{{ currentOrder.consignee }}</span></div>
            <div class="info-item"><span class="info-label">联系电话</span><span>{{ currentOrder.phone }}</span></div>
            <div class="info-item full-width"><span class="info-label">收货地址</span><span>{{ currentOrder.province }}{{ currentOrder.city }}{{ currentOrder.district }}{{ currentOrder.detail }}</span></div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>商品清单</h4>
          <table class="product-table">
            <thead><tr><th>商品名称</th><th>规格</th><th>单价</th><th>数量</th><th>小计</th></tr></thead>
            <tbody>
              <tr v-for="item in (currentOrder.items || [])" :key="item.id">
                <td>{{ item.productName }}</td>
                <td>{{ item.specName || '-' }}</td>
                <td>¥{{ item.price?.toFixed(2) }}</td>
                <td>{{ item.quantity }}</td>
                <td class="subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div class="detail-section total-section">
          <div class="total-row">
            <span>订单总额</span>
            <span class="total-price">¥{{ calculateTotalPrice(currentOrder) }}</span>
          </div>
          <div class="total-row discount-row" v-if="currentOrder.discountAmount > 0">
            <span>优惠抵扣</span>
            <span class="discount">-¥{{ currentOrder.discountAmount?.toFixed(2) }}</span>
          </div>
          <div class="total-row final-row" v-if="currentOrder.payAmount">
            <span>实付金额</span>
            <span class="final-price">¥{{ currentOrder.payAmount?.toFixed(2) }}</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="currentOrder?.status === 1" type="primary" @click="handleShipFromDialog">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useSellerStore } from '@/stores/seller';
import api from '@/utils/api';
import { ElMessage, ElMessageBox } from 'element-plus';
import List from './List.vue';

const sellerStore = useSellerStore();
const orders = ref([]);
const loading = ref(false);
const dialogVisible = ref(false)
const currentOrder = ref(null)

const isMobile = ref(window.innerWidth <= 768)
const updateIsMobile = () => { isMobile.value = window.innerWidth <= 768; }
onMounted(() => { window.addEventListener('resize', updateIsMobile); })
onUnmounted(() => { window.removeEventListener('resize', updateIsMobile); })
const dialogWidth = computed(() => isMobile.value ? '90%' : '620px');

const pendingShipCount = computed(() => orders.value.filter(o => o.status === 1).length);

const loadOrders = async () => {
  loading.value = true;
  try {
    const sellerId = sellerStore.user?.id;
    if (!sellerId) { ElMessage.error('请先登录'); return; }
    const response = await api.getSellerOrders(sellerId);
    orders.value = response || [];
  } catch (error) {
    ElMessage.error('加载订单失败，请重试');
  } finally {
    loading.value = false;
  }
};

const handleViewOrder = (order) => { currentOrder.value = order; dialogVisible.value = true; };

const handleShipFromDialog = async () => {
  if (!currentOrder.value) return;
  try {
    const response = await api.shipOrder(currentOrder.value.id);
    if (response) { ElMessage.success('发货成功'); dialogVisible.value = false; loadOrders(); }
      else ElMessage.error('发货失败');
  } catch (error) { ElMessage.error('发货失败，请重试'); }
};

const handleShipOrder = async (order) => {
  try {
    await ElMessageBox.confirm('确定要对该订单进行发货吗？', '确认发货', { confirmButtonText: '确定发货', cancelButtonText: '取消', type: 'warning' });
    const response = await api.shipOrder(order.id);
    if (response) { ElMessage.success('发货成功'); loadOrders(); }
    else ElMessage.error('发货失败');
  } catch (error) { if (error !== 'cancel') ElMessage.error('发货失败，请重试'); }
};

const getStatusText = (s) => ({0:'待付款',1:'待发货',2:'待收货',3:'已完成',4:'已取消'})[s] || '未知';
const getStatusType = (s) => ({0:'warning',1:'primary',2:'success',3:'info',4:'danger'})[s] || 'info';

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-';
  if (Array.isArray(dateStr)) {
    const [y,m,d,h,min] = dateStr;
    return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(h).padStart(2,'0')}:${String(min).padStart(2,'0')}`;
  }
  const d = new Date(dateStr);
  return isNaN(d.getTime()) ? '-' : d.toLocaleString('zh-CN');
};

const calculateTotalPrice = (order) => {
  if (!order) return '0.00';
  const val = order.totalPrice || order.payAmount;
  if (val) return typeof val === 'number' ? val.toFixed(2) : val;
  if (order.items?.length) {
    return order.items.reduce((s, i) => s + (i.price || 0) * (i.quantity || 0), 0).toFixed(2);
  }
  return '0.00';
};

onMounted(loadOrders);
</script>

<style scoped>
.order-page { animation: fadeIn 0.4s ease; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; }
.order-stats { display: flex; gap: 8px; }
.stat-badge { padding: 5px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.stat-badge.warning { background: #fef3c7; color: var(--color-primary-600); }
.stat-badge.success { background: #ecfdf5; color: #059669; }

.order-detail { padding: 4px 0; }
.detail-section { margin-bottom: 20px; }
.detail-section h4 {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid var(--border-light);
}
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 20px; }
.info-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.info-item.full-width { grid-column: 1 / -1; }
.info-label { color: var(--text-secondary); min-width: 56px; flex-shrink: 0; }

.product-table { width: 100%; border-collapse: collapse; }
.product-table th {
  padding: 10px 12px; text-align: left; font-size: 12px; font-weight: 600;
  color: var(--text-secondary); background: var(--border-light); border-radius: 6px 6px 0 0;
}
.product-table td { padding: 10px 12px; font-size: 13px; border-bottom: 1px solid var(--border-light); }
.product-table .subtotal { color: var(--color-error); font-weight: 600; }

.total-section {
  background: var(--border-light);
  border-radius: var(--radius);
  padding: 14px 18px;
}
.total-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 14px; color: var(--text-secondary); }
.total-price { font-size: 22px; font-weight: 700; color: var(--primary); }
.discount-row { color: #059669; }
.discount { font-weight: 600; }
.final-row { font-weight: 600; color: var(--text-primary); }
.final-price { font-size: 22px; font-weight: 700; color: var(--color-error); }
</style>
