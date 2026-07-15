<template>
  <div class="product-form-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <h2 class="form-title">{{ isEdit ? '编辑商品' : '添加商品' }}</h2>
          <el-tag :type="isEdit ? 'warning' : 'success'">{{ isEdit ? '编辑模式' : '新增模式' }}</el-tag>
        </div>
      </template>
      
      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-width="100px"
        size="large"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input 
            v-model="form.name" 
            placeholder="请输入商品名称"
            clearable
            maxlength="100"
            show-word-limit
          >
            <template #prefix>
              <el-icon><ShoppingCart /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="商品价格" prop="price">
          <el-input-number 
            v-model="form.price" 
            :min="0" 
            :max="999999" 
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 100%"
          />
          <span class="form-tip">单位：元，最小值 0</span>
        </el-form-item>
        
        <el-form-item label="商品库存" prop="stock">
          <el-input-number 
            v-model="form.stock" 
            :min="0" 
            :max="999999" 
            controls-position="right"
            style="width: 100%"
          />
          <span class="form-tip">单位：件，最小值 0</span>
        </el-form-item>
        
        <el-form-item label="商品分类" prop="categoryId">
          <el-select 
            v-model="form.categoryId" 
            placeholder="请选择商品分类"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="商品图片" prop="cover">
          <el-input 
            v-model="form.cover" 
            placeholder="请输入商品图片 URL"
            clearable
          >
            <template #prefix>
              <el-icon><Picture /></el-icon>
            </template>
          </el-input>
          <div v-if="form.cover" class="image-preview">
            <el-image 
              :src="form.cover" 
              fit="cover"
              style="width: 100px; height: 100px; border-radius: 4px;"
              :preview-src-list="[form.cover]"
            />
          </div>
        </el-form-item>
        
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入商品描述（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item>
          <el-space style="float: right; margin-top: 20px;">
            <el-button @click="handleCancel">
              <el-icon><Close /></el-icon>
              取消
            </el-button>
            <el-button 
              type="primary" 
              @click="handleSubmit"
              :loading="loading"
            >
              <el-icon v-if="loading"><Loading /></el-icon>
              <el-icon v-else><Check /></el-icon>
              {{ isEdit ? '更新商品' : '添加商品' }}
            </el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useSellerStore } from '@/stores/seller';
import { ElMessage } from 'element-plus';
import {
  ShoppingCart,
  Picture,
  Close,
  Check,
  Loading
} from '@element-plus/icons-vue';
import api from '@/utils/api';

const router = useRouter();
const route = useRoute();
const sellerStore = useSellerStore();
const formRef = ref(null);

const isEdit = ref(false);
const loading = ref(false);
const categories = ref([]);

const form = reactive({
  id: null,
  name: '',
  price: 0,
  stock: 0,
  categoryId: null,
  cover: '',
  description: '',
  sellerId: null
});

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格必须大于 0', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入商品库存', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ]
};

// 加载分类列表
const loadCategories = async () => {
  try {
    // 从后端获取分类列表
    const result = await api.getCategories();
    console.log('获取分类列表:', result);
    if (result && result.length > 0) {
      categories.value = result;
    } else {
      console.warn('分类数据为空，使用空列表');
      categories.value = [];
    }
  } catch (error) {
    console.error('加载分类列表失败:', error);
    categories.value = [];
  }
};

// 加载商品信息（编辑模式）
const loadProduct = async () => {
  const productId = route.params.id;
  if (!productId) return;
  
  try {
    const result = await api.getProductById(productId);
    if (result) {
      const product = result;
      Object.assign(form, {
        id: product.id,
        name: product.name,
        price: product.price,
        stock: product.stock,
        categoryId: product.categoryId,
        cover: product.cover,
        description: product.description,
        sellerId: product.sellerId
      });
      isEdit.value = true;
    }
  } catch (error) {
    console.error('加载商品信息失败:', error);
    ElMessage.error('加载商品信息失败');
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    
    loading.value = true;
    
    // 确保 sellerId 正确
    form.sellerId = sellerStore.user.id;
    
    if (isEdit.value) {
      // 更新商品
      await api.updateProduct(form.id, form);
      ElMessage.success('商品更新成功');
    } else {
      // 添加商品
      await api.createProduct(form);
      ElMessage.success('商品添加成功');
    }
    
    // 跳转到商品列表
    router.push('/products');
  } catch (error) {
    console.error('保存商品失败:', error);
    if (error.message) {
      ElMessage.error(error.message);
    } else {
      ElMessage.error('保存商品失败');
    }
  } finally {
    loading.value = false;
  }
};

// 取消操作
const handleCancel = () => {
  router.back();
};

onMounted(() => {
  loadCategories();
  // 检查是否是编辑模式
  if (route.params.id) {
    loadProduct();
  } else {
    // 新商品，设置默认 sellerId
    form.sellerId = sellerStore.user.id;
  }
});
</script>

<style scoped>
.product-form-container {
  max-width: 800px;
  margin: 0 auto;
}

.form-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-tip {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.image-preview {
  margin-top: 12px;
  padding: 8px;
  background: var(--color-bg-page);
  border-radius: 4px;
  display: inline-block;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--color-text-secondary);
}

:deep(.el-input__wrapper),
:deep(.el-textarea__wrapper) {
  border-radius: 4px;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  border-radius: 0;
}

:deep(.el-button--primary) {
  min-width: 100px;
}

.form-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
  text-align: center;
}

.product-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.form-input,
.form-select,
.form-textarea {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #ff6b6b;
  box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.2);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-primary {
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-primary:hover:not(:disabled) {
  background-color: #ff5252;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
}

.btn-primary:disabled {
  background-color: #ffb3b3;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: var(--color-bg-page);
  color: var(--color-text-secondary);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-secondary:hover {
  background-color: #e4e7ed;
  border-color: #c0c4cc;
}

.loading-spinner {
  animation: spin 1s linear infinite;
  font-size: 14px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .product-form-container {
    max-width: 100%;
  }

  :deep(.el-form) {
    label-position: top;
  }

  :deep(.el-form-item__label) {
    float: none !important;
    display: block !important;
    text-align: left !important;
    padding-bottom: 4px !important;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .product-form-container {
    padding: 0;
  }

  .form-card {
    border-radius: 8px;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }

  .form-title {
    font-size: 18px;
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}
</style>