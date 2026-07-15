<template>
  <div class="activity-detail-page" v-if="activity">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回</button>
      <span class="act-badge" :class="'type-'+activity.type">
        {{ activity.type === 1 ? '满减' : activity.type === 2 ? '限时折扣' : '秒杀' }}
      </span>
      <h2>{{ activity.name }}</h2>
      <p class="act-desc" v-if="activity.description">{{ activity.description }}</p>
      <div class="countdown-bar" v-if="countdownText">
        <span class="countdown-icon">⏰</span>
        <span :class="{ urgent: isUrgent }">{{ countdownText }}</span>
      </div>
    </div>

    <div class="rule-card" v-if="activity.type === 1">
      <div class="rule-icon">🏷️</div>
      <div class="rule-content">
        <span>满 <strong>¥{{ activity.threshold }}</strong> 减 <strong>¥{{ activity.reduceAmount }}</strong></span>
      </div>
    </div>
    <div class="rule-card" v-if="activity.type === 2">
      <div class="rule-icon">🏷️</div>
      <div class="rule-content">
        <span>全场 <strong>{{ activity.discountRate }}折</strong></span>
      </div>
    </div>

    <div class="products-section">
      <h3>活动商品 ({{ products.length }})</h3>
      <div class="product-list">
        <div class="product-item" v-for="dp in products" :key="dp.productId" @click="$router.push(`/product/${dp.productId}`)">
          <div class="p-info">
            <span class="p-name">{{ dp.productName }}</span>
          </div>
          <div class="p-price">
            <span class="p-original">¥{{ dp.originalPrice }}</span>
            <span class="p-discount">¥{{ dp.discountPrice }}</span>
          </div>
          <button class="buy-btn">立即抢购</button>
        </div>
      </div>
    </div>

    <div class="coupon-section" v-if="coupons.length > 0">
      <h3>可领优惠券</h3>
      <div class="coupon-list">
        <div class="coupon-item" v-for="c in coupons" :key="c.id">
          <div class="c-left">
            <span class="c-value" v-if="c.type === 1">¥{{ c.discountValue }}</span>
            <span class="c-value" v-else>{{ c.discountValue }}折</span>
            <span class="c-condition" v-if="c.type === 1 && c.threshold > 0">满{{ c.threshold }}元</span>
          </div>
          <div class="c-right">
            <span class="c-name">{{ c.name }}</span>
            <button class="c-claim" @click.stop="claimCoupon(c)">立即领取</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import api from '@/utils/api';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const activity = ref(null);
const products = ref([]);
const coupons = ref([]);
const countdownText = ref('');
const isUrgent = ref(false);
let timer = null;

const loadActivity = async () => {
  try {
    const id = route.params.id;
    // 改用 /active-with-products 端点：和小程序、列表页用同一数据源，
    // 避免 /discount/{id}/products 端点行为不一致导致空列表
    const res = await api.get('/discount/active-with-products');
    if (res.code === 0 && res.data) {
      const found = (res.data || []).find(act => String(act.id) === String(id));
      if (found) {
        activity.value = found;
        // N+1 补全：商品名称/原价在响应里可能是 null（后端 SQL LEFT JOIN 的产物），
        // 用 /product/{id} 单独拉一次补全
        const list = found.products || [];
        const productInfoMap = {};
        await Promise.all(
          list
            .filter(dp => dp.productId)
            .map(dp => api.get(`/product/${dp.productId}`)
              .then(pRes => {
                if (pRes.code === 0 && pRes.data) {
                  productInfoMap[dp.productId] = pRes.data;
                }
              })
              .catch(() => { /* 单个失败不影响整体 */ })
            )
        );
        list.forEach(dp => {
          const p = productInfoMap[dp.productId];
          if (p) {
            if (!dp.productName) dp.productName = p.name;
            if (dp.originalPrice == null && p.price != null) dp.originalPrice = p.price;
          }
        });
        products.value = list;
      } else {
        // 兜底：活动不在 active 列表中（比如已结束但 status 还是 1），
        // 退回到单独 /discount/{id} 端点拿基础信息
        const single = await api.get(`/discount/${id}`);
        if (single.code === 0) activity.value = single.data;
        console.warn('活动未在 active-with-products 列表中，可能已结束');
      }
    }
    const cRes = await api.get('/coupon/available?page=1&size=3');
    if (cRes.code === 0) coupons.value = cRes.data?.records || [];
    updateCountdown();
    timer = setInterval(updateCountdown, 30000);
  } catch (e) {
    console.error('加载活动失败:', e);
  }
};

const updateCountdown = () => {
  if (!activity.value?.endTime) return;
  const diff = new Date(activity.value.endTime) - new Date();
  if (diff <= 0) { countdownText.value = '已结束'; return; }
  const d = Math.floor(diff / 86400000);
  const h = Math.floor((diff % 86400000) / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  if (d > 0) countdownText.value = `距结束 ${d}天${h}小时${m}分钟`;
  else countdownText.value = `距结束 ${h}小时${m}分钟`;
  isUrgent.value = diff < 3600000;
};

const claimCoupon = async (coupon) => {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return; }
  try {
    const res = await api.post(`/coupon/claim/${coupon.id}?userId=${userStore.user.id}`);
    if (res.code === 0) ElMessage.success('领取成功！');
    else ElMessage.error(res.message || '领取失败');
  } catch (e) { ElMessage.error('领取失败'); }
};

onMounted(loadActivity);
</script>

<style scoped>
.activity-detail-page { max-width: 760px; margin: 0 auto; padding: 20px; }
.loading { text-align: center; padding: 80px 0; }

.page-header { text-align: center; margin-bottom: 28px; }
.back-btn { display: block; background: none; border: none; font-size: 14px; color: var(--color-primary-600); cursor: pointer; margin-bottom: 16px; text-align: left; }
.act-badge { padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; color: #fff; }
.act-badge.type-1 { background: linear-gradient(135deg, var(--color-warning), var(--color-primary-600)); }
.act-badge.type-2 { background: linear-gradient(135deg, var(--color-error), #dc2626); }
.act-badge.type-3 { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
.page-header h2 { font-size: 24px; font-weight: 700; color: #1f2937; margin: 12px 0 6px; }
.act-desc { color: #6b7280; font-size: 14px; margin: 0; }

.countdown-bar { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 12px; font-size: 15px; font-weight: 600; color: var(--color-primary-600); background: #fef3c7; padding: 8px 16px; border-radius: 10px; width: fit-content; margin-left: auto; margin-right: auto; }
.countdown-bar .urgent { color: var(--color-error); animation: pulse 0.5s infinite; }

.rule-card { display: flex; align-items: center; gap: 12px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 12px; padding: 16px 20px; margin-bottom: 20px; }
.rule-icon { font-size: 32px; }
.rule-content { font-size: 16px; color: #1f2937; }
.rule-content strong { color: var(--color-primary-600); font-size: 20px; }

.products-section { margin-bottom: 28px; }
.products-section h3, .coupon-section h3 { font-size: 17px; font-weight: 700; color: #1f2937; margin-bottom: 14px; }

.product-list { display: flex; flex-direction: column; gap: 8px; }
.product-item { display: flex; align-items: center; gap: 16px; padding: 14px 16px; background: #fff; border-radius: 10px; border: 1px solid #f3f4f6; cursor: pointer; transition: all 0.2s; }
.product-item:hover { border-color: var(--color-primary-600); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.p-info { flex: 1; }
.p-name { font-size: 14px; font-weight: 500; color: #1f2937; }
.p-price { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.p-original { font-size: 13px; color: #9ca3af; text-decoration: line-through; }
.p-discount { font-size: 18px; font-weight: 700; color: var(--color-error); }
.buy-btn { padding: 8px 18px; border: none; border-radius: 20px; background: linear-gradient(135deg, var(--color-error), #dc2626); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; flex-shrink: 0; }
.buy-btn:hover { opacity: 0.9; }

.coupon-list { display: flex; flex-direction: column; gap: 10px; }
.coupon-item { display: flex; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.c-left { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 14px 20px; background: linear-gradient(135deg, var(--color-error), #dc2626); color: #fff; min-width: 90px; }
.c-value { font-size: 22px; font-weight: 800; }
.c-condition { font-size: 11px; opacity: 0.8; margin-top: 2px; }
.c-right { flex: 1; display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; background: #fff; }
.c-name { font-size: 14px; font-weight: 600; color: #1f2937; }
.c-claim { padding: 6px 16px; border: 1px solid var(--color-error); border-radius: 20px; background: #fff; color: var(--color-error); font-size: 12px; font-weight: 600; cursor: pointer; }
.c-claim:hover { background: #fef2f2; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

@media (max-width: 768px) {
  .activity-detail-page { padding: 12px; }
  .page-header h2 { font-size: 20px; }
  .rule-card { flex-direction: column; text-align: center; gap: 8px; }
  .product-item { flex-direction: column; align-items: flex-start; gap: 10px; }
  .p-price { width: 100%; justify-content: flex-start; }
  .buy-btn { align-self: flex-start; }
  .coupon-item { flex-direction: column; }
  .c-left { flex-direction: row; min-width: auto; padding: 10px 16px; gap: 8px; }
  .c-right { flex-direction: column; gap: 8px; align-items: flex-start; }
}

@media (max-width: 480px) {
  .activity-detail-page { padding: 8px; }
  .page-header h2 { font-size: 18px; }
  .act-badge { font-size: 11px; padding: 3px 10px; }
  .rule-card { padding: 12px; }
  .rule-icon { font-size: 26px; }
  .rule-content { font-size: 14px; }
  .rule-content strong { font-size: 17px; }
  .countdown-bar { font-size: 13px; padding: 6px 12px; }
  .products-section h3, .coupon-section h3 { font-size: 15px; }
  .product-item { padding: 10px 12px; }
  .p-name { font-size: 13px; }
  .p-discount { font-size: 16px; }
  .buy-btn { padding: 6px 14px; font-size: 12px; }
  .c-value { font-size: 18px; }
  .c-name { font-size: 13px; }
}
</style>
