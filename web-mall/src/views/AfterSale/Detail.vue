<template>
  <div class="detail-container">
    <div class="back-bar" @click="goBack">
      <span class="back-arrow">←</span>
      <span>返回售后列表</span>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!detail" class="empty">售后申请不存在</div>
    <template v-else>
      <div class="status-banner" :class="'banner-' + detail.status">
        <div class="status-icon">{{ statusIcon(detail.status) }}</div>
        <div class="status-info">
          <div class="status-title">{{ statusText(detail.status) }}</div>
          <div class="status-desc">{{ statusDesc(detail.status) }}</div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">售后信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">售后编号</span>
            <span class="value">{{ detail.id }}</span>
          </div>
          <div class="info-item">
            <span class="label">服务类型</span>
            <span class="value">{{ serviceTypeText(detail.serviceType) }}</span>
          </div>
          <div class="info-item">
            <span class="label">申请时间</span>
            <span class="value">{{ formatTime(detail.createdAt) }}</span>
          </div>
          <div class="info-item" v-if="detail.refundAmount">
            <span class="label">退款金额</span>
            <span class="value price">¥{{ detail.refundAmount }}</span>
          </div>
          <div class="info-item" v-if="detail.processedAt">
            <span class="label">处理时间</span>
            <span class="value">{{ formatTime(detail.processedAt) }}</span>
          </div>
          <div class="info-item" v-if="detail.contactPhone">
            <span class="label">联系电话</span>
            <span class="value">{{ detail.contactPhone }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">售后原因</h3>
        <div class="reason-box">{{ detail.reason }}</div>
      </div>

      <div class="section" v-if="detail.images">
        <h3 class="section-title">凭证图片</h3>
        <div class="image-list">
          <img v-for="(img, idx) in parseImages(detail.images)" :key="idx"
               :src="img" class="evidence-img"
               @click="previewImage(img)" />
        </div>
      </div>

      <div class="section" v-if="detail.supplementaryEvidence">
        <h3 class="section-title">补充证据</h3>
        <div class="image-list">
          <img v-for="(img, idx) in parseImages(detail.supplementaryEvidence)" :key="idx"
               :src="img" class="evidence-img"
               @click="previewImage(img)" />
        </div>
      </div>

      <div class="section" v-if="detail.serviceResult">
        <h3 class="section-title">处理结果</h3>
        <div class="result-box">{{ detail.serviceResult }}</div>
      </div>

      <div class="section" v-if="detail.closeReason && detail.status === 3">
        <h3 class="section-title">关闭原因</h3>
        <div class="result-box result-closed">{{ detail.closeReason }}</div>
      </div>

      <div class="section" v-if="detail.returnLogistics">
        <h3 class="section-title">退货物流</h3>
        <div class="logistics-box">
          <div class="logistics-row">
            <span class="label">物流公司：</span>
            <span>{{ detail.returnLogisticsCompany || '未知' }}</span>
          </div>
          <div class="logistics-row">
            <span class="label">物流单号：</span>
            <span>{{ detail.returnLogistics }}</span>
          </div>
        </div>
      </div>

      <div class="section" v-if="detail.status === 1 && !detail.returnLogistics">
        <h3 class="section-title">填写退货物流</h3>
        <div class="logistics-form">
          <input v-model="logisticsCompany" placeholder="物流公司（如：顺丰速运）" class="form-input" />
          <input v-model="logisticsNo" placeholder="物流单号" class="form-input" />
          <button @click="submitLogistics" class="btn-submit" :disabled="!logisticsCompany || !logisticsNo">提交物流信息</button>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">服务记录</h3>
        <div v-if="records.length === 0" class="no-records">暂无记录</div>
        <div v-else class="timeline">
          <div class="timeline-item" v-for="rec in records" :key="rec.id">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-text">{{ rec.operationContent }}</div>
              <div class="timeline-time">{{ formatTime(rec.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="action-bar" v-if="detail.status === 0 || detail.status === 1">
        <button v-if="detail.status === 0" @click="showCancelConfirm" class="btn-cancel-action">取消申请</button>
        <button v-if="detail.status === 0 || detail.status === 1" @click="showEvidenceDialog" class="btn-evidence">补充证据</button>
      </div>
    </template>

    <el-dialog v-model="cancelVisible" title="取消确认" width="320px">
      <p>确定要取消这个售后申请吗？</p>
      <template #footer>
        <el-button @click="cancelVisible = false">再想想</el-button>
        <el-button type="danger" @click="confirmCancel">确定取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evidenceVisible" title="补充证据" width="380px">
      <div class="evidence-upload">
        <textarea v-model="newEvidence" placeholder="请描述补充的证据内容，或输入图片URL（多张用逗号分隔）" class="evidence-textarea" rows="4"></textarea>
      </div>
      <template #footer>
        <el-button @click="evidenceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEvidence" :disabled="!newEvidence.trim()">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/utils/api';
import { useUserStore } from '@/stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const detail = ref(null);
const records = ref([]);
const loading = ref(false);

const cancelVisible = ref(false);
const evidenceVisible = ref(false);
const newEvidence = ref('');
const logisticsCompany = ref('');
const logisticsNo = ref('');

const serviceTypeText = (type) => {
  const map = { 1: '退货退款', 2: '换货', 3: '维修' };
  return map[type] || '其他';
};

const statusText = (status) => {
  const map = { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已关闭' };
  return map[status] || '未知';
};

const statusIcon = (status) => {
  const map = { 0: '⏳', 1: '🔄', 2: '✅', 3: '📋' };
  return map[status] || '❓';
};

const statusDesc = (status) => {
  const map = {
    0: '商家正在等待处理您的售后申请',
    1: '商家已受理，正在为您处理',
    2: '售后申请已处理完毕',
    3: '售后申请已关闭'
  };
  return map[status] || '';
};

const formatTime = (time) => {
  if (!time) return '';
  if (Array.isArray(time)) {
    return `${time[0]}-${String(time[1]).padStart(2, '0')}-${String(time[2]).padStart(2, '0')} ${String(time[3]).padStart(2, '0')}:${String(time[4]).padStart(2, '0')}`;
  }
  return time.replace('T', ' ').substring(0, 16);
};

const parseImages = (images) => {
  if (!images) return [];
  try {
    const parsed = JSON.parse(images);
    return Array.isArray(parsed) ? parsed : [images];
  } catch {
    return images.split(',').map(s => s.trim()).filter(Boolean);
  }
};

const previewImage = (url) => {
  window.open(url, '_blank');
};

const goBack = () => {
  router.push('/aftersale');
};

const loadDetail = async () => {
  const id = route.params.id;
  if (!id) return;

  loading.value = true;
  try {
    const res = await api.getAfterSaleById(id);
    if (res.code === 0) {
      detail.value = res.data;
    } else {
      ElMessage.error(res.message || '加载失败');
    }

    const recRes = await api.getServiceRecords(id);
    if (recRes.code === 0) {
      records.value = recRes.data || [];
    }
  } catch (err) {
    console.error('加载售后详情失败:', err);
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const showCancelConfirm = () => {
  cancelVisible.value = true;
};

const confirmCancel = async () => {
  try {
    const res = await api.cancelAfterSale(detail.value.id, { userId: userStore.userInfo.id });
    if (res.code === 0) {
      ElMessage.success('已取消售后申请');
      cancelVisible.value = false;
      loadDetail();
    } else {
      ElMessage.error(res.message || '取消失败');
    }
  } catch (err) {
    ElMessage.error('取消失败');
  }
};

const showEvidenceDialog = () => {
  newEvidence.value = detail.value.supplementaryEvidence || '';
  evidenceVisible.value = true;
};

const submitEvidence = async () => {
  try {
    const res = await api.addSupplementaryEvidence(detail.value.id, {
      evidence: newEvidence.value.trim(),
      userId: userStore.userInfo.id
    });
    if (res.code === 0) {
      ElMessage.success('证据已提交');
      evidenceVisible.value = false;
      loadDetail();
    } else {
      ElMessage.error(res.message || '提交失败');
    }
  } catch (err) {
    ElMessage.error('提交失败');
  }
};

const submitLogistics = async () => {
  try {
    const res = await api.updateReturnLogistics(detail.value.id, {
      logisticsCompany: logisticsCompany.value.trim(),
      logisticsNo: logisticsNo.value.trim(),
      userId: userStore.userInfo.id
    });
    if (res.code === 0) {
      ElMessage.success('物流信息已提交');
      loadDetail();
    } else {
      ElMessage.error(res.message || '提交失败');
    }
  } catch (err) {
    ElMessage.error('提交失败');
  }
};

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.detail-container { max-width: 700px; margin: 0 auto; padding: 16px; }
.back-bar { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #666; font-size: 14px; margin-bottom: 16px; }
.back-bar:hover { color: #1890ff; }
.back-arrow { font-size: 18px; }

.status-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.banner-0 { background: #fff7e6; }
.banner-1 { background: #e6f7ff; }
.banner-2 { background: #f6ffed; }
.banner-3 { background: #f5f5f5; }

.status-icon { font-size: 32px; }
.status-title { font-size: 18px; font-weight: 600; color: #1a1a1a; }
.status-desc { font-size: 13px; color: #666; margin-top: 4px; }

.section { background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.section-title { font-size: 15px; font-weight: 500; color: #333; margin: 0 0 12px; padding-bottom: 8px; border-bottom: 1px solid #f5f5f5; }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.info-item { display: flex; flex-direction: column; gap: 2px; }
.info-item .label { font-size: 12px; color: #999; }
.info-item .value { font-size: 14px; color: #333; }
.price { color: #f5222d; font-weight: 500; }

.reason-box, .result-box {
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}
.result-closed { color: #999; }

.image-list { display: flex; flex-wrap: wrap; gap: 8px; }
.evidence-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
}
.evidence-img:hover { opacity: 0.8; }

.logistics-box { display: flex; flex-direction: column; gap: 8px; }
.logistics-row { font-size: 14px; color: #333; }
.logistics-row .label { color: #999; }

.logistics-form { display: flex; flex-direction: column; gap: 10px; }
.form-input {
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
}
.form-input:focus { border-color: #1890ff; }

.btn-submit {
  padding: 10px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}
.btn-submit:disabled { background: #d9d9d9; cursor: not-allowed; }
.btn-submit:hover:not(:disabled) { background: #40a9ff; }

.no-records { text-align: center; color: #999; font-size: 13px; padding: 16px; }

.timeline { position: relative; padding-left: 20px; }
.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: #e8e8e8;
}
.timeline-item { position: relative; padding-bottom: 16px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
  position: absolute;
  left: -16px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #1890ff;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #1890ff;
}
.timeline-content { padding-left: 8px; }
.timeline-text { font-size: 14px; color: #333; }
.timeline-time { font-size: 12px; color: #999; margin-top: 2px; }

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 12px 0 24px;
}
.btn-cancel-action {
  padding: 10px 28px;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}
.btn-cancel-action:hover { background: #ff4d4f; color: #fff; }
.btn-evidence {
  padding: 10px 28px;
  border: 1px solid #1890ff;
  color: #1890ff;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}
.btn-evidence:hover { background: #1890ff; color: #fff; }

.evidence-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}
.evidence-textarea:focus { border-color: #1890ff; }

.loading, .empty { text-align: center; padding: 60px 0; color: #999; }

@media (max-width: 768px) {
  .detail-container { padding: 10px; }
  .status-banner { flex-direction: column; text-align: center; gap: 10px; }
  .info-grid { grid-template-columns: 1fr; }
  .timeline { padding-left: 16px; }
  .logistics-form { flex-direction: column; }
  .form-input { width: 100%; box-sizing: border-box; }
}

@media (max-width: 480px) {
  .detail-container { padding: 6px; }
  .status-banner { padding: 14px; }
  .status-icon { font-size: 26px; }
  .status-title { font-size: 16px; }
  .section { padding: 12px; }
  .section-title { font-size: 14px; }
  .info-item .label { font-size: 11px; }
  .info-item .value { font-size: 13px; }
  .evidence-img { width: 60px; height: 60px; }
  .action-bar { flex-direction: column; gap: 8px; }
  .btn-cancel-action, .btn-evidence { width: 100%; text-align: center; }
}
</style>