<template>
  <div class="aftersale-page">
    <div class="page-header">
      <h2>售后管理</h2>
      <div class="filter-tabs">
        <button :class="{ active: activeTab === 'all' }" @click="switchTab('all')">全部</button>
        <button :class="{ active: activeTab === '0' }" @click="switchTab('0')">待处理</button>
        <button :class="{ active: activeTab === '1' }" @click="switchTab('1')">处理中</button>
        <button :class="{ active: activeTab === '2' }" @click="switchTab('2')">已解决</button>
        <button :class="{ active: activeTab === '3' }" @click="switchTab('3')">已关闭</button>
      </div>
    </div>

    <el-table :data="list" stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="售后编号" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column label="售后类型" width="100">
        <template #default="{ row }">{{ serviceTypeText(row.serviceType) }}</template>
      </el-table-column>
      <el-table-column label="原因" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason }}</template>
      </el-table-column>
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="退款金额" width="100">
        <template #default="{ row }">
          <span v-if="row.refundAmount" class="price">¥{{ row.refundAmount }}</span>
          <span v-else class="no-price">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 0 || row.status === 1" size="small" type="success" plain @click="showProcess(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > size">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadList" />
    </div>

    <el-dialog v-model="detailVisible" title="售后详情" width="600px">
      <template v-if="detail">
        <div class="detail-grid">
          <div class="detail-item"><span class="label">售后编号</span><span>{{ detail.id }}</span></div>
          <div class="detail-item"><span class="label">用户ID</span><span>{{ detail.userId }}</span></div>
          <div class="detail-item"><span class="label">订单ID</span><span>{{ detail.orderId }}</span></div>
          <div class="detail-item"><span class="label">售后类型</span><span>{{ serviceTypeText(detail.serviceType) }}</span></div>
          <div class="detail-item"><span class="label">申请时间</span><span>{{ formatTime(detail.createdAt) }}</span></div>
          <div class="detail-item" v-if="detail.contactPhone"><span class="label">联系电话</span><span>{{ detail.contactPhone }}</span></div>
          <div class="detail-item" v-if="detail.refundAmount"><span class="label">退款金额</span><span class="price">¥{{ detail.refundAmount }}</span></div>
          <div class="detail-item" v-if="detail.returnLogistics"><span class="label">退货物流</span><span>{{ detail.returnLogisticsCompany || '-' }} {{ detail.returnLogistics }}</span></div>
        </div>
        <div class="detail-section"><h4>售后原因</h4><p>{{ detail.reason }}</p></div>
        <div class="detail-section" v-if="detail.images"><h4>凭证图片</h4><div class="image-list"><img v-for="(img, idx) in parseImages(detail.images)" :key="idx" :src="img" class="thumb" @click="previewImg(img)" /></div></div>
        <div class="detail-section" v-if="detail.supplementaryEvidence"><h4>补充证据</h4><div class="image-list"><img v-for="(img, idx) in parseImages(detail.supplementaryEvidence)" :key="idx" :src="img" class="thumb" @click="previewImg(img)" /></div></div>
        <div class="detail-section" v-if="detail.serviceResult"><h4>处理结果</h4><p>{{ detail.serviceResult }}</p></div>
        <div class="detail-section" v-if="records.length > 0"><h4>服务记录</h4><div class="record-item" v-for="rec in records" :key="rec.id"><span class="record-time">{{ formatTime(rec.createdAt) }}</span><span>{{ rec.operationContent }}</span></div></div>
      </template>
    </el-dialog>

    <el-dialog v-model="processVisible" title="处理售后申请" width="500px">
      <template v-if="processingItem">
        <div class="process-info">
          <p><strong>售后编号：</strong>{{ processingItem.id }}</p>
          <p><strong>售后类型：</strong>{{ serviceTypeText(processingItem.serviceType) }}</p>
          <p><strong>售后原因：</strong>{{ processingItem.reason }}</p>
        </div>
        <el-form label-width="100px">
          <el-form-item label="处理结果"><el-radio-group v-model="processForm.status"><el-radio :value="2">已解决</el-radio><el-radio :value="3">已关闭</el-radio></el-radio-group></el-form-item>
          <el-form-item label="退款金额" v-if="processForm.status === 2"><el-input-number v-model="processForm.refundAmount" :min="0" :step="0.01" :precision="2" /></el-form-item>
          <el-form-item label="处理意见"><el-input v-model="processForm.serviceResult" type="textarea" :rows="3" placeholder="请填写处理意见说明" /></el-form-item>
        </el-form>
      </template>
      <template #footer><el-button @click="processVisible = false">取消</el-button><el-button type="primary" @click="submitProcess" :disabled="!processForm.serviceResult.trim()">确认处理</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';
import { useSellerStore } from '@/stores/seller';

const sellerStore = useSellerStore();
const list = ref([]);
const loading = ref(false);
const activeTab = ref('all');
const current = ref(1);
const size = ref(10);
const total = ref(0);

const detailVisible = ref(false);
const detail = ref(null);
const records = ref([]);

const processVisible = ref(false);
const processingItem = ref(null);
const processForm = reactive({ status: 2, serviceResult: '', refundAmount: 0 });

const serviceTypeText = (t) => ({1:'退货退款',2:'换货',3:'维修'})[t] || '其他';
const statusText = (s) => ({0:'待处理',1:'处理中',2:'已解决',3:'已关闭'})[s] || '未知';
const tagType = (s) => ({0:'warning',1:'primary',2:'success',3:'info'})[s] || '';

const formatTime = (t) => {
  if (!t) return '';
  if (Array.isArray(t)) return `${t[0]}-${String(t[1]).padStart(2,'0')}-${String(t[2]).padStart(2,'0')} ${String(t[3]).padStart(2,'0')}:${String(t[4]).padStart(2,'0')}`;
  return t.replace('T', ' ').substring(0, 16);
};

const parseImages = (images) => {
  if (!images) return [];
  try { const p = JSON.parse(images); return Array.isArray(p) ? p : [images]; }
  catch { return images.split(',').map(s => s.trim()).filter(Boolean); }
};

const previewImg = (url) => window.open(url, '_blank');

const switchTab = (tab) => { activeTab.value = tab; current.value = 1; loadList(); };

const loadList = async () => {
  loading.value = true;
  try {
    const sellerId = sellerStore.user?.id;
    if (!sellerId) {
      ElMessage.warning('请先登录');
      list.value = [];
      total.value = 0;
      return;
    }

    const statusFilter = activeTab.value === 'all' ? null : parseInt(activeTab.value);
    const res = await api.getSellerAfterSalesPage(sellerId, current.value, size.value, statusFilter);
    if (res) { list.value = res.records || []; total.value = res.total || 0; }
    else ElMessage.error('加载失败');
  } catch (err) { ElMessage.error('加载失败'); }
  finally { loading.value = false; }
};

const viewDetail = async (item) => {
  detail.value = item; detailVisible.value = true; records.value = [];
  try { const res = await api.getServiceRecords(item.id); if (res) records.value = res || []; }
  catch (err) {}
};

const showProcess = (item) => {
  processingItem.value = item; processForm.status = 2; processForm.serviceResult = ''; processForm.refundAmount = item.refundAmount || 0; processVisible.value = true;
};

const submitProcess = async () => {
  const sellerId = sellerStore.user?.id;
  if (!sellerId) { ElMessage.warning('请先登录'); return; }
  try {
    const res = await api.processAfterSale(processingItem.value.id, processForm.status, processForm.serviceResult.trim(), parseInt(sellerId), processForm.status === 2 ? processForm.refundAmount : null);
    ElMessage.success('处理成功'); processVisible.value = false; loadList();
  } catch (err) { ElMessage.error('处理失败'); }
};

onMounted(() => loadList());
</script>

<style scoped>
.aftersale-page { animation: fadeIn 0.4s ease; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; }

.filter-tabs { display: flex; gap: 4px; background: var(--border-light); padding: 3px; border-radius: var(--radius); }
.filter-tabs button { padding: 6px 14px; border: none; background: transparent; font-size: 13px; font-weight: 500; color: var(--text-secondary); border-radius: 7px; cursor: pointer; transition: all var(--transition); }
.filter-tabs button:hover { color: var(--text-primary); }
.filter-tabs button.active { background: var(--bg-white); color: var(--primary); font-weight: 600; box-shadow: var(--shadow-sm); }

.price { color: var(--color-error); font-weight: 600; }
.no-price { color: #d1d5db; }

.pagination { margin-top: 20px; display: flex; justify-content: center; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin-bottom: 16px; }
.detail-item { font-size: 13px; display: flex; gap: 6px; }
.detail-item .label { color: var(--text-secondary); }

.detail-section { margin-bottom: 14px; }
.detail-section h4 { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid var(--border-light); }
.detail-section p { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin: 0; }

.image-list { display: flex; flex-wrap: wrap; gap: 6px; }
.thumb { width: 60px; height: 60px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 1px solid var(--border); transition: opacity 0.2s; }
.thumb:hover { opacity: 0.7; }

.record-item { display: flex; gap: 12px; font-size: 13px; padding: 4px 0; }
.record-time { color: var(--text-muted); white-space: nowrap; min-width: 120px; }

.process-info { background: var(--border-light); padding: 12px; border-radius: var(--radius); margin-bottom: 16px; }
.process-info p { margin: 4px 0; font-size: 13px; }
</style>
