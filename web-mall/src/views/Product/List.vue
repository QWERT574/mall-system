<template>
  <div class="product-list-page">
    <div class="page-header">
      <h2>全部商品</h2>
      <p>优质农产品，新鲜直达</p>
    </div>

    <!-- 移动端分类抽屉 -->
    <div class="mobile-category-bar" @click="drawerVisible = true">
      <span class="current-category">{{ currentCategoryName }}</span>
      <el-icon><ArrowRight /></el-icon>
    </div>
    <el-drawer
      v-model="drawerVisible"
      title="商品分类"
      direction="left"
      size="280px"
      class="category-drawer"
    >
      <div class="drawer-category-list">
        <div
          class="drawer-category-item"
          :class="{ active: categoryFilter === '' }"
          @click="selectCategory('')"
        >
          <span class="cat-icon">📋</span>
          <span>全部分类</span>
        </div>
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="drawer-category-item"
          :class="{ active: categoryFilter === cat.id }"
          @click="selectCategory(cat.id)"
        >
          <span class="cat-icon">{{ categoryIcons[cat.id % categoryIcons.length] }}</span>
          <span>{{ cat.name }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- 主体布局 -->
    <div class="product-layout">
      <!-- 侧边分类导航 -->
      <aside class="category-sidebar">
        <div class="sidebar-header">
          <el-icon :size="20"><Grid /></el-icon>
          <span>商品分类</span>
        </div>
        <div class="sidebar-category-list">
          <div
            class="sidebar-category-item"
            :class="{ active: categoryFilter === '' }"
            @click="selectCategory('')"
          >
            <span class="cat-dot all"></span>
            <span class="cat-name">全部分类</span>
          </div>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="sidebar-category-item"
            :class="{ active: categoryFilter === cat.id }"
            @click="selectCategory(cat.id)"
          >
            <span class="cat-dot"></span>
            <span class="cat-icon-small">{{ categoryIcons[cat.id % categoryIcons.length] }}</span>
            <span class="cat-name">{{ cat.name }}</span>
          </div>
        </div>
      </aside>

      <!-- 商品主区域 -->
      <div class="product-main">
        <!-- 搜索筛选栏 -->
        <div class="filter-bar">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>

          <div class="filter-actions">
            <el-select v-model="categoryFilter" placeholder="商品分类" clearable @change="loadProducts">
              <el-option label="全部分类" value="" />
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>

            <el-select v-model="sortBy" placeholder="排序方式" @change="loadProducts">
              <el-option label="默认排序" value="" />
              <el-option label="销量优先" value="sales" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
            </el-select>
          </div>
        </div>

        <!-- 当前分类提示 -->
        <div v-if="categoryFilter" class="category-tag">
          当前分类：
          <el-tag closable @close="selectCategory('')" type="warning">
            {{ currentCategoryName }}
          </el-tag>
        </div>

        <!-- 商品列表 -->
        <div class="products-container">
          <div v-if="loading" class="loading-state">
            <el-icon class="loading-spinner"><Loading /></el-icon>
            <p>加载商品中...</p>
          </div>

          <div v-else-if="products.length === 0" class="empty-state">
            <el-empty description="暂无商品" />
          </div>

          <div v-else class="product-grid">
            <div
              class="product-card"
              v-for="product in products"
              :key="product.id"
              @click="goToDetail(product.id)"
            >
              <div class="product-image-wrapper">
                <img :src="product.cover || '/images/product-default.svg'" :alt="product.name" class="product-image" loading="lazy" decoding="async" @error="handleImageError" />
                <div class="product-tags">
                  <span v-if="product.discountLabel" class="tag-discount">{{ product.discountLabel }}</span>
                  <span v-if="product.sales > 50" class="tag-hot">热销</span>
                </div>
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-desc">{{ product.description }}</p>
                <div class="product-footer">
                  <span class="product-price">{{ product.price }}</span>
                  <span class="product-sold">已售{{ product.sales || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="loadProducts"
            @current-change="loadProducts"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Search, Loading, Grid, ArrowRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const route = useRoute();

const products = ref([]);
const categories = ref([]);
const loading = ref(false);
const discountMap = ref({});
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);
const searchKeyword = ref('');
const categoryFilter = ref('');
const sortBy = ref('');
const drawerVisible = ref(false);

const categoryIcons = ['🌾', '🥬', '🍎', '🥩', '🥛', '🍄', '🌽', '🍠', '🥜', '🍯'];

const currentCategoryName = computed(() => {
  if (!categoryFilter.value) return '全部分类';
  const cat = categories.value.find(c => c.id === categoryFilter.value);
  return cat ? cat.name : '全部分类';
});

const selectCategory = (catId) => {
  categoryFilter.value = catId;
  currentPage.value = 1;
  drawerVisible.value = false;
  loadProducts();
};

const loadDiscountLabels = async () => {
  try {
    const res = await api.get('/discount/active-with-products');
    if (res.code === 0 && res.data) {
      const map = {};
      for (const act of res.data) {
        if (act.products) {
          let label = '';
          if (act.type === 1) label = '满减';
          else if (act.type === 2) label = (act.discountRate || '') + '折';
          else if (act.type === 3) label = '秒杀';
          for (const p of act.products) {
            map[p.productId] = label;
          }
        }
      }
      discountMap.value = map;
    }
  } catch (e) {}
};

const loadCategories = async () => {
  try {
    const response = await api.get('/category/list');
    if (response.code === 0 && response.data) {
      categories.value = response.data;
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
};

const loadProducts = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || '',
      categoryId: categoryFilter.value || undefined,
      sortBy: sortBy.value || undefined
    };

    const response = await api.searchProducts(params);

    if (response.code === 0) {
      products.value = (response.data.records || []).map(p => {
        const d = discountMap.value[p.id];
        return { ...p, discountLabel: d || null };
      });
      total.value = response.data.total || 0;
    } else {
      ElMessage.error(response.message || '加载商品失败');
    }
  } catch (error) {
    console.error('加载商品失败:', error);
    ElMessage.error('加载商品失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadProducts();
};

const goToDetail = (productId) => {
  router.push(`/product/${productId}`);
};

const handleImageError = (e) => {
  if (!e.target.src.includes('product-default.svg')) {
    e.target.src = '/images/product-default.svg';
  }
};

onMounted(() => {
  if (route.query.search) {
    searchKeyword.value = route.query.search;
  }
  loadCategories();
  loadDiscountLabels().then(() => loadProducts());
});
</script>

<style scoped>
.product-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  margin-bottom: 32px;
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, var(--bg-1), var(--bg-2));
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(var(--color-primary-500-rgb), 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(var(--color-green-500-rgb), 0.15) 0%, transparent 50%);
  pointer-events: none;
}

.page-header h2 {
  font-size: var(--font-size-5xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-3);
  position: relative;
  z-index: 1;
}

.page-header p {
  font-size: var(--font-size-md);
  color: var(--text-secondary);
  position: relative;
  z-index: 1;
}

/* 移动端分类条 */
.mobile-category-bar {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  margin-bottom: 16px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.mobile-category-bar:hover {
  border-color: var(--accent);
}

.current-category {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 主布局 */
.product-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 侧边分类导航 */
.category-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  position: sticky;
  top: 88px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 20px;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: white;
  font-size: 16px;
  font-weight: 700;
}

.sidebar-category-list {
  padding: 8px;
}

.sidebar-category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  position: relative;
}

.sidebar-category-item:hover {
  background: var(--bg-1);
  color: var(--accent-dark);
}

.sidebar-category-item.active {
  background: linear-gradient(135deg, rgba(var(--color-primary-500-rgb), 0.12), rgba(var(--color-primary-500-rgb), 0.06));
  color: var(--accent-dark);
  font-weight: 700;
}

.sidebar-category-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--accent), var(--accent-dark));
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--border);
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.sidebar-category-item.active .cat-dot {
  background: var(--accent);
  box-shadow: var(--shadow-glow-sm);
}

.cat-dot.all {
  width: 10px;
  height: 10px;
}

.cat-icon-small {
  font-size: 16px;
  flex-shrink: 0;
}

.cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 商品主区域 */
.product-main {
  flex: 1;
  min-width: 0;
}

/* 搜索筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 20px;
  flex-wrap: wrap;
  padding: 20px 24px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.search-box {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 280px;
}

.search-box .el-input {
  flex: 1;
}

.search-box .el-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-full);
  border: 2px solid var(--border);
  box-shadow: none;
  transition: all var(--transition-base);
}

.search-box .el-input :deep(.el-input__wrapper:focus-within) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-500-rgb), 0.1);
}

.search-box .el-button {
  border-radius: var(--radius-full);
  padding: var(--spacing-3) var(--spacing-7);
  background: var(--gradient-brand);
  border: none;
  font-weight: 600;
  box-shadow: var(--shadow-glow-sm);
  transition: all var(--transition-spring);
}

.search-box .el-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow-md);
}

.filter-actions {
  display: flex;
  gap: 12px;
}

.filter-actions .el-select {
  width: 150px;
}

.filter-actions .el-select :deep(.el-input__wrapper) {
  border-radius: var(--radius-full);
  border: 2px solid var(--border);
  box-shadow: none;
  transition: all var(--transition-base);
}

.filter-actions .el-select :deep(.el-input__wrapper:hover) {
  border-color: var(--accent);
}

/* 分类标签 */
.category-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.products-container {
  min-height: 400px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
}

.loading-spinner {
  font-size: 56px;
  color: var(--accent);
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

.loading-state p {
  font-size: 16px;
  color: var(--text-secondary);
}

.empty-state {
  padding: 80px 20px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.product-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  position: relative;
}

.product-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gradient-brand);
  transform: scaleX(0);
  transition: transform var(--transition-smooth);
  z-index: 2;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--color-primary-200);
}

.product-card:hover::before {
  transform: scaleX(1);
}

.product-image-wrapper {
  width: 100%;
  height: 220px;
  overflow: hidden;
  position: relative;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.product-card:hover .product-image {
  transform: scale(1.08);
}

.product-tags {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 2;
}

.tag-discount {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-error), var(--color-error-dark));
}

.tag-hot {
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-warning), var(--color-primary-600));
}

.product-info {
  padding: 16px 20px 20px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 14px;
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
  font-size: 22px;
  font-weight: 700;
  color: var(--warm-orange);
}

.product-price::before {
  content: '¥';
  font-size: 14px;
  font-weight: 600;
}

.product-sold {
  font-size: 13px;
  color: var(--text-tertiary);
}

.pagination-container {
  margin-top: 48px;
  display: flex;
  justify-content: center;
}

.pagination-container :deep(.el-pagination) {
  gap: 8px;
}

.pagination-container :deep(.el-pagination .el-pager li) {
  border-radius: var(--radius-md);
  min-width: 40px;
  height: 40px;
  line-height: 40px;
  font-weight: 600;
  transition: all var(--transition-base);
}

.pagination-container :deep(.el-pagination .el-pager li:hover) {
  background: var(--bg-1);
}

.pagination-container :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: white;
}

.pagination-container :deep(.el-pagination button) {
  border-radius: var(--radius-md);
  min-width: 40px;
  height: 40px;
  transition: all var(--transition-base);
}

.pagination-container :deep(.el-pagination button:hover) {
  background: var(--bg-1);
}

/* Drawer 分类样式 */
.category-drawer :deep(.el-drawer__header) {
  font-weight: 700;
  font-size: 18px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.drawer-category-list {
  padding: 8px 0;
}

.drawer-category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  border-radius: var(--radius-md);
  margin: 2px 12px;
}

.drawer-category-item:hover {
  background: var(--bg-1);
}

.drawer-category-item.active {
  background: linear-gradient(135deg, rgba(212, 165, 116, 0.15), rgba(212, 165, 116, 0.05));
  color: var(--accent-dark);
  font-weight: 700;
}

.cat-icon {
  font-size: 22px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .category-sidebar {
    display: none;
  }

  .mobile-category-bar {
    display: flex;
  }

  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .page-header h2 {
    font-size: 2.5rem;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .product-list-page {
    padding: 0 16px;
  }

  .page-header {
    padding: 40px 16px;
    margin-bottom: 24px;
  }

  .page-header h2 {
    font-size: 2rem;
  }

  .page-header p {
    font-size: 15px;
  }

  .filter-bar {
    flex-direction: column;
    padding: 16px;
    margin-bottom: 16px;
  }

  .search-box {
    width: 100%;
    min-width: auto;
    flex-direction: column;
  }

  .search-box .el-button {
    width: 100%;
  }

  .filter-actions {
    width: 100%;
    justify-content: space-between;
  }

  .filter-actions .el-select {
    flex: 1;
    width: auto;
  }

  .product-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .product-image-wrapper {
    height: 200px;
  }
}
</style>
