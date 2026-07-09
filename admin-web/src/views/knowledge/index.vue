<template>
  <div class="knowledge-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card shadow="hover" class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="24"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="openDocDialog()">新增知识文档</el-button>
      <el-button type="success" :icon="Plus" @click="openFaqDialog()">新增FAQ</el-button>
      <el-button type="warning" :icon="MagicStick" :loading="vectorizeAllLoading" @click="handleVectorizeAll">
        批量向量化
      </el-button>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="content-tabs">
      <!-- ==================== 知识文档管理 ==================== -->
      <el-tab-pane label="知识文档" name="documents">
        <div class="filter-bar">
          <el-input v-model="docFilter.category" placeholder="按分类筛选" clearable style="width: 180px" @keyup.enter="loadDocuments" />
          <el-select v-model="docFilter.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="docFilter.page = 1; loadDocuments()">查询</el-button>
        </div>

        <el-table :data="documents" v-loading="docLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.category" size="small">{{ row.category }}</el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <el-tag :type="sourceTypeTag(row.sourceType)" size="small">{{ sourceTypeText(row.sourceType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分块/向量化" width="120">
            <template #default="{ row }">
              <span>{{ row.vectorizedChunkCount || 0 }} / {{ row.chunkCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="MagicStick" @click="handleVectorize(row)">向量化</el-button>
              <el-button size="small" :icon="Edit" @click="openDocDialog(row)">编辑</el-button>
              <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteDoc(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="docFilter.page"
            v-model:page-size="docFilter.size"
            :total="docTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadDocuments"
            @current-change="loadDocuments"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== FAQ 管理 ==================== -->
      <el-tab-pane label="FAQ问答对" name="faqs">
        <div class="filter-bar">
          <el-input v-model="faqFilter.category" placeholder="按分类筛选" clearable style="width: 180px" @keyup.enter="loadFaqs" />
          <el-button type="primary" :icon="Search" @click="faqFilter.page = 1; loadFaqs()">查询</el-button>
        </div>

        <el-table :data="faqs" v-loading="faqLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="answer" label="答案" min-width="260" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.category" size="small">{{ row.category }}</el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="90" sortable />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Edit" @click="openFaqDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteFaq(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="faqFilter.page"
            v-model:page-size="faqFilter.size"
            :total="faqTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadFaqs"
            @current-change="loadFaqs"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 对话历史 ==================== -->
      <el-tab-pane label="对话历史" name="conversations">
        <div class="filter-bar">
          <el-input v-model="convFilter.userId" placeholder="按用户ID筛选" clearable style="width: 180px" @keyup.enter="loadConversations" />
          <el-button type="primary" :icon="Search" @click="loadConversations">查询</el-button>
        </div>

        <el-table :data="conversations" v-loading="convLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="sessionToken" label="会话令牌" min-width="200" show-overflow-tooltip />
          <el-table-column prop="userId" label="用户ID" width="90" />
          <el-table-column label="服务类型" width="110">
            <template #default="{ row }">
              <el-tag :type="serviceTypeTag(row.serviceType)" size="small">{{ serviceTypeText(row.serviceType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="messageCount" label="消息数" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
                {{ row.status === 0 ? '活跃' : '已关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="openMessageDrawer(row)">查看消息</el-button>
              <el-button v-if="row.status === 0" size="small" type="warning" @click="handleCloseConv(row)">关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 文档编辑对话框 ==================== -->
    <el-dialog v-model="docDialogVisible" :title="docForm.id ? '编辑知识文档' : '新增知识文档'" width="720px" destroy-on-close>
      <el-form :model="docForm" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="docForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="docForm.category" placeholder="如：售后政策、配送说明" style="width: 280px" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="docForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="docForm.sourceType" style="width: 200px">
            <el-option label="手动录入" :value="0" />
            <el-option label="文档导入" :value="1" />
            <el-option label="FAQ转化" :value="2" />
            <el-option label="历史对话" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="docForm.content"
            type="textarea"
            :rows="12"
            placeholder="请输入文档内容，系统将自动进行分块与向量化处理"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            文档将按滑动窗口自动分块（默认500字符，重叠100字符），并生成向量索引以支持RAG检索
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="docDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="docSubmitLoading" @click="submitDoc">保存并向量化</el-button>
      </template>
    </el-dialog>

    <!-- ==================== FAQ编辑对话框 ==================== -->
    <el-dialog v-model="faqDialogVisible" :title="faqForm.id ? '编辑FAQ' : '新增FAQ'" width="640px" destroy-on-close>
      <el-form :model="faqForm" label-width="90px">
        <el-form-item label="问题" required>
          <el-input v-model="faqForm.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="faqForm.answer" type="textarea" :rows="6" placeholder="请输入答案" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="faqForm.category" placeholder="如：支付、物流" style="width: 240px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="faqForm.keywords" placeholder="多个关键词用逗号分隔，提升匹配精度" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="faqForm.priority" :min="0" :max="100" />
          <span class="form-tip" style="margin-left: 12px">数值越高，检索时排序越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faqDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="faqSubmitLoading" @click="submitFaq">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 消息详情抽屉 ==================== -->
    <el-drawer v-model="messageDrawerVisible" title="对话消息详情" size="600px" direction="rtl">
      <div v-loading="messageLoading" class="message-list">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
          <div class="message-role">
            <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small">
              {{ msg.role === 'user' ? '用户' : 'AI' }}
            </el-tag>
            <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
            <span v-if="msg.responseTimeMs" class="message-meta">耗时 {{ msg.responseTimeMs }}ms</span>
            <span v-if="msg.retrievalScore" class="message-meta">相似度 {{ Number(msg.retrievalScore).toFixed(3) }}</span>
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div v-if="msg.sources" class="message-sources">
            <el-icon><Link /></el-icon>
            <span>知识来源：{{ msg.sources }}</span>
          </div>
        </div>
        <el-empty v-if="!messageLoading && messages.length === 0" description="暂无消息" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, Refresh, Search, MagicStick, View,
  InfoFilled, Link, Document, ChatDotRound, DataAnalysis, Files
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getDocumentList, createDocument, updateDocument, deleteDocument,
  vectorizeDocument, vectorizeAllDocuments, setDocumentStatus,
  getFaqList, createFaq, updateFaq, deleteFaq,
  getConversations, getConversationMessages, closeConversation,
  getKnowledgeStats,
  type KnowledgeDocument, type KnowledgeFaq, type ConversationSession, type ConversationMessage, type KnowledgeStats
} from '@/api/knowledge'

// ==================== 统计 ====================
const stats = ref<KnowledgeStats>({
  documentCount: 0, faqCount: 0, vectorizedChunkCount: 0,
  vectorStoreChunkCount: 0, vectorStoreFaqCount: 0
})

const statCards = computed(() => [
  { label: '知识文档', value: stats.value.documentCount, icon: Document, color: '#409eff' },
  { label: 'FAQ问答对', value: stats.value.faqCount, icon: ChatDotRound, color: '#67c23a' },
  { label: '已向量化分块', value: stats.value.vectorizedChunkCount, icon: Files, color: '#e6a23c' },
  { label: '内存向量数', value: stats.value.vectorStoreChunkCount + stats.value.vectorStoreFaqCount, icon: DataAnalysis, color: '#f56c6c' }
])

const loadStats = async () => {
  try {
    const data = await getKnowledgeStats()
    stats.value = data as KnowledgeStats
  } catch (e) { /* ignore */ }
}

// ==================== Tab 状态 ====================
const activeTab = ref('documents')

// ==================== 知识文档 ====================
const documents = ref<KnowledgeDocument[]>([])
const docLoading = ref(false)
const docTotal = ref(0)
const docFilter = reactive({ page: 1, size: 10, category: '', status: undefined as number | undefined })

const loadDocuments = async () => {
  docLoading.value = true
  try {
    const data = await getDocumentList(docFilter) as any
    documents.value = data.records || []
    docTotal.value = data.total || 0
  } catch (e) { /* ignore */ } finally {
    docLoading.value = false
  }
}

// 文档对话框
const docDialogVisible = ref(false)
const docSubmitLoading = ref(false)
const docForm = reactive({
  id: undefined as number | undefined,
  title: '', content: '', category: '', tags: '', sourceType: 0
})

const openDocDialog = (row?: KnowledgeDocument) => {
  if (row) {
    docForm.id = row.id
    docForm.title = row.title
    docForm.content = row.content
    docForm.category = row.category || ''
    docForm.tags = row.tags || ''
    docForm.sourceType = row.sourceType ?? 0
  } else {
    docForm.id = undefined
    docForm.title = ''
    docForm.content = ''
    docForm.category = ''
    docForm.tags = ''
    docForm.sourceType = 0
  }
  docDialogVisible.value = true
}

const submitDoc = async () => {
  if (!docForm.title.trim() || !docForm.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  docSubmitLoading.value = true
  try {
    if (docForm.id) {
      await updateDocument(docForm.id, {
        title: docForm.title, content: docForm.content,
        category: docForm.category, tags: docForm.tags
      })
      ElMessage.success('更新成功，已自动重新向量化')
    } else {
      await createDocument({
        title: docForm.title, content: docForm.content,
        sourceType: docForm.sourceType, category: docForm.category, tags: docForm.tags
      })
      ElMessage.success('创建成功，已自动分块并向量化')
    }
    docDialogVisible.value = false
    loadDocuments()
    loadStats()
  } catch (e) { /* ignore */ } finally {
    docSubmitLoading.value = false
  }
}

const handleDeleteDoc = (row: KnowledgeDocument) => {
  ElMessageBox.confirm(`确定删除文档「${row.title}」？此操作将同时删除其所有分块与向量索引。`, '删除确认', {
    type: 'warning'
  }).then(async () => {
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
    loadStats()
  }).catch(() => {})
}

const handleVectorize = async (row: KnowledgeDocument) => {
  try {
    await vectorizeDocument(row.id)
    ElMessage.success(`文档「${row.title}」向量化完成`)
    loadDocuments()
    loadStats()
  } catch (e) { /* ignore */ }
}

const handleToggleStatus = async (row: KnowledgeDocument) => {
  const newStatus = row.status === 1 ? 0 : 1
  await setDocumentStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  loadDocuments()
}

const vectorizeAllLoading = ref(false)
const handleVectorizeAll = async () => {
  vectorizeAllLoading.value = true
  try {
    const data = await vectorizeAllDocuments() as any
    ElMessage.success(`批量向量化完成，共处理 ${data.vectorizedCount} 篇文档`)
    loadDocuments()
    loadStats()
  } catch (e) { /* ignore */ } finally {
    vectorizeAllLoading.value = false
  }
}

// ==================== FAQ ====================
const faqs = ref<KnowledgeFaq[]>([])
const faqLoading = ref(false)
const faqTotal = ref(0)
const faqFilter = reactive({ page: 1, size: 10, category: '' })

const loadFaqs = async () => {
  faqLoading.value = true
  try {
    const data = await getFaqList(faqFilter) as any
    faqs.value = data.records || []
    faqTotal.value = data.total || 0
  } catch (e) { /* ignore */ } finally {
    faqLoading.value = false
  }
}

const faqDialogVisible = ref(false)
const faqSubmitLoading = ref(false)
const faqForm = reactive({
  id: undefined as number | undefined,
  question: '', answer: '', category: '', keywords: '', priority: 0
})

const openFaqDialog = (row?: KnowledgeFaq) => {
  if (row) {
    faqForm.id = row.id
    faqForm.question = row.question
    faqForm.answer = row.answer
    faqForm.category = row.category || ''
    faqForm.keywords = row.keywords || ''
    faqForm.priority = row.priority ?? 0
  } else {
    faqForm.id = undefined
    faqForm.question = ''
    faqForm.answer = ''
    faqForm.category = ''
    faqForm.keywords = ''
    faqForm.priority = 0
  }
  faqDialogVisible.value = true
}

const submitFaq = async () => {
  if (!faqForm.question.trim() || !faqForm.answer.trim()) {
    ElMessage.warning('问题和答案不能为空')
    return
  }
  faqSubmitLoading.value = true
  try {
    const payload = {
      question: faqForm.question, answer: faqForm.answer,
      category: faqForm.category, keywords: faqForm.keywords, priority: faqForm.priority
    }
    if (faqForm.id) {
      await updateFaq(faqForm.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createFaq(payload)
      ElMessage.success('创建成功，已自动向量化')
    }
    faqDialogVisible.value = false
    loadFaqs()
    loadStats()
  } catch (e) { /* ignore */ } finally {
    faqSubmitLoading.value = false
  }
}

const handleDeleteFaq = (row: KnowledgeFaq) => {
  ElMessageBox.confirm(`确定删除该FAQ？`, '删除确认', { type: 'warning' }).then(async () => {
    await deleteFaq(row.id)
    ElMessage.success('删除成功')
    loadFaqs()
    loadStats()
  }).catch(() => {})
}

// ==================== 对话历史 ====================
const conversations = ref<ConversationSession[]>([])
const convLoading = ref(false)
const convFilter = reactive({ userId: '' })

const loadConversations = async () => {
  convLoading.value = true
  try {
    const uid = convFilter.userId ? Number(convFilter.userId) : undefined
    conversations.value = (await getConversations(uid)) as ConversationSession[]
  } catch (e) { /* ignore */ } finally {
    convLoading.value = false
  }
}

const messageDrawerVisible = ref(false)
const messageLoading = ref(false)
const messages = ref<ConversationMessage[]>([])

const openMessageDrawer = async (row: ConversationSession) => {
  messageDrawerVisible.value = true
  messageLoading.value = true
  messages.value = []
  try {
    messages.value = (await getConversationMessages(row.id)) as ConversationMessage[]
  } catch (e) { /* ignore */ } finally {
    messageLoading.value = false
  }
}

const handleCloseConv = (row: ConversationSession) => {
  ElMessageBox.confirm('确定关闭该对话会话？', '关闭确认', { type: 'warning' }).then(async () => {
    await closeConversation(row.id)
    ElMessage.success('会话已关闭')
    loadConversations()
  }).catch(() => {})
}

// ==================== 工具方法 ====================
const formatTime = (t: string) => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'

const sourceTypeText = (t: number) => ({ 0: '手动录入', 1: '文档导入', 2: 'FAQ转化', 3: '历史对话' } as any)[t] || '未知'
const sourceTypeTag = (t: number): any => ({ 0: '', 1: 'success', 2: 'warning', 3: 'info' } as any)[t] || ''
const serviceTypeText = (t: number) => ({ 1: '商品咨询', 2: '订单咨询', 3: '其他' } as any)[t] || '未知'
const serviceTypeTag = (t: number): any => ({ 1: 'success', 2: 'warning', 3: 'info' } as any)[t] || ''

const loadAll = () => {
  loadStats()
  loadDocuments()
  loadFaqs()
  loadConversations()
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped lang="scss">
.knowledge-page {
  padding: 4px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 18px 20px;
  }
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.content-tabs {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: #c0c4cc;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.message-list {
  padding: 0 4px;
}

.message-item {
  margin-bottom: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;

  &.user {
    background: #ecf5ff;
  }
  &.assistant {
    background: #f0f9eb;
  }
}

.message-role {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.message-time {
  font-size: 12px;
}

.message-meta {
  color: #e6a23c;
  font-size: 12px;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-size: 14px;
  color: #303133;
}

.message-sources {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #dcdfe6;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  word-break: break-all;
}
</style>
