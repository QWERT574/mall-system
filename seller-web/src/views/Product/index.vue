<template>
  <div class="product-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <button class="add-btn" @click="handleAddProduct">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        添加商品
      </button>
    </div>
    <ProductList 
      :products="products" 
      :loading="loading"
      @add-product="handleAddProduct"
      @edit-product="handleEditProduct"
      @delete-product="handleDeleteProduct"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useSellerStore } from '@/stores/seller';
import api from '@/utils/api';
import ProductList from './List.vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const sellerStore = useSellerStore();

const products = ref([]);
const loading = ref(false);

const loadProducts = async () => {
  loading.value = true;
  try {
    const sellerId = sellerStore.user?.id;
    if (!sellerId) {
      ElMessage.error('请先登录');
      return;
    }
    const response = await api.getProductsBySellerId(sellerId);
    products.value = response.records || [];
  } catch (error) {
    ElMessage.error('加载商品失败，请重试');
  } finally {
    loading.value = false;
  }
};

const handleAddProduct = () => router.push('/product/create');

const handleEditProduct = (product) => router.push(`/product/edit/${product.id}`);

const handleDeleteProduct = async (productId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const response = await api.deleteProduct(productId);
    ElMessage.success('删除成功');
    loadProducts();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试');
    }
  }
};

onMounted(() => loadProducts());
</script>

<style scoped>
.product-page { animation: fadeIn 0.4s ease; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border: none;
  border-radius: var(--radius);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, var(--primary), var(--color-warning));
  color: #fff;
  transition: all var(--transition);
  box-shadow: 0 2px 8px rgba(217,119,6,0.2);
}
.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(217,119,6,0.3);
}
.add-btn svg { width: 16px; height: 16px; }
</style>
