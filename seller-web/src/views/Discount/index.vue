<template>
  <div class="discount-manage">
    <div class="page-header">
      <h2>打折活动管理</h2>
      <el-button type="primary" @click="showCreateDialog">创建活动</el-button>
    </div>

    <el-table :data="activities" stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="活动名称" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeText(row.type) }}</template>
      </el-table-column>
      <el-table-column label="折扣" width="100">
        <template #default="{ row }">
          <template v-if="row.type === 2">{{ row.discountRate }}折</template>
          <template v-else>-</template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="200">
        <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="manageProducts(row)">管理商品</el-button>
          <el-button size="small" @click="editActivity(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteActivity(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑活动' : '创建活动'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">限时折扣</el-radio>
            <el-radio :value="3">秒杀</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="折扣率" v-if="form.type === 2">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="9.9" :step="0.1" :precision="1" />
          <span style="margin-left:4px">折</span>
        </el-form-item>
        <el-form-item label="满减门槛" v-if="form.type === 1">
          <el-input-number v-model="form.threshold" :min="0" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="减额" v-if="form.type === 1">
          <el-input-number v-model="form.reduceAmount" :min="0" :step="1" :precision="2" />
        </el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">进行中</el-radio><el-radio :value="0">已停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="productDialogVisible" title="管理活动商品" width="700px">
      <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select
          v-model="selectedProductId"
          placeholder="选择要添加的商品"
          filterable
          clearable
          style="width:280px"
          @change="onProductSelect"
        >
          <el-option
            v-for="item in availableProducts"
            :key="item.id"
            :label="`${item.name} (¥${item.price})`"
            :value="item.id"
          >
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>{{ item.name }}</span>
              <span style="color:#999;font-size:12px">¥{{ item.price }}</span>
            </div>
          </el-option>
        </el-select>

        <el-input-number
          v-model="newDiscountPrice"
          :min="0"
          :step="0.1"
          :precision="2"
          placeholder="活动价"
          style="width:140px"
          :disabled="!selectedProductId"
        />

        <el-button type="primary" @click="addProduct" :disabled="!selectedProductId || newDiscountPrice <= 0">
          添加商品
        </el-button>
      </div>

      <div v-if="selectedProductInfo" style="margin-bottom:12px;padding:8px 12px;background:var(--color-bg-page);border-radius:4px;font-size:13px;color:#666">
        <span>已选商品：</span>
        <strong style="color:#409eff">{{ selectedProductInfo.name }}</strong>
        <span style="margin-left:12px">原价：¥{{ selectedProductInfo.price }}</span>
      </div>

      <el-table :data="activityProducts" stripe empty-text="暂无活动商品">
        <el-table-column prop="productId" label="商品ID" width="80" />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column label="原价" width="100"><template #default="{ row }">¥{{ row.originalPrice || '-' }}</template></el-table-column>
        <el-table-column label="活动价" width="100"><template #default="{ row }"><span style="color:#e6a23c;font-weight:600">¥{{ row.discountPrice }}</span></template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{ row }"><el-button size="small" type="danger" @click="removeProduct(row.id)">移除</el-button></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useSellerStore } from '@/stores/seller';
import api from '@/utils/api';
import { ElMessage, ElMessageBox } from 'element-plus';

const sellerStore = useSellerStore();
const loading = ref(false);
const activities = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const form = ref({ name:'', type:2, discountRate:9, threshold:0, reduceAmount:0, startTime:null, endTime:null, status:1, description:'' });
const productDialogVisible = ref(false);
const currentActivityId = ref(null);
const activityProducts = ref([]);
const newProductId = ref('');
const newDiscountPrice = ref(0);

const availableProducts = ref([]);
const selectedProductId = ref(null);
const selectedProductInfo = ref(null);

const fmt = (t) => t ? new Date(t).toLocaleString('zh-CN') : '';
const typeText = (t) => ({ 1:'满减', 2:'限时折扣', 3:'秒杀' }[t] || '');

const load = async () => {
  loading.value = true;
  try {
    const res = await api.get('/discount/list?page=1&size=100');
    if (res) activities.value = res.records || [];
  } catch (e) { ElMessage.error('加载失败'); }
  finally { loading.value = false; }
};

const showCreateDialog = () => {
  isEdit.value = false; editId.value = null;
  form.value = { name:'', type:2, discountRate:9, threshold:0, reduceAmount:0, startTime:null, endTime:null, status:1, description:'' };
  dialogVisible.value = true;
};

const editActivity = (row) => {
  isEdit.value = true; editId.value = row.id;
  form.value = { ...row };
  dialogVisible.value = true;
};

const submitForm = async () => {
  try {
    const data = { ...form.value, sellerId: sellerStore.user.id };
    if (data.startTime) data.startTime = new Date(data.startTime).toISOString().replace('T',' ').substring(0,19);
    if (data.endTime) data.endTime = new Date(data.endTime).toISOString().replace('T',' ').substring(0,19);
    let res;
    if (isEdit.value) res = await api.put(`/discount/${editId.value}`, data);
    else res = await api.post('/discount', data);
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功'); dialogVisible.value = false; load();
  } catch (e) {
    // 把后端真实原因显示出来，便于排查
    ElMessage.error(e.message || '操作失败');
  }
};

const deleteActivity = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' });
    const res = await api.delete(`/discount/${id}`);
    ElMessage.success('删除成功'); load();
  } catch (e) {}
};

const manageProducts = async (row) => {
  currentActivityId.value = row.id;
  selectedProductId.value = null;
  selectedProductInfo.value = null;
  newDiscountPrice.value = 0;
  productDialogVisible.value = true;

  try {
    const [productsRes, activityProductsRes] = await Promise.all([
      api.get(`/product/seller/${sellerStore.user.id}/all`),
      api.get(`/discount/${row.id}/products`)
    ]);

    const allProducts = Array.isArray(productsRes) ? productsRes : (productsRes.records || []);
    const existingProductIds = new Set();

    activityProducts.value = activityProductsRes || [];
    activityProductsRes.forEach(p => existingProductIds.add(p.productId));

    availableProducts.value = allProducts.filter(p => !existingProductIds.has(p.id));
  } catch (e) {
    console.error('加载数据失败:', e);
  }
};

const onProductSelect = (productId) => {
  if (!productId) {
    selectedProductInfo.value = null;
    return;
  }

  const product = availableProducts.value.find(p => p.id === productId);
  if (product) {
    selectedProductInfo.value = product;
    const discountRate = 0.85;
    newDiscountPrice.value = Math.round(product.price * discountRate * 100) / 100;
  }
};

const addProduct = async () => {
  if (!selectedProductId.value) { ElMessage.warning('请先选择商品'); return; }
  if (!newDiscountPrice.value || newDiscountPrice.value <= 0) { ElMessage.warning('请输入有效的活动价格'); return; }

  try {
    const res = await api.post(`/discount/${currentActivityId.value}/product?productId=${selectedProductId.value}&discountPrice=${newDiscountPrice.value}`);
    if (res) {
      ElMessage.success('添加成功');
      selectedProductId.value = null;
      selectedProductInfo.value = null;
      newDiscountPrice.value = 0;
      const currentActivity = activities.value.find(a => a.id === currentActivityId.value);
      if (currentActivity) await manageProducts(currentActivity);
    }
  } catch (e) {
    ElMessage.error('添加失败');
  }
};

const removeProduct = async (id) => {
  try {
    await ElMessageBox.confirm('确定移除？', '确认', { type: 'warning' });
    const res = await api.delete(`/discount/product/${id}`);
    ElMessage.success('已移除'); await manageProducts(activities.value.find(a => a.id === currentActivityId.value));
  } catch (e) {}
};

onMounted(load);
</script>

<style scoped>
.discount-manage { padding: 0; animation: fadeIn 0.4s ease; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
</style>
