<template>
  <div class="cart-container">
    <div class="page-header">
      <h2 class="page-title">
        购物车
        <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
      </h2>
      <router-link to="/product/list" class="continue-shopping-link">
        <span class="continue-icon">←</span> 继续购物
      </router-link>
    </div>

    <div v-if="cartItems.length === 0" class="empty-cart fade-in">
      <el-empty description="购物车是空的" :image-size="120" />
      <router-link to="/" class="shop-btn">
        <span class="shop-btn-icon">🛒</span> 去购物
      </router-link>
    </div>

    <div v-else class="cart-content">
      <div class="cart-items">
        <div class="select-all-bar">
          <label class="select-all-label">
            <input type="checkbox" class="select-all-checkbox" />
            <span>全选</span>
          </label>
        </div>
        <div class="cart-item" v-for="item in cartItems" :key="item.id">
          <img
            :src="item.cover || '/images/product-default.svg'"
            :alt="item.name"
            class="item-image"
            @error="handleImageError"
          />
          <div class="item-info">
            <h3 class="item-name">{{ item.name }}</h3>
            <p class="item-price">¥{{ item.price }}</p>
            <div class="item-quantity">
              <button @click="decreaseQuantity(item.id)">-</button>
              <span>{{ item.count }}</span>
              <button @click="increaseQuantity(item.id)">+</button>
            </div>
          </div>
          <div class="item-total">
            <div class="subtotal-block">
              <span class="subtotal-label">小计</span>
              <p class="total-price">¥{{ (item.price * item.count).toFixed(2) }}</p>
            </div>
            <button @click="removeItem(item.id)" class="remove-btn">删除</button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <div class="summary-row">
          <span>商品总数：</span>
          <span>{{ cartCount }} 件</span>
        </div>
        <div class="summary-row total">
          <span>合计：</span>
          <span class="total-amount">¥{{ cartTotal.toFixed(2) }}</span>
        </div>
        <div class="coupon-section" v-if="userCoupons.length > 0">
          <div class="coupon-label">可用优惠券：</div>
          <div class="coupon-select" v-for="uc in userCoupons" :key="uc.id"
            :class="{ selected: selectedCouponId === uc.id }"
            @click="selectedCouponId = selectedCouponId === uc.id ? null : uc.id">
            <span class="coupon-tag">{{ uc.couponType === 1 ? '满减' : '折扣' }}</span>
            <span class="coupon-name">{{ uc.couponName }}</span>
            <span v-if="selectedCouponId === uc.id" class="coupon-check">✓</span>
          </div>
        </div>
        <div class="summary-row discount-row" v-if="discountAmount > 0">
          <span>优惠券抵扣：</span>
          <span class="discount-amount">-¥{{ discountAmount.toFixed(2) }}</span>
        </div>
        <div class="summary-row total" v-if="discountAmount > 0">
          <span>应付金额：</span>
          <span class="final-amount">¥{{ (cartTotal - discountAmount).toFixed(2) }}</span>
        </div>
        <button
          @click="checkout"
          class="checkout-btn"
          :disabled="cartItems.length === 0"
          :loading="checkingOut"
        >
          <span v-if="checkingOut" class="checkout-loading">
            <span class="spinner"></span>
            结算中...
          </span>
          <span v-else>去结算</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cart';
import { useUserStore } from '@/stores/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const cartStore = useCartStore();
const userStore = useUserStore();
const checkingOut = ref(false);
const userCoupons = ref([]);
const selectedCouponId = ref(null);
const discountAmount = ref(0);

const cartItems = computed(() => cartStore.cartItems);
const cartCount = computed(() => cartStore.cartCount);
const cartTotal = computed(() => cartStore.cartTotal);
const user = computed(() => userStore.user);

const handleImageError = (e) => {
  e.target.src = '/images/product-default.svg';
};

const increaseQuantity = (id) => {
  const item = cartItems.value.find(i => i.id === id);
  if (item) {
    cartStore.updateItemCount(id, item.count + 1);
  }
};

const decreaseQuantity = (id) => {
  const item = cartItems.value.find(i => i.id === id);
  if (item && item.count > 1) {
    cartStore.updateItemCount(id, item.count - 1);
  }
};

const removeItem = async (id) => {
  try {
    const result = await ElMessageBox.confirm('确定要删除这个商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    if (result) {
      cartStore.removeFromCart(id);
      ElMessage.success({
        message: '删除成功',
        duration: 2000
      });
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败:', err);
      ElMessage.error({
        message: '删除失败，请稍后重试',
        duration: 2000
      });
    }
  }
};

const loadUserCoupons = async () => {
  if (!userStore.isLoggedIn || !user.value) return;
  try {
    const res = await api.get(`/coupon/user/${user.value.id}`);
    if (res.code === 0) {
      userCoupons.value = (res.data || []).filter(uc => uc.status === 0);
      if (userCoupons.value.length > 0 && selectedCouponId.value) {
        updateDiscount();
      }
    }
  } catch (e) {}
};

const updateDiscount = () => {
  if (!selectedCouponId.value || cartTotal.value <= 0) {
    discountAmount.value = 0;
    return;
  }
  const uc = userCoupons.value.find(c => c.id === selectedCouponId.value);
  if (!uc) { discountAmount.value = 0; return; }
  if (uc.couponType === 1) {
    if (cartTotal.value >= (uc.threshold || 0)) {
      discountAmount.value = parseFloat(uc.discountValue) || 0;
    } else {
      discountAmount.value = 0;
    }
  } else if (uc.couponType === 2) {
    discountAmount.value = cartTotal.value * (1 - (parseFloat(uc.discountValue) || 10) / 10);
    discountAmount.value = Math.floor(discountAmount.value * 100) / 100;
  }
};

const checkout = async () => {
  if (cartItems.value.length === 0) {
    ElMessage.warning({
      message: '购物车是空的',
      duration: 2000
    });
    return;
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning({
      message: '请先登录',
      duration: 2000
    });
    router.push('/login');
    return;
  }

  if (checkingOut.value) {
    return;
  }

  try {
    checkingOut.value = true;

    const items = cartItems.value.map(item => ({
      productId: item.id,
      quantity: item.count,
      price: item.price,
      specId: item.specId || null,
      cover: item.cover
    }));

    let addressId = null;
    try {
      const addressResponse = await api.get('/user/address/list?userId=' + user.value.id);
      if (addressResponse.code === 0 && addressResponse.data && addressResponse.data.length > 0) {
        const defaultAddress = addressResponse.data.find(addr => addr.isDefault === 1) || addressResponse.data[0];
        addressId = defaultAddress.id;
        console.log('使用地址 ID:', addressId, '地址:', defaultAddress);
      }
    } catch (error) {
      console.warn('获取地址列表失败，将继续创建订单:', error);
    }

    const orderData = {
      userId: user.value.id,
      openid: user.value.openid,
      addressId: addressId,
      items: items
    };
    if (selectedCouponId.value) {
      orderData.userCouponId = selectedCouponId.value;
    }

    const response = await api.post('/order/create', orderData);

    if (response.code === 0) {
      ElMessage.success({
        message: '订单创建成功，正在跳转支付页面...',
        duration: 2000
      });
      cartStore.clearCart();
      setTimeout(() => {
        router.push(`/payment/${response.data.id}`);
      }, 1000);
    } else {
      if (response.message && (response.message.includes('Product not found') || response.message.includes('商品不存在'))) {
        ElMessage.error({
          message: '购物车中的商品信息已过期或不存在，请清空购物车后重新添加商品',
          duration: 4000
        });
      } else if (response.message && response.message.includes('stock')) {
        ElMessage.error({
          message: '商品库存不足，请调整购买数量',
          duration: 3000
        });
      } else {
        ElMessage.error({
          message: response.message || '创建订单失败',
          duration: 3000
        });
      }
    }
  } catch (error) {
    console.error('创建订单失败:', error);
    const errorMsg = error.message || error.response?.data?.message || '';
    const isProductNotFoundError =
      errorMsg.includes('Product not found') ||
      errorMsg.includes('商品不存在') ||
      errorMsg.includes('系统错误') ||
      errorMsg.includes('Transaction rolled back') ||
      errorMsg.includes('rollback-only');

    if (isProductNotFoundError) {
      ElMessageBox.confirm(
        '购物车中的商品信息已过期或不存在，请清空购物车后重新添加商品',
        '创建订单失败',
        {
          confirmButtonText: '清空购物车',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        cartStore.clearCart();
        ElMessage.success('已清空购物车');
      });
    } else if (errorMsg.includes('stock') || errorMsg.includes('库存')) {
      ElMessage.error({
        message: '商品库存不足，请调整购买数量',
        duration: 3000
      });
    } else {
      ElMessage.error({
        message: error.response?.data?.message || '创建订单失败，请稍后重试',
        duration: 3000
      });
    }
  } finally {
    checkingOut.value = false;
  }
};

watch(selectedCouponId, () => updateDiscount());
watch(cartTotal, () => updateDiscount());
onMounted(() => loadUserCoupons());
</script>

<style scoped>
.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-8);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.cart-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: #fff;
  background: var(--warm-orange);
  border-radius: 12px;
  line-height: 1;
}

.continue-shopping-link {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  color: var(--color-primary-500);
  font-size: var(--font-size-base);
  font-weight: 500;
  text-decoration: none;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-primary-200);
  transition: all var(--transition-base);
}

.continue-shopping-link:hover {
  background: rgba(var(--color-primary-500-rgb), 0.06);
  border-color: var(--color-primary-500);
}

.continue-icon {
  font-size: var(--font-size-lg);
  line-height: 1;
}

.empty-cart {
  text-align: center;
  padding: var(--spacing-20) var(--spacing-5);
}

.fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.shop-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  padding: var(--spacing-5) var(--spacing-12);
  background: var(--gradient-brand);
  color: white;
  border-radius: var(--radius-lg);
  text-decoration: none;
  font-size: var(--font-size-lg);
  font-weight: 700;
  box-shadow: var(--shadow-glow-md);
  transition: all var(--transition-spring);
  letter-spacing: 1px;
}

.shop-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: var(--shadow-glow-lg);
}

.shop-btn:active {
  transform: translateY(-1px) scale(0.98);
}

.shop-btn-icon {
  font-size: var(--font-size-xl);
}

.cart-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: var(--spacing-8);
}

.select-all-bar {
  display: flex;
  align-items: center;
  padding: var(--spacing-3) var(--spacing-4);
  margin-bottom: var(--spacing-4);
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.select-all-label {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  user-select: none;
}

.select-all-checkbox {
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary-500);
  cursor: pointer;
}

.cart-item {
  display: flex;
  gap: var(--spacing-5);
  padding: var(--spacing-5);
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--spacing-5);
  transition: all var(--transition-smooth);
}
.cart-item:hover {
  box-shadow: var(--shadow-glow-sm);
  border-color: var(--color-primary-200);
}

.item-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: var(--radius-md);
  background: var(--bg-1);
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-3);
}

.item-price {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--warm-orange);
  margin-bottom: var(--spacing-4);
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  background: var(--bg-1);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
  width: fit-content;
}

.item-quantity button {
  width: var(--spacing-7);
  height: var(--spacing-7);
  border: none;
  background: var(--card-bg);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  font-weight: bold;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-xs);
}

.item-quantity button:hover {
  background: var(--gradient-brand);
  color: white;
}

.item-quantity button:active {
  transform: scale(0.95);
}

.item-quantity span {
  font-size: var(--font-size-sm);
  font-weight: 600;
  min-width: var(--spacing-8);
  text-align: center;
  color: var(--text-primary);
}

.item-total {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
}

.subtotal-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--spacing-1);
}

.subtotal-label {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  font-weight: 500;
}

.total-price {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--warm-orange);
}

.remove-btn {
  padding: var(--spacing-2) var(--spacing-5);
  background: linear-gradient(135deg, var(--color-error), var(--color-error-dark));
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-glow-sm);
}

.remove-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(var(--color-error-rgb), 0.4);
}

.remove-btn:active {
  transform: translateY(0);
}

.cart-summary {
  background: var(--card-bg);
  padding: var(--spacing-8);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  height: fit-content;
  position: sticky;
  top: var(--spacing-5);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}

.summary-row.total {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  border-top: 1px solid var(--border-light);
  padding-top: var(--spacing-4);
}

.total-amount {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--warm-orange);
}

.coupon-section {
  margin: var(--spacing-3) 0;
  padding: var(--spacing-3) 0;
  border-top: 1px dashed var(--border-default);
  border-bottom: 1px dashed var(--border-default);
}

.coupon-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-2);
}

.coupon-select {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-3);
  margin-bottom: var(--spacing-2);
  border: 2px solid var(--border-default);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-xs);
}

.coupon-select:hover {
  border-color: var(--color-primary-500);
}

.coupon-select.selected {
  border-color: var(--color-primary-500);
  background: rgba(var(--color-primary-500-rgb), 0.06);
}

.coupon-tag {
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  font-size: var(--font-size-xs);
  font-weight: 600;
  background: var(--gradient-brand);
  color: #fff;
  flex-shrink: 0;
}

.coupon-select .coupon-name {
  flex: 1;
  color: var(--text-primary);
}

.coupon-check {
  color: var(--color-primary-500);
  font-weight: 700;
}

.discount-row {
  color: var(--color-success);
}

.discount-amount {
  font-weight: 600;
  color: var(--color-success);
}

.final-amount {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--warm-orange);
}

.checkout-btn {
  width: 100%;
  padding: var(--spacing-4);
  background: var(--gradient-brand);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-md);
  font-weight: 600;
  cursor: pointer;
  margin-top: var(--spacing-5);
  transition: all var(--transition-spring);
  box-shadow: var(--shadow-glow-md);
  position: relative;
  overflow: hidden;
  min-height: 48px;
}

.checkout-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow-lg);
}

.checkout-btn:active:not(:disabled) {
  transform: translateY(0);
}

.checkout-btn:disabled {
  background: linear-gradient(135deg, #ccc 0%, #ddd 100%);
  cursor: not-allowed;
  box-shadow: none;
}

.checkout-btn::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(45deg);
  transition: all 0.3s;
}

.checkout-btn:hover::after {
  left: 100%;
}

.checkout-loading {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
}

.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .cart-content {
    grid-template-columns: 1fr;
  }

  .cart-item {
    flex-direction: column;
  }

  .item-image {
    width: 100%;
    height: 200px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }
}
</style>
