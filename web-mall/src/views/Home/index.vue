<template>
  <div class="home-container ml-page-enter">
    <div class="hero-banner" v-if="bannerList.length > 0">
      <div class="banner-slider">
        <div
          class="banner-slide"
          v-for="(banner, index) in bannerList"
          :key="index"
          :class="{ active: currentBanner === index }"
        >
          <div
            class="banner-bg"
            :class="{ 'banner-bg-fallback': banner.imageError }"
            :style="banner.imageError ? {} : { backgroundImage: `url(${banner.image})` }"
          ></div>
          <img :src="banner.image" class="banner-preload" @error="handleBannerError(index)" />
          <div class="banner-overlay"></div>
          <div class="banner-content animate-fade-in-up">
            <div class="banner-badge">{{ banner.badge || '精选推荐' }}</div>
            <h1 class="banner-title">{{ banner.title }}</h1>
            <p class="banner-desc">{{ banner.description }}</p>
            <button class="banner-cta ml-btn-press" @click="$router.push('/product/list')">
              立即探索
              <span class="cta-arrow">→</span>
            </button>
          </div>
        </div>
      </div>

      <div class="banner-controls">
        <button
          v-for="(banner, index) in bannerList"
          :key="index"
          :class="['banner-dot', { active: currentBanner === index }]"
          @click="currentBanner = index"
        >
          <span class="dot-progress" v-if="currentBanner === index"></span>
        </button>
      </div>

      <div class="banner-nav">
        <button class="nav-btn prev" @click="prevBanner">
          <span>←</span>
        </button>
        <button class="nav-btn next" @click="nextBanner">
          <span>→</span>
        </button>
      </div>
    </div>

    <div class="quick-entry animate-fade-in-up delay-200">
      <div class="entry-item ml-card-lift" @click="$router.push('/product/list')">
        <div class="entry-icon">
          <span class="icon-emoji">🥬</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">全部商品</span>
          <span class="entry-desc">新鲜直达</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/activity/list')">
        <div class="entry-icon">
          <span class="icon-emoji">🌾</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">助农活动</span>
          <span class="entry-desc">爱心助农</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/order')">
        <div class="entry-icon">
          <span class="icon-emoji">📦</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">我的订单</span>
          <span class="entry-desc">物流追踪</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/cart')">
        <div class="entry-icon">
          <span class="icon-emoji">🛒</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">购物车</span>
          <span class="entry-desc">已选商品</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/coupons')">
        <div class="entry-icon">
          <span class="icon-emoji">🎫</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">领券中心</span>
          <span class="entry-desc">超值优惠</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/discount')">
        <div class="entry-icon">
          <span class="icon-emoji">🏷️</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">限时活动</span>
          <span class="entry-desc">折扣秒杀</span>
        </div>
      </div>
      <div class="entry-item ml-card-lift" @click="$router.push('/profile')">
        <div class="entry-icon">
          <span class="icon-emoji">👤</span>
        </div>
        <div class="entry-text">
          <span class="entry-title">个人中心</span>
          <span class="entry-desc">账户管理</span>
        </div>
      </div>
    </div>

    <section id="activity-section" class="activity-section animate-fade-in-up delay-300">
      <div class="section-header">
        <div class="section-title-group">
          <h2 class="section-title">助农活动</h2>
          <p class="section-subtitle">携手农户，共创美好</p>
        </div>
        <button class="section-more ml-btn-press" @click="scrollAndNavigate('/activity/list', 'activity-section')">
          查看更多
          <span class="more-arrow">→</span>
        </button>
      </div>

      <div class="activity-grid" v-if="activities.length > 0">
        <div
          class="activity-card ml-card-lift"
          v-for="(activity, index) in activities"
          :key="activity.id"
          :style="{ animationDelay: `${index * 0.1}s` }"
          @click="goToActivity(activity.id)"
        >
          <div class="activity-image-wrapper ml-img-zoom">
            <img
              :src="activity.coverImage || '/images/activity-default.jpg'"
              :alt="activity.name"
              class="activity-image"
              @error="handleActivityImageError"
            />
            <div class="activity-overlay">
              <span class="activity-badge">助农活动</span>
            </div>
          </div>
          <div class="activity-info">
            <h3 class="activity-name">{{ activity.name }}</h3>
            <p class="activity-desc">{{ activity.description }}</p>
            <div class="activity-footer">
              <div class="activity-time">
                <span class="time-icon">📅</span>
                <span>{{ formatDate(activity.startTime) }}</span>
              </div>
              <div class="activity-participants">
                <span class="participants-icon">👥</span>
                <span>{{ activity.currentParticipants || 0 }}/{{ activity.maxParticipants || 0 }}人</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无活动" />
    </section>

    <section id="discount-section" class="discount-section animate-fade-in-up delay-350">
      <div class="section-header">
        <div class="section-title-group">
          <h2 class="section-title">优惠活动</h2>
          <p class="section-subtitle">折扣秒杀，限时抢购</p>
        </div>
        <button class="section-more ml-btn-press" @click="scrollAndNavigate('/discount', 'discount-section')">
          查看更多<span class="more-arrow">→</span>
        </button>
      </div>
      <div class="discount-grid" v-if="discountActivities.length > 0">
        <div class="discount-card ml-card-lift" v-for="act in discountActivities" :key="act.id" @click="$router.push(`/discount/${act.id}`)">
          <div class="discount-badge" :class="'type-'+act.type">
            {{ act.type === 1 ? '满减' : act.type === 2 ? '折扣' : '秒杀' }}
          </div>
          <h3 class="discount-name">{{ act.name }}</h3>
          <p class="discount-desc" v-if="act.description">{{ act.description }}</p>
          <div class="discount-products" v-if="act.products && act.products.length > 0">
            <div class="dp-item" v-for="dp in act.products.slice(0, 4)" :key="dp.productId">
              <span class="dp-name">{{ dp.productName }}</span>
              <span class="dp-prices">
                <span class="dp-original">¥{{ dp.originalPrice }}</span>
                <span class="dp-discount">¥{{ dp.discountPrice }}</span>
              </span>
            </div>
          </div>
          <div class="discount-footer">
            <span class="discount-time">{{ formatDiscountTime(act.endTime) }}</span>
            <span class="discount-arrow">立即抢购 →</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无优惠活动" />
    </section>

    <section id="product-section" class="product-section animate-fade-in-up delay-400">
      <div class="section-header">
        <div class="section-title-group">
          <h2 class="section-title">热门推荐</h2>
          <p class="section-subtitle">精选好物，品质保证</p>
        </div>
        <button class="section-more ml-btn-press" @click="scrollAndNavigate('/product/list', 'product-section')">
          查看更多
          <span class="more-arrow">→</span>
        </button>
      </div>

      <div class="product-grid" v-if="!productsLoading && products.length > 0">
        <div
          class="product-card ml-card-lift"
          v-for="(product, index) in products"
          :key="product.id"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="goToProduct(product.id)"
        >
          <div class="product-image-wrapper ml-img-zoom">
            <img
              :src="product.cover || '/images/product-default.svg'"
              :alt="product.name"
              class="product-image"
              loading="lazy"
              decoding="async"
              @error="handleImageError"
            />
            <div class="product-overlay">
              <span class="view-btn">查看详情</span>
            </div>
            <div class="product-badges">
              <span v-if="product.sales > 100" class="badge hot">热销</span>
              <span v-if="product.stock < 10" class="badge low-stock">库存紧张</span>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc">{{ product.description }}</p>
            <div class="product-footer">
              <div class="product-price">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ product.price }}</span>
              </div>
              <div class="product-sales">
                <span class="sales-icon">🔥</span>
                <span class="sales-text">已售{{ product.sales || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="product-skeleton" v-if="productsLoading">
        <div class="skeleton-card" v-for="i in 8" :key="i">
          <div class="skeleton-image"></div>
          <div class="skeleton-info">
            <div class="skeleton-line skeleton-line-long"></div>
            <div class="skeleton-line skeleton-line-short"></div>
            <div class="skeleton-line skeleton-line-medium"></div>
          </div>
        </div>
      </div>

      <el-empty v-if="!productsLoading && products.length === 0" description="暂无商品" />

      <div class="no-more-indicator" v-if="!productsLoading && products.length > 0">
        <span class="no-more-line"></span>
        <span class="no-more-text">已展示全部商品</span>
        <span class="no-more-line"></span>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';

const router = useRouter();

const bannerList = ref([
  {
    title: '乡村振兴',
    description: '优质农产品，新鲜直达您的餐桌',
    image: '/images/banner-farm.svg',
    badge: '品质之选',
    imageError: false
  },
  {
    title: '新鲜水果',
    description: '当季水果，产地直供，自然成熟',
    image: '/images/banner-fruit.svg',
    badge: '时令鲜果',
    imageError: false
  },
  {
    title: '有机蔬菜',
    description: '绿色健康，天然有机，安心之选',
    image: '/images/banner-vegetable.svg',
    badge: '有机认证',
    imageError: false
  }
]);

const currentBanner = ref(0);
const products = ref([]);
const activities = ref([]);
const discountActivities = ref([]);
const productsLoading = ref(false);
let bannerTimer = null;

const handleImageError = (e) => {
  if (!e.target.src.includes('product-default.svg')) {
    e.target.src = '/images/product-default.svg';
  }
};

const handleActivityImageError = (e) => {
  if (!e.target.src.includes('activity-default.jpg')) {
    e.target.src = '/images/activity-default.jpg';
  }
};

const handleBannerError = (index) => {
  bannerList.value[index].imageError = true;
};

const loadProducts = async () => {
  productsLoading.value = true;
  try {
    const response = await api.getProducts({ page: 1, pageSize: 12 });
    if (response.code === 0) {
      products.value = response.data.records || [];
    }
  } catch (error) {
    // silently handle
  } finally {
    productsLoading.value = false;
  }
};

const loadActivities = async () => {
  try {
    const response = await api.get('/activity/recommended', {
      params: { page: 1, size: 4 }
    });
    if (response.code === 0 || response.code === 200) {
      activities.value = response.data.records || [];
    }
  } catch (error) {
    // silently handle
  }
};

const loadDiscountActivities = async () => {
  try {
    const response = await api.get('/discount/active-with-products');
    if (response.code === 0 && response.data) {
      discountActivities.value = response.data || [];
    }
  } catch (error) {
    // silently handle
  }
};

const goToProduct = (productId) => {
  router.push(`/product/${productId}`);
};

const goToActivity = (activityId) => {
  router.push(`/activity/${activityId}`);
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
};

const formatDiscountTime = (time) => {
  if (!time) return '';
  const end = new Date(time);
  const now = new Date();
  const diff = end - now;
  if (diff < 0) return '已结束';
  const days = Math.floor(diff / 86400000);
  const hours = Math.floor((diff % 86400000) / 3600000);
  if (days > 0) return `距结束 ${days}天${hours}时`;
  const mins = Math.floor((diff % 3600000) / 60000);
  return `距结束 ${hours}时${mins}分`;
};

const prevBanner = () => {
  currentBanner.value = (currentBanner.value - 1 + bannerList.value.length) % bannerList.value.length;
};

const nextBanner = () => {
  currentBanner.value = (currentBanner.value + 1) % bannerList.value.length;
};

const startAutoPlay = () => {
  bannerTimer = setInterval(() => {
    currentBanner.value = (currentBanner.value + 1) % bannerList.value.length;
  }, 5000);
};

const scrollAndNavigate = (path, sectionId) => {
  const section = document.getElementById(sectionId);
  if (section) {
    section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setTimeout(() => {
      router.push(path);
    }, 400);
  } else {
    router.push(path);
  }
};

onMounted(() => {
  // 并行加载三类数据,避免串行等待
  Promise.allSettled([
    loadProducts(),
    loadActivities(),
    loadDiscountActivities()
  ]);
  startAutoPlay();
});

onUnmounted(() => {
  if (bannerTimer) {
    clearInterval(bannerTimer);
  }
});
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
}

.hero-banner {
  position: relative;
  height: 600px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  margin-bottom: var(--spacing-10);
  box-shadow: var(--shadow-2xl);
}

.banner-slider {
  width: 100%;
  height: 100%;
  position: relative;
}

.banner-slide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 1s var(--transition-smooth);
}

.banner-slide.active {
  opacity: 1;
}

.banner-bg {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  filter: brightness(0.7);
}

.banner-bg-fallback {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%) !important;
  filter: none;
}

.banner-preload {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
  z-index: -1;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--gradient-hero);
}

.banner-content {
  position: absolute;
  bottom: 80px;
  left: 60px;
  max-width: 600px;
  z-index: 2;
}

.banner-badge {
  display: inline-block;
  background: var(--gradient-warm);
  color: white;
  padding: var(--spacing-2) var(--spacing-5);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: 600;
  letter-spacing: 0.05em;
  margin-bottom: var(--spacing-5);
  box-shadow: var(--shadow-glow-md);
}

.banner-title {
  font-size: var(--font-size-5xl);
  font-weight: 700;
  color: white;
  margin-bottom: var(--spacing-4);
  text-shadow: 2px 4px 12px rgba(0, 0, 0, 0.3);
  line-height: 1.1;
}

.banner-desc {
  font-size: var(--font-size-lg);
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: var(--spacing-8);
  line-height: 1.6;
  text-shadow: 1px 2px 8px rgba(0, 0, 0, 0.3);
}

.banner-cta {
  background: var(--gradient-brand);
  color: white;
  padding: var(--spacing-4) var(--spacing-10);
  border-radius: var(--radius-full);
  border: none;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;
  letter-spacing: 0.02em;
  transition: all var(--transition-spring);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-3);
  box-shadow: var(--shadow-glow-md);
}

.banner-cta:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-glow-lg);
}

.cta-arrow {
  font-size: var(--font-size-lg);
  transition: transform var(--transition-base);
}

.banner-cta:hover .cta-arrow {
  transform: translateX(4px);
}

.banner-controls {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  z-index: 3;
}

.banner-dot {
  width: var(--spacing-10);
  height: 6px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.4);
  border: none;
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}

.banner-dot.active {
  background: rgba(255, 255, 255, 0.3);
  width: 60px;
}

.dot-progress {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: white;
  border-radius: var(--radius-full);
  animation: progress 5s linear;
}

@keyframes progress {
  from { width: 0; }
  to { width: 100%; }
}

.banner-nav {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 3;
  pointer-events: none;
}

.nav-btn {
  width: var(--spacing-12);
  height: var(--spacing-12);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-size: var(--font-size-xl);
  cursor: pointer;
  transition: all var(--transition-base);
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.quick-entry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-12);
  padding: 0 var(--spacing-5);
}

.entry-item {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6) var(--spacing-4);
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-3);
  position: relative;
  overflow: hidden;
}

.entry-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), var(--nature-green));
  transform: scaleX(0);
  transition: transform var(--transition-base);
}

.entry-item:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--border);
}

.entry-item:hover::before {
  transform: scaleX(1);
}

.entry-icon {
  width: var(--spacing-16);
  height: var(--spacing-16);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--bg-1), var(--bg-2));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-3xl);
  transition: transform var(--transition-base);
}

.entry-icon .el-icon {
  color: var(--text-secondary);
}

.entry-item:hover .entry-icon {
  transform: scale(1.1) rotate(5deg);
}

.entry-text {
  text-align: center;
}

.entry-title {
  display: block;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-1);
}

.entry-desc {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.activity-section,
.product-section,
.discount-section {
  margin-bottom: var(--spacing-16);
  padding: 0 var(--spacing-5);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-8);
}

.section-title-group {
  flex: 1;
}

.section-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-2);
}

.section-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin: 0;
}

.section-more {
  background: transparent;
  color: var(--accent-dark);
  padding: var(--spacing-2) var(--spacing-6);
  border-radius: var(--radius-full);
  border: 2px solid var(--accent);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
}

.section-more:hover {
  background: var(--accent);
  color: white;
  transform: translateX(4px);
}

.more-arrow {
  transition: transform var(--transition-base);
}

.section-more:hover .more-arrow {
  transform: translateX(4px);
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
}

.activity-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  animation: fadeInUp 0.6s ease-out backwards;
}

.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--color-primary-200);
}

.activity-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.activity-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.activity-card:hover .activity-image {
  transform: scale(1.1);
}

.activity-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 2;
}

.activity-badge {
  background: linear-gradient(135deg, var(--warm-orange), var(--warm-orange-dark));
  color: white;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.03em;
  box-shadow: 0 2px 8px rgba(var(--color-orange-500-rgb), 0.4);
}

.activity-info {
  padding: var(--spacing-5);
}

.activity-name {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-4);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.activity-time,
.activity-participants {
  display: flex;
  align-items: center;
  gap: 6px;
}

.time-icon,
.participants-icon {
  font-size: 14px;
}

.discount-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-5);
}

.discount-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}
.discount-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary-600);
}

.discount-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: #fff;
  width: fit-content;
}
.discount-badge.type-1 { background: linear-gradient(135deg, var(--color-warning), var(--color-primary-600)); }
.discount-badge.type-2 { background: linear-gradient(135deg, var(--color-error), var(--color-error-dark)); }
.discount-badge.type-3 { background: linear-gradient(135deg, var(--color-info), var(--color-info-dark)); }

.discount-name {
  font-size: var(--font-size-base);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.discount-desc {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin: 0;
  line-height: 1.5;
}

.discount-products {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--bg-cream);
  border-radius: var(--radius-sm);
  padding: var(--spacing-3);
}
.dp-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs);
}
.dp-name {
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}
.dp-prices {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.dp-original {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  text-decoration: line-through;
}
.dp-discount {
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--color-error);
}

.discount-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-2);
  border-top: 1px solid var(--border-light);
}
.discount-time {
  font-size: var(--font-size-xs);
  color: var(--color-error);
  font-weight: 500;
}
.discount-arrow {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-primary-600);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
}

.product-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  animation: fadeInUp 0.6s ease-out backwards;
}

.product-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--color-primary-200);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.product-card:hover .product-image {
  transform: scale(1.1);
}

.product-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(var(--color-primary-900-rgb), 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.product-card:hover .product-overlay {
  opacity: 1;
}

.view-btn {
  background: white;
  color: var(--text-primary);
  padding: var(--spacing-3) var(--spacing-6);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: 600;
  transform: translateY(20px);
  transition: all var(--transition-base);
}

.product-card:hover .view-btn {
  transform: translateY(0);
}

.product-badges {
  position: absolute;
  top: var(--spacing-3);
  right: var(--spacing-3);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  z-index: 2;
}

.badge {
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.03em;
}

.badge.hot {
  background: linear-gradient(135deg, var(--warm-orange), var(--warm-orange-dark));
  color: white;
  box-shadow: 0 2px 8px rgba(var(--color-orange-500-rgb), 0.4);
}

.badge.low-stock {
  background: linear-gradient(135deg, var(--error), var(--color-error-dark));
  color: white;
  box-shadow: 0 2px 8px rgba(var(--color-error-rgb), 0.4);
}

.product-info {
  padding: var(--spacing-5);
}

.product-name {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-4);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price-symbol {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--warm-orange);
}

.price-value {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--warm-orange);
}

.product-sales {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.sales-icon {
  font-size: 14px;
}

.product-skeleton {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-6);
}

.skeleton-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border-light);
}

.skeleton-image {
  width: 100%;
  height: 240px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-info {
  padding: var(--spacing-5);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line-long {
  width: 80%;
}

.skeleton-line-short {
  width: 50%;
  height: 12px;
}

.skeleton-line-medium {
  width: 60%;
  height: 20px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.no-more-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-4);
  padding: var(--spacing-10) 0 var(--spacing-6);
}

.no-more-line {
  flex: 1;
  max-width: 120px;
  height: 1px;
  background: var(--border-light);
}

.no-more-text {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .activity-grid,
  .product-grid,
  .product-skeleton {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .hero-banner {
    height: 500px;
  }

  .banner-title {
    font-size: 3rem;
  }

  .quick-entry {
    grid-template-columns: repeat(3, 1fr);
  }

  .activity-grid,
  .product-grid,
  .product-skeleton {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-banner {
    height: 400px;
    border-radius: var(--radius-md);
  }

  .banner-content {
    bottom: 60px;
    left: 30px;
    right: 30px;
  }

  .banner-title {
    font-size: 2.5rem;
  }

  .banner-desc {
    font-size: 1rem;
  }

  .banner-nav {
    display: none;
  }

  .quick-entry {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .entry-item {
    padding: var(--spacing-4) var(--spacing-3);
  }

  .entry-icon {
    width: var(--spacing-12);
    height: var(--spacing-12);
    font-size: var(--font-size-xl);
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .section-more {
    width: 100%;
    justify-content: center;
  }

  .activity-grid,
  .product-grid,
  .product-skeleton {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
html {
  scroll-behavior: smooth;
}
</style>

