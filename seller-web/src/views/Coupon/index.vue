<template>
  <div class="coupon-manage">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="showCreateDialog">创建优惠券</el-button>
    </div>

    <el-table :data="coupons" stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.type === 1 ? '满减券' : '折扣券' }}</template>
      </el-table-column>
      <el-table-column label="门槛" width="100">
        <template #default="{ row }">{{ row.type === 1 ? '¥' + (row.threshold || 0) : '-' }}</template>
      </el-table-column>
      <el-table-column label="优惠" width="100">
        <template #default="{ row }">{{ row.type === 1 ? '减¥' + row.discountValue : row.discountValue + '折' }}</template>
      </el-table-column>
      <el-table-column label="库存" width="100">
        <template #default="{ row }">{{ row.usedCount || 0 }} / {{ row.totalCount || '无限' }}</template>
      </el-table-column>
      <el-table-column prop="perUserLimit" label="限领" width="60" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="editCoupon(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteCoupon(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '创建优惠券'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满减门槛" v-if="form.type === 1">
          <el-input-number v-model="form.threshold" :min="0" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="优惠值" required>
          <el-input-number v-model="form.discountValue" :min="0.01" :step="form.type === 1 ? 1 : 0.1" :precision="form.type === 1 ? 2 : 1" />
          <span style="margin-left:4px">{{ form.type === 1 ? '元' : '折（如9=9折）' }}</span>
        </el-form-item>
        <el-form-item label="发行总量"><el-input-number v-model="form.totalCount" :min="0" /></el-form-item>
        <el-form-item label="每人限领"><el-input-number v-model="form.perUserLimit" :min="1" :max="10" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">进行中</el-radio><el-radio :value="0">已停用</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
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
const coupons = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const form = ref({ name:'', type:1, threshold:0, discountValue:10, totalCount:0, perUserLimit:1, startTime:null, endTime:null, status:1, description:'' });

const fmt = (t) => t ? new Date(t).toLocaleString('zh-CN') : '';

const load = async () => {
  loading.value = true;
  try {
    const res = await api.get(`/coupon/list?sellerId=${sellerStore.user.id}&page=1&size=100`);
    if (res) coupons.value = res.records || [];
  } catch (e) { ElMessage.error('加载失败'); }
  finally { loading.value = false; }
};

const showCreateDialog = () => {
  isEdit.value = false; editId.value = null;
  form.value = { name:'', type:1, threshold:0, discountValue:10, totalCount:0, perUserLimit:1, startTime:null, endTime:null, status:1, description:'' };
  dialogVisible.value = true;
};

const editCoupon = (row) => {
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
    if (isEdit.value) {
      res = await api.put(`/coupon/${editId.value}`, data);
    } else {
      res = await api.post('/coupon', data);
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功');
      dialogVisible.value = false;
      load();
  } catch (e) { ElMessage.error('操作失败'); }
};

const deleteCoupon = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该优惠券吗？', '确认', { type: 'warning' });
    const res = await api.delete(`/coupon/${id}`);
    ElMessage.success('删除成功'); load();
  } catch (e) {}
};

onMounted(load);
</script>

<style scoped>
.coupon-manage { padding: 0; animation: fadeIn 0.4s ease; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
</style>
