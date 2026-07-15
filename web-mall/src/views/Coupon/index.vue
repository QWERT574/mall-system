<template>
  <div class="coupon-page ml-page-enter">
    <h2 class="page-title">优惠券中心</h2>

    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="filteredCoupons.length === 0" class="empty-state">
      <svg class="empty-illustration" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="50" y="60" width="100" height="80" rx="6" stroke="var(--color-gray-300)" stroke-width="2" fill="var(--color-gray-50)"/>
        <path d="M50 80 L100 110 L150 80" stroke="var(--color-gray-300)" stroke-width="2" fill="none"/>
        <circle cx="100" cy="45" r="15" stroke="var(--color-gray-300)" stroke-width="2" fill="var(--color-gray-50)"/>
        <text x="100" y="50" text-anchor="middle" font-size="14" fill="var(--color-primary-500)">%</text>
      </svg>
      <p class="empty-text">暂无优惠券</p>
    </div>

    <div v-else class="coupon-list">
      <div
        v-for="coupon in filteredCoupons"
        :key="coupon.id"
        :class="['coupon-card', { 'coupon-used': coupon.status === 1, 'coupon-expired': coupon.status === 2 }]"
      >
        <div class="coupon-left">
          <div class="coupon-amount">
            <span class="amount-symbol">¥</span>
            <span class="amount-value">{{ coupon.discount || coupon.amount || 0 }}</span>
          </div>
          <div class="coupon-condition">满{{ coupon.minAmount || coupon.minPoint || 0 }}可用</div>
        </div>

        <div class="coupon-divider">
          <div class="divider-notch top"></div>
          <div class="divider-line"></div>
          <div class="divider-notch bottom"></div>
        </div>

        <div class="coupon-right">
          <div class="coupon-info">
            <h3 class="coupon-name">{{ coupon.name || coupon.title || '优惠券' }}</h3>
            <p class="coupon-desc">{{ coupon.description || coupon.desc || '' }}</p>
            <p class="coupon-time">{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</p>
          </div>
          <div class="coupon-action">
            <button
              v-if="coupon.status === 0"
              :class="['claim-btn', { 'claiming': claimingId === coupon.id }]"
              @click="claimCoupon(coupon)"
              :disabled="claimingId === coupon.id"
            >
              <span v-if="claimingId === coupon.id" class="claim-spinner"></span>
              {{ claimingId === coupon.id ? '领取中' : '立即领取' }}
            </button>
            <span v-else-if="coupon.status === 1" class="coupon-status-tag used">已使用</span>
            <span v-else-if="coupon.status === 2" class="coupon-status-tag expired">已过期</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';

const coupons = ref([]);
const loading = ref(false);
const activeTab = ref('available');
const claimingId = ref(null);

const tabs = [
  { label: '可领取', value: 'available' },
  { label: '已领取', value: 'claimed' },
  { label: '已使用', value: 'used' },
  { label: '已过期', value: 'expired' }
];

const filteredCoupons = computed(() => {
  switch (activeTab.value) {
    case 'available': return coupons.value.filter(c => c.status === 0);
    case 'claimed': return coupons.value.filter(c => c.status === 0 && c.claimed);
    case 'used': return coupons.value.filter(c => c.status === 1);
    case 'expired': return coupons.value.filter(c => c.status === 2);
    default: return coupons.value;
  }
});

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
};

const claimCoupon = async (coupon) => {
  if (claimingId.value) return;
  claimingId.value = coupon.id;
  try {
    const res = await api.post(`/coupon/claim/${coupon.id}`);
    if (res.code === 0) {
      ElMessage.success('领取成功');
      coupon.claimed = true;
    } else {
      ElMessage.error(res.message || '领取失败');
    }
  } catch (e) {
    ElMessage.error('领取失败，请稍后重试');
  } finally {
    claimingId.value = null;
  }
};

const loadCoupons = async () => {
  loading.value = true;
  try {
    const res = await api.get('/coupon/list');
    coupons.value = res.data || res.list || res || [];
  } catch (e) {
    console.error('加载优惠券失败:', e);
    coupons.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadCoupons();
});
</script>

<style scoped>
.coupon-page { max-width: 960px; margin: 0 auto; padding: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: var(--text-primary); margin: 0 0 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-gray-200); }

.tab-bar { display: flex; gap: 0; margin-bottom: 20px; background: var(--color-gray-100); border-radius: var(--radius-lg); padding: 4px; }
.tab { flex: 1; padding: 10px 16px; border: none; background: transparent; font-size: 14px; font-weight: 500; color: var(--text-secondary); cursor: pointer; border-radius: var(--radius-md); transition: all var(--transition-fast); }
.tab.active { background: var(--card-bg); color: var(--color-primary-500); box-shadow: var(--shadow-sm); font-weight: 600; }
.tab:hover:not(.active) { color: var(--text-primary); }

.loading-state { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 20px; color: var(--text-tertiary); font-size: 14px; }
.loading-spinner { width: 32px; height: 32px; border: 3px solid var(--color-gray-200); border-top-color: var(--accent); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-illustration { width: 140px; height: 140px; margin: 0 auto 16px; }
.empty-text { font-size: 16px; color: var(--text-tertiary); margin: 0; }

.coupon-list { display: flex; flex-direction: column; gap: 14px; }

.coupon-card {
  display: flex; border-radius: var(--radius-lg); overflow: hidden;
  background: var(--card-bg); border: 1px solid var(--color-gray-200);
  transition: all var(--transition-base); position: relative;
  box-shadow: var(--shadow-sm);
}
.coupon-card:hover { box-shadow: var(--shadow-glow-sm); transform: translateY(-2px); }
.coupon-card.coupon-used, .coupon-card.coupon-expired { opacity: 0.55; }
.coupon-card.coupon-used:hover, .coupon-card.coupon-expired:hover { transform: none; box-shadow: var(--shadow-sm); }

.coupon-left {
  width: 140px; flex-shrink: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 20px 16px;
  background: linear-gradient(135deg, var(--warm-orange), var(--color-error));
  color: var(--color-text-inverse); position: relative;
}
.coupon-card.coupon-used .coupon-left,
.coupon-card.coupon-expired .coupon-left {
  background: linear-gradient(135deg, var(--color-gray-400), var(--color-gray-500));
}

.coupon-amount { display: flex; align-items: baseline; gap: 2px; }
.amount-symbol { font-size: 16px; font-weight: 600; opacity: 0.9; }
.amount-value { font-size: 36px; font-weight: 700; line-height: 1; letter-spacing: -1px; }
.coupon-condition { font-size: 12px; margin-top: 6px; opacity: 0.85; white-space: nowrap; }

.coupon-divider { width: 1px; position: relative; display: flex; flex-direction: column; align-items: center; }
.divider-line { flex: 1; width: 0; border-left: 2px dashed rgba(var(--color-primary-500-rgb), 0.15); }
.divider-notch { width: 16px; height: 8px; flex-shrink: 0; }
.divider-notch.top { border-radius: 0 0 8px 8px; background: var(--color-gray-100); }
.divider-notch.bottom { border-radius: 8px 8px 0 0; background: var(--color-gray-100); }

.coupon-right { flex: 1; display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; gap: 16px; }
.coupon-info { flex: 1; min-width: 0; }
.coupon-name { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0 0 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.coupon-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.coupon-time { font-size: 12px; color: var(--text-tertiary); margin: 0; }

.coupon-action { flex-shrink: 0; }
.claim-btn {
  padding: 8px 20px; border: none; border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--warm-orange), var(--color-error));
  color: var(--color-text-inverse); font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all var(--transition-fast); white-space: nowrap;
  display: inline-flex; align-items: center; gap: 6px;
}
.claim-btn:hover:not(:disabled) { transform: scale(1.05); box-shadow: 0 4px 12px rgba(var(--color-error-rgb), 0.3); }
.claim-btn:disabled { opacity: 0.7; cursor: not-allowed; transform: none; }
.claim-btn.claiming { padding: 8px 16px; }

.claim-spinner {
  width: 14px; height: 14px; border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--color-text-inverse); border-radius: 50%;
  animation: spin 0.7s linear infinite; display: inline-block;
}

.coupon-status-tag {
  display: inline-block; padding: 4px 14px; border-radius: var(--radius-full);
  font-size: 12px; font-weight: 500;
}
.coupon-status-tag.used { color: var(--text-tertiary); background: var(--color-gray-100); border: 1px solid var(--color-gray-300); }
.coupon-status-tag.expired { color: var(--text-tertiary); background: var(--color-gray-100); border: 1px solid var(--color-gray-300); }

@media (max-width: 480px) {
  .coupon-left { width: 110px; padding: 16px 10px; }
  .amount-value { font-size: 28px; }
  .coupon-right { padding: 12px 14px; }
  .claim-btn { padding: 6px 14px; font-size: 12px; }
}
</style>
