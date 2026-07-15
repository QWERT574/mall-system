<template>
  <div class="product-list-container">
    <div class="list-header">
      <h2 class="page-header">商品列表</h2>
      <button class="btn-add" @click="handleAddProduct">
        <span class="btn-icon">➕</span>
        添加商品
      </button>
    </div>
    
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">⟳</div>
      <p>加载商品中...</p>
    </div>
    
    <div v-else-if="products.length === 0" class="empty-state">
      <el-empty description="暂无商品" :image-size="100" />
      <button class="btn-primary" @click="handleAddProduct">添加第一个商品</button>
    </div>
    
    <div v-else class="products-grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <div class="product-image">
          <img :src="product.cover || 'https://via.placeholder.com/200x200?text=商品图片'" :alt="product.name">
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <div class="product-meta">
            <span class="product-price">¥{{ product.price }}</span>
            <span class="product-stock">库存：{{ product.stock }}</span>
          </div>
          <div class="product-actions">
            <button class="btn-edit" @click="handleEditProduct(product)">
              <span>✏️</span> 编辑
            </button>
            <button class="btn-delete" @click="handleDeleteProduct(product.id)">
              <span>🗑️</span> 删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

// 定义 props
const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const handleAddProduct = () => {
  router.push('/product/create');
};

const handleEditProduct = (product) => {
  router.push(`/product/edit/${product.id}`);
};

const handleDeleteProduct = (productId) => {
  if (confirm('确定要删除这个商品吗？')) {
    // 这里后续会调用 API 删除商品
    console.log('删除商品:', productId);
  }
};
</script>

<style scoped>
.product-list-container {
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.btn-add {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-add:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-icon {
  font-size: 16px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.loading-spinner {
  font-size: 48px;
  animation: spin 1s linear infinite;
  color: var(--color-primary-500);
}

.loading-state p {
  margin-top: 16px;
  color: #666;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
  margin-bottom: 24px;
}

.btn-primary {
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: var(--color-bg-page);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.product-price {
  font-size: 20px;
  font-weight: 600;
  color: #ff4757;
}

.product-stock {
  font-size: 14px;
  color: #666;
}

.product-actions {
  display: flex;
  gap: 8px;
}

.btn-edit,
.btn-delete {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.3s;
}

.btn-edit {
  background: #f0f0f0;
  color: #333;
}

.btn-edit:hover {
  background: #e0e0e0;
}

.btn-delete {
  background: #fff0f0;
  color: #ff4757;
}

.btn-delete:hover {
  background: #ffe0e0;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .btn-add {
    width: 100%;
    justify-content: center;
  }

  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
  }

  .product-image {
    height: 150px;
  }
}

@media (max-width: 480px) {
  .product-list-container {
    padding: 12px;
  }

  .products-grid {
    grid-template-columns: 1fr;
  }

  .product-info {
    padding: 12px;
  }

  .product-price {
    font-size: 18px;
  }
}
</style>
