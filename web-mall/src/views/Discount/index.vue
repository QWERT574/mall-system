<template>
  <div class="discount-page">
    <div class="page-header">
      <h2>🏷️ 优惠活动</h2>
      <p>限时折扣、秒杀、满减，超值好物等你来</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="activities.length === 0" class="empty">
      <el-empty description="暂无进行中的优惠活动，敬请期待" />
    </div>
    <div v-else class="activity-list">
      <div class="activity-card" v-for="act in activities" :key="act.id" @click="goDetail(act.id)">
        <div class="act-header">
          <span class="act-badge" :class="'type-'+act.type">
            {{ act.type === 1 ? '满减' : act.type === 2 ? '限时折扣' : '秒杀' }}
          </span>
          <h3 class="act-name">{{ act.name }}</h3>
          <span class="act-countdown" :class="{ urgent: act.urgent }">{{ formatCountdown(act.endTime) }}</span>
        </div>
        <p class="act-desc" v-if="act.description">{{ act.description }}</p>
        <div class="act-rule" v-if="act.type === 1 && act.threshold">
          满 <strong>¥{{ act.threshold }}</strong> 减 <strong>¥{{ act.reduceAmount }}</strong>
        </div>
        <div class="act-rule" v-if="act.type === 2 && act.discountRate">
          全场 <strong>{{ act.discountRate }}折</strong>
        </div>
        <div class="act-products" v-if="act.products && act.products.length > 0">
          <div class="ap-item" v-for="dp in act.products" :key="dp.productId" @click.stop="$router.push(`/product/${dp.productId}`)">
            <div class="ap-left">
              <span class="ap-name">{{ dp.productName }}</span>
            </div>
            <div class="ap-right">
              <span class="ap-original">¥{{ dp.originalPrice }}</span>
              <span class="ap-discount">¥{{ dp.discountPrice }}</span>
              <span class="ap-save">省{{ (dp.originalPrice - dp.discountPrice).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <div class="act-footer">
          <span>{{ act.products?.length || 0 }} 件商品参与</span>
          <span class="act-link">查看详情 →</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';

const router = useRouter();
const activities = ref([]);
const loading = ref(true);

const formatCountdown = (time) => {
  if (!time) return '';
  const end = new Date(time);
  const now = new Date();
  const diff = end - now;
  if (diff < 0) return '已结束';
  const days = Math.floor(diff / 86400000);
  const h = Math.floor((diff % 86400000) / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  if (days > 0) return `${days}天${h}时${m}分`;
  return `${h}时${m}分`;
};

const loadActivities = async () => {
  try {
    const res = await api.get('/discount/active-with-products');
    if (res.code === 0 && res.data) {
      const list = (res.data || []).map(act => {
        const end = new Date(act.endTime);
        const now = new Date();
        act.urgent = (end - now) < 3600000;
        return act;
      });
      // N+1 补全：活动商品中 productName/originalPrice 是后端非 DB 字段，
      // 这里通过 /product/{id} 接口批量拉取商品详情补全，避免依赖后端改造
      const productIdSet = new Set();
      list.forEach(act => {
        (act.products || []).forEach(dp => {
          if (dp.productId) productIdSet.add(dp.productId);
        });
      });
      const productInfoMap = {};
      await Promise.all(
        Array.from(productIdSet).map(pid =>
          api.get(`/product/${pid}`)
            .then(pRes => {
              if (pRes.code === 0 && pRes.data) {
                productInfoMap[pid] = pRes.data;
              }
            })
            .catch(() => { /* 单个商品拉取失败不影响整体 */ })
        )
      );
      list.forEach(act => {
        (act.products || []).forEach(dp => {
          const p = productInfoMap[dp.productId];
          if (p) {
            if (!dp.productName) dp.productName = p.name;
            if (dp.originalPrice == null && p.price != null) dp.originalPrice = p.price;
          }
        });
      });
      activities.value = list;
    }
  } catch (e) {
    console.error('加载活动失败:', e);
  } finally {
    loading.value = false;
  }
};

const goDetail = (id) => router.push(`/discount/${id}`);

onMounted(loadActivities);
</script>

<style scoped>
.discount-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}
.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}
.page-header p {
  font-size: 14px;
  color: #9ca3af;
  margin-top: 8px;
}

.loading, .empty { text-align: center; padding: 80px 0; }

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border: 1px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.25s ease;
}
.activity-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border-color: var(--color-primary-600);
}

.act-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.act-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.act-badge.type-1 { background: linear-gradient(135deg, var(--color-warning), var(--color-primary-600)); }
.act-badge.type-2 { background: linear-gradient(135deg, var(--color-error), #dc2626); }
.act-badge.type-3 { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }

.act-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  flex: 1;
}

.act-countdown {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-600);
  background: #fef3c7;
  padding: 4px 12px;
  border-radius: 20px;
}
.act-countdown.urgent {
  color: var(--color-error);
  background: #fef2f2;
  animation: pulse 1s infinite;
}

.act-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 10px;
}

.act-rule {
  font-size: 15px;
  color: #1f2937;
  margin-bottom: 14px;
  padding: 8px 14px;
  background: #fffbeb;
  border-radius: 8px;
  border-left: 3px solid var(--color-warning);
}
.act-rule strong { color: var(--color-primary-600); }

.act-products {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f9fafb;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
}

.ap-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;
}
.ap-item:last-child { border-bottom: none; }
.ap-item:hover { background: #fef3c7; border-radius: 4px; padding-left: 6px; padding-right: 6px; }

.ap-name {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.ap-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.ap-original {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: line-through;
}

.ap-discount {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-error);
}

.ap-save {
  font-size: 11px;
  color: #fff;
  background: var(--color-error);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.act-footer {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #9ca3af;
}

.act-link {
  color: var(--color-primary-600);
  font-weight: 600;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 768px) {
  .discount-page { padding: 12px; }
  .page-header h2 { font-size: 22px; }
  .activity-card { padding: 16px; }
  .act-name { font-size: 16px; }
  .ap-right { gap: 6px; }
  .ap-discount { font-size: 14px; }
}

@media (max-width: 480px) {
  .discount-page { padding: 8px; }
  .page-header h2 { font-size: 20px; }
  .page-header p { font-size: 13px; }
  .activity-card { padding: 12px; }
  .act-header { gap: 8px; }
  .act-name { font-size: 15px; }
  .act-countdown { font-size: 12px; padding: 3px 8px; }
  .act-desc { font-size: 13px; }
  .act-rule { font-size: 14px; padding: 6px 10px; }
  .act-rule strong { font-size: 16px; }
  .ap-item { flex-direction: column; align-items: flex-start; gap: 6px; }
  .ap-right { flex-wrap: wrap; }
  .act-footer { font-size: 12px; }
}
</style>
