<template>
  <div class="aftersale-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>售后管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="售后申请" name="aftersale">
          <el-form :inline="true" :model="queryParams" class="search-form">
            <el-form-item label="服务类型">
              <el-select v-model="queryParams.serviceType" placeholder="请选择类型" clearable>
                <el-option label="全部" value="" />
                <el-option label="退货" :value="1" />
                <el-option label="换货" :value="2" />
                <el-option label="维修" :value="3" />
                <el-option label="投诉" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="售后状态">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
                <el-option label="全部" value="" />
                <el-option label="待处理" :value="0" />
                <el-option label="处理中" :value="1" />
                <el-option label="已解决" :value="2" />
                <el-option label="已关闭" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button @click="handleReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="tableData" border stripe v-loading="loading">
            <el-table-column prop="id" label="售后ID" width="80" />
            <el-table-column prop="orderId" label="订单ID" width="100" />
            <el-table-column prop="serviceType" label="服务类型" width="100">
              <template #default="{ row }">
                <el-tag :type="(getServiceTypeType(row.serviceType) as any)">
                  {{ getServiceTypeText(row.serviceType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="售后原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="售后状态" width="100">
              <template #default="{ row }">
                <el-tag :type="(getStatusType(row.status) as any)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="serviceResult" label="处理结果" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleDetail(row)">
                  详情
                </el-button>
                <el-button link type="success" size="small" @click="handleProcess(row)" v-if="row.status === 0 || row.status === 1">
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </el-tab-pane>

        <el-tab-pane label="介入请求" name="intervention">
          <el-form :inline="true" :model="interventionQueryParams" class="search-form">
            <el-form-item label="问题类型">
              <el-select v-model="interventionQueryParams.issueType" placeholder="请选择类型" clearable>
                <el-option label="全部" value="" />
                <el-option label="商品质量问题" value="quality" />
                <el-option label="物流问题" value="logistics" />
                <el-option label="售后服务问题" value="service" />
                <el-option label="商家态度问题" value="attitude" />
                <el-option label="聊天会话纠纷" value="chat_dispute" />
                <el-option label="其他问题" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="interventionQueryParams.status" placeholder="请选择状态" clearable>
                <el-option label="全部" value="" />
                <el-option label="待处理" :value="0" />
                <el-option label="处理中" :value="1" />
                <el-option label="已解决" :value="2" />
                <el-option label="已关闭" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleInterventionQuery">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button @click="handleInterventionReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="interventionTableData" border stripe v-loading="interventionLoading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column label="买家" min-width="90" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.buyerName || ('用户#' + row.userId) }}
              </template>
            </el-table-column>
            <el-table-column label="商家" min-width="90" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.sellerName || row.sellerShopName || ('商家#' + row.sellerId) }}
              </template>
            </el-table-column>
            <el-table-column prop="issueType" label="问题类型" width="110">
              <template #default="{ row }">
                <el-tag>{{ getIssueTypeText(row.issueType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="130" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="(getInterventionStatusType(row.status) as any)">
                  {{ getInterventionStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleInterventionDetail(row)">
                  详情
                </el-button>
                <el-button link type="warning" size="small" @click="openChat(row)">
                  调解
                </el-button>
                <el-button link type="success" size="small" @click="handleInterventionProcess(row)" v-if="row.status === 0 || row.status === 1">
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="interventionQueryParams.page"
            v-model:page-size="interventionQueryParams.pageSize"
            :total="interventionTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleInterventionSizeChange"
            @current-change="handleInterventionPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <el-dialog v-model="detailDialogVisible" title="售后详情" :width="detailDialogWidth">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="售后ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ currentRow.orderId }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">
          <el-tag :type="(getServiceTypeType(currentRow.serviceType ?? 0) as any)">
            {{ getServiceTypeText(currentRow.serviceType ?? 0) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="售后原因">{{ currentRow.reason }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">
          <el-tag :type="(getStatusType(currentRow.status ?? 0) as any)">
            {{ getStatusText(currentRow.status ?? 0) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理结果">{{ currentRow.serviceResult || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 处理售后对话框 -->
    <el-dialog v-model="processDialogVisible" title="处理售后申请" :width="processDialogWidth">
      <div class="after-sale-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="售后ID">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ currentRow.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申请类型">
            <el-tag :type="(getServiceTypeType(currentRow.serviceType ?? 0) as any)">
              {{ getServiceTypeText(currentRow.serviceType ?? 0) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="(getStatusType(currentRow.status ?? 0) as any)">
              {{ getStatusText(currentRow.status ?? 0) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentRow.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="原因描述" :span="2">{{ currentRow.reason }}</el-descriptions-item>
        </el-descriptions>

        <!-- 凭证图片 -->
        <div v-if="currentRow.images && currentRow.images.length > 0" style="margin-top: 16px;">
          <p style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 8px;">用户提供的凭证图片：</p>
          <el-image
            v-for="(img, index) in currentRow.images"
            :key="index"
            :src="img"
            :preview-src-list="currentRow.images"
            fit="cover"
            style="width: 100px; height: 100px; margin-right: 8px; border-radius: 4px;"
          />
        </div>

        <el-divider content-position="left">处理操作</el-divider>

        <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
          <el-form-item label="处理结果" prop="result">
            <el-radio-group v-model="processForm.result">
              <el-radio :label="1">同意</el-radio>
              <el-radio :label="2">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item 
            label="退款金额" 
            prop="refundAmount"
            v-if="processForm.result === 1 && currentRow.serviceType !== undefined"
          >
            <el-input-number 
              v-model="processForm.refundAmount" 
              :min="0" 
              :max="99999"
              :precision="2"
              style="width: 200px;"
            />
            <span style="margin-left: 10px; color: var(--color-text-tertiary);">元（原订单金额：¥{{ currentRow.refundAmount?.toFixed(2) || '0.00' }}）</span>
          </el-form-item>
          
          <el-form-item label="处理说明" prop="remark">
            <el-input
              v-model="processForm.remark"
              type="textarea"
              :rows="4"
              placeholder="请输入处理说明，将通知给买卖双方"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProcessConfirm">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 管理员介入仲裁对话框 -->
    <el-dialog v-model="interventionDialogVisible" title="管理员介入仲裁" :width="interventionDialogWidth" destroy-on-close>
      <div class="intervention-container">
        <!-- 售后基本信息 -->
        <el-alert
          title="该售后申请已升级为管理员介入，请您公正裁决"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />

        <el-descriptions :column="2" border size="small" class="info-section">
          <template #title>
            <div class="section-title">
              <el-icon><InfoFilled /></el-icon> 售后基本信息
            </div>
          </template>
          <el-descriptions-item label="售后ID">{{ currentIntervention.afterSaleId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ currentIntervention.orderNo || (currentIntervention.orderId ? '订单#' + currentIntervention.orderId : '-') }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ currentIntervention.buyerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ currentIntervention.sellerName || (currentIntervention.sellerShopName || '-') }}</el-descriptions-item>
          <el-descriptions-item label="申请类型">
            {{ getServiceTypeText(currentIntervention.serviceType ?? 0) }}
          </el-descriptions-item>
          <el-descriptions-item label="问题类型">
            {{ getIssueTypeText(currentIntervention.issueType) }}
          </el-descriptions-item>
          <el-descriptions-item label="纠纷金额">¥{{ (currentIntervention.amount || currentIntervention.orderAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="介入时间">{{ currentIntervention.interventionAt || currentIntervention.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 问题描述与证据 -->
        <div class="info-section" style="margin-top: 20px;">
          <div class="section-title">
            <el-icon><ChatDotRound /></el-icon> 问题描述
          </div>
          <el-card shadow="never" class="chat-message-card">
            <p class="message-content" style="white-space: pre-wrap;">{{ currentIntervention.description || '无描述' }}</p>
            <div v-if="currentIntervention.evidenceImages" style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
              <el-image
                v-for="(img, imgIdx) in currentIntervention.evidenceImages.split(',')"
                :key="imgIdx"
                :src="img"
                :preview-src-list="currentIntervention.evidenceImages.split(',')"
                fit="cover"
                style="width: 80px; height: 60px; border-radius: 4px;"
              />
            </div>
          </el-card>
        </div>

        <!-- 仲裁表单 -->
        <el-divider content-position="left">仲裁裁决</el-divider>
        
        <el-form :model="interventionForm" :rules="interventionRules" ref="interventionFormRef" label-width="120px">
          <el-form-item label="仲裁结果" prop="decision" required>
            <el-radio-group v-model="interventionForm.decision">
              <el-radio-button :label="1">
                <el-icon><CircleCheck /></el-icon> 支持买家
              </el-radio-button>
              <el-radio-button :label="2">
                <el-icon><CircleClose /></el-icon> 支持商家
              </el-radio-button>
              <el-radio-button :label="3">
                <el-icon><Warning /></el-icon> 部分赔偿
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item 
            label="退款金额(元)" 
            prop="refundAmount"
            v-if="interventionForm.decision === 1 || interventionForm.decision === 3"
          >
            <el-input-number 
              v-model="interventionForm.refundAmount" 
              :min="0" 
              :max="currentIntervention.amount || 99999"
              :precision="2"
              style="width: 250px;"
            />
            <span style="margin-left: 12px; color: var(--color-text-tertiary);">
              纠纷金额上限：¥{{ (currentIntervention.amount || 0).toFixed(2) }}
            </span>
          </el-form-item>

          <el-form-item label="处罚措施" prop="penalty" v-if="interventionForm.decision === 2">
            <el-checkbox-group v-model="interventionForm.penalty">
              <el-checkbox label="warning">警告一次</el-checkbox>
              <el-checkbox label="fine">罚款（可输入金额）</el-checkbox>
              <el-checkbox label="suspend">暂停账号7天</el-checkbox>
              <el-checkbox label="ban">永久封禁</el-checkbox>
            </el-checkbox-group>
            <el-input-number 
              v-if="interventionForm.penalty.includes('fine')"
              v-model="interventionForm.fineAmount"
              :min="0" 
              :max="9999"
              :precision="2"
              placeholder="罚款金额"
              style="width: 150px; margin-left: 12px;"
            />
          </el-form-item>

          <el-form-item label="仲裁说明" prop="reason" required>
            <el-input
              v-model="interventionForm.reason"
              type="textarea"
              :rows="5"
              placeholder="请详细说明您的仲裁理由和依据，此内容将对买卖双方可见..."
            />
          </el-form-item>

          <el-form-item label="补充证据" prop="evidenceImages">
            <el-upload
              action="/api/upload/chat"
              :headers="uploadHeaders"
              list-type="picture-card"
              :auto-upload="true"
              :limit="5"
              :on-success="handleEvidenceUploadSuccess"
              :on-remove="handleEvidenceRemove"
              accept=".jpg,.jpeg,.png,.webp"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">支持 jpg/png/webp 格式，单张不超过 5MB，最多5张</div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="interventionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInterventionConfirm" :loading="interventionLoading">
          提交仲裁结果
        </el-button>
      </template>
    </el-dialog>

    <!-- 实时沟通聊天面板 -->
    <el-dialog
      v-model="chatDialogVisible"
      title="调解沟通"
      :width="chatDialogWidth"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <MediationChatPanel
        :intervention-data="chatInterventionData"
        :buyer-info="chatBuyerInfo"
        :seller-info="chatSellerInfo"
        @message-sent="onMessageSent"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { getAfterSaleList, processAfterSale, submitIntervention, type AfterSale } from '@/api/aftersale'
import { getInterventionList, getInterventionDetail, type Intervention, type InterventionListParams } from '@/api/intervention'
import MediationChatPanel from '@/views/Intervention/components/MediationChatPanel.vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const loading = ref(false)
const tableData = ref<AfterSale[]>([])
const total = ref(0)
const detailDialogVisible = ref(false)
const processDialogVisible = ref(false)
const interventionDialogVisible = ref(false)
const processFormRef = ref<any>(null)
const interventionFormRef = ref<any>(null)
const currentRow = ref<Partial<AfterSale>>({})
const currentIntervention = ref<Partial<Intervention>>({})
const interventionLoading = ref(false)

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => { window.addEventListener('resize', onResize); fetchData(); initUploadHeaders() })
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); disconnectWebSocket() })
const detailDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '700px')
const processDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '600px')
const interventionDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '700px')
const chatDialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '800px')

const activeTab = ref('aftersale')

const interventionTableData = ref<Intervention[]>([])
const interventionTotal = ref(0)
const interventionQueryParams = reactive<InterventionListParams>({
  page: 1,
  pageSize: 20,
  status: '',
  issueType: ''
})

let stompClient: Client | null = null

const chatDialogVisible = ref(false)
const chatInterventionData = ref<any>(null)
const chatBuyerInfo = ref<any>(null)
const chatSellerInfo = ref<any>(null)

const queryParams = reactive({
  page: 1,
  pageSize: 20,
  status: '' as string | number,
  serviceType: '' as string | number
})

const processForm = reactive({
  afterSaleId: 0,
  result: 1 as number,
  remark: '',
  refundAmount: 0
})

const interventionForm = reactive({
  afterSaleId: null as number | null,
  decision: 1 as number,
  refundAmount: 0,
  penalty: [] as string[],
  fineAmount: 0,
  reason: '',
  evidenceImages: [] as string[]
})

const uploadHeaders = ref<Record<string, string>>({})

const initUploadHeaders = () => {
  const token = localStorage.getItem('admin_token') || ''
  uploadHeaders.value = { Authorization: `Bearer ${token}` }
}

const processRules = {
  result: [
    { required: true, message: '请选择处理结果', trigger: 'change' }
  ],
  remark: [
    { required: true, message: '请输入处理说明', trigger: 'blur' }
  ]
}

const interventionRules = {
  decision: [
    { required: true, message: '请选择仲裁结果', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请输入仲裁说明', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAfterSaleList(queryParams) as any
    if (res && res.records) {
      tableData.value = res.records
      total.value = res.total || 0
    } else if (Array.isArray(res)) {
      tableData.value = res
      total.value = res.length
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取售后列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.status = ''
  queryParams.serviceType = ''
  queryParams.page = 1
  fetchData()
}

const handleTabChange = (tabName: string | number) => {
  if (tabName === 'intervention') {
    fetchInterventionData()
    initWebSocket()
  }
}

const fetchInterventionData = async () => {
  interventionLoading.value = true
  try {
    const res = await getInterventionList({
      page: interventionQueryParams.page,
      size: interventionQueryParams.pageSize,
      status: interventionQueryParams.status ? Number(interventionQueryParams.status) : undefined
    }) as any
    if (res && res.records) {
      interventionTableData.value = res.records
      interventionTotal.value = res.total || 0
    } else if (Array.isArray(res)) {
      interventionTableData.value = res
      interventionTotal.value = res.length
    } else {
      interventionTableData.value = []
      interventionTotal.value = 0
    }
  } catch (error) {
    console.error('获取介入列表失败:', error)
  } finally {
    interventionLoading.value = false
  }
}

const handleInterventionQuery = () => {
  interventionQueryParams.page = 1
  fetchInterventionData()
}

const handleInterventionReset = () => {
  interventionQueryParams.status = ''
  interventionQueryParams.issueType = ''
  interventionQueryParams.page = 1
  fetchInterventionData()
}

const handleInterventionSizeChange = (size: number) => {
  interventionQueryParams.pageSize = size
  fetchInterventionData()
}

const handleInterventionPageChange = (page: number) => {
  interventionQueryParams.page = page
  fetchInterventionData()
}

const handleInterventionDetail = async (row: Intervention) => {
  try {
    const detail = await getInterventionDetail(row.id)
    currentIntervention.value = detail || row
  } catch {
    currentIntervention.value = row
  }
  interventionDialogVisible.value = true
}

const openChat = async (row: Intervention) => {
  let detail = row
  try {
    detail = await getInterventionDetail(row.id) || row
  } catch {}
  chatInterventionData.value = {
    id: detail.id,
    sessionId: detail.sessionId || detail.id,
    issueType: detail.issueType,
    status: detail.status
  }
  chatBuyerInfo.value = {
    id: detail.userId,
    username: detail.buyerName || '用户',
    nickname: detail.buyerName || '用户'
  }
  chatSellerInfo.value = {
    id: detail.sellerId,
    username: detail.sellerName || '商家',
    nickname: detail.sellerName || '商家',
    shopName: detail.sellerShopName || '商家店铺'
  }
  chatDialogVisible.value = true
}

const onMessageSent = (message: any) => {
  console.log('消息已发送:', message)
}

const handleInterventionProcess = async (row: Intervention) => {
  try {
    const detail = await getInterventionDetail(row.id)
    currentIntervention.value = detail || row
  } catch {
    currentIntervention.value = row
  }
  interventionForm.afterSaleId = row.id
  interventionForm.decision = 1
  interventionForm.refundAmount = currentIntervention.value.amount || currentIntervention.value.orderAmount || 0
  interventionForm.penalty = []
  interventionForm.fineAmount = 0
  interventionForm.reason = ''
  interventionForm.evidenceImages = []
  interventionDialogVisible.value = true
}

const getIssueTypeText = (type: string | undefined) => {
  const typeMap: Record<string, string> = {
    quality: '商品质量问题',
    logistics: '物流问题',
    service: '售后服务问题',
    attitude: '商家态度问题',
    chat_dispute: '聊天会话纠纷',
    other: '其他问题'
  }
  return typeMap[type || ''] || type || '未知'
}

const getInterventionStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待处理',
    1: '处理中',
    2: '已解决',
    3: '已关闭'
  }
  return statusMap[status] || '未知'
}

const getInterventionStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: 'warning',
    1: '',
    2: 'success',
    3: 'info'
  }
  return typeMap[status] ?? 'info'
}

const initWebSocket = () => {
  if (stompClient?.active) return
  const token = localStorage.getItem('admin_token') || ''
  if (!token) {
    console.log('未登录，跳过WebSocket连接')
    return
  }
  stompClient = new Client({
    webSocketFactory: () => SockJS(import.meta.env.VITE_WS_URL || 'http://localhost:8081/ws-chat'),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    heartbeatIncoming: 15000,
    heartbeatOutgoing: 15000,
    onConnect: () => {
      console.log('Aftersale Admin WebSocket connected')

      stompClient!.subscribe('/topic/admin/intervention/pending', (message: any) => {
        console.log('收到新的介入申请通知:', message.body)
        try {
          const newIntervention = JSON.parse(message.body)
          if (activeTab.value === 'intervention') {
            interventionTableData.value.unshift(newIntervention)
            interventionTotal.value++
          }

          ElNotification({
            title: '新的介入申请',
            message: `用户 ${newIntervention.userId} 提交了新的介入申请`,
            type: 'warning',
            duration: 5000
          })
        } catch (e) {
          console.error('解析介入通知失败:', e)
        }
      })
    },
    onStompError: (frame: any) => {
      const msg = frame.headers['message'] || ''
      if (msg.includes('Invalid') || msg.includes('expired')) {
        console.warn('[WebSocket] Token无效或已过期，跳过连接')
        disconnectWebSocket()
      } else {
        console.error('WebSocket 连接失败:', msg)
      }
    }
  })

  stompClient.activate()
}

const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

const getServiceTypeText = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '退货',
    2: '换货',
    3: '维修',
    4: '投诉'
  }
  return typeMap[type] || '未知'
}

const getServiceTypeType = (type: number) => {
  const typeMap: Record<number, string> = {
    1: 'danger',
    2: 'warning',
    3: 'info',
    4: 'danger'
  }
  return typeMap[type] ?? 'info'
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待处理',
    1: '处理中',
    2: '已解决',
    3: '已关闭'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: 'warning',
    1: '',
    2: 'success',
    3: 'info'
  }
  return typeMap[status] ?? 'info'
}

const handleDetail = (row: AfterSale) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

const handleProcess = (row: AfterSale) => {
  processForm.afterSaleId = row.id
  processForm.result = 1
  processForm.remark = ''
  processForm.refundAmount = 0
  currentRow.value = row
  processDialogVisible.value = true
}

const handleProcessConfirm = async () => {
  if (!processFormRef.value) return
  
  await processFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        await processAfterSale(processForm)
        ElMessage.success('处理成功')
        processDialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('处理失败:', error)
      }
    }
  })
}

const handleEvidenceUploadSuccess = (response: any) => {
  const url = response?.data?.url || response?.url || ''
  if (url) {
    interventionForm.evidenceImages.push(url)
  }
}

const handleEvidenceRemove = (file: any) => {
  const url = file?.response?.data?.url || file?.response?.url || file?.url || ''
  const index = interventionForm.evidenceImages.indexOf(url)
  if (index > -1) {
    interventionForm.evidenceImages.splice(index, 1)
  }
}

const handleInterventionConfirm = async () => {
  if (!interventionFormRef.value) return
  
  await interventionFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        interventionLoading.value = true
        
        await submitIntervention(currentIntervention.value.id!, {
          decision: interventionForm.decision,
          refundAmount: interventionForm.decision === 1 || interventionForm.decision === 3 
            ? interventionForm.refundAmount 
            : 0,
          penalty: interventionForm.penalty.join(','),
          fineAmount: interventionForm.fineAmount || 0,
          reason: interventionForm.reason,
          evidenceImages: interventionForm.evidenceImages.join(',')
        })
        
        ElMessage.success('仲裁结果已提交，系统将通知买卖双方')
        interventionDialogVisible.value = false
        fetchInterventionData()
        fetchData()
      } catch (error) {
        console.error('仲裁提交失败:', error)
        ElMessage.error('提交失败，请重试')
      } finally {
        interventionLoading.value = false
      }
    }
  })
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}
</script>

<style scoped>
.aftersale-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.after-sale-detail {
  max-height: 50vh;
  overflow-y: auto;
}

.intervention-container {
  max-height: 65vh;
  overflow-y: auto;
}

.info-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #E6A23C;
}

.chat-message-card {
  padding: 10px 14px;
  border-left: 3px solid var(--color-primary-500);
  background-color: #fafafa;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.message-content {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.message-images {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .aftersale-container {
    padding: 12px;
  }
  .search-form :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  .search-form :deep(.el-input),
  .search-form :deep(.el-select) {
    width: 100% !important;
  }
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 6px 0;
  }
  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .aftersale-container {
    padding: 8px;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
}
</style>
